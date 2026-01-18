package com.oracle.runbook.integration.e2e;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.oracle.runbook.domain.*;
import com.oracle.runbook.integration.IntegrationTestBase;
import com.oracle.runbook.output.*;
import com.oracle.runbook.output.adapters.GenericWebhookDestination;
import com.oracle.runbook.rag.*;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.WebServerConfig;
import io.helidon.webserver.testing.junit5.SetUpServer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * End-to-end integration tests for the complete alert-to-checklist flow.
 *
 * <p>Tests verify the full pipeline: Alert Ingestion → Context Enrichment → RAG Pipeline →
 * Checklist Generation → Webhook Dispatch.
 */
class AlertToChecklistIT extends IntegrationTestBase {

  private InMemoryVectorStore vectorStore;
  private TestLlmProvider llmProvider;
  private TestEmbeddingService embeddingService;
  private DefaultChecklistGenerator checklistGenerator;
  private TestRunbookRetriever retriever;

  AlertToChecklistIT(WebServer server) {
    super(server);
  }

  @SetUpServer
  static void setup(WebServerConfig.Builder builder) {
    builder.routing(routing -> routing.get("/health", (req, res) -> res.send("OK")));
  }

  @BeforeEach
  void setUp() {
    resetWireMock();
    vectorStore = new InMemoryVectorStore();
    llmProvider = new TestLlmProvider();
    embeddingService = new TestEmbeddingService();
    checklistGenerator = new DefaultChecklistGenerator(llmProvider);
    retriever = new TestRunbookRetriever(vectorStore, embeddingService);
  }

  @Test
  void fullFlow_AlertToChecklistWithWebhookDispatch() {
    // ===== SETUP MOCKS =====

    // Mock OCI Monitoring API
    wireMockServer.stubFor(
        get(urlPathMatching("/monitoring/.*"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                                                        {"metricData": [{"name": "MemoryUtilization", "value": 92.5}]}
                                                        """)));

    // Mock OCI Logging API
    wireMockServer.stubFor(
        post(urlPathMatching("/logging/.*"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                                                        {"logs": [{"message": "Memory pressure detected", "timestamp": "2026-01-17T00:00:00Z"}]}
                                                        """)));

    // Mock webhook destination
    wireMockServer.stubFor(
        post(urlPathEqualTo("/webhook/oncall"))
            .willReturn(aResponse().withStatus(200).withBody("ok")));

    // Mock LLM response
    llmProvider.setResponse(
        "Memory Troubleshooting Checklist\n\n"
            + "Step 1: Check current memory usage with 'free -h'\n"
            + "Step 2: Identify memory-heavy processes with 'top -o %MEM'\n"
            + "Step 3: Review application logs for OOM errors\n"
            + "Step 4: Consider restarting memory-intensive services");

    // Seed vector store with relevant runbook chunks
    seedRunbookChunks();

    // ===== PHASE 1: ALERT INGESTION =====
    Alert alert = createOciMonitoringAlert();
    assertThat(alert).isNotNull();
    assertThat(alert.sourceService()).isEqualTo("oci-monitoring");

    // ===== PHASE 2: CONTEXT ENRICHMENT =====
    EnrichedContext context = enrichAlert(alert);
    assertThat(context.alert()).isEqualTo(alert);
    assertThat(context.resource()).isNotNull();

    // ===== PHASE 3: RAG RETRIEVAL =====
    List<RetrievedChunk> chunks = retriever.retrieve(context, 5);
    assertThat(chunks).isNotEmpty();

    // ===== PHASE 4: CHECKLIST GENERATION =====
    DynamicChecklist checklist = checklistGenerator.generate(context, chunks);
    assertThat(checklist).isNotNull();
    assertThat(checklist.alertId()).isEqualTo(alert.id());
    assertThat(checklist.steps()).isNotEmpty();
    assertThat(checklist.sourceRunbooks()).isNotEmpty();
    assertThat(checklist.llmProviderUsed()).isEqualTo("test-llm");

    // ===== PHASE 5: WEBHOOK DISPATCH =====
    WebhookConfig webhookConfig =
        WebhookConfig.builder()
            .name("oncall-alerts")
            .type("generic")
            .url(wireMockBaseUrl() + "/webhook/oncall")
            .build();

    WebhookDispatcher dispatcher =
        new WebhookDispatcher(List.of(new GenericWebhookDestination(webhookConfig)));

    List<WebhookResult> results = dispatcher.dispatchSync(checklist);

    // ===== VERIFICATION =====
    assertThat(results).hasSize(1);
    assertThat(results.get(0).isSuccess()).isTrue();

    // Verify webhook was called
    wireMockServer.verify(1, postRequestedFor(urlPathEqualTo("/webhook/oncall")));
  }

