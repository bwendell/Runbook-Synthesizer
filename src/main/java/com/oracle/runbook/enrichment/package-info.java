/**
 * Context enrichment service and observability adapters.
 * <p>
 * Gathers real-time infrastructure state from multiple sources:
 * <ul>
 *   <li>OCI Monitoring - compute/DB metrics</li>
 *   <li>OCI Logging - service and custom logs</li>
 *   <li>Prometheus - existing metrics</li>
 *   <li>Grafana Loki - log aggregation</li>
 * </ul>
 * 
 * <h2>Ports/Interfaces (Hexagonal Architecture)</h2>
 * <ul>
 *   <li>{@link com.oracle.runbook.enrichment.MetricsSourceAdapter} - Fetches metrics from observability sources</li>
 *   <li>{@link com.oracle.runbook.enrichment.LogSourceAdapter} - Fetches logs from log aggregation sources</li>
 *   <li>{@link com.oracle.runbook.enrichment.ContextEnrichmentService} - Orchestrates enrichment with all sources</li>
 * </ul>
 */
package com.oracle.runbook.enrichment;

