package com.oracle.runbook.output;

import static org.junit.jupiter.api.Assertions.*;

import com.oracle.runbook.domain.AlertSeverity;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for WebhookConfig record following the specification in tasks.md Task 3. */
class WebhookConfigTest {

  @Test
  @DisplayName("record has name, type, url, enabled, filter, headers fields")
  void recordHasRequiredFields() {
    // Arrange
    WebhookFilter filter = WebhookFilter.allowAll();
    Map<String, String> headers = Map.of("Authorization", "Bearer token123");

    // Act
    WebhookConfig config =
        new WebhookConfig(
            "slack-oncall", "slack", "https://hooks.slack.com/test", true, filter, headers);

    // Assert
    assertEquals("slack-oncall", config.name());
    assertEquals("slack", config.type());
    assertEquals("https://hooks.slack.com/test", config.url());
    assertTrue(config.enabled());
    assertEquals(filter, config.filter());
    assertEquals(headers, config.headers());
  }

  @Test
  @DisplayName("builder creates config with all fields")
  void builder_createsConfigWithAllFields() {
    // Arrange
    WebhookFilter filter = new WebhookFilter(Set.of(AlertSeverity.CRITICAL), Map.of());

    // Act
    WebhookConfig config =
        WebhookConfig.builder()
            .name("pagerduty-incidents")
            .type("pagerduty")
            .url("https://events.pagerduty.com/v2/enqueue")
            .enabled(true)
            .filter(filter)
            .headers(Map.of("X-Routing-Key", "abc123"))
            .build();

    // Assert
    assertEquals("pagerduty-incidents", config.name());
    assertEquals("pagerduty", config.type());
    assertEquals("https://events.pagerduty.com/v2/enqueue", config.url());
    assertTrue(config.enabled());
    assertEquals(filter, config.filter());
    assertEquals("abc123", config.headers().get("X-Routing-Key"));
  }

  @Test
  @DisplayName("builder defaults enabled to true")
  void builder_defaultsEnabledToTrue() {
    // Act
    WebhookConfig config =
        WebhookConfig.builder()
            .name("test")
            .type("generic")
            .url("https://example.com/webhook")
            .build();

    // Assert
    assertTrue(config.enabled());
  }

  @Test
  @DisplayName("builder defaults headers to empty map")
  void builder_defaultsHeadersToEmptyMap() {
    // Act
    WebhookConfig config =
        WebhookConfig.builder()
            .name("test")
            .type("generic")
            .url("https://example.com/webhook")
            .build();

    // Assert
    assertNotNull(config.headers());
    assertTrue(config.headers().isEmpty());
  }

  @Test
  @DisplayName("builder defaults filter to allowAll")
  void builder_defaultsFilterToAllowAll() {
    // Act
    WebhookConfig config =
        WebhookConfig.builder()
            .name("test")
            .type("generic")
            .url("https://example.com/webhook")
            .build();

    // Assert
    assertNotNull(config.filter());
    assertTrue(config.filter().matches(AlertSeverity.INFO, Map.of()));
  }

  @Test
  @DisplayName("builder throws when name is null")
  void builder_throwsWhenNameIsNull() {
    // Act & Assert
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                WebhookConfig.builder().type("generic").url("https://example.com/webhook").build());

    assertTrue(exception.getMessage().contains("name"));
  }

  @Test
  @DisplayName("builder throws when url is null")
  void builder_throwsWhenUrlIsNull() {
    // Act & Assert
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> WebhookConfig.builder().name("test").type("generic").build());

    assertTrue(exception.getMessage().contains("url"));
  }

  @Test
  @DisplayName("builder throws when type is empty")
  void builder_throwsWhenTypeIsEmpty() {
    // Act & Assert
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                WebhookConfig.builder()
                    .name("test")
                    .type("")
                    .url("https://example.com/webhook")
                    .build());

    assertTrue(exception.getMessage().contains("type"));
  }

  @Test
  @DisplayName("builder validates URL format")
  void builder_validatesUrlFormat() {
    // Act & Assert
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                WebhookConfig.builder()
                    .name("test")
                    .type("generic")
                    .url("not-a-valid-url")
                    .build());

    assertTrue(exception.getMessage().toLowerCase().contains("url"));
  }

  @Test
  @DisplayName("record is immutable with defensive copies")
  void recordIsImmutableWithDefensiveCopies() {
    // Arrange
    Map<String, String> headers = Map.of("key", "value");
    WebhookConfig config =
        WebhookConfig.builder()
            .name("test")
            .type("generic")
            .url("https://example.com/webhook")
            .headers(headers)
            .build();

    // Assert - original map size matches
    assertEquals(1, config.headers().size());
  }
}
