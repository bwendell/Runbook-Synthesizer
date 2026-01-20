package com.oracle.runbook.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.oracle.runbook.api.AlertResource;
import com.oracle.runbook.domain.*;
import com.oracle.runbook.enrichment.ContextEnrichmentService;
import com.oracle.runbook.infrastructure.cloud.local.InMemoryVectorStoreRepository;
import com.oracle.runbook.output.WebhookDispatcher;
import com.oracle.runbook.rag.*;
import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.io.StringReader;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for production wiring of the RAG pipeline.
 *
 * <p>These tests verify that the complete pipeline from alert ingestion to checklist generation
 * works correctly when the app is configured for real mode (stub-mode=false).
 */
@ServerTest
class ProductionWiringIT {

  // Initialize all static test components eagerly
  private static final TestLlmProvider TEST_LLM_PROVIDER = new TestLlmProvider();
  private static final InMemoryVectorStoreRepository TEST_VECTOR_STORE;
  private static final TestEmbeddingService TEST_EMBEDDING_SERVICE = new TestEmbeddingService();
  private static final TestEnrichmentService TEST_ENRICHMENT_SERVICE = new TestEnrichmentService();
  private static final RagPipelineService TEST_PIPELINE;
  private static final WebhookDispatcher TEST_DISPATCHER;

  static {
    // Initialize vector store with test data
    TEST_VECTOR_STORE = new InMemoryVectorStoreRepository();
    TEST_VECTOR_STORE.store(
        new RunbookChunk(
            "chunk-001",
            "runbooks/memory-troubleshooting.md",
            "Memory Investigation",
            "Use 'free -h' to check memory. Monitor with 'top'.",
            List.of("memory"),
            List.of("*"),
            new float[] {0.9f, 0.1f, 0.0f, 0.0f}));

    DefaultChecklistGenerator generator = new DefaultChecklistGenerator(TEST_LLM_PROVIDER);
    DefaultRunbookRetriever retriever =
        new DefaultRunbookRetriever(TEST_EMBEDDING_SERVICE, TEST_VECTOR_STORE);
    TEST_PIPELINE = new RagPipelineService(TEST_ENRICHMENT_SERVICE, retriever, generator);

    // Empty dispatcher for testing
    TEST_DISPATCHER = new WebhookDispatcher(List.of());
  }

  private final Http1Client client;

  ProductionWiringIT(Http1Client client) {
    this.client = client;
  }

  @SetUpRoute
  static void route(HttpRouting.Builder routing) {
    // Create AlertResource in real mode (stubMode=false)
    AlertResource alertResource = new AlertResource(TEST_PIPELINE, TEST_DISPATCHER, false);
    routing.register("/api/v1/alerts", alertResource);
    routing.get("/health", (req, res) -> res.send("OK"));
  }

  // ========== End-to-End Flow Tests ==========

  @Test
  @DisplayName("Should process alert through real pipeline when real mode enabled")
  void shouldProcessAlert_ThroughRealPipeline_WhenRealModeEnabled() {
    String requestBody =
        """
        {
          "title": "High Memory Usage",
          "message": "Memory utilization exceeded 90%",
          "severity": "WARNING"
        }
        """;

    try (Http1ClientResponse response =
        client
            .post("/api/v1/alerts")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit(requestBody)) {

      assertThat(response.status()).isEqualTo(Status.OK_200);

      String body = response.as(String.class);
      JsonObject json = parseJson(body);

      assertThat(json.containsKey("alertId")).isTrue();
      assertThat(json.containsKey("steps")).isTrue();
      assertThat(json.getJsonArray("steps")).isNotEmpty();
    }
  }

  @Test
  @DisplayName("Should return checklist with non-stub LLM provider")
  void shouldReturnChecklist_WithNonStubLlmProvider() {
    String requestBody =
        """
        {
          "title": "CPU Spike",
          "message": "CPU above threshold",
          "severity": "CRITICAL"
        }
        """;

    try (Http1ClientResponse response =
        client
            .post("/api/v1/alerts")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit(requestBody)) {

      assertThat(response.status()).isEqualTo(Status.OK_200);

      String body = response.as(String.class);
      JsonObject json = parseJson(body);

      // Verify LLM provider is test-llm, not "stub"
      assertThat(json.getString("llmProviderUsed")).isEqualTo("test-llm");
    }
  }

  @Test
  @DisplayName("Should include source runbooks in response")
  void shouldIncludeSourceRunbooks_InResponse() {
    String requestBody =
        """
        {
          "title": "Memory Alert",
          "message": "High memory usage detected",
          "severity": "WARNING"
        }
        """;

    try (Http1ClientResponse response =
        client
            .post("/api/v1/alerts")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit(requestBody)) {

      assertThat(response.status()).isEqualTo(Status.OK_200);

      String body = response.as(String.class);
      JsonObject json = parseJson(body);

      assertThat(json.containsKey("sourceRunbooks")).isTrue();
      assertThat(json.getJsonArray("sourceRunbooks")).isNotEmpty();
    }
  }

