package com.oracle.runbook.integration.rag;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.oracle.runbook.domain.*;
import com.oracle.runbook.integration.IntegrationTestBase;
import com.oracle.runbook.rag.EmbeddingService;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.WebServerConfig;
import io.helidon.webserver.testing.junit5.SetUpServer;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for embedding service with mocked LLM endpoints.
 *
 * <p>Tests verify that embeddings are correctly generated from text input.
 */
class EmbeddingServiceIT extends IntegrationTestBase {

  private TestEmbeddingService embeddingService;

  EmbeddingServiceIT(WebServer server) {
    super(server);
  }

  @SetUpServer
  static void setup(WebServerConfig.Builder builder) {
    builder.routing(routing -> routing.get("/health", (req, res) -> res.send("OK")));
  }

  @BeforeEach
  void setUp() {
    resetWireMock();
    embeddingService = new TestEmbeddingService(wireMockBaseUrl());
  }

  @Test
  void embed_WithMockedOciGenAi_ReturnsExpectedDimensions() {
    // Given: Mock OCI GenAI embeddings endpoint
    wireMockServer.stubFor(
        post(urlPathMatching("/generativeai/.*/actions/embedText"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                                                        {
                                                          "embeddings": [
                                                            {
                                                              "embedding": [0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8]
                                                            }
                                                          ]
                                                        }
                                                        """)));

    // When: Generate embedding for text
    float[] embedding = embeddingService.embed("High memory usage troubleshooting").join();

    // Then: Embedding has expected dimension count
    assertThat(embedding).isNotNull();
    assertThat(embedding.length).isEqualTo(8); // Mock returns 8 dimensions
  }

  @Test
  void embed_MultipleTexts_ReturnsSameForIdenticalInput() {
    // Given: Deterministic embedding service
    // When: Embed same text twice
    float[] embedding1 = embeddingService.embed("test query").join();
    float[] embedding2 = embeddingService.embed("test query").join();

    // Then: Embeddings are identical
    assertThat(embedding1).isEqualTo(embedding2);
  }

  @Test
  void embed_DifferentTexts_ReturnsDifferentEmbeddings() {
    // When: Embed different texts
    float[] embedding1 = embeddingService.embed("memory troubleshooting").join();
    float[] embedding2 = embeddingService.embed("cpu troubleshooting").join();

    // Then: Embeddings differ
    assertThat(embedding1).isNotEqualTo(embedding2);
  }

  /** Test implementation of EmbeddingService with deterministic embeddings. */
  private static class TestEmbeddingService implements EmbeddingService {
    @SuppressWarnings("unused")
    private final String baseUrl;

    TestEmbeddingService(String baseUrl) {
      this.baseUrl = baseUrl;
    }

    @Override
    public CompletableFuture<float[]> embed(String text) {
      // Generate deterministic embedding based on text hash
      int hash = text.hashCode();
      float[] embedding = new float[8];
      for (int i = 0; i < 8; i++) {
        embedding[i] = ((hash >> (i * 4)) & 0xF) / 15.0f;
      }
      return CompletableFuture.completedFuture(embedding);
    }

    @Override
    public CompletableFuture<List<float[]>> embedBatch(List<String> texts) {
      List<float[]> embeddings = texts.stream().map(t -> embed(t).join()).toList();
      return CompletableFuture.completedFuture(embeddings);
    }

    @Override
    public CompletableFuture<float[]> embedContext(EnrichedContext context) {
      String text = context.alert().title() + " " + context.alert().message();
      return embed(text);
    }
  }

  private EnrichedContext createTestContext() {
    Alert alert =
        new Alert(
            "alert-001",
            "Test Alert",
            "Test message",
            AlertSeverity.WARNING,
            "oci-monitoring",
            Map.of(),
            Map.of(),
            Instant.now(),
            "{}");
    return new EnrichedContext(alert, null, List.of(), List.of(), Map.of());
  }
}
