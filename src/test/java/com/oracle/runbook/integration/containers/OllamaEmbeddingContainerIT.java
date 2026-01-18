package com.oracle.runbook.integration.containers;

import static org.assertj.core.api.Assertions.assertThat;

import com.oracle.runbook.integration.OllamaContainerSupport;
import com.oracle.runbook.integration.OracleContainerBase;
import io.helidon.http.HeaderNames;
import io.helidon.webclient.api.HttpClientResponse;
import io.helidon.webclient.api.WebClient;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.io.StringReader;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.testcontainers.containers.GenericContainer;

/**
 * Integration tests for Ollama embedding generation.
 *
 * <p>These tests verify that the Ollama container can generate embeddings using real models.
 *
 * <p>Requires Docker to be running. Enable with: {@code -Dtest.use.containers=true}
 *
 * <p>Note: First run may take several minutes to download the embedding model.
 */
@EnabledIfSystemProperty(named = "test.use.containers", matches = "true")
@Tag("container")
class OllamaEmbeddingContainerIT extends OracleContainerBase {

  private static GenericContainer<?> ollama;
  private static WebClient ollamaClient;

  // Use a smaller embedding model for faster tests
  private static final String EMBEDDING_MODEL = "nomic-embed-text";
  private static final int EXPECTED_DIMENSIONS = 768;

  @BeforeAll
  static void startOllamaContainer() throws Exception {
    // Reuse the shared network from OracleContainerBase
    ollama = OllamaContainerSupport.createContainer(getSharedNetwork());
    ollama.start();

    String ollamaUrl = OllamaContainerSupport.getOllamaUrl(ollama);
    ollamaClient =
        WebClient.builder()
            .baseUri(ollamaUrl)
            .connectTimeout(Duration.ofSeconds(30))
            .readTimeout(Duration.ofMinutes(5)) // Model download can take time
            .build();

    // Pull the embedding model (may take time on first run)
    pullModel(EMBEDDING_MODEL);
  }

  @AfterAll
  static void stopOllamaContainer() {
    if (ollama != null) {
      ollama.stop();
    }
  }

  @Test
  @DisplayName("Task 3.1: Should generate 768-dimensional embedding from text")
  void embed_WithNomicModel_Returns768Dimensions() {
    // Given: Sample text to embed
    String text = "High memory utilization troubleshooting guide";

    // When: Call Ollama embedding API
    String requestBody =
        Json.createObjectBuilder()
            .add("model", EMBEDDING_MODEL)
            .add("input", text)
            .build()
            .toString();

    try (HttpClientResponse response =
        ollamaClient
            .post("/api/embed")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit(requestBody)) {

      // Then: Response is successful
      assertThat(response.status().code()).isEqualTo(200);

      String responseBody = response.as(String.class);
      JsonObject json;
      try (JsonReader reader = Json.createReader(new StringReader(responseBody))) {
        json = reader.readObject();
      }

      // Then: Embedding has expected dimensions
      JsonArray embeddingsArray = json.getJsonArray("embeddings");
      assertThat(embeddingsArray).isNotNull().hasSize(1);

      JsonArray embedding = embeddingsArray.getJsonArray(0);
      assertThat(embedding).hasSize(EXPECTED_DIMENSIONS);

      // Then: Embedding values are normalized floats
      for (int i = 0; i < Math.min(10, embedding.size()); i++) {
        double value = embedding.getJsonNumber(i).doubleValue();
        assertThat(value).isBetween(-1.0, 1.0);
      }
    }
  }

  @Test
  @DisplayName("Should generate different embeddings for semantically different texts")
  void embed_DifferentTexts_ProducesDifferentEmbeddings() {
    // Given: Two semantically different texts
    String memoryText = "Memory troubleshooting with free -h and top commands";
    String networkText = "Network connectivity testing with ping and traceroute";

    // When: Generate embeddings for both
    float[] memoryEmbedding = generateEmbedding(memoryText);
    float[] networkEmbedding = generateEmbedding(networkText);

    // Then: Embeddings should be different
    assertThat(memoryEmbedding).isNotEqualTo(networkEmbedding);

    // And: Cosine similarity should be less than perfect (< 1.0)
    double similarity = cosineSimilarity(memoryEmbedding, networkEmbedding);
    assertThat(similarity).isLessThan(0.95); // Not identical
    assertThat(similarity).isGreaterThan(0.0); // But somewhat related (both tech content)
  }

  @Test
  @DisplayName("Should generate similar embeddings for semantically similar texts")
  void embed_SimilarTexts_ProducesSimilarEmbeddings() {
    // Given: Two semantically similar texts
    String text1 = "Check memory usage with free -h command";
    String text2 = "Monitor RAM usage using the free command with human-readable format";

    // When: Generate embeddings
    float[] embedding1 = generateEmbedding(text1);
    float[] embedding2 = generateEmbedding(text2);

    // Then: Embeddings should be similar (high cosine similarity)
    double similarity = cosineSimilarity(embedding1, embedding2);
    assertThat(similarity).isGreaterThan(0.7); // High similarity for related content
  }

  // --- Helper Methods ---

  private static void pullModel(String model) {
    String requestBody = Json.createObjectBuilder().add("model", model).build().toString();

    try (HttpClientResponse response =
        ollamaClient
            .post("/api/pull")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit(requestBody)) {
      // Pull is a streaming response, just consume it
      response.as(String.class);
    }
  }

  private float[] generateEmbedding(String text) {
    String requestBody =
        Json.createObjectBuilder()
            .add("model", EMBEDDING_MODEL)
            .add("input", text)
            .build()
            .toString();

    try (HttpClientResponse response =
        ollamaClient
            .post("/api/embed")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit(requestBody)) {

      assertThat(response.status().code()).isEqualTo(200);

      String responseBody = response.as(String.class);
      JsonObject json;
      try (JsonReader reader = Json.createReader(new StringReader(responseBody))) {
        json = reader.readObject();
      }

      JsonArray embedding = json.getJsonArray("embeddings").getJsonArray(0);
      float[] result = new float[embedding.size()];
      for (int i = 0; i < embedding.size(); i++) {
        result[i] = (float) embedding.getJsonNumber(i).doubleValue();
      }
      return result;
    }
  }

  private double cosineSimilarity(float[] a, float[] b) {
    double dotProduct = 0.0;
    double normA = 0.0;
    double normB = 0.0;
    for (int i = 0; i < Math.min(a.length, b.length); i++) {
      dotProduct += a[i] * b[i];
      normA += a[i] * a[i];
      normB += b[i] * b[i];
    }
    return (normA == 0 || normB == 0) ? 0.0 : dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
  }
}
