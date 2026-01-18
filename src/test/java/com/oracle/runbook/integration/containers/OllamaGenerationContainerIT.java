package com.oracle.runbook.integration.containers;

import static org.assertj.core.api.Assertions.assertThat;

import com.oracle.runbook.integration.OllamaContainerSupport;
import com.oracle.runbook.integration.OracleContainerBase;
import io.helidon.http.HeaderNames;
import io.helidon.webclient.api.HttpClientResponse;
import io.helidon.webclient.api.WebClient;
import jakarta.json.Json;
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
 * Integration tests for Ollama text generation.
 *
 * <p>These tests verify that the Ollama container can generate text using real LLM models.
 *
 * <p>Requires Docker to be running. Enable with: {@code -Dtest.use.containers=true}
 *
 * <p>Note: First run may take several minutes to download the generation model.
 */
@EnabledIfSystemProperty(named = "test.use.containers", matches = "true")
@Tag("container")
class OllamaGenerationContainerIT extends OracleContainerBase {

  private static GenericContainer<?> ollama;
  private static WebClient ollamaClient;

  // Use a small model for faster tests
  private static final String GENERATION_MODEL = "llama3.2:1b";

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
            .readTimeout(Duration.ofMinutes(10)) // Model download and generation can take time
            .build();

    // Pull the generation model (may take time on first run)
    pullModel(GENERATION_MODEL);
  }

  @AfterAll
  static void stopOllamaContainer() {
    if (ollama != null) {
      ollama.stop();
    }
  }

  @Test
  @DisplayName("Task 3.2: Should generate structured checklist steps from prompt")
  void generateText_WithChecklistPrompt_ReturnsStructuredSteps() {
    // Given: A prompt requesting structured troubleshooting steps
    String prompt =
        """
        You are a troubleshooting assistant. Given the following context, generate a \
        numbered checklist of steps to investigate the issue.

        Context:
        - Alert: High Memory Utilization (92%)
        - Server: prod-app-server-01
        - Runbook excerpt: Use 'free -h' to check available memory. Monitor with 'top -o %MEM'.

        Generate exactly 3 numbered steps. Be concise.
        """;

    // When: Call Ollama generation API
    String requestBody =
        Json.createObjectBuilder()
            .add("model", GENERATION_MODEL)
            .add("prompt", prompt)
            .add("stream", false)
            .build()
            .toString();

    try (HttpClientResponse response =
        ollamaClient
            .post("/api/generate")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit(requestBody)) {

      // Then: Response is successful
      assertThat(response.status().code()).isEqualTo(200);

      String responseBody = response.as(String.class);
      JsonObject json;
      try (JsonReader reader = Json.createReader(new StringReader(responseBody))) {
        json = reader.readObject();
      }

      // Then: Response contains generated text
      String generatedText = json.getString("response", "");
      assertThat(generatedText).isNotEmpty();

      // Then: Generated text contains numbered steps (Step 1, Step 2, etc. or 1., 2., etc.)
      assertThat(generatedText)
          .satisfiesAnyOf(
              text -> assertThat(text).containsIgnoringCase("step"),
              text -> assertThat(text).containsPattern("\\d\\.\\s"),
              text -> assertThat(text).containsPattern("\\d\\)\\s"));
    }
  }

  @Test
  @DisplayName("Should include context from provided runbook content")
  void generateText_WithRunbookContext_IncludesRelevantContent() {
    // Given: A prompt with specific runbook content
    String prompt =
        """
        Based on this runbook section, provide a brief summary:

        Runbook: Memory Troubleshooting
        Steps:
        1. Check current memory with 'free -h'
        2. Identify memory-hungry processes with 'top -o %MEM'
        3. Clear page cache if safe: 'sync; echo 3 > /proc/sys/vm/drop_caches'

        Summarize in one sentence.
        """;

    // When: Generate response
    String response = generateText(prompt);

    // Then: Response references the runbook content
    assertThat(response.toLowerCase())
        .satisfiesAnyOf(
            text -> assertThat(text).contains("memory"),
            text -> assertThat(text).contains("free"),
            text -> assertThat(text).contains("top"),
            text -> assertThat(text).contains("process"));
  }

  @Test
  @DisplayName("Should respect generation parameters")
  void generateText_WithMaxTokens_RespectsLimit() {
    // Given: A prompt that could generate long output
    String prompt = "List 10 Linux commands for system monitoring. Be very detailed.";

    // When: Request with strict max_tokens
    String requestBody =
        Json.createObjectBuilder()
            .add("model", GENERATION_MODEL)
            .add("prompt", prompt)
            .add("stream", false)
            .add("options", Json.createObjectBuilder().add("num_predict", 50)) // Limit tokens
            .build()
            .toString();

    try (HttpClientResponse response =
        ollamaClient
            .post("/api/generate")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit(requestBody)) {

      assertThat(response.status().code()).isEqualTo(200);

      String responseBody = response.as(String.class);
      JsonObject json;
      try (JsonReader reader = Json.createReader(new StringReader(responseBody))) {
        json = reader.readObject();
      }

      String generatedText = json.getString("response", "");
      // Then: Response is relatively short (token limit applied)
      // Note: Token != word, but 50 tokens ~= 35-50 words roughly
      assertThat(generatedText.split("\\s+").length).isLessThan(200);
    }
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

  private String generateText(String prompt) {
    String requestBody =
        Json.createObjectBuilder()
            .add("model", GENERATION_MODEL)
            .add("prompt", prompt)
            .add("stream", false)
            .build()
            .toString();

    try (HttpClientResponse response =
        ollamaClient
            .post("/api/generate")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit(requestBody)) {

      assertThat(response.status().code()).isEqualTo(200);

      String responseBody = response.as(String.class);
      JsonObject json;
      try (JsonReader reader = Json.createReader(new StringReader(responseBody))) {
        json = reader.readObject();
      }
      return json.getString("response", "");
    }
  }
}
