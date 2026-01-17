package com.oracle.runbook.api.dto;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration for a webhook destination.
 *
 * @param name unique name for this webhook
 * @param type the webhook type (e.g., SLACK, PAGERDUTY, GENERIC)
 * @param url the webhook URL
 * @param enabled whether this webhook is active
 * @param filterSeverities only send alerts with these severities (empty = all)
 * @param headers custom HTTP headers to include in webhook calls
 */
public record WebhookConfig(
    String name,
    String type,
    String url,
    boolean enabled,
    List<String> filterSeverities,
    Map<String, String> headers) {

  /** Compact constructor with validation and defensive copies. */
  public WebhookConfig {
    Objects.requireNonNull(name, "name is required");
    Objects.requireNonNull(url, "url is required");

    // Defensive copies for immutability
    filterSeverities = filterSeverities != null ? List.copyOf(filterSeverities) : List.of();
    headers = headers != null ? Map.copyOf(headers) : Map.of();
  }
}
