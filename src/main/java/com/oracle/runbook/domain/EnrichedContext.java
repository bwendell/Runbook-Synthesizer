package com.oracle.runbook.domain;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Aggregated context for troubleshooting, combining alert data with resource metadata, recent
 * metrics, logs, and custom properties.
 *
 * @param alert the triggering alert
 * @param resource metadata about the affected resource
 * @param recentMetrics recent metric snapshots for context
 * @param recentLogs recent log entries for context
 * @param customProperties additional context data (GPU status, DB health, etc.)
 */
public record EnrichedContext(
    Alert alert,
    ResourceMetadata resource,
    List<MetricSnapshot> recentMetrics,
    List<LogEntry> recentLogs,
    Map<String, Object> customProperties) {
  /** Compact constructor with validation and defensive copies. */
  public EnrichedContext {
    Objects.requireNonNull(alert, "EnrichedContext alert cannot be null");

    // Defensive copies for immutability
    recentMetrics = recentMetrics != null ? List.copyOf(recentMetrics) : List.of();
    recentLogs = recentLogs != null ? List.copyOf(recentLogs) : List.of();
    customProperties = customProperties != null ? Map.copyOf(customProperties) : Map.of();
  }
}
