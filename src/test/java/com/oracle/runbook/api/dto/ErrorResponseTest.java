package com.oracle.runbook.api.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Unit tests for ErrorResponse DTO. */
class ErrorResponseTest {

  @Test
  void testCreation_WithRequiredFields() {
    var response =
        new ErrorResponse(
            "correlation-123",
            "VALIDATION_ERROR",
            "Request validation failed",
            Instant.now(),
            Map.of());

    assertEquals("correlation-123", response.correlationId());
    assertEquals("VALIDATION_ERROR", response.errorCode());
    assertEquals("Request validation failed", response.message());
    assertNotNull(response.timestamp());
    assertTrue(response.details().isEmpty());
  }

  @Test
  void testCreation_WithValidationDetails() {
    var details =
        Map.of(
            "title", "cannot be null",
            "severity", "must be one of: CRITICAL, WARNING, INFO");

    var response =
        new ErrorResponse(
            "correlation-456",
            "VALIDATION_ERROR",
            "Multiple validation errors",
            Instant.now(),
            details);

    assertEquals(2, response.details().size());
    assertEquals("cannot be null", response.details().get("title"));
  }

  @Test
  void testNullCorrelationId_ThrowsException() {
    assertThrows(
        NullPointerException.class,
        () -> new ErrorResponse(null, "ERROR", "message", Instant.now(), Map.of()));
  }

  @Test
  void testNullErrorCode_ThrowsException() {
    assertThrows(
        NullPointerException.class,
        () -> new ErrorResponse("id", null, "message", Instant.now(), Map.of()));
  }

  @Test
  void testNullTimestamp_ThrowsException() {
    assertThrows(
        NullPointerException.class,
        () -> new ErrorResponse("id", "ERROR", "message", null, Map.of()));
  }

  @Test
  void testNullDetails_DefaultsToEmptyMap() {
    var response = new ErrorResponse("id", "ERROR", "msg", Instant.now(), null);
    assertNotNull(response.details());
    assertTrue(response.details().isEmpty());
  }
}
