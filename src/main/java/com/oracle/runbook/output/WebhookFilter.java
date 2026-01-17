package com.oracle.runbook.output;

import com.oracle.runbook.domain.AlertSeverity;
import java.util.Map;
import java.util.Set;

/**
 * Filter configuration for determining which checklists should be sent to a webhook.
 *
 * <p>Filters based on alert severity and required labels. Empty filters match everything
 * (permissive default).
 *
 * @param severities set of severities to match, empty matches any severity
 * @param requiredLabels labels that must be present and match, empty matches any labels
 */
public record WebhookFilter(Set<AlertSeverity> severities, Map<String, String> requiredLabels) {

  /** Compact constructor with defensive copies for immutability. */
  public WebhookFilter {
    severities = severities != null ? Set.copyOf(severities) : Set.of();
    requiredLabels = requiredLabels != null ? Map.copyOf(requiredLabels) : Map.of();
  }

  /**
   * Factory method for creating a permissive filter that matches everything.
   *
   * @return a filter with no restrictions
   */
  public static WebhookFilter allowAll() {
    return new WebhookFilter(Set.of(), Map.of());
  }

  /**
   * Determines if the given severity and labels match this filter.
   *
   * <p>An empty severities set matches any severity. An empty requiredLabels map matches any
   * labels. All required labels must be present with matching values.
   *
   * @param severity the alert severity to check
   * @param labels the labels to check against required labels
   * @return true if the severity and labels match this filter
   */
  public boolean matches(AlertSeverity severity, Map<String, String> labels) {
    // Empty severities means match any severity
    if (!severities.isEmpty() && !severities.contains(severity)) {
      return false;
    }

    // All required labels must be present and match
    for (Map.Entry<String, String> required : requiredLabels.entrySet()) {
      String actualValue = labels.get(required.getKey());
      if (!required.getValue().equals(actualValue)) {
        return false;
      }
    }

    return true;
  }
}
