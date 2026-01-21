package com.oracle.runbook.integration.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.oracle.runbook.domain.RunbookChunk;
import com.oracle.runbook.infrastructure.cloud.aws.AwsS3StorageAdapter;
import com.oracle.runbook.infrastructure.cloud.local.InMemoryVectorStoreRepository;
import com.oracle.runbook.integration.DockerSupport;
import com.oracle.runbook.integration.LocalStackContainerBase;
import com.oracle.runbook.rag.EmbeddingService;
import com.oracle.runbook.rag.RunbookChunker;
import com.oracle.runbook.rag.RunbookIngestionService;
import com.oracle.runbook.rag.ScoredChunk;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * End-to-end integration test for the runbook ingestion pipeline.
 *
 * <p>This test validates the complete S3-to-vector-store ingestion flow:
 *
 * <ul>
 *   <li>Upload markdown runbooks to S3 bucket
 *   <li>Invoke {@link RunbookIngestionService#ingestAll(String)}
 *   <li>Verify correct number of chunks stored in vector store
 *   <li>Verify chunks are searchable via {@link InMemoryVectorStoreRepository#search(float[], int)}
 * </ul>
 *
 * <p>Uses LocalStack for S3 and in-memory implementations for embedding and vector store.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RunbookIngestionE2EIT extends LocalStackContainerBase {

  private static final String TEST_BUCKET = "e2e-runbook-ingestion";
  private static final int EMBEDDING_DIMENSION = 4;

  private static S3AsyncClient s3Client;
  private static InMemoryVectorStoreRepository vectorStore;
  private static RunbookIngestionService ingestionService;
  private static AwsS3StorageAdapter storageAdapter;

  @BeforeAll
  static void setupContainersAndClients() throws Exception {
    DockerSupport.ensureDockerAvailable();

    // Create AWS S3 client pointing to LocalStack
    s3Client = createS3Client();

    // Create test bucket
    s3Client
        .createBucket(CreateBucketRequest.builder().bucket(TEST_BUCKET).build())
        .get(30, TimeUnit.SECONDS);

    // Initialize components for ingestion pipeline
    storageAdapter = new AwsS3StorageAdapter(s3Client);
    vectorStore = new InMemoryVectorStoreRepository();
    RunbookChunker chunker = new RunbookChunker();
    EmbeddingService embeddingService = new TestEmbeddingService();

    ingestionService =
        new RunbookIngestionService(storageAdapter, chunker, embeddingService, vectorStore);
  }

  @AfterAll
  static void cleanup() {
    if (s3Client != null) {
      s3Client.close();
    }
  }

  @Test
  @Order(1)
  @DisplayName("Should upload runbooks to S3 bucket")
  void shouldUploadRunbooksToS3Bucket() throws Exception {
    // Given: Sample runbook markdown content
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

        ## Identify Memory-Heavy Processes

        Use `top -o %MEM` to find processes consuming most memory.

        ## Clear System Caches

        ```bash
        sync && echo 3 > /proc/sys/vm/drop_caches
        ```
        """;

    String cpuRunbook =
        """
        ---
        title: CPU Troubleshooting Guide
        tags:
          - cpu
          - performance
        applicable_shapes:
          - "VM.Standard2.*"
        ---

        # CPU Troubleshooting

        ## Check CPU Usage

        Run `top` or `htop` to view real-time CPU usage.

        ## Identify CPU-Intensive Processes

        Use `ps aux --sort=-%cpu | head -20` to see top CPU consumers.
        """;

    // When: Upload runbooks to S3
    s3Client
        .putObject(
            PutObjectRequest.builder().bucket(TEST_BUCKET).key("memory-troubleshooting.md").build(),
            AsyncRequestBody.fromString(memoryRunbook))
        .get(30, TimeUnit.SECONDS);

    s3Client
        .putObject(
            PutObjectRequest.builder().bucket(TEST_BUCKET).key("cpu-troubleshooting.md").build(),
            AsyncRequestBody.fromString(cpuRunbook))
        .get(30, TimeUnit.SECONDS);

    // Then: Verify runbooks are listed in S3
    List<String> runbookPaths = storageAdapter.listRunbooks(TEST_BUCKET).get(30, TimeUnit.SECONDS);

    assertThat(runbookPaths)
        .as("Both runbooks should be listed in S3 bucket")
        .containsExactlyInAnyOrder("memory-troubleshooting.md", "cpu-troubleshooting.md");
  }

  @Test
  @Order(2)
  @DisplayName("Should ingest all runbooks from S3 and store chunks in vector store")
  void shouldIngestAllRunbooksAndStoreChunksInVectorStore() throws Exception {
    // When: Ingest all runbooks from S3
    int totalChunks = ingestionService.ingestAll(TEST_BUCKET).get(60, TimeUnit.SECONDS);

    // Then: Verify chunks were stored
    assertThat(totalChunks)
        .as("Should have ingested multiple chunks from both runbooks")
        .isGreaterThan(0);

    // And: Verify chunks are in vector store by searching
    // Using a memory-related embedding to find memory runbook chunks
    float[] memoryQueryEmbedding = new float[] {0.9f, 0.1f, 0.0f, 0.0f};
    List<ScoredChunk> memoryResults = vectorStore.search(memoryQueryEmbedding, 5);

    assertThat(memoryResults).as("Vector store should contain chunks after ingestion").isNotEmpty();

    // Verify chunks came from seeded runbooks
    List<String> foundRunbookPaths =
        memoryResults.stream().map(sc -> sc.chunk().runbookPath()).distinct().toList();

    assertThat(foundRunbookPaths)
        .as("Search results should include chunks from ingested runbooks")
        .containsAnyOf("memory-troubleshooting.md", "cpu-troubleshooting.md");
  }

  @Test
  @Order(3)
  @DisplayName("Ingested chunks should be searchable by semantic similarity")
  void ingestedChunksShouldBeSearchableBySemanticsimilarity() throws Exception {
    // Given: Query embedding that should match memory-related content
    // The test embedding service assigns embeddings based on content keywords
    float[] memoryQueryEmbedding = new float[] {0.9f, 0.1f, 0.0f, 0.0f};

    // When: Search vector store
    List<ScoredChunk> results = vectorStore.search(memoryQueryEmbedding, 10);

    // Then: Results should be non-empty and contain relevant chunks
    assertThat(results).as("Search should return chunks").isNotEmpty();

    // Verify we get chunks from the memory runbook first (higher similarity)
    ScoredChunk topResult = results.get(0);

    assertThat(topResult.chunk()).as("Top result should be a valid RunbookChunk").isNotNull();

    assertThat(topResult.chunk().runbookPath())
        .as("Top result should come from memory troubleshooting runbook")
        .isEqualTo("memory-troubleshooting.md");

    assertThat(topResult.similarityScore())
        .as("Top result should have positive similarity score")
        .isGreaterThan(0);
  }

  @Test
  @Order(4)
  @DisplayName("Ingested chunks should preserve runbook metadata")
  void ingestedChunksShouldPreserveRunbookMetadata() throws Exception {
    // Given: Search for chunks
    float[] queryEmbedding = new float[] {0.5f, 0.5f, 0.0f, 0.0f};
    List<ScoredChunk> results = vectorStore.search(queryEmbedding, 20);

    // Then: Verify chunks have valid metadata
    assertThat(results).isNotEmpty();

    for (ScoredChunk scoredChunk : results) {
      RunbookChunk chunk = scoredChunk.chunk();

      assertThat(chunk.id()).as("Chunk should have unique ID").isNotNull().isNotBlank();

      assertThat(chunk.runbookPath())
          .as("Chunk should have runbook path")
          .isNotNull()
          .endsWith(".md");

      assertThat(chunk.content()).as("Chunk should have content").isNotNull().isNotBlank();

      assertThat(chunk.embedding())
          .as("Chunk should have embedding")
          .isNotNull()
          .hasSize(EMBEDDING_DIMENSION);
    }
  }

  @Test
  @Order(5)
  @DisplayName("Should handle search with different query embeddings")
  void shouldHandleSearchWithDifferentQueryEmbeddings() throws Exception {
    // Given: CPU-related query embedding
    float[] cpuQueryEmbedding = new float[] {0.1f, 0.9f, 0.0f, 0.0f};

    // When: Search for CPU-related content
    List<ScoredChunk> cpuResults = vectorStore.search(cpuQueryEmbedding, 5);

    // Then: Should find CPU-related chunks
    assertThat(cpuResults).as("Search should return results for CPU query").isNotEmpty();

    // Verify at least one result comes from CPU runbook
    boolean hasCpuRunbook =
        cpuResults.stream()
            .anyMatch(sc -> sc.chunk().runbookPath().equals("cpu-troubleshooting.md"));

    assertThat(hasCpuRunbook)
        .as("CPU query should find chunks from CPU troubleshooting runbook")
        .isTrue();
  }

  // ========== Test Implementations ==========

  /**
   * Test embedding service that generates deterministic embeddings based on content.
   *
   * <p>This enables testing semantic search behavior with predictable results.
   */
  private static class TestEmbeddingService implements EmbeddingService {

    @Override
    public CompletableFuture<float[]> embed(String text) {
      // Generate embedding based on content keywords
      String lowerText = text.toLowerCase();
      float memoryScore = lowerText.contains("memory") ? 0.9f : 0.1f;
      float cpuScore = lowerText.contains("cpu") ? 0.9f : 0.1f;

      return CompletableFuture.completedFuture(new float[] {memoryScore, cpuScore, 0.0f, 0.0f});
    }

    @Override
    public CompletableFuture<List<float[]>> embedBatch(List<String> texts) {
      List<float[]> embeddings = texts.stream().map(t -> embed(t).join()).toList();
      return CompletableFuture.completedFuture(embeddings);
    }

    @Override
    public CompletableFuture<float[]> embedContext(
        com.oracle.runbook.domain.EnrichedContext context) {
      return embed(context.alert().title() + " " + context.alert().message());
    }
  }
}