  @Test
  @DisplayName("Should include LLM provider used in response")
  void shouldIncludeLlmProviderUsed_InResponse() {
    String requestBody =
        """
        {
          "title": "Test Alert",
          "message": "Test message",
          "severity": "INFO"
        }
        """;

    try (Http1ClientResponse response =
        client
            .post("/api/v1/alerts")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit(requestBody)) {

      assertThat(response.status()).isEqualTo(Status.OK_200);

      String body = response.as(String.class);
      JsonObject json = parseJson(body);

      assertThat(json.containsKey("llmProviderUsed")).isTrue();
      assertThat(json.getString("llmProviderUsed")).isNotEmpty();
    }
  }

  // ========== Alert Processing Tests ==========

  @Test
  @DisplayName("Should handle valid alert request with all fields")
  void shouldHandleValidAlertRequest_WithAllFields() {
    String requestBody =
        """
        {
          "title": "High Memory",
          "message": "Memory at 95%",
          "severity": "CRITICAL",
          "sourceService": "cloudwatch",
          "dimensions": {"instanceId": "i-12345"},
          "labels": {"environment": "production"}
        }
        """;

    try (Http1ClientResponse response =
        client
            .post("/api/v1/alerts")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit(requestBody)) {

      assertThat(response.status()).isEqualTo(Status.OK_200);
    }
  }

  @Test
  @DisplayName("Should handle minimal alert request with required fields only")
  void shouldHandleMinimalAlertRequest_WithRequiredFieldsOnly() {
    String requestBody =
        """
        {
          "title": "Minimal Alert",
          "message": "Minimal message",
          "severity": "INFO"
        }
        """;

    try (Http1ClientResponse response =
        client
            .post("/api/v1/alerts")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit(requestBody)) {

      assertThat(response.status()).isEqualTo(Status.OK_200);
    }
  }

  @Test
  @DisplayName("Should reject invalid severity with bad request")
  void shouldRejectInvalidSeverity_WithBadRequest() {
    String requestBody =
        """
        {
          "title": "Test Alert",
          "message": "Test message",
          "severity": "INVALID_SEVERITY"
        }
        """;

    try (Http1ClientResponse response =
        client
            .post("/api/v1/alerts")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit(requestBody)) {

      assertThat(response.status()).isEqualTo(Status.BAD_REQUEST_400);
    }
  }

  @Test
  @DisplayName("Should reject missing title with bad request")
  void shouldRejectMissingTitle_WithBadRequest() {
    String requestBody =
        """
        {
          "message": "Test message",
          "severity": "WARNING"
        }
        """;

    try (Http1ClientResponse response =
        client
            .post("/api/v1/alerts")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit(requestBody)) {

      // Should fail validation - title is required
      assertThat(response.status().code()).isGreaterThanOrEqualTo(400);
    }
  }

  // ========== Configuration Tests ==========

  @Test
  @DisplayName("Should use real mode when stub mode disabled")
  void shouldUseRealMode_WhenStubModeDisabled() {
    String requestBody =
        """
        {
          "title": "Config Test Alert",
          "message": "Testing real mode configuration",
          "severity": "INFO"
        }
        """;

    try (Http1ClientResponse response =
        client
            .post("/api/v1/alerts")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit(requestBody)) {

      assertThat(response.status()).isEqualTo(Status.OK_200);

      String body = response.as(String.class);
      JsonObject json = parseJson(body);

      // Real mode uses test-llm provider, not "stub"
      assertThat(json.getString("llmProviderUsed")).isNotEqualTo("stub");
    }
  }

  // ========== Helper Methods ==========

  private JsonObject parseJson(String body) {
    try (JsonReader reader = Json.createReader(new StringReader(body))) {
      return reader.readObject();
    }
  }

  // ========== Test Implementations ==========

  private static class TestLlmProvider implements LlmProvider {
    private final String response =
        "Memory Troubleshooting Checklist\n\n"
            + "Step 1: Check current memory usage with 'free -h'\n"
            + "Step 2: Identify memory-heavy processes with 'top -o %MEM'\n"
            + "Step 3: Review application logs for OOM errors";

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
      // Return embedding similar to vector store content for matching
      return CompletableFuture.completedFuture(new float[] {0.85f, 0.15f, 0.0f, 0.0f});
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

  private static class TestEnrichmentService implements ContextEnrichmentService {
    @Override
    public CompletableFuture<EnrichedContext> enrich(Alert alert) {
      ResourceMetadata resource =
          new ResourceMetadata(
              "test-instance-001",
              "test-server",
              null,
              "t3.medium",
              "us-west-2a",
              Map.of("environment", "test"),
              Map.of());

      List<MetricSnapshot> metrics =
          List.of(
              new MetricSnapshot("CPUUtilization", "AWS/EC2", 75.0, "%", Instant.now()),
              new MetricSnapshot("MemoryUtilization", "AWS/EC2", 92.5, "%", Instant.now()));

      List<LogEntry> logs =
          List.of(
              new LogEntry(
                  "log-001",
                  Instant.now().minusSeconds(60),
                  "WARNING",
                  "High memory detected",
                  Map.of()));

      return CompletableFuture.completedFuture(
          new EnrichedContext(alert, resource, metrics, logs, Map.of()));
    }
  }
}
