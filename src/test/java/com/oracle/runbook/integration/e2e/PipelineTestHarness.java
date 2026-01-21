package com.oracle.runbook.integration.e2e;

import com.oracle.runbook.domain.*;
import com.oracle.runbook.enrichment.ContextEnrichmentService;
import com.oracle.runbook.infrastructure.cloud.CloudStorageAdapter;
import com.oracle.runbook.infrastructure.cloud.aws.AwsS3StorageAdapter;
import com.oracle.runbook.infrastructure.cloud.local.InMemoryVectorStoreRepository;
import com.oracle.runbook.output.WebhookDestination;
import com.oracle.runbook.output.WebhookDispatcher;
import com.oracle.runbook.output.adapters.FileOutputAdapter;
import com.oracle.runbook.output.adapters.FileOutputConfig;
import com.oracle.runbook.rag.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import org.testcontainers.containers.localstack.LocalStackContainer;
import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * Test harness for full pipeline E2E integration tests.
 *
 * <p>Provides factory methods for configuring pipeline components and executing the complete
 * alert-to-checklist flow. Supports both LocalStack and real AWS modes.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * PipelineTestHarness harness = PipelineTestHarness.localStack(localStackContainer)
 *     .withOutputDirectory(tempDir)
 *     .build();
 *
 * harness.seedRunbooks("sample-runbooks/memory-troubleshooting.md");
 * DynamicChecklist result = harness.processAlert(testAlert);
 * Path outputFile = harness.getOutputFile(testAlert.id());
 * }</pre>
 */
public final class PipelineTestHarness {

  private final InMemoryVectorStoreRepository vectorStore;
  private final EmbeddingService embeddingService;
  private final LlmProvider llmProvider;
  private final ContextEnrichmentService enrichmentService;
  private final Path outputDirectory;
  private final List<WebhookDestination> destinations;
  private final long timeoutSeconds;
  private final CloudStorageAdapter storageAdapter;
  private final RunbookIngestionService ingestionService;

  private PipelineTestHarness(Builder builder) {
    this.vectorStore = builder.vectorStore;
    this.embeddingService = builder.embeddingService;
    this.llmProvider = builder.llmProvider;
    this.enrichmentService = builder.enrichmentService;
    this.outputDirectory = builder.outputDirectory;
    this.destinations = List.copyOf(builder.destinations);
    this.timeoutSeconds = builder.timeoutSeconds;
    this.storageAdapter = builder.storageAdapter;
    // Build ingestion service if storage adapter is available
    if (builder.storageAdapter != null) {
      RunbookChunker chunker = new RunbookChunker();
      this.ingestionService =
          new RunbookIngestionService(
              builder.storageAdapter, chunker, builder.embeddingService, builder.vectorStore);
    } else {
      this.ingestionService = null;
    }
  }

  /**
   * Creates a builder for LocalStack-based testing.
   *
   * @param localStack the LocalStack container to use for AWS services
   * @return a builder for configuring the harness
   */
  public static Builder localStack(LocalStackContainer localStack) {
    Objects.requireNonNull(localStack, "localStack cannot be null");
    return new Builder().withLocalStack(localStack);
  }

  /**
   * Creates a builder for real AWS-based testing.
   *
   * @param s3Client the S3 client configured for real AWS
   * @return a builder for configuring the harness
   */
  public static Builder realAws(S3AsyncClient s3Client) {
    Objects.requireNonNull(s3Client, "s3Client cannot be null");
    return new Builder().withRealAws(s3Client);
  }

  /**
   * Creates a builder with test implementations (no external dependencies).
   *
   * @return a builder for configuring the harness
   */
  public static Builder testMode() {
    return new Builder().withTestMode();
  }

  /**
   * Processes an alert through the full pipeline and returns the generated checklist.
   *
   * @param alert the alert to process
   * @return the generated DynamicChecklist
   * @throws TimeoutException if processing takes longer than the configured timeout
   */
  public DynamicChecklist processAlert(Alert alert) throws Exception {
    Objects.requireNonNull(alert, "alert cannot be null");

    // Build pipeline components
    RunbookRetriever retriever = new DefaultRunbookRetriever(embeddingService, vectorStore);
    ChecklistGenerator generator = new DefaultChecklistGenerator(llmProvider);
    RagPipelineService pipeline = new RagPipelineService(enrichmentService, retriever, generator);

    // Process alert
    DynamicChecklist checklist =
        pipeline.processAlert(alert, 5).get(timeoutSeconds, TimeUnit.SECONDS);

    // Dispatch to outputs
    if (!destinations.isEmpty()) {
      WebhookDispatcher dispatcher = new WebhookDispatcher(destinations);
      dispatcher.dispatchSync(checklist);
    }

    return checklist;
  }

