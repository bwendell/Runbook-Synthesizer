package com.oracle.runbook.rag;

import static org.junit.jupiter.api.Assertions.*;

import com.oracle.runbook.domain.GenerationConfig;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for the LlmProvider interface contract. */
class LlmProviderTest {

  @Test
  @DisplayName("providerId returns expected identifier")
  void providerId_returnsExpectedIdentifier() {
    // Arrange
    LlmProvider provider = new TestLlmProvider("oci-genai");

    // Act & Assert
    assertEquals("oci-genai", provider.providerId());
  }

  @Test
  @DisplayName("generateText accepts prompt and config, returns CompletableFuture<String>")
  void generateText_acceptsPromptAndConfig_returnsCompletableFuture() throws Exception {
    // Arrange
    LlmProvider provider = new TestLlmProvider("openai");
    String prompt = "Generate a troubleshooting checklist";
    GenerationConfig config = new GenerationConfig(0.7, 1000, Optional.empty());

    // Act
    CompletableFuture<String> future = provider.generateText(prompt, config);
    String result = future.get();

    // Assert
    assertNotNull(future);
    assertNotNull(result);
    assertTrue(result.contains("Step 1"));
  }

  @Test
  @DisplayName("generateEmbedding accepts text, returns CompletableFuture<float[]>")
  void generateEmbedding_acceptsText_returnsCompletableFuture() throws Exception {
    // Arrange
    LlmProvider provider = new TestLlmProvider("ollama");
    String text = "High memory utilization on server";

    // Act
    CompletableFuture<float[]> future = provider.generateEmbedding(text);
    float[] embedding = future.get();

    // Assert
    assertNotNull(future);
    assertNotNull(embedding);
    assertEquals(768, embedding.length);
  }

  @Test
  @DisplayName("generateEmbeddings accepts list of texts, returns CompletableFuture<List<float[]>>")
  void generateEmbeddings_acceptsTextList_returnsCompletableFuture() throws Exception {
    // Arrange
    LlmProvider provider = new TestLlmProvider("oci-genai");
    List<String> texts = List.of("Memory issue", "CPU spike", "Disk full");

    // Act
    CompletableFuture<List<float[]>> future = provider.generateEmbeddings(texts);
    List<float[]> embeddings = future.get();

    // Assert
    assertNotNull(future);
    assertNotNull(embeddings);
    assertEquals(3, embeddings.size());
    assertEquals(768, embeddings.get(0).length);
  }

  /** Test implementation of LlmProvider for verifying interface contract. */
  private static class TestLlmProvider implements LlmProvider {
    private final String providerId;

    TestLlmProvider(String providerId) {
      this.providerId = providerId;
    }

    @Override
    public String providerId() {
      return providerId;
    }

    @Override
    public CompletableFuture<String> generateText(String prompt, GenerationConfig config) {
      return CompletableFuture.completedFuture(
          "Step 1: Check system resources\nStep 2: Review logs");
    }

    @Override
    public CompletableFuture<float[]> generateEmbedding(String text) {
      float[] embedding = new float[768];
      for (int i = 0; i < 768; i++) {
        embedding[i] = 0.1f;
      }
      return CompletableFuture.completedFuture(embedding);
    }

    @Override
    public CompletableFuture<List<float[]>> generateEmbeddings(List<String> texts) {
      return CompletableFuture.completedFuture(
          texts.stream()
              .map(
                  t -> {
                    float[] embedding = new float[768];
                    for (int i = 0; i < 768; i++) {
                      embedding[i] = 0.1f;
                    }
                    return embedding;
                  })
              .toList());
    }
  }
}
