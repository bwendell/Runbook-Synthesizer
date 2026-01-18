package com.oracle.runbook.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oracle.runbook.domain.*;
import jakarta.json.JsonObject;
import java.util.Objects;
import org.junit.jupiter.api.Test;

/** Tests for TestFixtures utility class. */
class TestFixturesTest {

  @Test
  void loadJson_returnsValidJsonObject() {
    JsonObject json = TestFixtures.loadJson("alerts/high-memory-alert.json");

    Objects.requireNonNull(assertThat(json)).isNotNull();
    Objects.requireNonNull(assertThat(json.containsKey("title"))).isTrue();
  }

  @Test
  void loadAs_deserializesToExpectedType() {
    Alert alert = TestFixtures.loadAs("alerts/high-memory-alert.json", Alert.class);

    assertThat(alert).isNotNull();
    assertThat(alert.title()).isNotBlank();
  }

  @Test
  void loadJson_throwsForMissingFile() {
    Objects.requireNonNull(assertThatThrownBy(() -> TestFixtures.loadJson("nonexistent.json")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not found");
  }
}
