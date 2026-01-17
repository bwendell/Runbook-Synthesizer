/**
 * Alert source adapters and normalization.
 *
 * <p>Provides adapters for ingesting alerts from various sources:
 *
 * <ul>
 *   <li>OCI Monitoring Alarms via Events Service
 *   <li>OCI Events Service triggers
 *   <li>REST API manual submission
 * </ul>
 *
 * <h2>Ports/Interfaces (Hexagonal Architecture)</h2>
 *
 * <ul>
 *   <li>{@link com.oracle.runbook.ingestion.AlertSourceAdapter} - Parses alerts from various
 *       sources
 * </ul>
 */
package com.oracle.runbook.ingestion;
