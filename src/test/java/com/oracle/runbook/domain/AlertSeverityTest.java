package com.oracle.runbook.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for {@link AlertSeverity} enum. */
class AlertSeverityTest {

  @Test
  @DisplayName("All severity values exist")
  void allSeverityValuesExist() {
    assertThat(AlertSeverity.values()).hasSize(3);
    assertThat(AlertSeverity.CRITICAL).isNotNull();
    assertThat(AlertSeverity.WARNING).isNotNull();
    assertThat(AlertSeverity.INFO).isNotNull();
  }

  @ParameterizedTest
  @DisplayName("fromString handles case-insensitive input")
  @ValueSource(strings = {"critical", "CRITICAL", "Critical", "CrItIcAl"})
  void fromStringHandlesCaseInsensitiveInput(String input) {
    assertThat(AlertSeverity.fromString(input)).isEqualTo(AlertSeverity.CRITICAL);
  }

  @Test
  @DisplayName("fromString returns WARNING for warning strings")
  void fromStringReturnsWarning() {
    assertThat(AlertSeverity.fromString("warning")).isEqualTo(AlertSeverity.WARNING);
    assertThat(AlertSeverity.fromString("WARNING")).isEqualTo(AlertSeverity.WARNING);
  }

  @Test
  @DisplayName("fromString returns INFO for info strings")
  void fromStringReturnsInfo() {
    assertThat(AlertSeverity.fromString("info")).isEqualTo(AlertSeverity.INFO);
    assertThat(AlertSeverity.fromString("INFO")).isEqualTo(AlertSeverity.INFO);
  }

  @Test
  @DisplayName("fromString throws IllegalArgumentException for unknown values")
  void fromStringThrowsForUnknownValues() {
    assertThatThrownBy(() -> AlertSeverity.fromString("unknown"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> AlertSeverity.fromString(""))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> AlertSeverity.fromString(null))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