  @Test
  void fullFlow_WithMultipleRunbooks_MergesChunksInChecklist() {
    // Given: Mock LLM
    llmProvider.setResponse(
        "Combined Troubleshooting\n\n"
            + "Step 1: From memory runbook - check free memory\n"
            + "Step 2: From linux runbook - check system logs");

    // Given: Multiple runbooks in vector store
    vectorStore.store(
        new RunbookChunk(
            "mem-001",
            "runbooks/memory.md",
            "Memory",
            "Check free -h",
            List.of(),
            List.of(),
            new float[] {0.9f, 0.1f, 0.0f, 0.0f}));
    vectorStore.store(
        new RunbookChunk(
            "linux-001",
            "runbooks/linux.md",
            "Linux",
            "Check journalctl",
            List.of(),
            List.of(),
            new float[] {0.8f, 0.2f, 0.0f, 0.0f}));

    // When: Full flow
    Alert alert = createOciMonitoringAlert();
    EnrichedContext context = enrichAlert(alert);
    List<RetrievedChunk> chunks = retriever.retrieve(context, 5);
    DynamicChecklist checklist = checklistGenerator.generate(context, chunks);

    // Then: Checklist includes both runbooks as sources
    assertThat(checklist.sourceRunbooks())
        .containsExactlyInAnyOrder("runbooks/memory.md", "runbooks/linux.md");
  }

  // ========== Helper Methods ==========

  private void seedRunbookChunks() {
    vectorStore.store(
        new RunbookChunk(
            "chunk-001",
            "runbooks/memory-troubleshooting.md",
            "Memory Investigation",
            "Use 'free -h' to check memory. Monitor with 'top'.",
            List.of("memory"),
            List.of("VM.*"),
            new float[] {0.9f, 0.1f, 0.0f, 0.0f}));

    vectorStore.store(
        new RunbookChunk(
            "chunk-002",
            "runbooks/memory-troubleshooting.md",
            "Memory Cleanup",
            "Clear caches with 'sync; echo 3 > /proc/sys/vm/drop_caches'",
            List.of("memory"),
            List.of("VM.*"),
            new float[] {0.85f, 0.15f, 0.0f, 0.0f}));
  }

  private Alert createOciMonitoringAlert() {
    return new Alert(
        "alert-e2e-001",
        "High Memory Utilization",
        "Memory utilization has exceeded 90% threshold on prod-app-server-01",
        AlertSeverity.WARNING,
        "oci-monitoring",
        Map.of(
            "resourceId", "ocid1.instance.oc1..xyz", "compartmentId", "ocid1.compartment.oc1..abc"),
        Map.of("alarmName", "HighMemoryAlarm"),
        Instant.now(),
        "{}");
  }

  private EnrichedContext enrichAlert(Alert alert) {
    ResourceMetadata resource =
        new ResourceMetadata(
            alert.dimensions().get("resourceId"),
            "prod-app-server-01",
            alert.dimensions().get("compartmentId"),
            "VM.Standard.E4.Flex",
            "AD-1",
            Map.of("environment", "production"),
            Map.of());

    List<MetricSnapshot> metrics =
        List.of(
            new MetricSnapshot("MemoryUtilization", "oci_computeagent", 92.5, "%", Instant.now()),
            new MetricSnapshot("CpuUtilization", "oci_computeagent", 45.2, "%", Instant.now()));

    List<LogEntry> logs =
        List.of(
            new LogEntry(
                "log-001",
                Instant.now().minusSeconds(60),
                "WARNING",
                "Memory pressure detected, swapping active",
                Map.of("hostname", "prod-app-server-01")));

    return new EnrichedContext(alert, resource, metrics, logs, Map.of());
  }

