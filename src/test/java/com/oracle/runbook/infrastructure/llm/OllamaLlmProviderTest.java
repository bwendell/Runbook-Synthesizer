package com.oracle.runbook.infrastructure.llm;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.oracle.runbook.domain.GenerationConfig;
import com.oracle.runbook.rag.LlmProvider;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OllamaLlmProvider}.
 *
 * <p>Uses WireMock to mock Ollama HTTP endpoints per testing-patterns-java.
 */
class OllamaLlmProviderTest {

  private WireMockServer wireMock;
  private OllamaConfig config;
  private OllamaLlmProvider provider;

  @BeforeEach
  void setUp() {
    wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    wireMock.start();

    String baseUrl = "http://localhost:" + wireMock.port();
    config = new OllamaConfig(baseUrl, "llama3.2:3b", "nomic-embed-text");
    provider = new OllamaLlmProvider(config);
  }

  @AfterEach
  void tearDown() {
    wireMock.stop();
  }

  @Nested
  @DisplayName("LlmProvider interface implementation")
  class InterfaceImplementationTests {

    @Test
    @DisplayName("OllamaLlmProvider should implement LlmProvider")
    void shouldImplementLlmProvider() {
      assertThat(provider)
          .as("OllamaLlmProvider must implement LlmProvider")
          .isInstanceOf(LlmProvider.class);
    }

    @Test
    @DisplayName("providerId() should return 'ollama'")
    void providerIdShouldReturnOllama() {
      assertThat(provider.providerId()).as("providerId() must return 'ollama'").isEqualTo("ollama");
    }
  }

  @Nested
  @DisplayName("Constructor validation")
  class ConstructorValidationTests {