  /**
   * Gets the output file for a given alert ID.
   *
   * @param alertId the alert ID to find output for
   * @return the path to the output file, or null if not found
   * @throws IOException if an I/O error occurs
   */
  public Path getOutputFile(String alertId) throws IOException {
    if (outputDirectory == null) {
      return null;
    }

    try (Stream<Path> files = Files.list(outputDirectory)) {
      String safeAlertId = alertId.replaceAll("[^a-zA-Z0-9-_]", "_");
      return files
          .filter(p -> p.getFileName().toString().startsWith("checklist-" + safeAlertId))
          .findFirst()
          .orElse(null);
    }
  }

  /**
   * Seeds the vector store with runbooks from the given resource paths.
   *
   * @param resourcePaths paths relative to src/test/resources
   */
  public void seedRunbooks(String... resourcePaths) {
    for (String path : resourcePaths) {
      seedRunbook(path);
    }
  }

  /**
   * Seeds a single runbook from a resource path.
   *
   * @param resourcePath path relative to src/test/resources
   */
  public void seedRunbook(String resourcePath) {
    try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
      if (is == null) {
        throw new IllegalArgumentException("Runbook not found: " + resourcePath);
      }
      String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);

      // Create chunks from content (simplified chunking for tests)
      String title = extractTitle(content);
      List<String> tags = extractTags(content, resourcePath);

      // Generate embedding for chunk
      float[] embedding = embeddingService.embed(content).join();

