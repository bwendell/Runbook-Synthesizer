package com.oracle.runbook.config;

import static org.junit.jupiter.api.Assertions.*;

import com.oracle.runbook.output.WebhookConfig;
import io.helidon.common.media.type.MediaTypes;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link WebhookConfigLoader}. */
@DisplayName("WebhookConfigLoader")
class WebhookConfigLoaderTest {

  private Config createYamlConfig(String yaml) {
    ByteArrayInputStream stream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
    return Config.just(ConfigSources.create(stream, MediaTypes.APPLICATION_X_YAML));
  }

  @Test
  @DisplayName("loadWebhookConfigs() parses webhooks from config")
  void loadWebhookConfigsParsesWebhooks() {
    Config config =
        createYamlConfig(
            """
                        output:
                          webhooks:
                            - name: test-webhook
                              type: generic
                              url: https://example.com/webhook
                              enabled: true
                        """);

    WebhookConfigLoader loader = new WebhookConfigLoader(config);

    List<WebhookConfig> configs = loader.loadWebhookConfigs();

    assertEquals(1, configs.size());
    WebhookConfig webhookConfig = configs.getFirst();
    assertEquals("test-webhook", webhookConfig.name());
    assertEquals("generic", webhookConfig.type());
    assertEquals("https://example.com/webhook", webhookConfig.url());
    assertTrue(webhookConfig.enabled());
  }

  @Test
  @DisplayName("loadWebhookConfigs() parses multiple webhooks")
  void loadWebhookConfigsParsesMultipleWebhooks() {
    Config config =
        createYamlConfig(
            """
                        output:
                          webhooks:
                            - name: webhook-1
                              type: slack
                              url: https://hooks.slack.com/xxx
                              enabled: true
                            - name: webhook-2
                              type: pagerduty
                              url: https://events.pagerduty.com/v2/enqueue
                              enabled: true
                        """);

    WebhookConfigLoader loader = new WebhookConfigLoader(config);

    List<WebhookConfig> configs = loader.loadWebhookConfigs();

    assertEquals(2, configs.size());
    assertEquals("webhook-1", configs.get(0).name());
    assertEquals("webhook-2", configs.get(1).name());
  }

  @Test
  @DisplayName("loadWebhookConfigs() returns empty list when no webhooks configured")
  void loadWebhookConfigsReturnsEmptyWhenNoneConfigured() {
    Config config = createYamlConfig("server:\n  port: 8080");

    WebhookConfigLoader loader = new WebhookConfigLoader(config);

    List<WebhookConfig> configs = loader.loadWebhookConfigs();

    assertTrue(configs.isEmpty());
  }

  @Test
  @DisplayName("loadWebhookConfigs() parses headers from config")
  void loadWebhookConfigsParsesHeaders() {
    Config config =
        createYamlConfig(
            """
                        output:
                          webhooks:
                            - name: auth-webhook
                              type: generic
                              url: https://example.com/webhook
                              enabled: true
                              headers:
                                Authorization: Bearer token123
                                X-Custom-Header: custom-value
                        """);

    WebhookConfigLoader loader = new WebhookConfigLoader(config);

    List<WebhookConfig> configs = loader.loadWebhookConfigs();

    assertEquals(1, configs.size());
    WebhookConfig webhookConfig = configs.getFirst();
    assertEquals("Bearer token123", webhookConfig.headers().get("Authorization"));
    assertEquals("custom-value", webhookConfig.headers().get("X-Custom-Header"));
  }

  @Test
  @DisplayName("loadWebhookConfigs() skips disabled webhooks when using loadEnabledWebhookConfigs")
  void loadWebhookConfigsSkipsDisabled() {
    Config config =
        createYamlConfig(
            """
                        output:
                          webhooks:
                            - name: enabled-webhook
                              type: generic
                              url: https://example.com/enabled
                              enabled: true
                            - name: disabled-webhook
                              type: generic
                              url: https://example.com/disabled
                              enabled: false
                        """);

    WebhookConfigLoader loader = new WebhookConfigLoader(config);

    List<WebhookConfig> configs = loader.loadEnabledWebhookConfigs();

    assertEquals(1, configs.size());
    assertEquals("enabled-webhook", configs.getFirst().name());
  }

  @Test
  @DisplayName("loadWebhookConfigs() defaults enabled to true when not specified")
  void loadWebhookConfigsDefaultsEnabledToTrue() {
    Config config =
        createYamlConfig(
            """
                        output:
                          webhooks:
                            - name: default-enabled
                              type: generic
                              url: https://example.com/webhook
                        """);

    WebhookConfigLoader loader = new WebhookConfigLoader(config);

    List<WebhookConfig> configs = loader.loadWebhookConfigs();

    assertEquals(1, configs.size());
    assertTrue(configs.getFirst().enabled());
  }
}
