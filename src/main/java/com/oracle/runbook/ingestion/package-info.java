/**
 * Alert source adapters and normalization.
 * <p>
 * Provides adapters for ingesting alerts from various sources:
 * <ul>
 *   <li>OCI Monitoring Alarms via Events Service</li>
 *   <li>OCI Events Service triggers</li>
 *   <li>REST API manual submission</li>
 * </ul>
 * 
 * <h2>Ports/Interfaces (Hexagonal Architecture)</h2>
 * <ul>
 *   <li>{@link com.oracle.runbook.ingestion.AlertSourceAdapter} - Parses alerts from various sources</li>
 * </ul>
 */
package com.oracle.runbook.ingestion;

