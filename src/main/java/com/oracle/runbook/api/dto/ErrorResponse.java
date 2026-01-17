package com.oracle.runbook.api.dto;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Standardized error response for API endpoints.
 *
 * @param correlationId unique identifier for tracking this error across systems
 * @param errorCode machine-readable error code (e.g., VALIDATION_ERROR)
 * @param message human-readable error message
 * @param timestamp when the error occurred
 * @param details field-level validation errors or additional context
 */
public record ErrorResponse(
    String correlationId,
    String errorCode,
    String message,
    Instant timestamp,
    Map<String, String> details) {

  /** Compact constructor with validation and defensive copies. */
  public ErrorResponse {
    Objects.requireNonNull(correlationId, "correlationId is required");
    Objects.requireNonNull(errorCode, "errorCode is required");
    Objects.requireNonNull(timestamp, "timestamp is required");

    // Defensive copy for immutability
    details = details != null ? Map.copyOf(details) : Map.of();
  }
}
