package com.oracle.runbook.output;

import com.oracle.runbook.domain.DynamicChecklist;

import java.util.concurrent.CompletableFuture;

/**
 * Port interface for configurable webhook output destinations.
 * <p>
 * This interface defines the contract for webhook destinations in the Hexagonal
 * Architecture. Implementations provide concrete integrations with specific
 * platforms like Slack, PagerDuty, or custom HTTP endpoints.
 * <p>
 * All implementations must be non-blocking and return CompletableFuture to
 * support Helidon SE's reactive patterns.
 *
 * @see WebhookResult
 * @see DynamicChecklist
 */
public interface WebhookDestination {

	/**
	 * Returns the name of this webhook destination.
	 * <p>
	 * Examples: "slack-oncall", "pagerduty-incidents", "custom-ticketing"
	 *
	 * @return the webhook name, never null
	 */
	String name();

	/**
	 * Returns the type of this webhook destination.
	 * <p>
	 * Examples: "slack", "pagerduty", "generic"
	 *
	 * @return the webhook type, never null
	 */
	String type();

	/**
	 * Sends the generated checklist to this webhook destination.
	 * <p>
	 * Implementations should format the checklist appropriately for the target
	 * platform (e.g., Slack Block Kit for Slack, PD Events API v2 for PagerDuty).
	 *
	 * @param checklist
	 *            the generated troubleshooting checklist to send
	 * @return a CompletableFuture containing the result of the webhook call
	 */
	CompletableFuture<WebhookResult> send(DynamicChecklist checklist);

	/**
	 * Determines whether this webhook should receive the given checklist.
	 * <p>
	 * Enables filtering based on severity, labels, alert source, or other criteria.
	 * For example, a PagerDuty webhook might only send CRITICAL alerts.
	 *
	 * @param checklist
	 *            the checklist to evaluate
	 * @return true if this webhook should send the checklist, false otherwise
	 */
	boolean shouldSend(DynamicChecklist checklist);
}
