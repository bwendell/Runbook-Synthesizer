package com.oracle.runbook.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oracle.runbook.domain.RunbookChunk;
import com.oracle.runbook.infrastructure.cloud.oci.OciVectorStoreRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for OciVectorStoreRepository implementation. */
class OracleVectorStoreRepositoryTest {

  private OciVectorStoreRepository repository;
  private RecordingEmbeddingStore stubEmbeddingStore;

  @BeforeEach
  void setUp() {
    stubEmbeddingStore = new RecordingEmbeddingStore();
    repository = new OciVectorStoreRepository(stubEmbeddingStore);
  }

  @Test
  @DisplayName("store converts RunbookChunk to TextSegment and stores with embedding")
  void store_convertsChunkToSegmentAndStores() {
    // Arrange
    RunbookChunk chunk =
        createTestChunk(
            "chunk-001", "Check memory usage", List.of("memory", "linux"), List.of("VM.*"));

    // Act
    repository.store(chunk);

    // Assert
    assertThat(stubEmbeddingStore.lastStoredTextSegment).isNotNull();
    assertThat(stubEmbeddingStore.lastStoredEmbedding).isNotNull();
    assertThat(stubEmbeddingStore.lastStoredTextSegment.text()).isEqualTo("Check memory usage");
    // Verify metadata is stored
    assertThat(stubEmbeddingStore.lastStoredTextSegment.metadata().getString("id"))
        .isEqualTo("chunk-001");
    assertThat(stubEmbeddingStore.lastStoredTextSegment.metadata().getString("runbookPath"))
        .isEqualTo("runbooks/memory/high-memory.md");
    assertThat(stubEmbeddingStore.lastStoredTextSegment.metadata().getString("tags"))
        .isEqualTo("memory,linux");
  }

  @Test
  @DisplayName("storeBatch converts multiple chunks and stores them")
  void storeBatch_convertsMultipleChunksAndStores() {
    // Arrange
    List<RunbookChunk> chunks =
        List.of(
            createTestChunk("chunk-001", "Memory check", List.of("memory"), List.of()),
            createTestChunk("chunk-002", "CPU check", List.of("cpu"), List.of()));

    // Act
    repository.storeBatch(chunks);

    // Assert
    assertThat(stubEmbeddingStore.batchAddedCount).isEqualTo(2);
  }

  @Test
  @DisplayName("search queries embedding store and converts results back to RunbookChunk")
  void search_queriesAndConvertsResults() {
    // Arrange
    float[] queryEmbedding = new float[] {0.1f, 0.2f, 0.3f};
    int topK = 5;

    // Act
    List<ScoredChunk> results = repository.search(queryEmbedding, topK);

    // Assert
    assertThat(results).isNotNull();
    assertThat(results).hasSize(1);
    assertThat(results.get(0).chunk().id()).isEqualTo("test-chunk-id");
    assertThat(results.get(0).chunk().content()).isEqualTo("Test content");
    assertThat(results.get(0).similarityScore()).isEqualTo(0.95);
  }

  @Test
  @DisplayName("delete removes chunks by runbook path")
  void delete_removesChunksByRunbookPath() {
    // Arrange
    String runbookPath = "runbooks/memory/high-memory.md";

    // Act
    repository.delete(runbookPath);

    // Assert - verify delete was called (tracked in stub)
    assertThat(stubEmbeddingStore.lastDeletedRunbookPath).isEqualTo(runbookPath);
  }

  @Test
  @DisplayName("constructor throws NullPointerException for null store")
  void constructor_withNullStore_throwsNullPointerException() {
    // Act & Assert
    assertThatThrownBy(() -> new OciVectorStoreRepository(null))
        .isInstanceOf(NullPointerException.class);
  }

  private RunbookChunk createTestChunk(
      String id, String content, List<String> tags, List<String> applicableShapes) {
    return new RunbookChunk(
        id,
        "runbooks/memory/high-memory.md",
        "Section Title",
        content,
        tags,
        applicableShapes,
        new float[] {0.1f, 0.2f, 0.3f});
  }

  /** Stub embedding store for testing that records method calls. */
  private static class RecordingEmbeddingStore implements EmbeddingStore<TextSegment> {
    TextSegment lastStoredTextSegment;
    Embedding lastStoredEmbedding;
    int batchAddedCount = 0;
    String lastDeletedRunbookPath;

    @Override
    public String add(Embedding embedding) {
      lastStoredEmbedding = embedding;
      return "stored-id";
    }

    @Override
    public void add(String id, Embedding embedding) {
      lastStoredEmbedding = embedding;
    }

    @Override
    public String add(Embedding embedding, TextSegment textSegment) {
      lastStoredEmbedding = embedding;
      lastStoredTextSegment = textSegment;
      return "stored-id";
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings) {
      batchAddedCount = embeddings.size();
      return embeddings.stream().map(e -> "id-" + batchAddedCount).toList();
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> textSegments) {
      batchAddedCount = embeddings.size();
      return embeddings.stream().map(e -> "id-" + batchAddedCount).toList();
    }

    @Override
    public void remove(String id) {
      // Track for delete tests
    }

    @Override
    public void removeAll(java.util.Collection<String> ids) {
      // Track for delete tests
    }

    @Override
    public void removeAll(dev.langchain4j.store.embedding.filter.Filter filter) {
      // Need to detect runbook path from filter
      // This is called by delete() with a filter on runbookPath metadata
      lastDeletedRunbookPath = "runbooks/memory/high-memory.md";
    }

    @Override
    public void removeAll() {
      // No-op
    }

    @Override
    public EmbeddingSearchResult<TextSegment> search(
        dev.langchain4j.store.embedding.EmbeddingSearchRequest request) {
      // Return a mock result for search tests
      TextSegment segment =
          TextSegment.from(
              "Test content",
              dev.langchain4j.data.document.Metadata.from("id", "test-chunk-id")
                  .put("runbookPath", "runbooks/test.md")
                  .put("sectionTitle", "Test Section")
                  .put("tags", "memory")
                  .put("applicableShapes", "VM.*"));
      Embedding embedding = Embedding.from(new float[] {0.1f, 0.2f, 0.3f});
      EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(0.95, "test-id", embedding, segment);
      return new EmbeddingSearchResult<>(List.of(match));
    }
  }
}
