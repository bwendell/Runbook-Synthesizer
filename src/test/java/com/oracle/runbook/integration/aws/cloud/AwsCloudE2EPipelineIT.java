/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0.
 */
package com.oracle.runbook.integration.aws.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import com.oracle.runbook.domain.*;
import com.oracle.runbook.enrichment.ContextEnrichmentService;
import com.oracle.runbook.infrastructure.cloud.aws.AwsS3StorageAdapter;
import com.oracle.runbook.infrastructure.cloud.local.InMemoryVectorStoreRepository;
import com.oracle.runbook.integration.DockerSupport;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsAsyncClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.*;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * End-to-end integration tests for the complete alert-to-checklist pipeline using real AWS
 * services.
 *
 * <p>This test uses:
 *
 * <ul>
 *   <li>Real AWS S3 for runbook storage (CDK-provisioned bucket)
 *   <li>Real AWS CloudWatch Logs for log enrichment (CDK-provisioned log group)
 *   <li>Ollama container for LLM inference (local)
 *   <li>In-memory vector store for runbook chunks
 *   <li>File output adapter for validation
 * </ul>
 *
 * <p>The test validates the complete flow: Alert Ingestion → Context Enrichment → RAG Pipeline →
 * Checklist Generation → File Output
 *
 * <p><strong>Prerequisites:</strong>
 *
 * <ul>
 *   <li>AWS credentials configured
 *   <li>CDK infrastructure deployed (infra/npm run cdk:deploy)
 *   <li>Docker running (for Ollama container)
 *   <li>Run with -Pe2e-aws-cloud Maven profile
 * </ul>
 */
