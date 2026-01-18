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

/** Unit tests for {@link PagerDutyWebhookDestination}. */
@DisplayName("PagerDutyWebhookDestination")
class PagerDutyWebhookDestinationTest {

  private static final String ROUTING_KEY = "test-routing-key-12345";
  private WebhookConfig config;
  private PagerDutyWebhookDestination destination;

  @BeforeEach
  void setUp() {
    config =
        WebhookConfig.builder()
            .name("pagerduty-incidents")
            .type("pagerduty")
            .url("https://events.pagerduty.com/v2/enqueue")
            .build();
    destination = new PagerDutyWebhookDestination(config, ROUTING_KEY);
  }

  @Test
  @DisplayName("name() returns configured name")
  void nameReturnsConfiguredName() {
    assertThat(destination.name()).isEqualTo("pagerduty-incidents");
  }

  @Test
  @DisplayName("type() returns 'pagerduty'")
  void typeReturnsPagerduty() {
    assertThat(destination.type()).isEqualTo("pagerduty");
  }

  @Test
  @DisplayName("config() returns the original configuration")
  void configReturnsConfiguration() {
    assertThat(destination.config()).isSameAs(config);
  }

  @Test
  @DisplayName("routingKey() returns the provided routing key")
  void routingKeyReturnsProvidedKey() {
    assertThat(destination.routingKey()).isEqualTo(ROUTING_KEY);
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
  @DisplayName("buildPagerDutyPayload() returns null as placeholder")
  void buildPagerDutyPayloadReturnsNull() {
    DynamicChecklist checklist = createTestChecklist();
    assertThat(destination.buildPagerDutyPayload(checklist, ROUTING_KEY)).isNull();
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
