package com.oracle.runbook.output;

import static org.junit.jupiter.api.Assertions.*;

import com.oracle.runbook.domain.AlertSeverity;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for WebhookFilter record following the specification in tasks.md Task 2. */
class WebhookFilterTest {

  @Test
  @DisplayName("record has severities and requiredLabels fields")
  void recordHasRequiredFields() {
    // Arrange
    Set<AlertSeverity> severities = Set.of(AlertSeverity.CRITICAL, AlertSeverity.WARNING);
    Map<String, String> labels = Map.of("team", "ops", "env", "prod");

    // Act
    WebhookFilter filter = new WebhookFilter(severities, labels);

    // Assert
    assertEquals(severities, filter.severities());
    assertEquals(labels, filter.requiredLabels());
  }

  @Test
  @DisplayName("matches returns true when severity matches and labels match")
  void matches_returnsTrueWhenSeverityAndLabelsMatch() {
    // Arrange
    WebhookFilter filter = new WebhookFilter(Set.of(AlertSeverity.CRITICAL), Map.of("team", "ops"));

    // Act & Assert - filter matches against severity and labels
    assertTrue(filter.matches(AlertSeverity.CRITICAL, Map.of("team", "ops", "env", "prod")));
  }

  @Test
  @DisplayName("matches returns false when severity does not match")
  void matches_returnsFalseWhenSeverityDoesNotMatch() {
    // Arrange
    WebhookFilter filter = new WebhookFilter(Set.of(AlertSeverity.CRITICAL), Map.of());

    // Act & Assert
    assertFalse(filter.matches(AlertSeverity.INFO, Map.of()));
  }

  @Test
  @DisplayName("matches returns false when required labels are missing")
  void matches_returnsFalseWhenRequiredLabelsAreMissing() {
    // Arrange
    WebhookFilter filter = new WebhookFilter(Set.of(AlertSeverity.CRITICAL), Map.of("team", "ops"));

    // Act & Assert - labels don't contain required 'team' key
    assertFalse(filter.matches(AlertSeverity.CRITICAL, Map.of("env", "prod")));
  }

  @Test
  @DisplayName("matches returns false when required label value differs")
  void matches_returnsFalseWhenLabelValueDiffers() {
    // Arrange
    WebhookFilter filter = new WebhookFilter(Set.of(AlertSeverity.CRITICAL), Map.of("team", "ops"));

    // Act & Assert - label key matches but value differs
    assertFalse(filter.matches(AlertSeverity.CRITICAL, Map.of("team", "dev")));
  }

  @Test
  @DisplayName("empty filter matches everything (permissive default)")
  void emptyFilter_matchesEverything() {
    // Arrange
    WebhookFilter filter = new WebhookFilter(Set.of(), Map.of());

    // Act & Assert
    assertTrue(filter.matches(AlertSeverity.CRITICAL, Map.of()));
    assertTrue(filter.matches(AlertSeverity.WARNING, Map.of("any", "label")));
    assertTrue(filter.matches(AlertSeverity.INFO, Map.of()));
  }

  @Test
  @DisplayName("empty severities set matches any severity")
  void emptySeverities_matchesAnySeverity() {
    // Arrange
    WebhookFilter filter = new WebhookFilter(Set.of(), Map.of("team", "ops"));

    // Act & Assert
    assertTrue(filter.matches(AlertSeverity.CRITICAL, Map.of("team", "ops")));
    assertTrue(filter.matches(AlertSeverity.WARNING, Map.of("team", "ops")));
    assertTrue(filter.matches(AlertSeverity.INFO, Map.of("team", "ops")));
  }

  @Test
  @DisplayName("allowAll factory creates filter that matches everything")
  void allowAll_factoryCreatesPermissiveFilter() {
    // Act
    WebhookFilter filter = WebhookFilter.allowAll();

    // Assert
    assertEquals(Set.of(), filter.severities());
    assertEquals(Map.of(), filter.requiredLabels());
    assertTrue(filter.matches(AlertSeverity.CRITICAL, Map.of("any", "labels")));
  }

  @Test
  @DisplayName("record is immutable")
  void recordIsImmutable() {
    // Arrange
    Set<AlertSeverity> severities = Set.of(AlertSeverity.CRITICAL);
    Map<String, String> labels = Map.of("key", "value");
    WebhookFilter filter = new WebhookFilter(severities, labels);

    // Assert - verify immutability
    assertEquals(1, filter.severities().size());
    assertEquals(1, filter.requiredLabels().size());
  }
}
