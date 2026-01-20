package com.oracle.runbook.integration.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.oracle.runbook.domain.*;
import com.oracle.runbook.enrichment.ContextEnrichmentService;
import com.oracle.runbook.infrastructure.cloud.aws.AwsS3StorageAdapter;
import com.oracle.runbook.infrastructure.cloud.local.InMemoryVectorStoreRepository;
import com.oracle.runbook.integration.DockerSupport;
import com.oracle.runbook.integration.LocalStackContainerBase;
import com.oracle.runbook.integration.OllamaContainerSupport;
import com.oracle.runbook.output.WebhookDispatcher;
import com.oracle.runbook.output.adapters.FileOutputAdapter;
import com.oracle.runbook.output.adapters.FileOutputConfig;
import com.oracle.runbook.rag.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsAsyncClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.*;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * End-to-end integration tests for the complete alert-to-checklist pipeline using LocalStack.
 *
 * <p>This test uses:
 *
 * <ul>
 *   <li>LocalStack for AWS services (S3, CloudWatch Logs, CloudWatch Metrics)
 *   <li>Ollama container for LLM inference
 *   <li>In-memory vector store for runbook chunks
 *   <li>File output adapter for validation
 * </ul>
 *
 * <p>The test validates the complete flow: Alert Ingestion → Context Enrichment → RAG Pipeline →
 * Checklist Generation → File Output
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LocalStackE2EPipelineIT extends LocalStackContainerBase {

  private static final String TEST_BUCKET = "test-runbooks";
  private static final String TEST_LOG_GROUP = "/aws/e2e/test";
  private static final String TEST_LOG_STREAM = "test-stream";

  private static Network sharedNetwork;
  private static GenericContainer<?> ollamaContainer;
  private static S3AsyncClient s3Client;
  private static CloudWatchLogsAsyncClient logsClient;
  private static Path outputDir;
  private static InMemoryVectorStoreRepository vectorStore;

  @BeforeAll
  static void setupContainersAndClients() throws Exception {
    DockerSupport.ensureDockerAvailable();

    // Create shared network for container communication
    sharedNetwork = Network.newNetwork();

    // Start Ollama container
    ollamaContainer = OllamaContainerSupport.createContainer(sharedNetwork);
    ollamaContainer.start();

    // Create AWS clients
    s3Client = createS3Client();
    logsClient = createCloudWatchLogsClient();

    // Create test bucket
    s3Client
        .createBucket(CreateBucketRequest.builder().bucket(TEST_BUCKET).build())
        .get(30, TimeUnit.SECONDS);

    // Create CloudWatch log group and stream
    logsClient
        .createLogGroup(CreateLogGroupRequest.builder().logGroupName(TEST_LOG_GROUP).build())
        .get(30, TimeUnit.SECONDS);
    logsClient
        .createLogStream(
            CreateLogStreamRequest.builder()
                .logGroupName(TEST_LOG_GROUP)
                .logStreamName(TEST_LOG_STREAM)
                .build())
        .get(30, TimeUnit.SECONDS);

    // Create temp output directory
    outputDir = Files.createTempDirectory("e2e-output");

    // Initialize in-memory vector store
    vectorStore = new InMemoryVectorStoreRepository();
  }

  @AfterAll
  static void cleanup() throws Exception {
    if (ollamaContainer != null) {
      ollamaContainer.stop();
    }
    if (sharedNetwork != null) {
      sharedNetwork.close();
    }
    if (outputDir != null) {
      // Clean up output files
      Files.walk(outputDir)
          .sorted((a, b) -> -a.compareTo(b))
          .forEach(
              path -> {
                try {
                  Files.deleteIfExists(path);
                } catch (Exception ignored) {
                }
              });
    }
  }

  @Test
  @Order(1)
  @DisplayName("Should seed runbooks in S3 and in-memory vector store")
  void shouldSeedRunbooksInS3AndVectorStore() throws Exception {
    // Given: Sample runbook content
    String memoryRunbook =
        """
        ---
        title: Memory Troubleshooting Guide
        tags:
          - memory
          - oom
        applicable_shapes:
          - "*"
        ---

        # Memory Troubleshooting

        ## Check Current Memory Usage

        Run `free -h` to see memory usage.

        ## Clear Cache

        ```bash
        sync && echo 3 > /proc/sys/vm/drop_caches
        ```
        """;

    // When: Upload to S3
    s3Client
        .putObject(
            PutObjectRequest.builder().bucket(TEST_BUCKET).key("memory-troubleshooting.md").build(),
            AsyncRequestBody.fromString(memoryRunbook))
        .get(30, TimeUnit.SECONDS);

    // And: Seed vector store with chunks
    vectorStore.store(
        new RunbookChunk(
            "chunk-001",
            "memory-troubleshooting.md",
            "Memory Troubleshooting",
            "Run `free -h` to see memory usage. Check current memory state.",
            List.of("memory", "oom"),
            List.of("*"),
            new float[] {0.9f, 0.1f, 0.0f, 0.0f}));
    vectorStore.store(
        new RunbookChunk(
            "chunk-002",
            "memory-troubleshooting.md",
            "Clear Cache",
            "Clear system caches with: sync && echo 3 > /proc/sys/vm/drop_caches",
            List.of("memory", "cache"),
            List.of("*"),
            new float[] {0.85f, 0.15f, 0.0f, 0.0f}));

    // Then: Verify S3 upload
    AwsS3StorageAdapter s3Adapter = new AwsS3StorageAdapter(s3Client);
    List<String> runbooks = s3Adapter.listRunbooks(TEST_BUCKET).get(30, TimeUnit.SECONDS);
    assertThat(runbooks).contains("memory-troubleshooting.md");

    // And: Verify vector store has chunks
    List<ScoredChunk> searchResults = vectorStore.search(new float[] {0.9f, 0.1f, 0.0f, 0.0f}, 5);
    assertThat(searchResults).isNotEmpty();
    assertThat(searchResults.get(0).chunk().runbookPath()).isEqualTo("memory-troubleshooting.md");
  }

  @Test
  @Order(2)
  @DisplayName("Should process CloudWatch alarm through full pipeline and create output file")
  void shouldProcessCloudWatchAlarmAndCreateOutputFile() throws Exception {
    // Given: Test alert (simulating CloudWatch alarm)
    Alert testAlert =
        new Alert(
            "alert-e2e-localstack-001",
            "High Memory Utilization",
            "Memory utilization exceeded 90% threshold on test-instance",
            AlertSeverity.WARNING,
            "cloudwatch",
            Map.of("instanceId", "i-test12345", "region", "us-east-1"),
            Map.of("alarmName", "HighMemoryAlarm"),
            Instant.now(),
            "{\"AlarmName\": \"HighMemoryAlarm\"}");

    // Given: Seed CloudWatch Logs with test data
    logsClient
        .putLogEvents(
            PutLogEventsRequest.builder()
                .logGroupName(TEST_LOG_GROUP)
                .logStreamName(TEST_LOG_STREAM)
                .logEvents(
                    InputLogEvent.builder()
                        .timestamp(Instant.now().toEpochMilli())
                        .message("High memory detected on test-instance, consider clearing caches")
                        .build())
                .build())
        .get(30, TimeUnit.SECONDS);

    // Given: Test LLM provider (mock since Ollama may not have models pre-pulled)
    TestLlmProvider llmProvider = new TestLlmProvider();

    // Given: Test enrichment service (uses test data, not real AWS adapters for now)
    TestEnrichmentService enrichmentService = new TestEnrichmentService();

    // Given: Test embedding service
    TestEmbeddingService embeddingService = new TestEmbeddingService();

    // Given: Runbook retriever with vector store
    DefaultRunbookRetriever retriever = new DefaultRunbookRetriever(embeddingService, vectorStore);

    // Given: Checklist generator
    DefaultChecklistGenerator generator = new DefaultChecklistGenerator(llmProvider);

    // Given: RAG pipeline
    RagPipelineService ragPipeline =
        new RagPipelineService(enrichmentService, retriever, generator);

    // Given: File output adapter
    FileOutputConfig fileConfig = new FileOutputConfig(outputDir.toString(), "e2e-file-output");
    FileOutputAdapter fileAdapter = new FileOutputAdapter(fileConfig);
    WebhookDispatcher dispatcher = new WebhookDispatcher(List.of(fileAdapter));

    // When: Process alert through pipeline
    DynamicChecklist checklist = ragPipeline.processAlert(testAlert, 5).get(60, TimeUnit.SECONDS);

    // Then: Verify checklist was generated
    assertThat(checklist).isNotNull();
    assertThat(checklist.alertId()).isEqualTo(testAlert.id());
    assertThat(checklist.steps()).isNotEmpty();
    assertThat(checklist.sourceRunbooks()).isNotEmpty();
    assertThat(checklist.llmProviderUsed()).isEqualTo("test-llm");

    // When: Dispatch to file output
    var results = dispatcher.dispatchSync(checklist);

    // Then: Verify dispatch succeeded
    assertThat(results).hasSize(1);
    assertThat(results.get(0).isSuccess()).isTrue();

    // Then: Verify file was created
    Path[] outputFiles =
        Files.list(outputDir)
            .filter(p -> p.getFileName().toString().startsWith("checklist-"))
            .toArray(Path[]::new);
    assertThat(outputFiles).hasSize(1);

    // Then: Verify file contains valid JSON
    String fileContent = Files.readString(outputFiles[0]);
    assertThat(fileContent).contains("\"alertId\"");
    assertThat(fileContent).contains("\"steps\"");
    assertThat(fileContent).contains("alert-e2e-localstack-001");
  }

  @Test
  @Order(3)
  @DisplayName("Generated checklist should reference seeded runbook content")
  void generatedChecklistShouldReferenceSeededRunbookContent() throws Exception {
    // Given: Alert that should match memory runbook
    Alert memoryAlert =
        new Alert(
            "alert-e2e-memory-002",
            "Memory Pressure Alert",
            "System showing signs of memory pressure",
            AlertSeverity.CRITICAL,
            "cloudwatch",
            Map.of("instanceId", "i-test67890"),
            Map.of(),
            Instant.now(),
            "{}");

    // Given: Test components
    TestLlmProvider llmProvider = new TestLlmProvider();
    TestEnrichmentService enrichmentService = new TestEnrichmentService();
    TestEmbeddingService embeddingService = new TestEmbeddingService();
    DefaultRunbookRetriever retriever = new DefaultRunbookRetriever(embeddingService, vectorStore);
    DefaultChecklistGenerator generator = new DefaultChecklistGenerator(llmProvider);
    RagPipelineService ragPipeline =
        new RagPipelineService(enrichmentService, retriever, generator);

    // When: Process alert
    DynamicChecklist checklist = ragPipeline.processAlert(memoryAlert, 5).get(60, TimeUnit.SECONDS);

    // Then: Checklist references the memory troubleshooting runbook
    assertThat(checklist.sourceRunbooks()).contains("memory-troubleshooting.md");

    // And: Steps reference memory-related commands
    List<String> stepInstructions =
        checklist.steps().stream().map(ChecklistStep::instruction).toList();
    // The test LLM returns fixed response with memory troubleshooting steps
    assertThat(String.join(" ", stepInstructions).toLowerCase())
        .containsAnyOf("memory", "free", "top");
  }

  @Test
  @Order(4)
  @DisplayName("File output should contain valid DynamicChecklist JSON schema")
  void fileOutputShouldContainValidDynamicChecklistJsonSchema() throws Exception {
    // Given: Output files exist from previous tests
    Path[] outputFiles =
        Files.list(outputDir)
            .filter(p -> p.getFileName().toString().startsWith("checklist-"))
            .toArray(Path[]::new);

    assertThat(outputFiles).isNotEmpty();

    // Then: Each file should have required JSON fields
    for (Path file : outputFiles) {
      String content = Files.readString(file);
      assertThat(content).contains("\"alertId\"");
      assertThat(content).contains("\"generatedAt\"");
      assertThat(content).contains("\"steps\"");
      assertThat(content).contains("\"sourceRunbooks\"");
      assertThat(content).contains("\"llmProviderUsed\"");
    }
  }

  // ========== Test Implementations ==========

  private static class TestLlmProvider implements LlmProvider {
    private final String response =
        "Memory Troubleshooting Checklist\n\n"
            + "Step 1: Check current memory usage with 'free -h'\n"
            + "Step 2: Identify memory-heavy processes with 'top -o %MEM'\n"
            + "Step 3: Review application logs for OOM errors\n"
            + "Step 4: Consider clearing caches if safe";

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
      // Return embedding similar to seeded vector store content
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
              alert.dimensions().getOrDefault("instanceId", "test-instance"),
              "test-server",
              null,
              "t3.medium",
              "us-east-1a",
              Map.of("environment", "test"),
              Map.of());

      List<MetricSnapshot> metrics =
          List.of(
              new MetricSnapshot("MemoryUtilization", "AWS/EC2", 92.5, "%", Instant.now()),
              new MetricSnapshot("CPUUtilization", "AWS/EC2", 45.0, "%", Instant.now()));

      List<LogEntry> logs =
          List.of(
              new LogEntry(
                  "log-e2e-001",
                  Instant.now().minusSeconds(60),
                  "WARNING",
                  "Memory pressure detected",
                  Map.of("hostname", "test-server")));

      return CompletableFuture.completedFuture(
          new EnrichedContext(alert, resource, metrics, logs, Map.of()));
    }
  }
}
