package com.oracle.runbook.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Canonical alert model for normalized ingestion from multiple sources.
 * Represents an alert from OCI Monitoring Alarms, Events Service, or manual API
 * submission.
 *
 * @param id
 *            the unique identifier for this alert
 * @param title
 *            the alert title/name
 * @param message
 *            the detailed alert message
 * @param severity
 *            the severity level of the alert
 * @param sourceService
 *            the originating service (e.g., "oci-monitoring", "oci-events")
 * @param dimensions
 *            key-value pairs from the alert source (e.g., compartmentId,
 *            resourceId)
 * @param labels
 *            custom labels/tags associated with the alert
 * @param timestamp
 *            when the alert was triggered
 * @param rawPayload
 *            the original JSON payload for debugging
 */
public record Alert(String id, String title, String message, AlertSeverity severity, String sourceService,
		Map<String, String> dimensions, Map<String, String> labels, Instant timestamp, String rawPayload) {
	/**
	 * Compact constructor with validation and defensive copies.
	 */
	public Alert {
		Objects.requireNonNull(id, "Alert id cannot be null");
		Objects.requireNonNull(title, "Alert title cannot be null");
		Objects.requireNonNull(severity, "Alert severity cannot be null");
		Objects.requireNonNull(timestamp, "Alert timestamp cannot be null");

		// Defensive copies for immutability
		dimensions = dimensions != null ? Map.copyOf(dimensions) : Map.of();
		labels = labels != null ? Map.copyOf(labels) : Map.of();
	}
}
