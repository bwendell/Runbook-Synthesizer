package com.oracle.runbook.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oracle.runbook.domain.GenerationConfig;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for DefaultEmbeddingService implementation. */
class DefaultEmbeddingServiceTest {

  @Test
  @DisplayName("embed delegates to LlmProvider.generateEmbedding")
  void embed_delegatesToLlmProvider() throws Exception {
    // Arrange
    float[] expectedEmbedding = new float[] {0.1f, 0.2f, 0.3f};
    AtomicReference<String> capturedText = new AtomicReference<>();
    LlmProvider stubProvider =
        new StubLlmProvider() {
          @Override
          public CompletableFuture<float[]> generateEmbedding(String text) {
            capturedText.set(text);
            return CompletableFuture.completedFuture(expectedEmbedding);
          }
        };
    DefaultEmbeddingService embeddingService = new DefaultEmbeddingService(stubProvider);
    String text = "High memory utilization on server";

    // Act
    CompletableFuture<float[]> resultFuture = embeddingService.embed(text);
    float[] result = resultFuture.get();

    // Assert
    assertThat(result).isNotNull();
    assertThat(result).containsExactly(expectedEmbedding);
    assertThat(capturedText.get()).isEqualTo(text);
  }

  @Test
  @DisplayName("embedBatch delegates to LlmProvider.generateEmbeddings")
  void embedBatch_delegatesToLlmProvider() throws Exception {
    // Arrange
    List<float[]> expectedEmbeddings =
        List.of(new float[] {0.1f, 0.2f}, new float[] {0.3f, 0.4f}, new float[] {0.5f, 0.6f});
    AtomicReference<List<String>> capturedTexts = new AtomicReference<>();
    LlmProvider stubProvider =
        new StubLlmProvider() {
          @Override
          public CompletableFuture<List<float[]>> generateEmbeddings(List<String> texts) {
            capturedTexts.set(texts);
            return CompletableFuture.completedFuture(expectedEmbeddings);
          }
        };
    DefaultEmbeddingService embeddingService = new DefaultEmbeddingService(stubProvider);
    List<String> texts = List.of("Memory issue", "CPU spike", "Disk full");

    // Act
    CompletableFuture<List<float[]>> resultFuture = embeddingService.embedBatch(texts);
    List<float[]> result = resultFuture.get();

    // Assert
    assertThat(result).isNotNull();
    assertThat(result).hasSize(3);
    assertThat(result.get(0)).containsExactly(expectedEmbeddings.get(0));
    assertThat(capturedTexts.get()).isEqualTo(texts);
  }

  @Test
  @DisplayName("embed throws NullPointerException for null text")
  void embed_withNullText_throwsNullPointerException() {
    // Arrange
    LlmProvider stubProvider = new StubLlmProvider();
    DefaultEmbeddingService embeddingService = new DefaultEmbeddingService(stubProvider);

    // Act & Assert
    assertThatThrownBy(() -> embeddingService.embed(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("embedBatch throws NullPointerException for null list")
  void embedBatch_withNullList_throwsNullPointerException() {
    // Arrange
    LlmProvider stubProvider = new StubLlmProvider();
    DefaultEmbeddingService embeddingService = new DefaultEmbeddingService(stubProvider);

    // Act & Assert
    assertThatThrownBy(() -> embeddingService.embedBatch(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("embedBatch handles empty list")
  void embedBatch_withEmptyList_returnsEmptyList() throws Exception {
    // Arrange
    LlmProvider stubProvider =
        new StubLlmProvider() {
          @Override
          public CompletableFuture<List<float[]>> generateEmbeddings(List<String> texts) {
            return CompletableFuture.completedFuture(List.of());
          }
        };
    DefaultEmbeddingService embeddingService = new DefaultEmbeddingService(stubProvider);

    // Act
    CompletableFuture<List<float[]>> resultFuture = embeddingService.embedBatch(List.of());
    List<float[]> result = resultFuture.get();

    // Assert
    assertThat(result).isNotNull().isEmpty();
  }

  @Test
  @DisplayName("embedContext delegates to LlmProvider.generateEmbedding")
  void embedContext_delegatesToLlmProvider() throws Exception {
    // Arrange
    float[] expectedEmbedding = new float[] {0.7f, 0.8f, 0.9f};
    AtomicReference<String> capturedText = new AtomicReference<>();
    LlmProvider stubProvider =
        new StubLlmProvider() {
          @Override
          public CompletableFuture<float[]> generateEmbedding(String text) {
            capturedText.set(text);
            return CompletableFuture.completedFuture(expectedEmbedding);
          }
        };
    DefaultEmbeddingService embeddingService = new DefaultEmbeddingService(stubProvider);
    com.oracle.runbook.domain.Alert alert =
        new com.oracle.runbook.domain.Alert(
            "alert-123",
            "High CPU",
            "CPU is at 99%",
            com.oracle.runbook.domain.AlertSeverity.CRITICAL,
            "oci-monitoring",
            java.util.Map.of(),
            java.util.Map.of(),
            java.time.Instant.now(),
            "{}");
    com.oracle.runbook.domain.ResourceMetadata resource =
        new com.oracle.runbook.domain.ResourceMetadata(
            "ocid1.instance.123",
            "web-server-01",
            "compartment-123",
            "VM.Standard2.1",
            "PHX-AD-1",
            java.util.Map.of(),
            java.util.Map.of());
    com.oracle.runbook.domain.EnrichedContext context =
        new com.oracle.runbook.domain.EnrichedContext(
            alert, resource, List.of(), List.of(), java.util.Map.of());

    // Act
    CompletableFuture<float[]> resultFuture = embeddingService.embedContext(context);
    float[] result = resultFuture.get();

    // Assert
    assertThat(result).isNotNull();
    assertThat(result).containsExactly(expectedEmbedding);
    assertThat(capturedText.get()).contains("High CPU");
    assertThat(capturedText.get()).contains("web-server-01");
  }

  @Test
  @DisplayName("constructor throws NullPointerException for null provider")
  void constructor_withNullProvider_throwsNullPointerException() {
    // Act & Assert
    assertThatThrownBy(() -> new DefaultEmbeddingService(null))
        .isInstanceOf(NullPointerException.class);
  }

  /** Stub implementation of LlmProvider for testing. */
  private static class StubLlmProvider implements LlmProvider {
    @Override
    public String providerId() {
      return "test-stub";
    }

    @Override
    public CompletableFuture<String> generateText(String prompt, GenerationConfig config) {
      return CompletableFuture.completedFuture("generated text");
    }

    @Override
    public CompletableFuture<float[]> generateEmbedding(String text) {
      return CompletableFuture.completedFuture(new float[] {0.1f});
    }

    @Override
    public CompletableFuture<List<float[]>> generateEmbeddings(List<String> texts) {
      return CompletableFuture.completedFuture(List.of());
    }
  }
}