  // ========== Test Implementations ==========

  private static class TestLlmProvider implements LlmProvider {
    private String response = "Step 1: Default step";

    void setResponse(String response) {
      this.response = response;
    }

    @Override
    public String providerId() {
      return "test-llm";
    }

    @Override
    public CompletableFuture<String> generateText(String prompt, GenerationConfig config) {
      return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<float[]> generateEmbedding(String text) {
      return CompletableFuture.completedFuture(new float[] {0.5f, 0.5f, 0.0f, 0.0f});
    }

    @Override
    public CompletableFuture<List<float[]>> generateEmbeddings(List<String> texts) {
      return CompletableFuture.completedFuture(
          texts.stream().map(t -> new float[] {0.5f, 0.5f, 0.0f, 0.0f}).toList());
    }
  }

  private static class TestEmbeddingService implements EmbeddingService {
    @Override
    public CompletableFuture<float[]> embed(String text) {
      int hash = text.hashCode();
      float[] embedding = new float[4];
      for (int i = 0; i < 4; i++) {
        embedding[i] = ((hash >> (i * 8)) & 0xFF) / 255.0f;
      }
      return CompletableFuture.completedFuture(embedding);
    }

    @Override
    public CompletableFuture<List<float[]>> embedBatch(List<String> texts) {
      return CompletableFuture.completedFuture(texts.stream().map(t -> embed(t).join()).toList());
    }

    @Override
    public CompletableFuture<float[]> embedContext(EnrichedContext context) {
      return embed(context.alert().title() + " " + context.alert().message());
    }
  }

  private static class TestRunbookRetriever implements RunbookRetriever {
    private final InMemoryVectorStore vectorStore;
    private final TestEmbeddingService embeddingService;

    TestRunbookRetriever(InMemoryVectorStore vectorStore, TestEmbeddingService embeddingService) {
      this.vectorStore = vectorStore;
      this.embeddingService = embeddingService;
    }

    @Override
    public List<RetrievedChunk> retrieve(EnrichedContext context, int topK) {
      float[] queryEmbedding = embeddingService.embedContext(context).join();
      List<ScoredChunk> scored = vectorStore.search(queryEmbedding, topK);
      return scored.stream()
          .map(
              sc -> new RetrievedChunk(sc.chunk(), sc.similarityScore(), 0.0, sc.similarityScore()))
          .toList();
    }
  }

  private static class InMemoryVectorStore implements VectorStoreRepository {
    private final Map<String, RunbookChunk> chunks = new HashMap<>();

    @Override
    public void store(RunbookChunk chunk) {
      chunks.put(chunk.id(), chunk);
    }

    @Override
    public void storeBatch(List<RunbookChunk> chunkList) {
      chunkList.forEach(this::store);
    }

    @Override
    public List<ScoredChunk> search(float[] queryEmbedding, int topK) {
      List<ScoredChunk> scored = new ArrayList<>();
      for (RunbookChunk chunk : chunks.values()) {
        double score = cosineSimilarity(queryEmbedding, chunk.embedding());
        scored.add(new ScoredChunk(chunk, score));
      }
      scored.sort((a, b) -> Double.compare(b.similarityScore(), a.similarityScore()));
      return scored.subList(0, Math.min(topK, scored.size()));
    }

    @Override
    public void delete(String runbookPath) {
      chunks.entrySet().removeIf(e -> e.getValue().runbookPath().equals(runbookPath));
    }

    private double cosineSimilarity(float[] a, float[] b) {
      double dotProduct = 0.0, normA = 0.0, normB = 0.0;
      for (int i = 0; i < Math.min(a.length, b.length); i++) {
        dotProduct += a[i] * b[i];
        normA += a[i] * a[i];
        normB += b[i] * b[i];
      }
      return (normA == 0 || normB == 0) ? 0.0 : dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
  }
}
