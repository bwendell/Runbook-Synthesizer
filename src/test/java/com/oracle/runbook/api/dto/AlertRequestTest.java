package com.oracle.runbook.api.dto;

import static org.junit.jupiter.api.Assertions.*;

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

    assertEquals("Database connection failure", request.title());
    assertEquals("CRITICAL", request.severity());
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

    assertEquals("High CPU", request.title());
    assertEquals("prometheus", request.sourceService());
    assertEquals("ocid1.compartment.xxx", request.dimensions().get("compartmentId"));
    assertEquals("production", request.labels().get("env"));
    assertEquals("{\"raw\": \"payload\"}", request.rawPayload());
  }

  @Test
  void testNullTitle_ThrowsException() {
    assertThrows(
        NullPointerException.class,
        () -> new AlertRequest(null, "msg", "CRITICAL", null, null, null, null));
  }

  @Test
  void testNullSeverity_ThrowsException() {
    assertThrows(
        NullPointerException.class,
        () -> new AlertRequest("title", "msg", null, null, null, null, null));
  }

  @Test
  void testNullDimensions_DefaultsToEmptyMap() {
    var request = new AlertRequest("title", "msg", "INFO", null, null, null, null);
    assertNotNull(request.dimensions());
    assertTrue(request.dimensions().isEmpty());
  }

  @Test
  void testNullLabels_DefaultsToEmptyMap() {
    var request = new AlertRequest("title", "msg", "INFO", null, null, null, null);
    assertNotNull(request.labels());
    assertTrue(request.labels().isEmpty());
  }
}
