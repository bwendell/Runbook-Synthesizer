package com.oracle.runbook.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.oracle.runbook.domain.RunbookChunk;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for the VectorStoreRepository interface contract. */
class VectorStoreRepositoryTest {

  @Test
  @DisplayName("store accepts a RunbookChunk")
  void store_acceptsRunbookChunk() {
    // Arrange
    VectorStoreRepository repository = new TestVectorStoreRepository();
    RunbookChunk chunk = createTestChunk("chunk-001");

    // Act & Assert - should not throw
    assertThatCode(() -> repository.store(chunk)).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("storeBatch accepts list of RunbookChunks")
  void storeBatch_acceptsChunkList() {
    // Arrange
    VectorStoreRepository repository = new TestVectorStoreRepository();
    List<RunbookChunk> chunks = List.of(createTestChunk("chunk-001"), createTestChunk("chunk-002"));

    // Act & Assert - should not throw
    assertThatCode(() -> repository.storeBatch(chunks)).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("search accepts embedding and topK, returns List<RunbookChunk>")
  void search_acceptsEmbeddingAndTopK_returnsChunks() {
    // Arrange
    VectorStoreRepository repository = new TestVectorStoreRepository();
    float[] queryEmbedding = new float[768];
    int topK = 5;

    // Act
    List<ScoredChunk> results = repository.search(queryEmbedding, topK);

    // Assert
    assertThat(results).isNotNull();
    assertThat(results).hasSize(2);
  }

  @Test
  @DisplayName("delete removes all chunks for a runbook path")
  void delete_removesChunksForPath() {
    // Arrange
    VectorStoreRepository repository = new TestVectorStoreRepository();
    String runbookPath = "runbooks/memory/high-memory.md";

    // Act & Assert - should not throw
    assertThatCode(() -> repository.delete(runbookPath)).doesNotThrowAnyException();
  }

  private RunbookChunk createTestChunk(String id) {
    return new RunbookChunk(
        id,
        "runbooks/memory/high-memory.md",
        "Step 1: Check memory",
        "Run free -h to check memory",
        List.of("memory"),
        List.of("VM.*"),
        new float[768]);
  }

  /** Test implementation of VectorStoreRepository for verifying interface contract. */
  private static class TestVectorStoreRepository implements VectorStoreRepository {
    @Override
    public void store(RunbookChunk chunk) {
      // No-op for test
    }

    @Override
    public void storeBatch(List<RunbookChunk> chunks) {
      // No-op for test
    }

    @Override
    public List<ScoredChunk> search(float[] queryEmbedding, int topK) {
      return List.of(
          new ScoredChunk(createTestChunk("chunk-001"), 0.9),
          new ScoredChunk(createTestChunk("chunk-002"), 0.8));
    }

    @Override
    public void delete(String runbookPath) {
      // No-op for test
    }

    private RunbookChunk createTestChunk(String id) {
      return new RunbookChunk(
          id,
          "runbooks/memory/high-memory.md",
          "Step 1: Check memory",
          "Run free -h to check memory",
          List.of("memory"),
          List.of("VM.*"),
          new float[768]);
    }
  }
}
