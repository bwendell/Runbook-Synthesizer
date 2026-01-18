package com.oracle.runbook.api.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    assertThat(response.correlationId()).isEqualTo("correlation-123");
    assertThat(response.errorCode()).isEqualTo("VALIDATION_ERROR");
    assertThat(response.message()).isEqualTo("Request validation failed");
    assertThat(response.timestamp()).isNotNull();
    assertThat(response.details()).isEmpty();
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

    assertThat(response.details()).hasSize(2);
    assertThat(response.details().get("title")).isEqualTo("cannot be null");
  }

  @Test
  void testNullCorrelationId_ThrowsException() {
    assertThatThrownBy(() -> new ErrorResponse(null, "ERROR", "message", Instant.now(), Map.of()))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void testNullErrorCode_ThrowsException() {
    assertThatThrownBy(() -> new ErrorResponse("id", null, "message", Instant.now(), Map.of()))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void testNullTimestamp_ThrowsException() {
    assertThatThrownBy(() -> new ErrorResponse("id", "ERROR", "message", null, Map.of()))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void testNullDetails_DefaultsToEmptyMap() {
    var response = new ErrorResponse("id", "ERROR", "msg", Instant.now(), null);
    assertThat(response.details()).isNotNull().isEmpty();
  }
}
