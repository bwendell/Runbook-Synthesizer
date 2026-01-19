package com.oracle.runbook.infrastructure.cloud.local;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oracle.runbook.domain.RunbookChunk;
import com.oracle.runbook.infrastructure.cloud.VectorStoreRepository;
import com.oracle.runbook.rag.ScoredChunk;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link InMemoryVectorStoreRepository}.
 *
 * <p>Tests follow TDD red-green-refactor cycle. Written FIRST before implementation.
 */
class InMemoryVectorStoreRepositoryTest {

  private InMemoryVectorStoreRepository repository;

  @BeforeEach
  void setUp() {
    repository = new InMemoryVectorStoreRepository();
  }

  @Nested
  @DisplayName("providerType()")
  class ProviderTypeTests {

    @Test
    @DisplayName("should return 'local' as provider type")
    void shouldReturnLocalProviderType() {
      assertThat(repository.providerType())
          .as("Provider type should be 'local'")
          .isEqualTo("local");
    }

    @Test
    @DisplayName("should implement VectorStoreRepository interface")
    void shouldImplementVectorStoreRepository() {
      assertThat(repository)
          .as("InMemoryVectorStoreRepository should implement VectorStoreRepository")
          .isInstanceOf(VectorStoreRepository.class);
    }
  }

  @Nested
  @DisplayName("store()")
  class StoreTests {

    @Test
    @DisplayName("should store a single chunk successfully")
    void shouldStoreSingleChunk() {
      RunbookChunk chunk =
          createChunk("chunk-001", "memory troubleshooting", new float[] {1.0f, 0.0f, 0.0f});

      repository.store(chunk);

      List<ScoredChunk> results = repository.search(new float[] {1.0f, 0.0f, 0.0f}, 1);
      assertThat(results).hasSize(1);
      assertThat(results.get(0).chunk().id()).isEqualTo("chunk-001");
    }

