package com.oracle.runbook.api.dto;

import static org.junit.jupiter.api.Assertions.*;

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

    assertEquals("slack-notify", config.name());
    assertEquals("SLACK", config.type());
    assertEquals("https://hooks.slack.com/xxx", config.url());
    assertTrue(config.enabled());
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

    assertEquals(2, config.filterSeverities().size());
    assertTrue(config.filterSeverities().contains("CRITICAL"));
    assertEquals("secret", config.headers().get("X-API-Key"));
  }

  @Test
  void testNullName_ThrowsException() {
    assertThrows(
        NullPointerException.class,
        () -> new WebhookConfig(null, "SLACK", "url", true, null, null));
  }

  @Test
  void testNullUrl_ThrowsException() {
    assertThrows(
        NullPointerException.class,
        () -> new WebhookConfig("name", "SLACK", null, true, null, null));
  }

  @Test
  void testNullFilterSeverities_DefaultsToEmptyList() {
    var config = new WebhookConfig("name", "SLACK", "url", true, null, null);
    assertNotNull(config.filterSeverities());
    assertTrue(config.filterSeverities().isEmpty());
  }

  @Test
  void testNullHeaders_DefaultsToEmptyMap() {
    var config = new WebhookConfig("name", "SLACK", "url", true, null, null);
    assertNotNull(config.headers());
    assertTrue(config.headers().isEmpty());
  }
}
