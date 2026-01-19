package com.oracle.runbook.rag;

import static org.assertj.core.api.Assertions.assertThat;

import com.oracle.runbook.domain.*;
import com.oracle.runbook.infrastructure.cloud.VectorStoreRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for DefaultRunbookRetriever implementation. */
class DefaultRunbookRetrieverTest {

  private DefaultRunbookRetriever retriever;
  private StubEmbeddingService embeddingService;
  private StubVectorStoreRepository vectorStore;

  @BeforeEach
  void setUp() {
    embeddingService = new StubEmbeddingService();
    vectorStore = new StubVectorStoreRepository();
    retriever = new DefaultRunbookRetriever(embeddingService, vectorStore);
  }

  @Test
  @DisplayName("retrieve embeds context and searches vector store")
  void retrieve_embedsContextAndSearches() {
    // Arrange
    EnrichedContext context =
        createTestContext("High Memory", "Memory is at 95%", "VM.Standard2.1");
    float[] queryEmbedding = new float[] {0.1f};
    embeddingService.setNextEmbedding(queryEmbedding);

    RunbookChunk chunk = createTestChunk("c1", "Check memory", List.of(), List.of());
    vectorStore.setSearchResults(List.of(new ScoredChunk(chunk, 0.9)));

    // Act
    List<RetrievedChunk> results = retriever.retrieve(context, 1);

    // Assert
    assertThat(results).isNotNull();
    assertThat(results).hasSize(1);
    assertThat(results.get(0).chunk().id()).isEqualTo("c1");
    // Verify it used the context (stub captures it)
    assertThat(embeddingService.lastContextEmbedded).isNotNull();
    assertThat(embeddingService.lastContextEmbedded.alert().title()).isEqualTo("High Memory");
  }

  @Test
  @DisplayName("retrieve applies metadata boost for matching tags")
  void retrieve_appliesTagBoost() {
    // Arrange
    EnrichedContext context = createTestContext("High Memory", "Memory issue", "VM.Standard2.1");
    // Alert has dimensions/labels that could match tags.
    // Let's assume tags "memory" and "oom" match.

    RunbookChunk chunkWithTags =
        createTestChunk("c1", "Memory fix", List.of("memory", "oom"), List.of());
    RunbookChunk chunkWithoutTags =
        createTestChunk("c2", "General fix", List.of("network"), List.of());

    vectorStore.setSearchResults(
        List.of(new ScoredChunk(chunkWithTags, 0.5), new ScoredChunk(chunkWithoutTags, 0.5)));
    // Mock similarity scores as 0.5 for both

    // Act
    List<RetrievedChunk> results = retriever.retrieve(context, 2);

    // Assert
    assertThat(results).hasSize(2);
    // c1 should have higher final score due to tag boost
    assertThat(results.get(0).finalScore())
        .as("Chunk with matching tags should have higher score")
        .isGreaterThan(results.get(1).finalScore());
    assertThat(results.get(0).chunk().id()).isEqualTo("c1");
  }

  @Test
  @DisplayName("retrieve applies metadata boost for matching shape patterns")
  void retrieve_appliesShapeBoost() {
    // Arrange
    EnrichedContext context = createTestContext("Compute slow", "CPU spike", "VM.Standard2.1");

    RunbookChunk chunkMatchShape = createTestChunk("c1", "VM fix", List.of(), List.of("VM.*"));
    RunbookChunk chunkNoMatchShape = createTestChunk("c2", "BM fix", List.of(), List.of("BM.*"));

    vectorStore.setSearchResults(
        List.of(new ScoredChunk(chunkMatchShape, 0.6), new ScoredChunk(chunkNoMatchShape, 0.6)));

    // Act
    List<RetrievedChunk> results = retriever.retrieve(context, 2);

    // Assert
    assertThat(results).hasSize(2);
    assertThat(results.get(0).metadataBoost())
        .as("Matching shape should get boost")
        .isGreaterThan(results.get(1).metadataBoost());
    assertThat(results.get(0).chunk().id()).isEqualTo("c1");
  }

  private EnrichedContext createTestContext(String title, String message, String shape) {
    Alert alert =
        new Alert(
            "a1",
            title,
            message,
            AlertSeverity.CRITICAL,
            "oci",
            Map.of("resourceId", "r1"),
            Map.of("app", "web"),
            Instant.now(),
            "{}");
    ResourceMetadata resource =
        new ResourceMetadata("r1", "web01", "comp1", shape, "AD1", Map.of(), Map.of());
    return new EnrichedContext(alert, resource, List.of(), List.of(), Map.of());
  }

  private RunbookChunk createTestChunk(
      String id, String content, List<String> tags, List<String> shapes) {
    return new RunbookChunk(id, "path", "Title", content, tags, shapes, new float[] {0.1f});
  }

  private static class StubEmbeddingService implements EmbeddingService {
    float[] nextEmbedding;
    EnrichedContext lastContextEmbedded;

    void setNextEmbedding(float[] e) {
      this.nextEmbedding = e;
    }

    @Override
    public CompletableFuture<float[]> embed(String text) {
      return CompletableFuture.completedFuture(new float[768]);
    }

    @Override
    public CompletableFuture<List<float[]>> embedBatch(List<String> texts) {
      return CompletableFuture.completedFuture(texts.stream().map(t -> new float[768]).toList());
    }

    @Override
    public CompletableFuture<float[]> embedContext(EnrichedContext context) {
      this.lastContextEmbedded = context;
      return CompletableFuture.completedFuture(nextEmbedding);
    }
  }

  private static class StubVectorStoreRepository implements VectorStoreRepository {
    List<ScoredChunk> results = List.of();

    void setSearchResults(List<ScoredChunk> r) {
      this.results = r;
    }

    @Override
    public String providerType() {
      return "test";
    }

    @Override
    public void store(RunbookChunk chunk) {}

    @Override
    public void storeBatch(List<RunbookChunk> chunks) {}

    @Override
    public void delete(String path) {}

    @Override
    public List<ScoredChunk> search(float[] embedding, int topK) {
      return results;
    }
  }
}
