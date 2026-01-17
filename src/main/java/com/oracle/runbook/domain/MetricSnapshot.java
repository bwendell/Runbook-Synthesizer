package com.oracle.runbook.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Point-in-time metric value representation from OCI Monitoring or Prometheus.
 *
 * @param metricName
 *            the name of the metric (e.g., CpuUtilization, MemoryUtilization)
 * @param namespace
 *            the metric namespace (e.g., oci_computeagent)
 * @param value
 *            the metric value
 * @param unit
 *            the unit of measurement (e.g., percent, bytes, count)
 * @param timestamp
 *            when this metric was recorded
 */
public record MetricSnapshot(String metricName, String namespace, double value, String unit, Instant timestamp) {
	/**
	 * Compact constructor with validation.
	 */
	public MetricSnapshot {
		Objects.requireNonNull(metricName, "MetricSnapshot metricName cannot be null");
		Objects.requireNonNull(timestamp, "MetricSnapshot timestamp cannot be null");
	}
}