    @Test
    @DisplayName("Should reject null config")
    void shouldRejectNullConfig() {
      assertThatThrownBy(() -> new OllamaLlmProvider(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("config");
    }
  }

  @Nested
  @DisplayName("generateText()")
  class GenerateTextTests {

    @Test
    @DisplayName("Should generate text from Ollama API")
    void shouldGenerateTextFromOllamaApi() throws Exception {
      // Stub Ollama /api/generate endpoint
      wireMock.stubFor(
          post(urlEqualTo("/api/generate"))
              .willReturn(
                  okJson(
                      """
                      {"response":"Step 1: Check system resources\\nStep 2: Review logs","done":true}
                      """)));

      GenerationConfig genConfig = new GenerationConfig(0.7, 1000, Optional.empty());
      String result = provider.generateText("Generate a checklist", genConfig).get();

      assertThat(result).as("Should return generated text from Ollama").contains("Step 1");

      wireMock.verify(
          postRequestedFor(urlEqualTo("/api/generate"))
              .withRequestBody(containing("\"model\":\"llama3.2:3b\"")));
    }

    @Test
    @DisplayName("Should use model override when provided")
    void shouldUseModelOverrideWhenProvided() throws Exception {
      wireMock.stubFor(
          post(urlEqualTo("/api/generate"))
              .willReturn(
                  okJson(
                      """
                  {"response":"Generated text","done":true}
                  """)));

      GenerationConfig genConfig = new GenerationConfig(0.5, 500, Optional.of("custom-model"));
      provider.generateText("Test prompt", genConfig).get();

      wireMock.verify(
          postRequestedFor(urlEqualTo("/api/generate"))
              .withRequestBody(containing("\"model\":\"custom-model\"")));
    }
  }

  @Nested
  @DisplayName("generateEmbedding()")
  class GenerateEmbeddingTests {

    @Test
    @DisplayName("Should generate embedding from Ollama API")
    void shouldGenerateEmbeddingFromOllamaApi() throws Exception {
      // Stub Ollama /api/embeddings endpoint
      wireMock.stubFor(
          post(urlEqualTo("/api/embeddings"))
              .willReturn(
                  okJson(
                      """
                      {"embedding":[0.1,0.2,0.3,0.4,0.5]}
                      """)));

      float[] embedding = provider.generateEmbedding("Test text").get();

      assertThat(embedding)
          .as("Should return embedding vector from Ollama")
          .hasSize(5)
          .containsExactly(0.1f, 0.2f, 0.3f, 0.4f, 0.5f);

      wireMock.verify(
          postRequestedFor(urlEqualTo("/api/embeddings"))
              .withRequestBody(containing("\"model\":\"nomic-embed-text\"")));
    }
  }

  @Nested
  @DisplayName("generateEmbeddings()")
  class GenerateEmbeddingsTests {

    @Test
    @DisplayName("Should generate batch embeddings from Ollama API")
    void shouldGenerateBatchEmbeddingsFromOllamaApi() throws Exception {
      // Stub for first text
      wireMock.stubFor(
          post(urlEqualTo("/api/embeddings"))
              .withRequestBody(containing("Text 1"))
              .willReturn(
                  okJson(
                      """
                  {"embedding":[0.1,0.2,0.3]}
                  """)));

      // Stub for second text
      wireMock.stubFor(
          post(urlEqualTo("/api/embeddings"))
              .withRequestBody(containing("Text 2"))
              .willReturn(
                  okJson(
                      """
                  {"embedding":[0.4,0.5,0.6]}
                  """)));

      List<float[]> embeddings = provider.generateEmbeddings(List.of("Text 1", "Text 2")).get();

      assertThat(embeddings).as("Should return list of embeddings").hasSize(2);

      assertThat(embeddings.get(0)).containsExactly(0.1f, 0.2f, 0.3f);
      assertThat(embeddings.get(1)).containsExactly(0.4f, 0.5f, 0.6f);
    }

    @Test
    @DisplayName("Should return empty list for empty input")
    void shouldReturnEmptyListForEmptyInput() throws Exception {
      List<float[]> embeddings = provider.generateEmbeddings(List.of()).get();

      assertThat(embeddings).isEmpty();
    }
  }

  @Nested
  @DisplayName("Error handling")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Should throw on API error response for text generation")
    void shouldThrowOnApiErrorResponseForTextGeneration() {
      wireMock.stubFor(
          post(urlEqualTo("/api/generate"))
              .willReturn(serverError().withBody("Internal Server Error")));

      GenerationConfig genConfig = new GenerationConfig(0.7, 1000, Optional.empty());

      assertThatThrownBy(() -> provider.generateText("Test", genConfig).get())
          .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should throw on API error response for embedding")
    void shouldThrowOnApiErrorResponseForEmbedding() {
      wireMock.stubFor(
          post(urlEqualTo("/api/embeddings"))
              .willReturn(serverError().withBody("Internal Server Error")));

      assertThatThrownBy(() -> provider.generateEmbedding("Test text").get())
          .isInstanceOf(Exception.class)
          .hasRootCauseMessage("Ollama API error: 500");
    }

    @Test
    @DisplayName("Should throw on malformed JSON response")
    void shouldThrowOnMalformedJsonResponse() {
      wireMock.stubFor(post(urlEqualTo("/api/generate")).willReturn(okJson("not valid json {{")));

      GenerationConfig genConfig = new GenerationConfig(0.7, 1000, Optional.empty());

      assertThatThrownBy(() -> provider.generateText("Test", genConfig).get())
          .isInstanceOf(Exception.class);
    }
  }

  @Nested
  @DisplayName("OllamaConfig validation")
  class OllamaConfigValidationTests {

    @Test
    @DisplayName("Should reject null baseUrl")
    void shouldRejectNullBaseUrl() {
      assertThatThrownBy(() -> new OllamaConfig(null, "model", "embed"))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("baseUrl");
    }

    @Test
    @DisplayName("Should reject null textModel")
    void shouldRejectNullTextModel() {
      assertThatThrownBy(() -> new OllamaConfig("http://localhost", null, "embed"))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("textModel");
    }

    @Test
    @DisplayName("Should reject null embeddingModel")
    void shouldRejectNullEmbeddingModel() {
      assertThatThrownBy(() -> new OllamaConfig("http://localhost", "model", null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("embeddingModel");
    }

    @Test
    @DisplayName("Should create valid config with all fields")
    void shouldCreateValidConfigWithAllFields() {
      OllamaConfig validConfig = new OllamaConfig("http://localhost:11434", "llama3", "nomic");

      assertThat(validConfig.baseUrl()).isEqualTo("http://localhost:11434");
      assertThat(validConfig.textModel()).isEqualTo("llama3");
      assertThat(validConfig.embeddingModel()).isEqualTo("nomic");
    }
  }
}
