/**
 * Context enrichment service and observability adapters.
 *
 * <p>Gathers real-time infrastructure state from multiple sources:
 *
 * <ul>
 *   <li>OCI Monitoring - compute/DB metrics
 *   <li>OCI Logging - service and custom logs
 *   <li>Prometheus - existing metrics
 *   <li>Grafana Loki - log aggregation
 * </ul>
 *
 * <h2>Ports/Interfaces (Hexagonal Architecture)</h2>
 *
 * <ul>
 *   <li>{@link com.oracle.runbook.enrichment.MetricsSourceAdapter} - Fetches metrics from
 *       observability sources
 *   <li>{@link com.oracle.runbook.enrichment.LogSourceAdapter} - Fetches logs from log aggregation
 *       sources
 *   <li>{@link com.oracle.runbook.enrichment.ContextEnrichmentService} - Orchestrates enrichment
 *       with all sources
 * </ul>
 *
 * <h2>OCI SDK Adapters</h2>
 *
 * <ul>
 *   <li>{@link com.oracle.runbook.enrichment.OciMonitoringAdapter} - OCI Monitoring API
 *       implementation
 *   <li>{@link com.oracle.runbook.enrichment.OciLoggingAdapter} - OCI Logging Search API
 *       implementation
 *   <li>{@link com.oracle.runbook.enrichment.OciComputeClient} - OCI Compute instance metadata
 *       retrieval
 * </ul>
 */
package com.oracle.runbook.enrichment;
