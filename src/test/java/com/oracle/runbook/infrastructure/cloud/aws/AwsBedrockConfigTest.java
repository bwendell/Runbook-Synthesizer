package com.oracle.runbook.infrastructure.cloud.aws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AwsBedrockConfig} configuration record.
 *
 * <p>Tests follow project testing patterns from testing-patterns-java skill.
 */
class AwsBedrockConfigTest {

  @Nested
  @DisplayName("Field validation")
  class FieldValidationTests {

    @Test
    @DisplayName("AwsBedrockConfig should reject null region")
    void shouldRejectNullRegion() {
      assertThatThrownBy(
              () ->
                  new AwsBedrockConfig(
                      null, "anthropic.claude-3-haiku-20240307-v1:0", "cohere.embed-english-v3"))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("region");
    }

    @Test
    @DisplayName("AwsBedrockConfig should reject null textModelId")
    void shouldRejectNullTextModelId() {
      assertThatThrownBy(() -> new AwsBedrockConfig("us-west-2", null, "cohere.embed-english-v3"))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("textModelId");
    }

    @Test
    @DisplayName("AwsBedrockConfig should reject null embeddingModelId")
    void shouldRejectNullEmbeddingModelId() {
      assertThatThrownBy(
              () ->
                  new AwsBedrockConfig("us-west-2", "anthropic.claude-3-haiku-20240307-v1:0", null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("embeddingModelId");
    }
  }

  @Nested
  @DisplayName("Field accessors")
  class FieldAccessorTests {

    @Test
    @DisplayName("region() should return configured region")
    void regionShouldReturnConfiguredValue() {
      AwsBedrockConfig config =
          new AwsBedrockConfig(
              "us-east-1", "anthropic.claude-3-haiku-20240307-v1:0", "cohere.embed-english-v3");

      assertThat(config.region()).isEqualTo("us-east-1");
    }

    @Test
    @DisplayName("textModelId() should return configured text model")
    void textModelIdShouldReturnConfiguredValue() {
      AwsBedrockConfig config =
          new AwsBedrockConfig(
              "us-west-2", "anthropic.claude-3-haiku-20240307-v1:0", "cohere.embed-english-v3");

      assertThat(config.textModelId()).isEqualTo("anthropic.claude-3-haiku-20240307-v1:0");
    }

    @Test
    @DisplayName("embeddingModelId() should return configured embedding model")
    void embeddingModelIdShouldReturnConfiguredValue() {
      AwsBedrockConfig config =
          new AwsBedrockConfig(
              "us-west-2", "anthropic.claude-3-haiku-20240307-v1:0", "cohere.embed-english-v3");

      assertThat(config.embeddingModelId()).isEqualTo("cohere.embed-english-v3");
    }
  }

  @Nested
  @DisplayName("Record equality")
  class RecordEqualityTests {

    @Test
    @DisplayName("AwsBedrockConfig records with same values should be equal")
    void recordsWithSameValuesShouldBeEqual() {
      AwsBedrockConfig config1 =
          new AwsBedrockConfig(
              "us-west-2", "anthropic.claude-3-haiku-20240307-v1:0", "cohere.embed-english-v3");
      AwsBedrockConfig config2 =
          new AwsBedrockConfig(
              "us-west-2", "anthropic.claude-3-haiku-20240307-v1:0", "cohere.embed-english-v3");

      assertThat(config1).isEqualTo(config2);
      assertThat(config1.hashCode()).isEqualTo(config2.hashCode());
    }

    @Test
    @DisplayName("AwsBedrockConfig records with different values should not be equal")
    void recordsWithDifferentValuesShouldNotBeEqual() {
      AwsBedrockConfig config1 =
          new AwsBedrockConfig(
              "us-west-2", "anthropic.claude-3-haiku-20240307-v1:0", "cohere.embed-english-v3");
      AwsBedrockConfig config2 =
          new AwsBedrockConfig(
              "us-east-1", "anthropic.claude-3-haiku-20240307-v1:0", "cohere.embed-english-v3");

      assertThat(config1).isNotEqualTo(config2);
    }
  }
}
