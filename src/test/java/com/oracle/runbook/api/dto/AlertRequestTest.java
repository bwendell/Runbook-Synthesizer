package com.oracle.runbook.api.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** Unit tests for AlertRequest DTO. */
class AlertRequestTest {

  @Test
  void testCreation_WithRequiredFields() {
    var request =
        new AlertRequest(
            "Database connection failure",
            "Connection pool exhausted",
            "CRITICAL",
            "oci-monitoring",
            Map.of(),
            Map.of(),
            null);

    assertThat(request.title()).isEqualTo("Database connection failure");
    assertThat(request.severity()).isEqualTo("CRITICAL");
  }

  @Test
  void testCreation_WithAllFields() {
    var dimensions = Map.of("compartmentId", "ocid1.compartment.xxx");
    var labels = Map.of("env", "production");

    var request =
        new AlertRequest(
            "High CPU",
            "CPU usage above 90%",
            "WARNING",
            "prometheus",
            dimensions,
            labels,
            "{\"raw\": \"payload\"}");

    assertThat(request.title()).isEqualTo("High CPU");
    assertThat(request.sourceService()).isEqualTo("prometheus");
    assertThat(request.dimensions().get("compartmentId")).isEqualTo("ocid1.compartment.xxx");
    assertThat(request.labels().get("env")).isEqualTo("production");
    assertThat(request.rawPayload()).isEqualTo("{\"raw\": \"payload\"}");
  }

  @Test
  void testNullTitle_ThrowsException() {
    assertThatThrownBy(() -> new AlertRequest(null, "msg", "CRITICAL", null, null, null, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void testNullSeverity_ThrowsException() {
    assertThatThrownBy(() -> new AlertRequest("title", "msg", null, null, null, null, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void testNullDimensions_DefaultsToEmptyMap() {
    var request = new AlertRequest("title", "msg", "INFO", null, null, null, null);
    assertThat(request.dimensions()).isNotNull().isEmpty();
  }

  @Test
  void testNullLabels_DefaultsToEmptyMap() {
    var request = new AlertRequest("title", "msg", "INFO", null, null, null, null);
    assertThat(request.labels()).isNotNull().isEmpty();
  }
}
