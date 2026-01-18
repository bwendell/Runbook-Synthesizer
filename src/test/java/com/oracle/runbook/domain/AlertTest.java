package com.oracle.runbook.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link Alert} record. */
class AlertTest {

  @Test
  @DisplayName("Alert construction with all required fields succeeds")
  void constructionWithAllFieldsSucceeds() {
    Instant now = Instant.now();
    Map<String, String> dimensions = Map.of("compartmentId", "ocid1.compartment.oc1..xxx");
    Map<String, String> labels = Map.of("env", "production");

    Alert alert =
        new Alert(
            "alert-123",
            "High CPU Usage",
            "CPU usage exceeded 90% threshold",
            AlertSeverity.CRITICAL,
            "oci-monitoring",
            dimensions,
            labels,
            now,
            "{\"raw\": \"payload\"}");

    assertThat(alert.id()).isEqualTo("alert-123");
    assertThat(alert.title()).isEqualTo("High CPU Usage");
    assertThat(alert.message()).isEqualTo("CPU usage exceeded 90% threshold");
    assertThat(alert.severity()).isEqualTo(AlertSeverity.CRITICAL);
    assertThat(alert.sourceService()).isEqualTo("oci-monitoring");
    assertThat(alert.dimensions()).isEqualTo(dimensions);
    assertThat(alert.labels()).isEqualTo(labels);
    assertThat(alert.timestamp()).isEqualTo(now);
    assertThat(alert.rawPayload()).isEqualTo("{\"raw\": \"payload\"}");
  }

  @Test
  @DisplayName("Alert throws NullPointerException for null id")
  void throwsForNullId() {
    assertThatThrownBy(
            () ->
                new Alert(
                    null,
                    "Title",
                    "Message",
                    AlertSeverity.WARNING,
                    "source",
                    Map.of(),
                    Map.of(),
                    Instant.now(),
                    "{}"))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("Alert throws NullPointerException for null title")
  void throwsForNullTitle() {
    assertThatThrownBy(
            () ->
                new Alert(
                    "id",
                    null,
                    "Message",
                    AlertSeverity.WARNING,
                    "source",
                    Map.of(),
                    Map.of(),
                    Instant.now(),
                    "{}"))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("Alert dimensions map is immutable")
  void dimensionsMapIsImmutable() {
    Map<String, String> mutableDimensions = new HashMap<>();
    mutableDimensions.put("key", "value");

    Alert alert =
        new Alert(
            "id",
            "Title",
            "Message",
            AlertSeverity.INFO,
            "source",
            mutableDimensions,
            Map.of(),
            Instant.now(),
            "{}");

    // Modifying original should not affect alert
    mutableDimensions.put("newKey", "newValue");
    assertThat(alert.dimensions()).doesNotContainKey("newKey");

    // Alert's dimensions should be unmodifiable
    assertThatThrownBy(() -> alert.dimensions().put("another", "value"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("Alert labels map is immutable")
  void labelsMapIsImmutable() {
    Map<String, String> mutableLabels = new HashMap<>();
    mutableLabels.put("env", "prod");

    Alert alert =
        new Alert(
            "id",
            "Title",
            "Message",
            AlertSeverity.INFO,
            "source",
            Map.of(),
            mutableLabels,
            Instant.now(),
            "{}");

    // Modifying original should not affect alert
    mutableLabels.put("newKey", "newValue");
    assertThat(alert.labels()).doesNotContainKey("newKey");

    // Alert's labels should be unmodifiable
    assertThatThrownBy(() -> alert.labels().put("another", "value"))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
