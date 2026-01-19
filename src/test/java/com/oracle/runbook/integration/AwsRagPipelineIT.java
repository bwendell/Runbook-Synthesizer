package com.oracle.runbook.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.oracle.runbook.domain.*;
import com.oracle.runbook.enrichment.ContextEnrichmentService;
import com.oracle.runbook.infrastructure.cloud.aws.AwsS3StorageAdapter;
import com.oracle.runbook.rag.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Integration tests for AWS-backed RAG pipeline using LocalStack.
 *
 * <p>Verifies that the RAG pipeline correctly retrieves runbooks from S3 and generates checklists.
 */
class AwsRagPipelineIT extends LocalStackContainerBase {

  private static final String TEST_BUCKET = "runbook-pipeline-test";
  private static S3AsyncClient s3Client;

  private InMemoryVectorStore vectorStore;
  private TestEmbeddingService embeddingService;
  private TestLlmProvider llmProvider;
  private DefaultChecklistGenerator checklistGenerator;
  private TestRunbookRetriever retriever;
  private TestEnrichmentService enrichmentService;
  private RagPipelineService ragPipelineService;

  @BeforeAll
  static void setupS3BucketWithRunbooks() throws Exception {
    s3Client = createS3Client();

    // Create test bucket
    s3Client.createBucket(CreateBucketRequest.builder().bucket(TEST_BUCKET).build()).get();

    // Upload sample runbook markdown files with AWS-specific content
    uploadRunbook(
        "runbooks/aws-memory.md",
        """
        # AWS Memory Troubleshooting

        ## Investigation Steps

        1. Check CloudWatch metrics for MemoryUtilization
        2. SSH to instance and run 'free -h'
        3. Review application logs in CloudWatch Logs
        4. Check for OOM killer activity in dmesg

        ## Resolution Actions

        - Restart the application service
        - Scale instance type if persistent
        """);

    uploadRunbook(
        "runbooks/aws-cpu.md",
        """
        # AWS CPU Troubleshooting

        ## Investigation Steps

        1. Check CloudWatch CPUUtilization metric
        2. Use 'top' to identify process consuming CPU
        3. Review recent deployments

        ## Resolution

        - Identify and restart problematic service
        - Consider autoscaling configuration
        """);

    uploadRunbook(
        "runbooks/aws-disk.md",
        """
        # AWS Disk Space Troubleshooting

        ## Steps

        1. Check EBS volume usage with 'df -h'
        2. Identify large files with 'du -sh /*'
        3. Clean up logs and temporary files
        """);

    // Upload non-runbook file (should be ignored)
    s3Client
        .putObject(
            PutObjectRequest.builder().bucket(TEST_BUCKET).key("config/settings.json").build(),
            AsyncRequestBody.fromString("{\"version\": \"1.0\"}"))
        .get();
  }

  private static void uploadRunbook(String path, String content) throws Exception {
    s3Client
        .putObject(
            PutObjectRequest.builder().bucket(TEST_BUCKET).key(path).build(),
            AsyncRequestBody.fromString(content))
        .get();
  }

  @BeforeEach
  void setUp() {
    // Create test implementations
    vectorStore = new InMemoryVectorStore();
    embeddingService = new TestEmbeddingService();
    llmProvider = new TestLlmProvider();
    checklistGenerator = new DefaultChecklistGenerator(llmProvider);
    retriever = new TestRunbookRetriever(vectorStore, embeddingService);
    enrichmentService = new TestEnrichmentService();
    ragPipelineService = new RagPipelineService(enrichmentService, retriever, checklistGenerator);
  }

  @Test
  @DisplayName("Should list markdown runbooks from S3 bucket")
  void shouldListMarkdownRunbooksFromS3Bucket() throws Exception {
    AwsS3StorageAdapter adapter = new AwsS3StorageAdapter(s3Client);

    List<String> runbooks = adapter.listRunbooks(TEST_BUCKET).get();

    assertThat(runbooks)
        .as("Should only list markdown files as runbooks")
        .hasSize(3)
        .containsExactlyInAnyOrder(
            "runbooks/aws-memory.md", "runbooks/aws-cpu.md", "runbooks/aws-disk.md");
  }

  @Test
  @DisplayName("Should read runbook content from S3")
  void shouldReadRunbookContentFromS3() throws Exception {
    AwsS3StorageAdapter adapter = new AwsS3StorageAdapter(s3Client);

    String content = adapter.getRunbookContent(TEST_BUCKET, "runbooks/aws-memory.md").get().get();

    assertThat(content)
        .as("Should retrieve runbook content from S3")
        .contains("AWS Memory Troubleshooting")
        .contains("CloudWatch metrics")
        .contains("free -h");
  }

