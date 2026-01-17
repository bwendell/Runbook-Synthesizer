package com.oracle.runbook.output.adapters;

import static org.junit.jupiter.api.Assertions.*;

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
    assertEquals("pagerduty-incidents", destination.name());
  }

  @Test
  @DisplayName("type() returns 'pagerduty'")
  void typeReturnsPagerduty() {
    assertEquals("pagerduty", destination.type());
  }

  @Test
  @DisplayName("config() returns the original configuration")
  void configReturnsConfiguration() {
    assertSame(config, destination.config());
  }

  @Test
  @DisplayName("routingKey() returns the provided routing key")
  void routingKeyReturnsProvidedKey() {
    assertEquals(ROUTING_KEY, destination.routingKey());
  }

  @Test
  @DisplayName("send() throws UnsupportedOperationException for v1.1 feature")
  void sendThrowsUnsupportedOperationException() {
    DynamicChecklist checklist = createTestChecklist();

    ExecutionException exception =
        assertThrows(ExecutionException.class, () -> destination.send(checklist).get());

    assertInstanceOf(UnsupportedOperationException.class, exception.getCause());
    assertTrue(
        exception.getCause().getMessage().contains("v1.1"),
        "Exception message should mention v1.1");
  }

  @Test
  @DisplayName("buildPagerDutyPayload() returns null as placeholder")
  void buildPagerDutyPayloadReturnsNull() {
    DynamicChecklist checklist = createTestChecklist();
    assertNull(destination.buildPagerDutyPayload(checklist, ROUTING_KEY));
  }

  @Test
  @DisplayName("shouldSend() returns true by default")
  void shouldSendReturnsTrue() {
    DynamicChecklist checklist = createTestChecklist();
    assertTrue(destination.shouldSend(checklist));
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
