package com.oracle.runbook.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for the EmbeddingService interface contract. */
class EmbeddingServiceTest {

  @Test
  @DisplayName("embed accepts text and returns CompletableFuture<float[]>")
  void embed_acceptsText_returnsEmbeddingFuture() throws Exception {
    // Arrange
    EmbeddingService service = new TestEmbeddingService();
    String text = "High memory utilization on server";

    // Act
    CompletableFuture<float[]> future = service.embed(text);
    float[] embedding = future.get();

    // Assert
    assertThat(future).isNotNull();
    assertThat(embedding).isNotNull();
    assertThat(embedding).hasSize(768);
  }

  @Test
  @DisplayName("embedBatch accepts list of texts and returns CompletableFuture<List<float[]>>")
  void embedBatch_acceptsTextList_returnsEmbeddingsFuture() throws Exception {
    // Arrange
    EmbeddingService service = new TestEmbeddingService();
    List<String> texts = List.of("Memory issue", "CPU spike", "Disk full");

    // Act
    CompletableFuture<List<float[]>> future = service.embedBatch(texts);
    List<float[]> embeddings = future.get();

    // Assert
    assertThat(future).isNotNull();
    assertThat(embeddings).isNotNull();
    assertThat(embeddings).hasSize(3);
    assertThat(embeddings.get(0)).hasSize(768);
  }

  /** Test implementation of EmbeddingService for verifying interface contract. */
  private static class TestEmbeddingService implements EmbeddingService {
    @Override
    public CompletableFuture<float[]> embed(String text) {
      float[] embedding = new float[768];
      for (int i = 0; i < 768; i++) {
        embedding[i] = 0.1f;
      }
      return CompletableFuture.completedFuture(embedding);
    }

    @Override
    public CompletableFuture<List<float[]>> embedBatch(List<String> texts) {
      return CompletableFuture.completedFuture(texts.stream().map(t -> new float[768]).toList());
    }

    @Override
    public CompletableFuture<float[]> embedContext(
        com.oracle.runbook.domain.EnrichedContext context) {
      return CompletableFuture.completedFuture(new float[768]);
    }
  }
}
