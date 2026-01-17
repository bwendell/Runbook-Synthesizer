package com.oracle.runbook.enrichment;

import com.oracle.runbook.domain.LogEntry;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Port interface for fetching logs from various log aggregation sources.
 * <p>
 * This interface defines the contract for log source adapters in the
 * Hexagonal Architecture. Implementations provide concrete integrations
 * with specific log backends like OCI Logging, Grafana Loki, etc.
 * <p>
 * All implementations must be non-blocking and return CompletableFuture
 * to support Helidon SE's reactive patterns.
 *
 * @see LogEntry
 */
public interface LogSourceAdapter {

    /**
     * Returns the identifier for this log source type.
     * <p>
     * Examples: "oci-logging", "loki"
     *
     * @return the source type identifier, never null
     */
    String sourceType();

    /**
     * Fetches logs for the specified resource over the given lookback period.
     * <p>
     * The query parameter allows source-specific query syntax:
     * <ul>
     *   <li>OCI Logging: filter expressions</li>
     *   <li>Grafana Loki: LogQL queries</li>
     * </ul>
     *
     * @param resourceId the identifier of the resource to fetch logs for
     *                   (e.g., OCID for OCI, label selector for Loki)
     * @param lookback   the time duration to look back from now
     * @param query      source-specific query string (may be null or empty)
     * @return a CompletableFuture containing the list of log entries,
     *         never null (may complete with empty list)
     */
    CompletableFuture<List<LogEntry>> fetchLogs(String resourceId, Duration lookback, String query);
}
