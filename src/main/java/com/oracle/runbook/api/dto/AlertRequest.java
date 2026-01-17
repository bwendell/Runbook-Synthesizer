package com.oracle.runbook.api.dto;

import java.util.Map;
import java.util.Objects;

/**
 * Request body for POST /api/v1/alerts endpoint.
 *
 * @param title the alert title/name
 * @param message the detailed alert message
 * @param severity the severity level (CRITICAL, WARNING, INFO)
 * @param sourceService the originating service (e.g., "oci-monitoring")
 * @param dimensions key-value pairs from the alert source
 * @param labels custom labels/tags associated with the alert
 * @param rawPayload the original JSON payload for debugging
 */
public record AlertRequest(
    String title,
    String message,
    String severity,
    String sourceService,
    Map<String, String> dimensions,
    Map<String, String> labels,
    String rawPayload) {

  /** Compact constructor with validation and defensive copies. */
  public AlertRequest {
    Objects.requireNonNull(title, "title is required");
    Objects.requireNonNull(severity, "severity is required");

    // Defensive copies for immutability
    dimensions = dimensions != null ? Map.copyOf(dimensions) : Map.of();
    labels = labels != null ? Map.copyOf(labels) : Map.of();
  }
}
