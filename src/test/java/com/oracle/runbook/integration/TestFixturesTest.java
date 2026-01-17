package com.oracle.runbook.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oracle.runbook.domain.Alert;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

/** Tests for TestFixtures utility class. */
class TestFixturesTest {

  @Test
  void loadJson_returnsValidJsonObject() {
    JsonObject json = TestFixtures.loadJson("alerts/high-memory-alert.json");

    assertThat(json).isNotNull();
    assertThat(json.containsKey("title")).isTrue();
  }

  @Test
  void loadAs_deserializesToExpectedType() {
    Alert alert = TestFixtures.loadAs("alerts/high-memory-alert.json", Alert.class);

    assertThat(alert).isNotNull();
    assertThat(alert.title()).isNotBlank();
  }

  @Test
  void loadJson_throwsForMissingFile() {
    assertThatThrownBy(() -> TestFixtures.loadJson("nonexistent.json"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not found");
  }
}
