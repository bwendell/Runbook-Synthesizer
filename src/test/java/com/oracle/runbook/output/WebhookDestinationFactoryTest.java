package com.oracle.runbook.output;

import static org.junit.jupiter.api.Assertions.*;

import com.oracle.runbook.output.adapters.GenericWebhookDestination;
import com.oracle.runbook.output.adapters.PagerDutyWebhookDestination;
import com.oracle.runbook.output.adapters.SlackWebhookDestination;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link WebhookDestinationFactory}. */
@DisplayName("WebhookDestinationFactory")
class WebhookDestinationFactoryTest {

  private WebhookDestinationFactory factory;

  @BeforeEach
  void setUp() {
    factory = new WebhookDestinationFactory();
  }

  @Test
  @DisplayName("create() returns GenericWebhookDestination for type 'generic'")
  void createReturnsGenericDestination() {
    WebhookConfig config =
        WebhookConfig.builder()
            .name("generic-webhook")
            .type("generic")
            .url("https://example.com/webhook")
            .build();

    WebhookDestination destination = factory.create(config);

    assertInstanceOf(GenericWebhookDestination.class, destination);
    assertEquals("generic-webhook", destination.name());
    assertEquals("generic", destination.type());
  }

  @Test
  @DisplayName("create() returns SlackWebhookDestination for type 'slack'")
  void createReturnsSlackDestination() {
    WebhookConfig config =
        WebhookConfig.builder()
            .name("slack-alerts")
            .type("slack")
            .url("https://hooks.slack.com/services/T00/B00/xxx")
            .build();

    WebhookDestination destination = factory.create(config);

    assertInstanceOf(SlackWebhookDestination.class, destination);
    assertEquals("slack-alerts", destination.name());
    assertEquals("slack", destination.type());
  }

  @Test
  @DisplayName("create() returns PagerDutyWebhookDestination for type 'pagerduty'")
  void createReturnsPagerDutyDestination() {
    WebhookConfig config =
        WebhookConfig.builder()
            .name("pagerduty-incidents")
            .type("pagerduty")
            .url("https://events.pagerduty.com/v2/enqueue")
            .headers(Map.of("X-Routing-Key", "routing-key-123"))
            .build();

    WebhookDestination destination = factory.create(config);

    assertInstanceOf(PagerDutyWebhookDestination.class, destination);
    assertEquals("pagerduty-incidents", destination.name());
    assertEquals("pagerduty", destination.type());

    PagerDutyWebhookDestination pdDest = (PagerDutyWebhookDestination) destination;
    assertEquals("routing-key-123", pdDest.routingKey());
  }

  @Test
  @DisplayName("create() throws IllegalArgumentException for unknown type")
  void createThrowsForUnknownType() {
    WebhookConfig config =
        WebhookConfig.builder()
            .name("unknown-webhook")
            .type("unknown")
            .url("https://example.com/webhook")
            .build();

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> factory.create(config));

    assertTrue(exception.getMessage().contains("unknown"));
  }

  @Test
  @DisplayName("create() is case-insensitive for type matching")
  void createIsCaseInsensitive() {
    WebhookConfig upperConfig =
        WebhookConfig.builder()
            .name("slack-upper")
            .type("SLACK")
            .url("https://hooks.slack.com/services/T00/B00/xxx")
            .build();

    WebhookConfig mixedConfig =
        WebhookConfig.builder()
            .name("generic-mixed")
            .type("Generic")
            .url("https://example.com/webhook")
            .build();

    assertInstanceOf(SlackWebhookDestination.class, factory.create(upperConfig));
    assertInstanceOf(GenericWebhookDestination.class, factory.create(mixedConfig));
  }

  @Test
  @DisplayName("create() extracts routing key from X-Routing-Key header for PagerDuty")
  void createExtractsRoutingKeyFromHeader() {
    WebhookConfig config =
        WebhookConfig.builder()
            .name("pagerduty-test")
            .type("pagerduty")
            .url("https://events.pagerduty.com/v2/enqueue")
            .headers(Map.of("X-Routing-Key", "my-routing-key"))
            .build();

    WebhookDestination destination = factory.create(config);

    PagerDutyWebhookDestination pdDest = (PagerDutyWebhookDestination) destination;
    assertEquals("my-routing-key", pdDest.routingKey());
  }

  @Test
  @DisplayName("create() uses empty routing key when X-Routing-Key header is missing")
  void createUsesEmptyRoutingKeyWhenMissing() {
    WebhookConfig config =
        WebhookConfig.builder()
            .name("pagerduty-no-key")
            .type("pagerduty")
            .url("https://events.pagerduty.com/v2/enqueue")
            .build();

    WebhookDestination destination = factory.create(config);

    PagerDutyWebhookDestination pdDest = (PagerDutyWebhookDestination) destination;
    assertEquals("", pdDest.routingKey());
  }
}
