package com.oracle.runbook.output.adapters;

import com.oracle.runbook.domain.DynamicChecklist;
import com.oracle.runbook.output.WebhookConfig;
import com.oracle.runbook.output.WebhookDestination;
import com.oracle.runbook.output.WebhookResult;
import java.util.concurrent.CompletableFuture;

/**
 * PagerDuty webhook destination that formats checklists for PagerDuty Events API v2.
 *
 * <p>This adapter integrates with PagerDuty's Events API v2 to trigger, acknowledge, or resolve
 * incidents with troubleshooting checklist data attached.
 *
 * <p><strong>Note:</strong> Full Events API v2 implementation is planned for v1.1. Currently, this
 * class throws {@link UnsupportedOperationException} when {@link #send} is called.
 *
 * <h2>Planned Features (v1.1)</h2>
 *
 * <ul>
 *   <li>Events API v2 payload formatting with dedup_key support
 *   <li>Trigger, acknowledge, and resolve event actions
 *   <li>Custom severity mapping from AlertSeverity to PagerDuty severity
 *   <li>Rich custom details with checklist items
 * </ul>
 *
 * <h2>PagerDuty Events API v2 Concepts</h2>
 *
 * <ul>
 *   <li><strong>routing_key</strong>: Integration key that routes events to the correct service
 *   <li><strong>dedup_key</strong>: Unique identifier for alert deduplication
 *   <li><strong>event_action</strong>: One of "trigger", "acknowledge", or "resolve"
 * </ul>
 *
 * @since 1.1 Full implementation available
 * @see WebhookDestination
 * @see <a href= "https://developer.pagerduty.com/docs/events-api-v2/overview/">PagerDuty Events API
 *     v2</a>
 */
public class PagerDutyWebhookDestination implements WebhookDestination {

  /** Standard PagerDuty Events API v2 endpoint. */
  public static final String PAGERDUTY_EVENTS_ENDPOINT = "https://events.pagerduty.com/v2/enqueue";

  private final WebhookConfig config;
  private final String routingKey;

  /**
   * Creates a new PagerDutyWebhookDestination with the given configuration and routing key.
   *
   * @param config the webhook configuration
   * @param routingKey the PagerDuty integration/routing key for event routing
   */
  public PagerDutyWebhookDestination(WebhookConfig config, String routingKey) {
    this.config = config;
    this.routingKey = routingKey;
  }

  @Override
  public String name() {
    return config.name();
  }

  @Override
  public String type() {
    return "pagerduty";
  }

  @Override
  public WebhookConfig config() {
    return config;
  }

  /**
   * Returns the PagerDuty routing key used for event routing.
   *
   * @return the routing key
   */
  public String routingKey() {
    return routingKey;
  }

  @Override
  public CompletableFuture<WebhookResult> send(DynamicChecklist checklist) {
    return CompletableFuture.supplyAsync(
        () -> {
          throw new UnsupportedOperationException("PagerDuty integration available in v1.1");
        });
  }

  @Override
  public boolean shouldSend(DynamicChecklist checklist) {
    // Default to true for now; filtering can be configured via WebhookFilter
    return true;
  }

  /**
   * Builds a PagerDuty Events API v2 payload for the given checklist.
   *
   * <p><strong>Note:</strong> This is a placeholder method for v1.1 implementation.
   *
   * <p>The payload structure will follow PagerDuty Events API v2 format:
   *
   * <pre>
   * {
   *   "routing_key": "...",
   *   "dedup_key": "alert-id",
   *   "event_action": "trigger",
   *   "payload": {
   *     "summary": "...",
   *     "source": "runbook-synthesizer",
   *     "severity": "critical|error|warning|info",
   *     "custom_details": { checklist items }
   *   }
   * }
   * </pre>
   *
   * @param checklist the dynamic checklist to format
   * @param routingKey the PagerDuty routing key
   * @return null (placeholder for v1.1 implementation)
   * @since 1.1
   */
  public String buildPagerDutyPayload(DynamicChecklist checklist, String routingKey) {
    // Placeholder for v1.1 PagerDuty Events API v2 implementation
    return null;
  }
}
