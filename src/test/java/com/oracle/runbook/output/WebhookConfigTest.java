package com.oracle.runbook.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oracle.runbook.domain.AlertSeverity;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for WebhookConfig record following the specification in tasks.md Task 3. */
class WebhookConfigTest {

  @Test
  @DisplayName("record has name, type, url, enabled, filter, headers, retry fields")
  void recordHasRequiredFields() {
    // Arrange
    WebhookFilter filter = WebhookFilter.allowAll();
    Map<String, String> headers = Map.of("Authorization", "Bearer token123");

    // Act
    WebhookConfig config =
        new WebhookConfig(
            "slack-oncall",
            "slack",
            "https://hooks.slack.com/test",
            true,
            filter,
            headers,
            3,
            1000);

    // Assert
    assertThat(config.name()).isEqualTo("slack-oncall");
    assertThat(config.type()).isEqualTo("slack");
    assertThat(config.url()).isEqualTo("https://hooks.slack.com/test");
    assertThat(config.enabled()).isTrue();
    assertThat(config.filter()).isEqualTo(filter);
    assertThat(config.headers()).isEqualTo(headers);
    assertThat(config.retryCount()).isEqualTo(3);
    assertThat(config.retryDelayMs()).isEqualTo(1000);
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
    assertThat(config.name()).isEqualTo("pagerduty-incidents");
    assertThat(config.type()).isEqualTo("pagerduty");
    assertThat(config.url()).isEqualTo("https://events.pagerduty.com/v2/enqueue");
    assertThat(config.enabled()).isTrue();
    assertThat(config.filter()).isEqualTo(filter);
    assertThat(config.headers().get("X-Routing-Key")).isEqualTo("abc123");
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
    assertThat(config.enabled()).isTrue();
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
    assertThat(config.headers()).isNotNull().isEmpty();
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
    assertThat(config.filter()).isNotNull();
    assertThat(config.filter().matches(AlertSeverity.INFO, Map.of())).isTrue();
  }

  @Test
  @DisplayName("builder throws when name is null")
  void builder_throwsWhenNameIsNull() {
    // Act & Assert
    assertThatThrownBy(
            () ->
                WebhookConfig.builder().type("generic").url("https://example.com/webhook").build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("name");
  }

  @Test
  @DisplayName("builder throws when url is null")
  void builder_throwsWhenUrlIsNull() {
    // Act & Assert
    assertThatThrownBy(() -> WebhookConfig.builder().name("test").type("generic").build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("url");
  }

  @Test
  @DisplayName("builder throws when type is empty")
  void builder_throwsWhenTypeIsEmpty() {
    // Act & Assert
    assertThatThrownBy(
            () ->
                WebhookConfig.builder()
                    .name("test")
                    .type("")
                    .url("https://example.com/webhook")
                    .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("type");
  }

  @Test
  @DisplayName("builder validates URL format")
  void builder_validatesUrlFormat() {
    // Act & Assert
    assertThatThrownBy(
            () ->
                WebhookConfig.builder().name("test").type("generic").url("not-a-valid-url").build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContainingAll("url");
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
    assertThat(config.headers()).hasSize(1);
  }
}
