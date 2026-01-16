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
 */
package com.oracle.runbook.enrichment;
