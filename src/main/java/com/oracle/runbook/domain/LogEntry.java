package com.oracle.runbook.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Log event representation from OCI Logging or Grafana Loki.
 *
 * @param id unique identifier for this log entry
 * @param timestamp when the log event occurred
 * @param level the log level (DEBUG, INFO, WARN, ERROR)
 * @param message the log message content
 * @param metadata additional key-value metadata for context
 */
public record LogEntry(
    String id, Instant timestamp, String level, String message, Map<String, String> metadata) {
  /** Compact constructor with validation and defensive copies. */
  public LogEntry {
    Objects.requireNonNull(id, "LogEntry id cannot be null");
    Objects.requireNonNull(timestamp, "LogEntry timestamp cannot be null");

    // Defensive copy for immutability
    metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
  }
}
