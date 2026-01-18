package com.oracle.runbook.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for {@link GenerationConfig} record. */
class GenerationConfigTest {

  @Test
  @DisplayName("GenerationConfig construction with valid values succeeds")
  void constructionWithValidValuesSucceeds() {
    GenerationConfig config = new GenerationConfig(0.7, 2048, Optional.of("gpt-4-turbo"));

    assertThat(config.temperature()).isEqualTo(0.7);
    assertThat(config.maxTokens()).isEqualTo(2048);
    assertThat(config.modelOverride()).isEqualTo(Optional.of("gpt-4-turbo"));
  }

  @Test
  @DisplayName("GenerationConfig allows empty modelOverride")
  void allowsEmptyModelOverride() {
    GenerationConfig config = new GenerationConfig(0.5, 1024, Optional.empty());

    assertThat(config.modelOverride()).isEmpty();
  }

  @ParameterizedTest
  @DisplayName("GenerationConfig throws for temperature outside 0.0-1.0 range")
  @ValueSource(doubles = {-0.1, -1.0, 1.1, 2.0, 100.0})
  void throwsForInvalidTemperature(double temperature) {
    assertThatThrownBy(() -> new GenerationConfig(temperature, 1024, Optional.empty()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("GenerationConfig accepts boundary temperature values")
  void acceptsBoundaryTemperatureValues() {
    assertThatCode(() -> new GenerationConfig(0.0, 1024, Optional.empty()))
        .doesNotThrowAnyException();
    assertThatCode(() -> new GenerationConfig(1.0, 1024, Optional.empty()))
        .doesNotThrowAnyException();
    assertThatCode(() -> new GenerationConfig(0.5, 1024, Optional.empty()))
        .doesNotThrowAnyException();
  }

  @ParameterizedTest
  @DisplayName("GenerationConfig throws for maxTokens <= 0")
  @ValueSource(ints = {0, -1, -100})
  void throwsForInvalidMaxTokens(int maxTokens) {
    assertThatThrownBy(() -> new GenerationConfig(0.5, maxTokens, Optional.empty()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("GenerationConfig accepts valid maxTokens")
  void acceptsValidMaxTokens() {
    assertThatCode(() -> new GenerationConfig(0.5, 1, Optional.empty())).doesNotThrowAnyException();
    assertThatCode(() -> new GenerationConfig(0.5, 4096, Optional.empty()))
        .doesNotThrowAnyException();
    assertThatCode(() -> new GenerationConfig(0.5, 100000, Optional.empty()))
        .doesNotThrowAnyException();
  }
}