  @Test
  @DisplayName("Should process alert through full RAG pipeline with S3-backed storage")
  void shouldProcessAlertThroughFullRagPipelineWithS3BackedStorage() throws Exception {
    // Given: Load runbooks from S3 and seed vector store
    AwsS3StorageAdapter adapter = new AwsS3StorageAdapter(s3Client);
    List<String> runbookPaths = adapter.listRunbooks(TEST_BUCKET).get();

    for (String path : runbookPaths) {
      String content = adapter.getRunbookContent(TEST_BUCKET, path).get().orElseThrow();
      // Create a chunk for the vector store
      float[] embedding = embeddingService.embed(content).get();
      vectorStore.store(
          new RunbookChunk(
              "chunk-" + path.hashCode(),
              path,
              extractTitle(content),
              content,
              extractKeywords(path),
              List.of(".*"),
              embedding));
    }

    // Given: Configure LLM response
    llmProvider.setResponse(
        """
        AWS Memory Alert Response

        Step 1: Check CloudWatch MemoryUtilization metrics for the affected instance
        Step 2: SSH to the instance and run 'free -h' to check current memory state
        Step 3: Review application logs in CloudWatch Logs for memory-related errors
        Step 4: If OOM detected, restart the application service
        """);

    // Given: Create an AWS-specific alert
    Alert alert =
        new Alert(
            "alert-aws-001",
            "High Memory Utilization",
            "Memory utilization exceeded 90% on i-1234567890abcdef0",
            AlertSeverity.WARNING,
            "aws-cloudwatch",
            Map.of("InstanceId", "i-1234567890abcdef0", "Region", "us-west-2"),
            Map.of("AlarmName", "HighMemoryAlarm"),
            Instant.now(),
            "{}");

    // When: Process through RAG pipeline
    DynamicChecklist checklist = ragPipelineService.processAlert(alert, 5).get();

    // Then: Verify checklist was generated
    assertThat(checklist).as("Checklist should be generated").isNotNull();
    assertThat(checklist.alertId()).isEqualTo(alert.id());
    assertThat(checklist.steps()).isNotEmpty();
    assertThat(checklist.llmProviderUsed()).isEqualTo("test-llm");

    // Then: Verify runbook sources are from S3
    assertThat(checklist.sourceRunbooks())
        .as("Source runbooks should come from S3")
        .isNotEmpty()
        .anyMatch(path -> path.contains("runbooks/"));
  }

  @Test
  @DisplayName("Should generate checklist with AWS-specific context")
  void shouldGenerateChecklistWithAwsSpecificContext() throws Exception {
    // Given: Seed vector store with relevant chunks
    float[] memEmbedding = embeddingService.embed("memory utilization high").get();
    vectorStore.store(
        new RunbookChunk(
            "aws-chunk-001",
            "runbooks/aws-memory.md",
            "AWS Memory Troubleshooting",
            "Check CloudWatch metrics, use free -h, review CloudWatch Logs",
            List.of("memory", "aws"),
            List.of("i-.*"),
            memEmbedding));

    // Given: LLM configured to return AWS-specific response
    llmProvider.setResponse(
        """
        Step 1: Check CloudWatch MemoryUtilization metric
        Step 2: Connect to EC2 instance via SSM Session Manager
        Step 3: Run diagnostic commands
        """);

    // Given: AWS CloudWatch alert
    Alert alert =
        new Alert(
            "alert-aws-002",
            "EC2 Memory Warning",
            "Instance i-abc123 memory at 95%",
            AlertSeverity.CRITICAL,
            "aws-cloudwatch",
            Map.of("InstanceId", "i-abc123"),
            Map.of(),
            Instant.now(),
            "{}");

    // When: Process alert
    DynamicChecklist checklist = ragPipelineService.processAlert(alert, 5).get();

    // Then: Verify AWS-specific content
    assertThat(checklist.steps()).hasSizeGreaterThan(0);
    assertThat(checklist.sourceRunbooks()).contains("runbooks/aws-memory.md");
  }

  // ========== Helper Methods ==========

  private String extractTitle(String content) {
    // Extract first line after #
    String[] lines = content.split("\n");
    for (String line : lines) {
      if (line.startsWith("# ")) {
        return line.substring(2).trim();
      }
    }
    return "Unknown Runbook";
  }

  private List<String> extractKeywords(String path) {
    List<String> keywords = new ArrayList<>();
    if (path.contains("memory")) keywords.add("memory");
    if (path.contains("cpu")) keywords.add("cpu");
    if (path.contains("disk")) keywords.add("disk");
    if (path.contains("aws")) keywords.add("aws");
    return keywords;
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

  private class TestRunbookRetriever implements RunbookRetriever {
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

  private class TestEnrichmentService implements ContextEnrichmentService {
    @Override
    public CompletableFuture<EnrichedContext> enrich(Alert alert) {
      ResourceMetadata resource =
          new ResourceMetadata(
              alert.dimensions().getOrDefault("InstanceId", "i-unknown"),
              "prod-server-01",
              "default",
              "t3.large",
              "us-west-2a",
              Map.of("environment", "production", "cloud", "aws"),
              Map.of());

      List<MetricSnapshot> metrics =
          List.of(
              new MetricSnapshot("MemoryUtilization", "aws/ec2", 92.5, "%", Instant.now()),
              new MetricSnapshot("CPUUtilization", "aws/ec2", 45.2, "%", Instant.now()));

      List<LogEntry> logs =
          List.of(
              new LogEntry(
                  "log-001",
                  Instant.now().minusSeconds(60),
                  "WARNING",
                  "Memory pressure detected",
                  Map.of("hostname", "prod-server-01")));

      return CompletableFuture.completedFuture(
          new EnrichedContext(alert, resource, metrics, logs, Map.of("cloudProvider", "aws")));
    }
  }
}
