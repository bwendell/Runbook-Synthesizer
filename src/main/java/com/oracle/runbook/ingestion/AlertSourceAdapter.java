package com.oracle.runbook.ingestion;

import com.oracle.runbook.domain.Alert;

/**
 * Port interface for parsing alerts from various ingestion sources.
 * <p>
 * This interface defines the contract for alert source adapters in the
 * Hexagonal Architecture. Implementations transform source-specific alert
 * payloads (e.g., OCI Monitoring Alarms, OCI Events) into the canonical
 * {@link Alert} domain model.
 * <p>
 * Unlike async enrichment operations, parsing is synchronous since it's fast
 * and doesn't require external calls.
 *
 * @see Alert
 */
public interface AlertSourceAdapter {

	/**
	 * Returns the identifier for this alert source type.
	 * <p>
	 * Examples: "oci-monitoring", "oci-events"
	 *
	 * @return the source type identifier, never null
	 */
	String sourceType();

	/**
	 * Parses a raw alert payload into the canonical Alert domain model.
	 * <p>
	 * Implementations should extract relevant fields from the source-specific JSON
	 * format and map them to the Alert record.
	 *
	 * @param rawPayload
	 *            the raw JSON payload from the alert source
	 * @return the parsed Alert domain object
	 * @throws IllegalArgumentException
	 *             if the payload cannot be parsed
	 */
	Alert parseAlert(String rawPayload);

	/**
	 * Determines whether this adapter can handle the given payload.
	 * <p>
	 * Used for routing incoming payloads to the correct adapter when multiple
	 * adapters are registered.
	 *
	 * @param rawPayload
	 *            the raw payload to check
	 * @return true if this adapter can parse the payload, false otherwise
	 */
	boolean canHandle(String rawPayload);
}