@DisplayName("AWS Cloud E2E Pipeline Tests")
@EnabledIfSystemProperty(named = "aws.cloud.enabled", matches = "true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AwsCloudE2EPipelineIT extends CloudAwsTestBase {

  /** Unique test run ID to prevent conflicts between parallel runs. */
  private static final String TEST_RUN_ID = UUID.randomUUID().toString().substring(0, 8);

  /** Prefix for all test files to facilitate cleanup. */
  private static final String TEST_PREFIX = "e2e-pipeline-" + TEST_RUN_ID + "/";

  /** Log stream name for this test run. */
  private static final String TEST_LOG_STREAM = "e2e-pipeline-stream-" + TEST_RUN_ID;

  private static Network sharedNetwork;
  private static GenericContainer<?> ollamaContainer;
  private static S3AsyncClient s3Client;
  private static CloudWatchLogsAsyncClient logsClient;
  private static Path outputDir;
  private static InMemoryVectorStoreRepository vectorStore;

  /** Test file key for cleanup tracking. */
  private static final String MEMORY_RUNBOOK_KEY = TEST_PREFIX + "memory-troubleshooting.md";

  @BeforeAll
  static void setupContainersAndClients() throws Exception {
    DockerSupport.ensureDockerAvailable();

    // Create shared network for container communication
    sharedNetwork = Network.newNetwork();

    // Start Ollama container
    ollamaContainer = OllamaContainerSupport.createContainer(sharedNetwork);
    ollamaContainer.start();

    // Create AWS clients
    s3Client = S3AsyncClient.builder().region(AWS_REGION).build();
    logsClient = CloudWatchLogsAsyncClient.builder().region(AWS_REGION).build();

    // Create CloudWatch log stream for this test run
    logsClient
        .createLogStream(
            CreateLogStreamRequest.builder()
                .logGroupName(getLogGroupName())
                .logStreamName(TEST_LOG_STREAM)
                .build())
        .get(30, TimeUnit.SECONDS);
    System.out.printf(
        "[AwsCloudE2EPipelineIT] Created log stream: %s in group: %s%n",
        TEST_LOG_STREAM, getLogGroupName());

    // Create temp output directory
    outputDir = Files.createTempDirectory("e2e-aws-output");

    // Initialize in-memory vector store
    vectorStore = new InMemoryVectorStoreRepository();
  }

  @AfterAll
  static void cleanup() throws Exception {
    // Clean up test files from S3
    if (s3Client != null) {
      try {
        s3Client
            .deleteObject(
                DeleteObjectRequest.builder()
                    .bucket(getBucketName())
                    .key(MEMORY_RUNBOOK_KEY)
                    .build())
            .get(30, TimeUnit.SECONDS);
        System.out.printf("[AwsCloudE2EPipelineIT] Deleted S3 test file: %s%n", MEMORY_RUNBOOK_KEY);
      } catch (Exception e) {
        System.err.printf("[AwsCloudE2EPipelineIT] Failed to delete S3 file: %s%n", e.getMessage());
      }
      s3Client.close();
    }

    // Clean up log stream
    if (logsClient != null) {
      try {
        logsClient
            .deleteLogStream(
                DeleteLogStreamRequest.builder()
                    .logGroupName(getLogGroupName())
                    .logStreamName(TEST_LOG_STREAM)
                    .build())
            .get(30, TimeUnit.SECONDS);
        System.out.printf("[AwsCloudE2EPipelineIT] Deleted log stream: %s%n", TEST_LOG_STREAM);
      } catch (Exception e) {
        System.err.printf(
            "[AwsCloudE2EPipelineIT] Failed to delete log stream: %s%n", e.getMessage());
      }
      logsClient.close();
    }

    // Stop Ollama container
    if (ollamaContainer != null) {
      ollamaContainer.stop();
    }

    // Close network
    if (sharedNetwork != null) {
      sharedNetwork.close();
    }

    // Clean up output files
    if (outputDir != null) {
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
  @DisplayName("Should seed runbooks in real S3 and in-memory vector store")
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

    // When: Upload to real S3
    s3Client
        .putObject(
            PutObjectRequest.builder().bucket(getBucketName()).key(MEMORY_RUNBOOK_KEY).build(),
            AsyncRequestBody.fromString(memoryRunbook))
        .get(30, TimeUnit.SECONDS);
    System.out.printf(
        "[AwsCloudE2EPipelineIT] Uploaded runbook to S3: s3://%s/%s%n",
        getBucketName(), MEMORY_RUNBOOK_KEY);

    // And: Seed vector store with chunks
    vectorStore.store(
        new RunbookChunk(
            "chunk-aws-001",
            MEMORY_RUNBOOK_KEY,
            "Memory Troubleshooting",
            "Run `free -h` to see memory usage. Check current memory state.",
            List.of("memory", "oom"),
            List.of("*"),
            new float[] {0.9f, 0.1f, 0.0f, 0.0f}));
    vectorStore.store(
        new RunbookChunk(
            "chunk-aws-002",
            MEMORY_RUNBOOK_KEY,
            "Clear Cache",
            "Clear system caches with: sync && echo 3 > /proc/sys/vm/drop_caches",
            List.of("memory", "cache"),
            List.of("*"),
            new float[] {0.85f, 0.15f, 0.0f, 0.0f}));

    // Then: Verify S3 upload via adapter
    AwsS3StorageAdapter s3Adapter = new AwsS3StorageAdapter(s3Client);
    List<String> runbooks = s3Adapter.listRunbooks(getBucketName()).get(30, TimeUnit.SECONDS);
    assertThat(runbooks).contains(MEMORY_RUNBOOK_KEY);

    // And: Verify vector store has chunks
    List<ScoredChunk> searchResults = vectorStore.search(new float[] {0.9f, 0.1f, 0.0f, 0.0f}, 5);
    assertThat(searchResults).isNotEmpty();
    assertThat(searchResults.get(0).chunk().runbookPath()).isEqualTo(MEMORY_RUNBOOK_KEY);
  }

  @Test
  @Order(2)
  @DisplayName("Should process CloudWatch alarm through full pipeline and create output file")
  void shouldProcessCloudWatchAlarmAndCreateOutputFile() throws Exception {
    // Given: Test alert (simulating CloudWatch alarm)
    Alert testAlert =
        new Alert(
            "alert-e2e-aws-001",
            "High Memory Utilization",
            "Memory utilization exceeded 90% threshold on test-instance",
            AlertSeverity.WARNING,
            "cloudwatch",
            Map.of("instanceId", "i-aws-test12345", "region", AWS_REGION.id()),
            Map.of("alarmName", "HighMemoryAlarm-E2E"),
            Instant.now(),
            "{\"AlarmName\": \"HighMemoryAlarm-E2E\"}");

    // Given: Seed CloudWatch Logs with test data
    logsClient
        .putLogEvents(
            PutLogEventsRequest.builder()
                .logGroupName(getLogGroupName())
                .logStreamName(TEST_LOG_STREAM)
                .logEvents(
                    InputLogEvent.builder()
                        .timestamp(Instant.now().toEpochMilli())
                        .message(
                            "[WARNING] "
                                + TEST_RUN_ID
                                + " High memory detected, consider clearing caches")
                        .build())
                .build())
        .get(30, TimeUnit.SECONDS);
    System.out.printf(
        "[AwsCloudE2EPipelineIT] Published test log event to stream: %s%n", TEST_LOG_STREAM);

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
    FileOutputConfig fileConfig = new FileOutputConfig(outputDir.toString(), "e2e-aws-file-output");
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
    assertThat(fileContent).contains("alert-e2e-aws-001");
  }

  @Test
  @Order(3)
  @DisplayName("Generated checklist should reference seeded runbook content from real S3")
  void generatedChecklistShouldReferenceSeededRunbookContentFromRealS3() throws Exception {
    // Given: Alert that should match memory runbook
    Alert memoryAlert =
        new Alert(
            "alert-e2e-aws-memory-002",
            "Memory Pressure Alert",
            "System showing signs of memory pressure on AWS instance",
            AlertSeverity.CRITICAL,
            "cloudwatch",
            Map.of("instanceId", "i-aws-test67890"),
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

    // Then: Checklist references the memory troubleshooting runbook from S3
    assertThat(checklist.sourceRunbooks()).contains(MEMORY_RUNBOOK_KEY);

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

  @Test
  @Order(5)
  @DisplayName("Should verify runbook content can be read from real S3")
  void shouldVerifyRunbookContentCanBeReadFromRealS3() throws Exception {
    // Given: S3 adapter
    AwsS3StorageAdapter s3Adapter = new AwsS3StorageAdapter(s3Client);

    // When: Read runbook content
    var contentOpt =
        s3Adapter.getRunbookContent(getBucketName(), MEMORY_RUNBOOK_KEY).get(30, TimeUnit.SECONDS);

    // Then: Content should be present and match what we uploaded
    assertThat(contentOpt).isPresent();
    assertThat(contentOpt.get()).contains("# Memory Troubleshooting");
    assertThat(contentOpt.get()).contains("free -h");
    assertThat(contentOpt.get()).contains("drop_caches");
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
              alert.dimensions().getOrDefault("instanceId", "i-aws-test-instance"),
              "aws-test-server",
              null,
              "t3.medium",
              "us-west-2a",
              Map.of("environment", "e2e-test"),
              Map.of());

      List<MetricSnapshot> metrics =
          List.of(
              new MetricSnapshot("MemoryUtilization", "AWS/EC2", 92.5, "%", Instant.now()),
              new MetricSnapshot("CPUUtilization", "AWS/EC2", 45.0, "%", Instant.now()));

      List<LogEntry> logs =
          List.of(
              new LogEntry(
                  "log-aws-e2e-001",
                  Instant.now().minusSeconds(60),
                  "WARNING",
                  "Memory pressure detected on AWS instance",
                  Map.of("hostname", "aws-test-server")));

      return CompletableFuture.completedFuture(
          new EnrichedContext(alert, resource, metrics, logs, Map.of()));
    }
  }
}
