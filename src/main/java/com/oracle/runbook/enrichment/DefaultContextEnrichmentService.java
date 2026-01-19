package com.oracle.runbook.enrichment;

import com.oracle.runbook.domain.*;
import com.oracle.runbook.infrastructure.cloud.ComputeMetadataAdapter;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Default implementation of {@link ContextEnrichmentService} that orchestrates cloud adapters to
 * enrich alerts with infrastructure context.
 *
 * <p>This service fetches metadata, metrics, and logs in parallel using {@link
 * CompletableFuture#allOf(CompletableFuture[])} for efficiency. It handles partial failures
 * gracefully - if any adapter fails, the remaining data is still returned with the failed component
 * set to empty/null.
 *
 * @see ContextEnrichmentService
 * @see ComputeMetadataAdapter
 * @see MetricsSourceAdapter
 * @see LogSourceAdapter
 */
public class DefaultContextEnrichmentService implements ContextEnrichmentService {

  private static final Logger LOGGER =
      System.getLogger(DefaultContextEnrichmentService.class.getName());
  private static final Duration DEFAULT_LOOKBACK = Duration.ofMinutes(15);

  private final ComputeMetadataAdapter metadataAdapter;
  private final MetricsSourceAdapter metricsAdapter;
  private final LogSourceAdapter logsAdapter;

  /**
   * Creates a new DefaultContextEnrichmentService with the specified adapters.
   *
   * @param metadataAdapter adapter for fetching compute instance metadata
   * @param metricsAdapter adapter for fetching metrics from monitoring source
   * @param logsAdapter adapter for fetching logs from log source
   * @throws NullPointerException if any adapter is null
   */
  public DefaultContextEnrichmentService(
      ComputeMetadataAdapter metadataAdapter,
      MetricsSourceAdapter metricsAdapter,
      LogSourceAdapter logsAdapter) {
    this.metadataAdapter =
        Objects.requireNonNull(metadataAdapter, "metadataAdapter cannot be null");
    this.metricsAdapter = Objects.requireNonNull(metricsAdapter, "metricsAdapter cannot be null");
    this.logsAdapter = Objects.requireNonNull(logsAdapter, "logsAdapter cannot be null");
  }

  @Override
  public CompletableFuture<EnrichedContext> enrich(Alert alert) {
    Objects.requireNonNull(alert, "alert cannot be null");

    String resourceId = extractResourceId(alert);

    // Launch all three adapter calls in parallel
    CompletableFuture<ResourceMetadata> metadataFuture = fetchMetadata(resourceId);
    CompletableFuture<List<MetricSnapshot>> metricsFuture = fetchMetrics(resourceId);
    CompletableFuture<List<LogEntry>> logsFuture = fetchLogs(resourceId);

    // Wait for all to complete and combine results
    return CompletableFuture.allOf(metadataFuture, metricsFuture, logsFuture)
        .thenApply(
            ignored -> {
              ResourceMetadata metadata = safeGet(metadataFuture, null);
              List<MetricSnapshot> metrics = safeGet(metricsFuture, Collections.emptyList());
              List<LogEntry> logs = safeGet(logsFuture, Collections.emptyList());

              return new EnrichedContext(alert, metadata, metrics, logs, Map.of());
            });
  }

  /**
   * Extracts the resource ID from the alert dimensions. Looks for common dimension keys like
   * "resourceId", "instanceId", etc.
   */
  private String extractResourceId(Alert alert) {
    Map<String, String> dimensions = alert.dimensions();

    // Check common dimension keys in order of preference
    if (dimensions.containsKey("resourceId")) {
      return dimensions.get("resourceId");
    }
    if (dimensions.containsKey("instanceId")) {
      return dimensions.get("instanceId");
    }
    if (dimensions.containsKey("InstanceId")) {
      return dimensions.get("InstanceId");
    }
    if (dimensions.containsKey("resource_id")) {
      return dimensions.get("resource_id");
    }

    // Fallback: return alert ID if no resource identifier found
    LOGGER.log(
        Level.WARNING,
        "No resource ID found in alert dimensions for alert {0}, using alert ID as fallback",
        alert.id());
    return alert.id();
  }

  /** Fetches compute instance metadata with graceful failure handling. */
  private CompletableFuture<ResourceMetadata> fetchMetadata(String resourceId) {
    return metadataAdapter
        .getInstance(resourceId)
        .thenApply(opt -> opt.orElse(null))
        .exceptionally(
            ex -> {
              LOGGER.log(
                  Level.WARNING,
                  "Failed to fetch metadata for resource {0}: {1}",
                  new Object[] {resourceId, ex.getMessage()});
              return null;
            });
  }

  /** Fetches recent metrics with graceful failure handling. */
  private CompletableFuture<List<MetricSnapshot>> fetchMetrics(String resourceId) {
    return metricsAdapter
        .fetchMetrics(resourceId, DEFAULT_LOOKBACK)
        .exceptionally(
            ex -> {
              LOGGER.log(
                  Level.WARNING,
                  "Failed to fetch metrics for resource {0}: {1}",
                  new Object[] {resourceId, ex.getMessage()});
              return Collections.emptyList();
            });
  }

  /** Fetches recent logs with graceful failure handling. */
  private CompletableFuture<List<LogEntry>> fetchLogs(String resourceId) {
    return logsAdapter
        .fetchLogs(resourceId, DEFAULT_LOOKBACK, null)
        .exceptionally(
            ex -> {
              LOGGER.log(
                  Level.WARNING,
                  "Failed to fetch logs for resource {0}: {1}",
                  new Object[] {resourceId, ex.getMessage()});
              return Collections.emptyList();
            });
  }

  /** Safely retrieves a value from a completed future, returning a default on failure. */
  private <T> T safeGet(CompletableFuture<T> future, T defaultValue) {
    try {
      return future.getNow(defaultValue);
    } catch (Exception ex) {
      LOGGER.log(Level.DEBUG, "Error getting future value, using default", ex);
      return defaultValue;
    }
  }
}
