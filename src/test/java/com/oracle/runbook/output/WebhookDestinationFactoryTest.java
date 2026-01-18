package com.oracle.runbook.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    assertThat(destination).isInstanceOf(GenericWebhookDestination.class);
    assertThat(destination.name()).isEqualTo("generic-webhook");
    assertThat(destination.type()).isEqualTo("generic");
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

    assertThat(destination).isInstanceOf(SlackWebhookDestination.class);
    assertThat(destination.name()).isEqualTo("slack-alerts");
    assertThat(destination.type()).isEqualTo("slack");
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

    assertThat(destination).isInstanceOf(PagerDutyWebhookDestination.class);
    assertThat(destination.name()).isEqualTo("pagerduty-incidents");
    assertThat(destination.type()).isEqualTo("pagerduty");

    PagerDutyWebhookDestination pdDest = (PagerDutyWebhookDestination) destination;
    assertThat(pdDest.routingKey()).isEqualTo("routing-key-123");
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

    assertThatThrownBy(() -> factory.create(config))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("unknown");
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

    assertThat(factory.create(upperConfig)).isInstanceOf(SlackWebhookDestination.class);
    assertThat(factory.create(mixedConfig)).isInstanceOf(GenericWebhookDestination.class);
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
    assertThat(pdDest.routingKey()).isEqualTo("my-routing-key");
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
    assertThat(pdDest.routingKey()).isEqualTo("");
  }
}
