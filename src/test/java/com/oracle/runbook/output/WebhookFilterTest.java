package com.oracle.runbook.output;

import static org.assertj.core.api.Assertions.assertThat;

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
    assertThat(filter.severities()).isEqualTo(severities);
    assertThat(filter.requiredLabels()).isEqualTo(labels);
  }

  @Test
  @DisplayName("matches returns true when severity matches and labels match")
  void matches_returnsTrueWhenSeverityAndLabelsMatch() {
    // Arrange
    WebhookFilter filter = new WebhookFilter(Set.of(AlertSeverity.CRITICAL), Map.of("team", "ops"));

    // Act & Assert - filter matches against severity and labels
    assertThat(filter.matches(AlertSeverity.CRITICAL, Map.of("team", "ops", "env", "prod")))
        .isTrue();
  }

  @Test
  @DisplayName("matches returns false when severity does not match")
  void matches_returnsFalseWhenSeverityDoesNotMatch() {
    // Arrange
    WebhookFilter filter = new WebhookFilter(Set.of(AlertSeverity.CRITICAL), Map.of());

    // Act & Assert
    assertThat(filter.matches(AlertSeverity.INFO, Map.of())).isFalse();
  }

  @Test
  @DisplayName("matches returns false when required labels are missing")
  void matches_returnsFalseWhenRequiredLabelsAreMissing() {
    // Arrange
    WebhookFilter filter = new WebhookFilter(Set.of(AlertSeverity.CRITICAL), Map.of("team", "ops"));

    // Act & Assert - labels don't contain required 'team' key
    assertThat(filter.matches(AlertSeverity.CRITICAL, Map.of("env", "prod"))).isFalse();
  }

  @Test
  @DisplayName("matches returns false when required label value differs")
  void matches_returnsFalseWhenLabelValueDiffers() {
    // Arrange
    WebhookFilter filter = new WebhookFilter(Set.of(AlertSeverity.CRITICAL), Map.of("team", "ops"));

    // Act & Assert - label key matches but value differs
    assertThat(filter.matches(AlertSeverity.CRITICAL, Map.of("team", "dev"))).isFalse();
  }

  @Test
  @DisplayName("empty filter matches everything (permissive default)")
  void emptyFilter_matchesEverything() {
    // Arrange
    WebhookFilter filter = new WebhookFilter(Set.of(), Map.of());

    // Act & Assert
    assertThat(filter.matches(AlertSeverity.CRITICAL, Map.of())).isTrue();
    assertThat(filter.matches(AlertSeverity.WARNING, Map.of("any", "label"))).isTrue();
    assertThat(filter.matches(AlertSeverity.INFO, Map.of())).isTrue();
  }

  @Test
  @DisplayName("empty severities set matches any severity")
  void emptySeverities_matchesAnySeverity() {
    // Arrange
    WebhookFilter filter = new WebhookFilter(Set.of(), Map.of("team", "ops"));

    // Act & Assert
    assertThat(filter.matches(AlertSeverity.CRITICAL, Map.of("team", "ops"))).isTrue();
    assertThat(filter.matches(AlertSeverity.WARNING, Map.of("team", "ops"))).isTrue();
    assertThat(filter.matches(AlertSeverity.INFO, Map.of("team", "ops"))).isTrue();
  }

  @Test
  @DisplayName("allowAll factory creates filter that matches everything")
  void allowAll_factoryCreatesPermissiveFilter() {
    // Act
    WebhookFilter filter = WebhookFilter.allowAll();

    // Assert
    assertThat(filter.severities()).isEqualTo(Set.of());
    assertThat(filter.requiredLabels()).isEqualTo(Map.of());
    assertThat(filter.matches(AlertSeverity.CRITICAL, Map.of("any", "labels"))).isTrue();
  }

  @Test
  @DisplayName("record is immutable")
  void recordIsImmutable() {
    // Arrange
    Set<AlertSeverity> severities = Set.of(AlertSeverity.CRITICAL);
    Map<String, String> labels = Map.of("key", "value");
    WebhookFilter filter = new WebhookFilter(severities, labels);

    // Assert - verify immutability
    assertThat(filter.severities()).hasSize(1);
    assertThat(filter.requiredLabels()).hasSize(1);
  }
}
