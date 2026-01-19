package com.oracle.runbook.integration.stubs;

import com.oracle.runbook.domain.LogEntry;
import com.oracle.runbook.enrichment.LogSourceAdapter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Stub implementation of {@link LogSourceAdapter} for testing.
 *
 * <p>This stub allows tests to run without cloud dependencies by returning configurable log data.
 * Use {@link #setLogs(List)} to configure the response.
 */
public class StubLogSourceAdapter implements LogSourceAdapter {

  private static final String SOURCE_TYPE = "stub-logs";

  private List<LogEntry> logs = new ArrayList<>();
  private RuntimeException exceptionToThrow;
  private boolean fetchLogsCalled = false;
  private String lastResourceId;
  private Duration lastLookback;
  private String lastQuery;

  @Override
  public String sourceType() {
    return SOURCE_TYPE;
  }

  @Override
  public CompletableFuture<List<LogEntry>> fetchLogs(
      String resourceId, Duration lookback, String query) {
    this.fetchLogsCalled = true;
    this.lastResourceId = resourceId;
    this.lastLookback = lookback;
    this.lastQuery = query;

    if (exceptionToThrow != null) {
      return CompletableFuture.failedFuture(exceptionToThrow);
    }
    return CompletableFuture.completedFuture(new ArrayList<>(logs));
  }

  /**
   * Sets the logs to return from {@link #fetchLogs(String, Duration, String)}.
   *
   * @param logs the logs to return
   */
  public void setLogs(List<LogEntry> logs) {
    this.logs = logs != null ? new ArrayList<>(logs) : new ArrayList<>();
    this.exceptionToThrow = null;
  }

  /**
   * Configures the adapter to throw an exception on the next fetch.
   *
   * @param exception the exception to throw
   */
  public void setException(RuntimeException exception) {
    this.exceptionToThrow = exception;
    this.logs = new ArrayList<>();
  }

  /** Resets the adapter to its initial state. */
  public void reset() {
    this.logs = new ArrayList<>();
    this.exceptionToThrow = null;
    this.fetchLogsCalled = false;
    this.lastResourceId = null;
    this.lastLookback = null;
    this.lastQuery = null;
  }

  /**
   * Returns whether fetchLogs was called.
   *
   * @return true if fetchLogs was called
   */
  public boolean wasFetchLogsCalled() {
    return fetchLogsCalled;
  }

  /**
   * Returns the last resourceId passed to fetchLogs.
   *
   * @return the last resourceId, or null if not called
   */
  public String getLastResourceId() {
    return lastResourceId;
  }

  /**
   * Returns the last lookback duration passed to fetchLogs.
   *
   * @return the last lookback, or null if not called
   */
  public Duration getLastLookback() {
    return lastLookback;
  }

  /**
   * Returns the last query passed to fetchLogs.
   *
   * @return the last query, or null if not called
   */
  public String getLastQuery() {
    return lastQuery;
  }
}
