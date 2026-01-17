package com.oracle.runbook.output;

import com.oracle.runbook.output.adapters.GenericWebhookDestination;
import com.oracle.runbook.output.adapters.PagerDutyWebhookDestination;
import com.oracle.runbook.output.adapters.SlackWebhookDestination;
import java.util.logging.Logger;

/**
 * Factory for creating {@link WebhookDestination} instances from configuration.
 *
 * <p>This factory supports creating destinations based on the type specified in the {@link
 * WebhookConfig}. Supported types are:
 *
 * <ul>
 *   <li>{@code generic} - Creates a {@link GenericWebhookDestination} for arbitrary HTTP endpoints
 *   <li>{@code slack} - Creates a {@link SlackWebhookDestination} for Slack webhooks
 *   <li>{@code pagerduty} - Creates a {@link PagerDutyWebhookDestination} for PagerDuty Events API
 * </ul>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * WebhookDestinationFactory factory = new WebhookDestinationFactory();
 * WebhookConfig config = WebhookConfig.builder()
 *         .name("my-webhook")
 *         .type("generic")
 *         .url("https://example.com/webhook")
 *         .build();
 * WebhookDestination destination = factory.create(config);
 * }</pre>
 *
 * @see WebhookDestination
 * @see WebhookConfig
 */
public class WebhookDestinationFactory {

  private static final Logger LOGGER = Logger.getLogger(WebhookDestinationFactory.class.getName());

  /** Header name for PagerDuty routing key. */
  private static final String PAGERDUTY_ROUTING_KEY_HEADER = "X-Routing-Key";

  /**
   * Creates a new WebhookDestination based on the provided configuration.
   *
   * @param config the webhook configuration
   * @return a new WebhookDestination instance
   * @throws IllegalArgumentException if the type is not recognized
   */
  public WebhookDestination create(WebhookConfig config) {
    String type = config.type().toLowerCase();

    LOGGER.info("Creating webhook destination '" + config.name() + "' of type '" + type + "'");

    return switch (type) {
      case "generic" -> new GenericWebhookDestination(config);
      case "slack" -> new SlackWebhookDestination(config);
      case "pagerduty" -> createPagerDutyDestination(config);
      default ->
          throw new IllegalArgumentException(
              "Unknown webhook destination type: '"
                  + config.type()
                  + "'. "
                  + "Supported types are: generic, slack, pagerduty");
    };
  }

  private PagerDutyWebhookDestination createPagerDutyDestination(WebhookConfig config) {
    String routingKey = config.headers().getOrDefault(PAGERDUTY_ROUTING_KEY_HEADER, "");
    return new PagerDutyWebhookDestination(config, routingKey);
  }
}
