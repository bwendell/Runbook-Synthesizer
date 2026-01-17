package com.oracle.runbook.config;

import com.oracle.runbook.output.WebhookConfig;
import io.helidon.config.Config;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Loads webhook configuration from Helidon Config.
 *
 * <p>This class reads the {@code output.webhooks} configuration section and transforms it into a
 * list of {@link WebhookConfig} objects that can be used to initialize webhook destinations.
 *
 * <h2>Configuration Format</h2>
 *
 * <pre>
 * output:
 *   webhooks:
 *     - name: webhook-name
 *       type: generic|slack|pagerduty
 *       url: https://example.com/webhook
 *       enabled: true|false
 *       headers:
 *         Authorization: Bearer token
 *         X-Custom-Header: value
 *       filter:
 *         severities: [CRITICAL, WARNING]
 * </pre>
 *
 * @see WebhookConfig
 */
public class WebhookConfigLoader {

  private static final Logger LOGGER = Logger.getLogger(WebhookConfigLoader.class.getName());
  private static final String WEBHOOKS_CONFIG_KEY = "output.webhooks";

  private final Config config;

  /**
   * Creates a new WebhookConfigLoader with the given Helidon Config.
   *
   * @param config the Helidon configuration object
   */
  public WebhookConfigLoader(Config config) {
    this.config = config;
  }

  /**
   * Loads all webhook configurations, including disabled ones.
   *
   * @return list of all webhook configurations
   */
  public List<WebhookConfig> loadWebhookConfigs() {
    Config webhooksConfig = config.get(WEBHOOKS_CONFIG_KEY);

    if (!webhooksConfig.exists() || webhooksConfig.isLeaf()) {
      LOGGER.info("No webhooks configured");
      return List.of();
    }

    List<WebhookConfig> configs =
        webhooksConfig.asNodeList().orElse(List.of()).stream().map(this::toWebhookConfig).toList();

    LOGGER.info("Loaded " + configs.size() + " webhook configuration(s)");
    return configs;
  }

  /**
   * Loads only enabled webhook configurations.
   *
   * <p>Disabled webhooks (where {@code enabled: false}) are filtered out.
   *
   * @return list of enabled webhook configurations
   */
  public List<WebhookConfig> loadEnabledWebhookConfigs() {
    return loadWebhookConfigs().stream().filter(WebhookConfig::enabled).toList();
  }

  private WebhookConfig toWebhookConfig(Config webhookNode) {
    String name = webhookNode.get("name").asString().orElse("unnamed");
    String type = webhookNode.get("type").asString().orElse("generic");
    String url = webhookNode.get("url").asString().orElse("");
    boolean enabled = webhookNode.get("enabled").asBoolean().orElse(true);

    Map<String, String> headers = parseHeaders(webhookNode.get("headers"));

    return WebhookConfig.builder()
        .name(name)
        .type(type)
        .url(url)
        .enabled(enabled)
        .headers(headers)
        .build();
  }

  private Map<String, String> parseHeaders(Config headersConfig) {
    Map<String, String> headers = new HashMap<>();

    if (headersConfig.exists() && !headersConfig.isLeaf()) {
      headersConfig
          .asNodeList()
          .orElse(List.of())
          .forEach(
              node -> {
                // For key-value style headers
                String key = node.key().name();
                String value = node.asString().orElse("");
                headers.put(key, value);
              });
    }

    return headers;
  }
}