    @Test
    @DisplayName("should throw NullPointerException for null chunk")
    void shouldThrowForNullChunk() {
      assertThatThrownBy(() -> repository.store(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("chunk");
    }
  }

  @Nested
  @DisplayName("storeBatch()")
  class StoreBatchTests {

    @Test
    @DisplayName("should store multiple chunks in batch")
    void shouldStoreMultipleChunks() {
      List<RunbookChunk> chunks =
          List.of(
              createChunk("chunk-001", "memory troubleshooting", new float[] {1.0f, 0.0f, 0.0f}),
              createChunk("chunk-002", "cpu troubleshooting", new float[] {0.0f, 1.0f, 0.0f}),
              createChunk("chunk-003", "disk troubleshooting", new float[] {0.0f, 0.0f, 1.0f}));

      repository.storeBatch(chunks);

      List<ScoredChunk> results = repository.search(new float[] {1.0f, 0.0f, 0.0f}, 10);
      assertThat(results).hasSize(3);
    }

    @Test
    @DisplayName("should throw NullPointerException for null list")
    void shouldThrowForNullList() {
      assertThatThrownBy(() -> repository.storeBatch(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("chunks");
    }
  }

  @Nested
  @DisplayName("search()")
  class SearchTests {

    @Test
    @DisplayName("should return chunks ranked by cosine similarity")
    void shouldRankByCosineSimilarity() {
      // Store chunks with different embeddings
      repository.store(createChunk("exact-match", "exact", new float[] {1.0f, 0.0f, 0.0f}));
      repository.store(createChunk("partial-match", "partial", new float[] {0.7f, 0.7f, 0.0f}));
      repository.store(createChunk("no-match", "none", new float[] {0.0f, 0.0f, 1.0f}));

      // Search with query similar to first chunk
      List<ScoredChunk> results = repository.search(new float[] {1.0f, 0.0f, 0.0f}, 3);

      assertThat(results).hasSize(3);
      assertThat(results.get(0).chunk().id())
          .as("First result should be exact match")
          .isEqualTo("exact-match");
      assertThat(results.get(0).similarityScore())
          .as("Exact match should have score ~1.0")
          .isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    @DisplayName("should return at most topK results")
    void shouldLimitToTopK() {
      for (int i = 0; i < 10; i++) {
        repository.store(createChunk("chunk-" + i, "content", new float[] {1.0f, 0.0f, 0.0f}));
      }

      List<ScoredChunk> results = repository.search(new float[] {1.0f, 0.0f, 0.0f}, 5);

      assertThat(results).hasSize(5);
    }

    @Test
    @DisplayName("should return empty list for empty store")
    void shouldReturnEmptyForEmptyStore() {
      List<ScoredChunk> results = repository.search(new float[] {1.0f, 0.0f, 0.0f}, 5);

      assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("should throw NullPointerException for null queryEmbedding")
    void shouldThrowForNullQuery() {
      assertThatThrownBy(() -> repository.search(null, 5))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("queryEmbedding");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for topK <= 0")
    void shouldThrowForInvalidTopK() {
      assertThatThrownBy(() -> repository.search(new float[] {1.0f}, 0))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("topK");

      assertThatThrownBy(() -> repository.search(new float[] {1.0f}, -1))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("should handle orthogonal vectors with zero similarity")
    void shouldHandleOrthogonalVectors() {
      repository.store(createChunk("x-axis", "x", new float[] {1.0f, 0.0f, 0.0f}));

      // Query with orthogonal vector
      List<ScoredChunk> results = repository.search(new float[] {0.0f, 1.0f, 0.0f}, 1);

      assertThat(results).hasSize(1);
      assertThat(results.get(0).similarityScore())
          .as("Orthogonal vectors should have score ~0.0")
          .isCloseTo(0.0, org.assertj.core.data.Offset.offset(0.01));
    }
  }

  @Nested
  @DisplayName("delete()")
  class DeleteTests {

    @Test
    @DisplayName("should delete all chunks for a runbook path")
    void shouldDeleteChunksForPath() {
      String targetPath = "runbooks/memory/high-memory.md";
      String otherPath = "runbooks/cpu/high-cpu.md";

      repository.store(createChunkWithPath("chunk-1", targetPath, new float[] {1.0f, 0.0f, 0.0f}));
      repository.store(createChunkWithPath("chunk-2", targetPath, new float[] {0.9f, 0.1f, 0.0f}));
      repository.store(createChunkWithPath("chunk-3", otherPath, new float[] {0.0f, 1.0f, 0.0f}));

      repository.delete(targetPath);

      List<ScoredChunk> results = repository.search(new float[] {1.0f, 0.0f, 0.0f}, 10);
      assertThat(results).hasSize(1);
      assertThat(results.get(0).chunk().runbookPath()).isEqualTo(otherPath);
    }

    @Test
    @DisplayName("should not throw when deleting non-existent path")
    void shouldNotThrowForNonExistentPath() {
      repository.delete("non-existent-path.md");
      // No exception = success
    }

    @Test
    @DisplayName("should throw NullPointerException for null path")
    void shouldThrowForNullPath() {
      assertThatThrownBy(() -> repository.delete(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("runbookPath");
    }
  }

  @Nested
  @DisplayName("Thread safety")
  class ThreadSafetyTests {

    @Test
    @DisplayName("should handle concurrent stores safely")
    void shouldHandleConcurrentStores() throws InterruptedException {
      int numThreads = 10;
      int chunksPerThread = 100;
      ExecutorService executor = Executors.newFixedThreadPool(numThreads);
      CountDownLatch latch = new CountDownLatch(numThreads);

      for (int t = 0; t < numThreads; t++) {
        final int threadId = t;
        executor.submit(
            () -> {
              try {
                for (int i = 0; i < chunksPerThread; i++) {
                  repository.store(
                      createChunk(
                          "thread-" + threadId + "-chunk-" + i,
                          "content",
                          new float[] {1.0f, 0.0f, 0.0f}));
                }
              } finally {
                latch.countDown();
              }
            });
      }

      latch.await(10, TimeUnit.SECONDS);
      executor.shutdown();

      List<ScoredChunk> results = repository.search(new float[] {1.0f, 0.0f, 0.0f}, 1500);
      assertThat(results).hasSize(numThreads * chunksPerThread);
    }
  }

  // Helper methods
  private RunbookChunk createChunk(String id, String content, float[] embedding) {
    return new RunbookChunk(
        id,
        "runbooks/test.md",
        "Test Section",
        content,
        List.of("test"),
        List.of("VM.*"),
        embedding);
  }

  private RunbookChunk createChunkWithPath(String id, String path, float[] embedding) {
    return new RunbookChunk(
        id, path, "Test Section", "content", List.of("test"), List.of("VM.*"), embedding);
  }
}
