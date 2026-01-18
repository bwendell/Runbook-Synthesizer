package com.oracle.runbook.api.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Unit tests for WebhookConfig DTO. */
class WebhookConfigTest {

  @Test
  void testCreation_WithRequiredFields() {
    var config =
        new WebhookConfig(
            "slack-notify", "SLACK", "https://hooks.slack.com/xxx", true, List.of(), Map.of());

    assertThat(config.name()).isEqualTo("slack-notify");
    assertThat(config.type()).isEqualTo("SLACK");
    assertThat(config.url()).isEqualTo("https://hooks.slack.com/xxx");
    assertThat(config.enabled()).isTrue();
  }

  @Test
  void testCreation_WithOptionalFields() {
    var config =
        new WebhookConfig(
            "pagerduty-critical",
            "PAGERDUTY",
            "https://events.pagerduty.com/xxx",
            false,
            List.of("CRITICAL", "WARNING"),
            Map.of("X-API-Key", "secret"));

    assertThat(config.filterSeverities()).hasSize(2);
    assertThat(config.filterSeverities()).contains("CRITICAL");
    assertThat(config.headers().get("X-API-Key")).isEqualTo("secret");
  }

  @Test
  void testNullName_ThrowsException() {
    assertThatThrownBy(() -> new WebhookConfig(null, "SLACK", "url", true, null, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void testNullUrl_ThrowsException() {
    assertThatThrownBy(() -> new WebhookConfig("name", "SLACK", null, true, null, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void testNullFilterSeverities_DefaultsToEmptyList() {
    var config = new WebhookConfig("name", "SLACK", "url", true, null, null);
    assertThat(config.filterSeverities()).isNotNull().isEmpty();
  }

  @Test
  void testNullHeaders_DefaultsToEmptyMap() {
    var config = new WebhookConfig("name", "SLACK", "url", true, null, null);
    assertThat(config.headers()).isNotNull().isEmpty();
  }
}
