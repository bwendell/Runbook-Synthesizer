package com.oracle.runbook.integration.stubs;

import com.oracle.runbook.domain.MetricSnapshot;
import com.oracle.runbook.enrichment.MetricsSourceAdapter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Stub implementation of {@link MetricsSourceAdapter} for testing.
 *
 * <p>This stub allows tests to run without cloud dependencies by returning configurable metrics
 * data. Use {@link #setMetrics(List)} to configure the response.
 */
public class StubMetricsSourceAdapter implements MetricsSourceAdapter {

  private static final String SOURCE_TYPE = "stub-metrics";

  private List<MetricSnapshot> metrics = new ArrayList<>();
  private RuntimeException exceptionToThrow;
  private boolean fetchMetricsCalled = false;
  private String lastResourceId;
  private Duration lastLookback;

  @Override
  public String sourceType() {
    return SOURCE_TYPE;
  }

  @Override
  public CompletableFuture<List<MetricSnapshot>> fetchMetrics(
      String resourceId, Duration lookback) {
    this.fetchMetricsCalled = true;
    this.lastResourceId = resourceId;
    this.lastLookback = lookback;

    if (exceptionToThrow != null) {
      return CompletableFuture.failedFuture(exceptionToThrow);
    }
    return CompletableFuture.completedFuture(new ArrayList<>(metrics));
  }

  /**
   * Sets the metrics to return from {@link #fetchMetrics(String, Duration)}.
   *
   * @param metrics the metrics to return
   */
  public void setMetrics(List<MetricSnapshot> metrics) {
    this.metrics = metrics != null ? new ArrayList<>(metrics) : new ArrayList<>();
    this.exceptionToThrow = null;
  }

  /**
   * Configures the adapter to throw an exception on the next fetch.
   *
   * @param exception the exception to throw
   */
  public void setException(RuntimeException exception) {
    this.exceptionToThrow = exception;
    this.metrics = new ArrayList<>();
  }

  /** Resets the adapter to its initial state. */
  public void reset() {
    this.metrics = new ArrayList<>();
    this.exceptionToThrow = null;
    this.fetchMetricsCalled = false;
    this.lastResourceId = null;
    this.lastLookback = null;
  }

  /**
   * Returns whether fetchMetrics was called.
   *
   * @return true if fetchMetrics was called
   */
  public boolean wasFetchMetricsCalled() {
    return fetchMetricsCalled;
  }

  /**
   * Returns the last resourceId passed to fetchMetrics.
   *
   * @return the last resourceId, or null if not called
   */
  public String getLastResourceId() {
    return lastResourceId;
  }

  /**
   * Returns the last lookback duration passed to fetchMetrics.
   *
   * @return the last lookback, or null if not called
   */
  public Duration getLastLookback() {
    return lastLookback;
  }
}
