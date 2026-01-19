package com.oracle.runbook.infrastructure.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.runbook.domain.GenerationConfig;
import com.oracle.runbook.rag.LlmProvider;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Ollama implementation of {@link LlmProvider}.
 *
 * <p>Provides LLM capabilities using Ollama REST API for local development. Uses
 * java.net.http.HttpClient for async non-blocking operations compatible with Helidon SE's reactive
 * patterns.
 *
 * <p>Ollama API endpoints: - Text generation: POST /api/generate - Embeddings: POST /api/embeddings
 */
public class OllamaLlmProvider implements LlmProvider {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final OllamaConfig config;
  private final HttpClient httpClient;

  /**
   * Creates a new OllamaLlmProvider.
   *
   * @param config the Ollama configuration
   * @throws NullPointerException if config is null
   */
  public OllamaLlmProvider(OllamaConfig config) {
    this.config = Objects.requireNonNull(config, "config cannot be null");
    this.httpClient = HttpClient.newHttpClient();
  }

  @Override
  public String providerId() {
    return "ollama";
  }

  @Override
  public CompletableFuture<String> generateText(String prompt, GenerationConfig genConfig) {
    String model = genConfig.modelOverride().orElse(config.textModel());

    String requestBody;
    try {
      requestBody =
          OBJECT_MAPPER.writeValueAsString(
              new GenerateRequest(model, prompt, false, new Options(genConfig.temperature())));
    } catch (Exception e) {
      return CompletableFuture.failedFuture(e);
    }

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(config.baseUrl() + "/api/generate"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

    return httpClient
        .sendAsync(request, HttpResponse.BodyHandlers.ofString())
        .thenApply(
            response -> {
              if (response.statusCode() >= 400) {
                throw new RuntimeException("Ollama API error: " + response.statusCode());
              }
              try {
                JsonNode node = OBJECT_MAPPER.readTree(response.body());
                return node.get("response").asText();
              } catch (Exception e) {
                throw new RuntimeException("Failed to parse Ollama response", e);
              }
            });
  }

  @Override
  public CompletableFuture<float[]> generateEmbedding(String text) {
    String requestBody;
    try {
      requestBody =
          OBJECT_MAPPER.writeValueAsString(new EmbeddingRequest(config.embeddingModel(), text));
    } catch (Exception e) {
      return CompletableFuture.failedFuture(e);
    }

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(config.baseUrl() + "/api/embeddings"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

    return httpClient
        .sendAsync(request, HttpResponse.BodyHandlers.ofString())
        .thenApply(
            response -> {
              if (response.statusCode() >= 400) {
                throw new RuntimeException("Ollama API error: " + response.statusCode());
              }
              try {
                JsonNode node = OBJECT_MAPPER.readTree(response.body());
                JsonNode embeddingNode = node.get("embedding");
                float[] embedding = new float[embeddingNode.size()];
                for (int i = 0; i < embeddingNode.size(); i++) {
                  embedding[i] = (float) embeddingNode.get(i).asDouble();
                }
                return embedding;
              } catch (Exception e) {
                throw new RuntimeException("Failed to parse Ollama embedding response", e);
              }
            });
  }

  @Override
  public CompletableFuture<List<float[]>> generateEmbeddings(List<String> texts) {
    if (texts.isEmpty()) {
      return CompletableFuture.completedFuture(List.of());
    }

    // Execute embeddings sequentially to maintain order
    List<CompletableFuture<float[]>> futures = new ArrayList<>();
    for (String text : texts) {
      futures.add(generateEmbedding(text));
    }

    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .thenApply(
            v -> {
              List<float[]> results = new ArrayList<>();
              for (CompletableFuture<float[]> future : futures) {
                results.add(future.join());
              }
              return results;
            });
  }

  // Request/response DTOs for Ollama API
  private record GenerateRequest(String model, String prompt, boolean stream, Options options) {}

  private record Options(double temperature) {}

  private record EmbeddingRequest(String model, String prompt) {}
}
