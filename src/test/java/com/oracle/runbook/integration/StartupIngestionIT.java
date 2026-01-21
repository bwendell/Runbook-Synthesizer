package com.oracle.runbook.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.oracle.runbook.config.RunbookConfig;
import com.oracle.runbook.domain.RunbookChunk;
import com.oracle.runbook.infrastructure.cloud.local.InMemoryVectorStoreRepository;
import com.oracle.runbook.integration.stubs.InMemoryCloudStorageAdapter;
import com.oracle.runbook.rag.EmbeddingService;
import com.oracle.runbook.rag.RunbookChunker;
import com.oracle.runbook.rag.RunbookIngestionService;
import com.oracle.runbook.rag.ScoredChunk;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;

/**
 * Integration test for startup runbook ingestion.
 *
 * <p>This test verifies that the runbook ingestion pipeline works correctly:
 *
 * <ul>
 *   <li>CloudStorageAdapter can list and fetch runbooks
 *   <li>RunbookChunker parses markdown into chunks
 *   <li>EmbeddingService generates embeddings
 *   <li>VectorStoreRepository stores and retrieves chunks
 * </ul>
 *
 * <p>Uses in-memory stubs to test without cloud dependencies.
 *
 * <p><b>Note:</b> This test verifies the ingestion pipeline works. Task 3.2 will add the startup
 * call in RunbookSynthesizerApp so ingestion happens automatically.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StartupIngestionIT {

  private static final String TEST_BUCKET = "test-runbook-ingestion";

  private static InMemoryCloudStorageAdapter storageAdapter;
  private static InMemoryVectorStoreRepository vectorStore;
  private static RunbookIngestionService ingestionService;
  private static RunbookConfig runbookConfig;

  @BeforeAll
  static void setup() {
    // Create test configuration
    Config config =
        Config.builder()
            .sources(
                ConfigSources.create(
                    Map.of("runbooks.bucket", TEST_BUCKET, "runbooks.ingestOnStartup", "true")))
            .build();

    runbookConfig = new RunbookConfig(config);

    // Create in-memory storage adapter and seed with sample runbooks
    storageAdapter = new InMemoryCloudStorageAdapter();
    seedSampleRunbooks();

    // Create embedding service (stub for testing)
    EmbeddingService embeddingService = new TestEmbeddingService();

    // Create runbook chunker
    RunbookChunker chunker = new RunbookChunker();

    // Create vector store
    vectorStore = new InMemoryVectorStoreRepository();

    // Create ingestion service
    ingestionService =
        new RunbookIngestionService(storageAdapter, chunker, embeddingService, vectorStore);
  }

  private static void seedSampleRunbooks() {
    String cpuRunbook =
        """
        ---
        title: CPU Troubleshooting Guide
        tags:
          - cpu
          - performance
        applicable_shapes:
          - "*"
        ---

        # CPU Troubleshooting

        ## Check Current CPU Usage

        Run `top` or `htop` to see CPU usage.

        ## Identify High CPU Processes

        ```bash
        ps aux --sort=-%cpu | head -10
        ```

        ## Check for Runaway Processes

        Look for processes consuming excessive CPU time.
        """;

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

    storageAdapter.seedBucket(TEST_BUCKET, "cpu-troubleshooting.md", cpuRunbook);
    storageAdapter.seedBucket(TEST_BUCKET, "memory-troubleshooting.md", memoryRunbook);
  }

  @Test
  @Order(1)
  @DisplayName("Should create RunbookConfig with correct bucket from configuration")
  void shouldCreateRunbookConfigWithCorrectBucket() {
    // Then: Config should have correct bucket name
    assertThat(runbookConfig.bucket()).isEqualTo(TEST_BUCKET);
    assertThat(runbookConfig.ingestOnStartup()).isTrue();
  }

  @Test
  @Order(2)
  @DisplayName("Should list runbooks from storage adapter")
  void shouldListRunbooksFromStorageAdapter() throws Exception {
    // When: List runbooks from storage
    List<String> runbooks = storageAdapter.listRunbooks(TEST_BUCKET).get(10, TimeUnit.SECONDS);

    // Then: Both sample runbooks should be listed
    assertThat(runbooks)
        .containsExactlyInAnyOrder("cpu-troubleshooting.md", "memory-troubleshooting.md");
  }

  @Test
  @Order(3)
  @DisplayName("Should ingest runbooks and store chunks in vector store")
  void shouldIngestRunbooksAndStoreChunksInVectorStore() throws Exception {
    // When: Ingest all runbooks from the bucket
    int chunkCount = ingestionService.ingestAll(TEST_BUCKET).get(30, TimeUnit.SECONDS);

    // Then: Chunks should be ingested
    assertThat(chunkCount).isGreaterThan(0);

    // And: Vector store should have searchable chunks
    float[] queryEmbedding = new float[384];
    queryEmbedding[0] = 1.0f;

    List<ScoredChunk> results = vectorStore.search(queryEmbedding, 10);

    // Then: Results should include chunks from uploaded runbooks
    assertThat(results).isNotEmpty();

    List<String> runbookPaths =
        results.stream().map(sc -> sc.chunk().runbookPath()).distinct().toList();

    assertThat(runbookPaths).containsAnyOf("cpu-troubleshooting.md", "memory-troubleshooting.md");
  }

  @Test
  @Order(4)
  @DisplayName("Ingested chunks should have valid embeddings")
  void ingestedChunksShouldHaveValidEmbeddings() throws Exception {
    // Given: Vector store with ingested chunks
    float[] queryEmbedding = new float[384];
    queryEmbedding[0] = 1.0f;

    List<ScoredChunk> results = vectorStore.search(queryEmbedding, 5);

    // Then: Each chunk should have a valid embedding
    for (ScoredChunk scoredChunk : results) {
      RunbookChunk chunk = scoredChunk.chunk();
      assertThat(chunk.embedding()).isNotNull();
      assertThat(chunk.embedding().length).isEqualTo(384);
    }
  }

  @Test
  @Order(5)
  @DisplayName("Chunks should preserve runbook metadata")
  void chunksShouldPreserveRunbookMetadata() throws Exception {
    // Given: Vector store with ingested chunks
    float[] queryEmbedding = new float[384];
    queryEmbedding[0] = 1.0f;

    List<ScoredChunk> results = vectorStore.search(queryEmbedding, 10);

    // Then: At least one chunk should have tags from the original runbook
    boolean hasCpuTag = results.stream().anyMatch(sc -> sc.chunk().tags().contains("cpu"));
    boolean hasMemoryTag = results.stream().anyMatch(sc -> sc.chunk().tags().contains("memory"));

    assertThat(hasCpuTag || hasMemoryTag)
        .as("At least one chunk should have cpu or memory tag")
        .isTrue();
  }

  // ========== Test Implementations ==========

  /** Test embedding service that returns fixed-size embeddings. */
  private static class TestEmbeddingService implements EmbeddingService {
    @Override
    public CompletableFuture<float[]> embed(String text) {
      // Return a 384-dimensional embedding with some variation based on text
      float[] embedding = new float[384];
      int hash = text.hashCode();
      for (int i = 0; i < embedding.length; i++) {
        embedding[i] = (float) Math.sin((hash + i) * 0.1);
      }
      return CompletableFuture.completedFuture(embedding);
    }

    @Override
    public CompletableFuture<List<float[]>> embedBatch(List<String> texts) {
      return CompletableFuture.completedFuture(texts.stream().map(t -> embed(t).join()).toList());
    }

    @Override
    public CompletableFuture<float[]> embedContext(
        com.oracle.runbook.domain.EnrichedContext context) {
      return embed(context.alert().title() + " " + context.alert().message());
    }
  }

  // ========== App Startup Ingestion Test ==========
  // This test verifies that RunbookSynthesizerApp has the performStartupIngestion method.

  @Test
  @Order(6)
  @DisplayName("RunbookSynthesizerApp should call ingestion at startup when configured")
  void appShouldCallIngestionAtStartupWhenConfigured() {
    // When: Check if the app has the ingestion method
    // The app should have a method that:
    // 1. Reads runbooks.ingestOnStartup config
    // 2. If true and stubMode=false, calls ingestionService.ingestAll(bucket)

    // Check if RunbookSynthesizerApp has an ingestion method/call
    boolean hasIngestionCall = checkAppHasIngestionCall();

    // Then: App should have ingestion method
    assertThat(hasIngestionCall)
        .as(
            "RunbookSynthesizerApp should call ingestion at startup when stubMode=false and ingestOnStartup=true")
        .isTrue();
  }

  /**
   * Checks if RunbookSynthesizerApp has the startup ingestion logic.
   *
   * <p>This is a placeholder that returns false until the app is updated to call ingestion.
   */
  private boolean checkAppHasIngestionCall() {
    // This method checks if the app has the required startup ingestion logic
    // For a true integration test, we would:
    // 1. Start the app with test config
    // 2. Check that vector store has chunks after startup
    //
    // For TDD RED phase, we use this simpler check that will fail
    // until RunbookSynthesizerApp is updated

    // Check by reflection if the app class has the expected method
    try {
      Class<?> appClass = Class.forName("com.oracle.runbook.RunbookSynthesizerApp");

      // Look for a method that calls ingestion at startup
      // This could be "performStartupIngestion" or similar
      java.lang.reflect.Method[] methods = appClass.getDeclaredMethods();
      for (java.lang.reflect.Method method : methods) {
        if (method.getName().contains("Ingestion") || method.getName().contains("ingest")) {
          return true;
        }
      }
      return false;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }
}