      // Store chunk
      RunbookChunk chunk =
          new RunbookChunk(
              "chunk-" + resourcePath.hashCode(),
              resourcePath,
              title,
              content,
              tags,
              List.of("*"),
              embedding);
      vectorStore.store(chunk);
    } catch (IOException e) {
      throw new RuntimeException("Failed to seed runbook: " + resourcePath, e);
    }
  }

  private String extractTitle(String content) {
    // Simple extraction from frontmatter or first header
    if (content.contains("title:")) {
      int start = content.indexOf("title:") + 6;
      int end = content.indexOf('\n', start);
      return content.substring(start, end).trim();
    }
    if (content.contains("# ")) {
      int start = content.indexOf("# ") + 2;
      int end = content.indexOf('\n', start);
      return content.substring(start, end).trim();
    }
    return "Unknown";
  }

  private List<String> extractTags(String content, String path) {
    // Extract from filename for simplicity
    List<String> tags = new ArrayList<>();
    if (path.contains("memory")) tags.add("memory");
    if (path.contains("cpu")) tags.add("cpu");
    if (path.contains("disk")) tags.add("disk");
    if (path.contains("network")) tags.add("network");
    return tags;
  }

  /** Returns the vector store for direct access in tests. */
  public InMemoryVectorStoreRepository getVectorStore() {
    return vectorStore;
  }

  /** Returns the output directory path. */
  public Path getOutputDirectory() {
    return outputDirectory;
  }

  /**
   * Ingests runbooks from S3 bucket into the vector store.
   *
   * <p>This method uses the real {@link RunbookIngestionService#ingestAll(String)} to fetch
   * runbooks from S3, chunk them, generate embeddings, and store in the vector store.
   *
   * @param bucket the S3 bucket containing runbooks
   * @return the number of chunks ingested
   * @throws IllegalStateException if harness was not configured with S3 support
   */
  public int ingestRunbooksFromS3(String bucket) throws Exception {
    Objects.requireNonNull(bucket, "bucket cannot be null");
    if (ingestionService == null) {
      throw new IllegalStateException(
          "Harness was not configured with S3 support. Use .withLocalStackS3() to enable S3 ingestion.");
    }
    return ingestionService.ingestAll(bucket).get(timeoutSeconds, TimeUnit.SECONDS);
  }

  // ========== Builder ==========

  /** Builder for PipelineTestHarness. */
  public static class Builder {
    private InMemoryVectorStoreRepository vectorStore = new InMemoryVectorStoreRepository();
    private EmbeddingService embeddingService;
    private LlmProvider llmProvider;
    private ContextEnrichmentService enrichmentService;
    private Path outputDirectory;
    private final List<WebhookDestination> destinations = new ArrayList<>();
    private long timeoutSeconds = 60;
    private boolean isTestMode = false;
    private CloudStorageAdapter storageAdapter;

    Builder withLocalStack(LocalStackContainer localStack) {
      // Configure for LocalStack - uses test implementations by default
      this.isTestMode = true;
      return this;
    }

    Builder withRealAws(S3AsyncClient s3Client) {
      // Real AWS configuration would go here
      return this;
    }

    Builder withTestMode() {
      this.isTestMode = true;
      return this;
    }

    /**
     * Configures the harness to use LocalStack S3 for runbook ingestion.
     *
     * @param s3Client the S3 client configured for LocalStack
     * @return this builder
     */
    public Builder withLocalStackS3(S3AsyncClient s3Client) {
      Objects.requireNonNull(s3Client, "s3Client cannot be null");
      this.storageAdapter = new AwsS3StorageAdapter(s3Client);
      return this;
    }

    /**
     * Sets the output directory for file output.
     *
     * @param directory the output directory path
     * @return this builder
     */
    public Builder withOutputDirectory(Path directory) {
      this.outputDirectory = directory;
      if (directory != null) {
        destinations.add(
            new FileOutputAdapter(new FileOutputConfig(directory.toString(), "test-file-output")));
      }
      return this;
    }

    /**
     * Adds a webhook destination.
     *
     * @param destination the destination to add
     * @return this builder
     */
    public Builder withDestination(WebhookDestination destination) {
      destinations.add(destination);
      return this;
    }

    /**
     * Sets the processing timeout.
     *
     * @param seconds timeout in seconds
     * @return this builder
     */
    public Builder withTimeout(long seconds) {
      this.timeoutSeconds = seconds;
      return this;
    }

    /**
     * Sets a custom LLM provider.
     *
     * @param provider the LLM provider to use
     * @return this builder
     */
    public Builder withLlmProvider(LlmProvider provider) {
      this.llmProvider = provider;
      return this;
    }

    /**
     * Sets a custom embedding service.
     *
     * @param service the embedding service to use
     * @return this builder
     */
    public Builder withEmbeddingService(EmbeddingService service) {
      this.embeddingService = service;
      return this;
    }

    /**
     * Sets a custom enrichment service.
     *
     * @param service the enrichment service to use
     * @return this builder
     */
    public Builder withEnrichmentService(ContextEnrichmentService service) {
      this.enrichmentService = service;
      return this;
    }

    /**
     * Builds the PipelineTestHarness.
     *
     * @return the configured harness
     */
    public PipelineTestHarness build() {
      // Set defaults for test mode
      if (embeddingService == null) {
        embeddingService = new TestEmbeddingService();
      }
      if (llmProvider == null) {
        llmProvider = new TestLlmProvider();
      }
      if (enrichmentService == null) {
        enrichmentService = new TestEnrichmentService();
      }
      return new PipelineTestHarness(this);
    }
  }

  // ========== Test Implementations ==========

  /** Test LLM provider that returns predictable responses. */
  static class TestLlmProvider implements LlmProvider {
    private String customResponse;

    TestLlmProvider() {
      this.customResponse = null;
    }

    TestLlmProvider(String customResponse) {
      this.customResponse = customResponse;
    }

    @Override
    public String providerId() {
      return "test-llm";
    }

    @Override
    public CompletableFuture<String> generateText(String prompt, GenerationConfig config) {
      if (customResponse != null) {
        return CompletableFuture.completedFuture(customResponse);
      }

      // Generate context-aware response based on prompt
      String response;
      if (prompt.toLowerCase().contains("memory")) {
        response =
            "Memory Troubleshooting Checklist\n\n"
                + "Step 1: Check current memory usage with 'free -h'\n"
                + "Step 2: Identify memory-heavy processes with 'top -o %MEM'\n"
                + "Step 3: Review application logs for OOM errors\n"
                + "Step 4: Consider clearing caches if safe";
      } else if (prompt.toLowerCase().contains("cpu")) {
        response =
            "CPU Troubleshooting Checklist\n\n"
                + "Step 1: Check CPU usage with 'top -o %CPU'\n"
                + "Step 2: Identify CPU-heavy processes with 'ps aux --sort=-%cpu'\n"
                + "Step 3: Check for runaway processes\n"
                + "Step 4: Adjust process priorities with renice";
      } else if (prompt.toLowerCase().contains("disk")) {
        response =
            "Disk Troubleshooting Checklist\n\n"
                + "Step 1: Check disk space with 'df -h'\n"
                + "Step 2: Find large files with 'du -sh /*'\n"
                + "Step 3: Clean up log files\n"
                + "Step 4: Remove package cache";
      } else if (prompt.toLowerCase().contains("network")) {
        response =
            "Network Troubleshooting Checklist\n\n"
                + "Step 1: Check connectivity with 'ping'\n"
                + "Step 2: Test DNS resolution with 'nslookup'\n"
                + "Step 3: Check firewall rules\n"
                + "Step 4: Verify network interface status";
      } else {
        response =
            "General Troubleshooting Checklist\n\n"
                + "Step 1: Gather system information\n"
                + "Step 2: Check relevant logs\n"
                + "Step 3: Identify root cause\n"
                + "Step 4: Apply remediation";
      }
      return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<float[]> generateEmbedding(String text) {
      // Generate embedding based on text content
      return CompletableFuture.completedFuture(generateEmbeddingSync(text));
    }

    private float[] generateEmbeddingSync(String text) {
      String lowerText = text.toLowerCase();
      float memory = lowerText.contains("memory") || lowerText.contains("oom") ? 0.9f : 0.1f;
      float cpu = lowerText.contains("cpu") || lowerText.contains("process") ? 0.9f : 0.1f;
      float disk = lowerText.contains("disk") || lowerText.contains("storage") ? 0.9f : 0.1f;
      float network = lowerText.contains("network") || lowerText.contains("latency") ? 0.9f : 0.1f;
      return new float[] {memory, cpu, disk, network};
    }

    @Override
    public CompletableFuture<List<float[]>> generateEmbeddings(List<String> texts) {
      return CompletableFuture.completedFuture(
          texts.stream().map(this::generateEmbeddingSync).toList());
    }
  }

  /** Test embedding service that generates content-aware embeddings. */
  static class TestEmbeddingService implements EmbeddingService {
    @Override
    public CompletableFuture<float[]> embed(String text) {
      String lowerText = text.toLowerCase();
      float memory = lowerText.contains("memory") || lowerText.contains("oom") ? 0.9f : 0.1f;
      float cpu = lowerText.contains("cpu") || lowerText.contains("process") ? 0.9f : 0.1f;
      float disk = lowerText.contains("disk") || lowerText.contains("storage") ? 0.9f : 0.1f;
      float network = lowerText.contains("network") || lowerText.contains("latency") ? 0.9f : 0.1f;
      return CompletableFuture.completedFuture(new float[] {memory, cpu, disk, network});
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

  /** Test enrichment service that provides predictable test data. */
  static class TestEnrichmentService implements ContextEnrichmentService {
    private boolean shouldFailMetrics = false;
    private boolean shouldFailLogs = false;
    private boolean shouldFailCompletely = false;

    TestEnrichmentService() {}

    TestEnrichmentService withPartialMetricsFailure() {
      this.shouldFailMetrics = true;
      return this;
    }

    TestEnrichmentService withPartialLogsFailure() {
      this.shouldFailLogs = true;
      return this;
    }

    TestEnrichmentService withCompleteFailure() {
      this.shouldFailCompletely = true;
      return this;
    }

    @Override
    public CompletableFuture<EnrichedContext> enrich(Alert alert) {
      if (shouldFailCompletely) {
        return CompletableFuture.failedFuture(
            new RuntimeException("Complete enrichment failure (test)"));
      }

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
          shouldFailMetrics
              ? List.of()
              : List.of(
                  new MetricSnapshot("MemoryUtilization", "AWS/EC2", 92.5, "%", Instant.now()),
                  new MetricSnapshot("CPUUtilization", "AWS/EC2", 45.0, "%", Instant.now()),
                  new MetricSnapshot("DiskSpaceUtilization", "CWAgent", 78.0, "%", Instant.now()));

      List<LogEntry> logs =
          shouldFailLogs
              ? List.of()
              : List.of(
                  new LogEntry(
                      "log-test-001",
                      Instant.now().minusSeconds(60),
                      "WARNING",
                      "High resource utilization detected",
                      Map.of("hostname", "test-server")),
                  new LogEntry(
                      "log-test-002",
                      Instant.now().minusSeconds(30),
                      "ERROR",
                      "Application performance degradation",
                      Map.of("hostname", "test-server")));

      return CompletableFuture.completedFuture(
          new EnrichedContext(alert, resource, metrics, logs, Map.of()));
    }
  }

  /** Test enrichment service that times out. */
  static class TimeoutEnrichmentService implements ContextEnrichmentService {
    private final long delayMillis;

    TimeoutEnrichmentService(long delayMillis) {
      this.delayMillis = delayMillis;
    }

    @Override
    public CompletableFuture<EnrichedContext> enrich(Alert alert) {
      return CompletableFuture.supplyAsync(
          () -> {
            try {
              Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
            // This should not be reached if timeout is configured properly
            return new EnrichedContext(alert, null, List.of(), List.of(), Map.of());
          });
    }
  }
}
