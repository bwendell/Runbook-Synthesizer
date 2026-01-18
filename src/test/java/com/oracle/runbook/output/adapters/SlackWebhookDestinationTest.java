package com.oracle.runbook.output.adapters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oracle.runbook.domain.DynamicChecklist;
import com.oracle.runbook.output.WebhookConfig;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link SlackWebhookDestination}. */
@DisplayName("SlackWebhookDestination")
class SlackWebhookDestinationTest {

  private WebhookConfig config;
  private SlackWebhookDestination destination;

  @BeforeEach
  void setUp() {
    config =
        WebhookConfig.builder()
            .name("slack-alerts")
            .type("slack")
            .url("https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXX")
            .build();
    destination = new SlackWebhookDestination(config);
  }

  @Test
  @DisplayName("name() returns configured name")
  void nameReturnsConfiguredName() {
    assertThat(destination.name()).isEqualTo("slack-alerts");
  }

  @Test
  @DisplayName("type() returns 'slack'")
  void typeReturnsSlack() {
    assertThat(destination.type()).isEqualTo("slack");
  }

  @Test
  @DisplayName("config() returns the original configuration")
  void configReturnsConfiguration() {
    assertThat(destination.config()).isSameAs(config);
  }

  @Test
  @DisplayName("send() throws UnsupportedOperationException for v1.1 feature")
  void sendThrowsUnsupportedOperationException() {
    DynamicChecklist checklist = createTestChecklist();

    assertThatThrownBy(() -> destination.send(checklist).get())
        .isInstanceOf(ExecutionException.class)
        .hasCauseInstanceOf(UnsupportedOperationException.class)
        .hasMessageContaining("v1.1");
  }

  @Test
  @DisplayName("formatAsBlockKit() returns null as placeholder")
  void formatAsBlockKitReturnsNull() {
    DynamicChecklist checklist = createTestChecklist();
    assertThat(destination.formatAsBlockKit(checklist)).isNull();
  }

  @Test
  @DisplayName("shouldSend() returns true by default")
  void shouldSendReturnsTrue() {
    DynamicChecklist checklist = createTestChecklist();
    assertThat(destination.shouldSend(checklist)).isTrue();
  }

  private DynamicChecklist createTestChecklist() {
    // DynamicChecklist(alertId, summary, steps, sourceRunbooks, generatedAt,
    // llmProviderUsed)
    return new DynamicChecklist(
        "alert-123",
        "Test Checklist Summary",
        List.of(),
        List.of("runbook-1"),
        Instant.now(),
        "test-llm");
  }
}
