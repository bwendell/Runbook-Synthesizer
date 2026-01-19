package com.oracle.runbook.infrastructure.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OllamaConfig} configuration record.
 *
 * <p>Tests follow project testing patterns from testing-patterns-java skill.
 */
class OllamaConfigTest {

  @Nested
  @DisplayName("Field validation")
  class FieldValidationTests {

    @Test
    @DisplayName("OllamaConfig should reject null baseUrl")
    void shouldRejectNullBaseUrl() {
      assertThatThrownBy(() -> new OllamaConfig(null, "llama3.2:3b", "nomic-embed-text"))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("baseUrl");
    }

    @Test
    @DisplayName("OllamaConfig should reject null textModel")
    void shouldRejectNullTextModel() {
      assertThatThrownBy(() -> new OllamaConfig("http://localhost:11434", null, "nomic-embed-text"))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("textModel");
    }

    @Test
    @DisplayName("OllamaConfig should reject null embeddingModel")
    void shouldRejectNullEmbeddingModel() {
      assertThatThrownBy(() -> new OllamaConfig("http://localhost:11434", "llama3.2:3b", null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("embeddingModel");
    }
  }

  @Nested
  @DisplayName("Field accessors")
  class FieldAccessorTests {

    @Test
    @DisplayName("baseUrl() should return configured URL")
    void baseUrlShouldReturnConfiguredValue() {
      OllamaConfig config =
          new OllamaConfig("http://localhost:11434", "llama3.2:3b", "nomic-embed-text");

      assertThat(config.baseUrl()).isEqualTo("http://localhost:11434");
    }

    @Test
    @DisplayName("textModel() should return configured text model")
    void textModelShouldReturnConfiguredValue() {
      OllamaConfig config =
          new OllamaConfig("http://localhost:11434", "llama3.2:3b", "nomic-embed-text");

      assertThat(config.textModel()).isEqualTo("llama3.2:3b");
    }

    @Test
    @DisplayName("embeddingModel() should return configured embedding model")
    void embeddingModelShouldReturnConfiguredValue() {
      OllamaConfig config =
          new OllamaConfig("http://localhost:11434", "llama3.2:3b", "nomic-embed-text");

      assertThat(config.embeddingModel()).isEqualTo("nomic-embed-text");
    }
  }

  @Nested
  @DisplayName("Record equality")
  class RecordEqualityTests {

    @Test
    @DisplayName("OllamaConfig records with same values should be equal")
    void recordsWithSameValuesShouldBeEqual() {
      OllamaConfig config1 =
          new OllamaConfig("http://localhost:11434", "llama3.2:3b", "nomic-embed-text");
      OllamaConfig config2 =
          new OllamaConfig("http://localhost:11434", "llama3.2:3b", "nomic-embed-text");

      assertThat(config1).isEqualTo(config2);
      assertThat(config1.hashCode()).isEqualTo(config2.hashCode());
    }

    @Test
    @DisplayName("OllamaConfig records with different values should not be equal")
    void recordsWithDifferentValuesShouldNotBeEqual() {
      OllamaConfig config1 =
          new OllamaConfig("http://localhost:11434", "llama3.2:3b", "nomic-embed-text");
      OllamaConfig config2 =
          new OllamaConfig("http://host:11434", "llama3.2:3b", "nomic-embed-text");

      assertThat(config1).isNotEqualTo(config2);
    }
  }

  @Nested
  @DisplayName("Default configuration")
  class DefaultConfigurationTests {

    @Test
    @DisplayName("Default MVP configuration should use expected values")
    void defaultMvpConfigurationShouldUseExpectedValues() {
      // Default MVP configuration as specified in proposal
      OllamaConfig config =
          new OllamaConfig("http://localhost:11434", "llama3.2:3b", "nomic-embed-text");

      assertThat(config.baseUrl())
          .as("Default Ollama URL should be localhost:11434")
          .isEqualTo("http://localhost:11434");
      assertThat(config.textModel())
          .as("Default text model should be llama3.2:3b")
          .isEqualTo("llama3.2:3b");
      assertThat(config.embeddingModel())
          .as("Default embedding model should be nomic-embed-text")
          .isEqualTo("nomic-embed-text");
    }
  }
}
