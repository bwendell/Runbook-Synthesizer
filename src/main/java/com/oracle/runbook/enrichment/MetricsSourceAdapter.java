package com.oracle.runbook.enrichment;

import com.oracle.runbook.domain.MetricSnapshot;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Port interface for fetching metrics from various observability sources.
 *
 * <p>This interface defines the contract for metrics source adapters in the Hexagonal Architecture.
 * Implementations provide concrete integrations with specific metrics backends like OCI Monitoring,
 * Prometheus, etc.
 *
 * <p>All implementations must be non-blocking and return CompletableFuture to support Helidon SE's
 * reactive patterns.
 *
 * @see MetricSnapshot
 */
public interface MetricsSourceAdapter {

  /**
   * Returns the identifier for this metrics source type.
   *
   * <p>Examples: "oci-monitoring", "prometheus"
   *
   * @return the source type identifier, never null
   */
  String sourceType();

  /**
   * Fetches metrics for the specified resource over the given lookback period.
   *
   * <p>The implementation should query the underlying metrics backend and return all relevant
   * metric snapshots for the resource within the time window.
   *
   * @param resourceId the identifier of the resource to fetch metrics for (e.g., OCID for OCI,
   *     instance ID for Prometheus)
   * @param lookback the time duration to look back from now
   * @return a CompletableFuture containing the list of metric snapshots, never null (may complete
   *     with empty list)
   */
  CompletableFuture<List<MetricSnapshot>> fetchMetrics(String resourceId, Duration lookback);
}
