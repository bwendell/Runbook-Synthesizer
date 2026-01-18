package com.oracle.runbook.config;

import static org.assertj.core.api.Assertions.assertThat;

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

    assertThat(configs).hasSize(1);
    WebhookConfig webhookConfig = configs.getFirst();
    assertThat(webhookConfig.name()).isEqualTo("test-webhook");
    assertThat(webhookConfig.type()).isEqualTo("generic");
    assertThat(webhookConfig.url()).isEqualTo("https://example.com/webhook");
    assertThat(webhookConfig.enabled()).isTrue();
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

    assertThat(configs).hasSize(2);
    assertThat(configs.get(0).name()).isEqualTo("webhook-1");
    assertThat(configs.get(1).name()).isEqualTo("webhook-2");
  }

  @Test
  @DisplayName("loadWebhookConfigs() returns empty list when no webhooks configured")
  void loadWebhookConfigsReturnsEmptyWhenNoneConfigured() {
    Config config = createYamlConfig("server:\n  port: 8080");

    WebhookConfigLoader loader = new WebhookConfigLoader(config);

    List<WebhookConfig> configs = loader.loadWebhookConfigs();

    assertThat(configs).isEmpty();
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

    assertThat(configs).hasSize(1);
    WebhookConfig webhookConfig = configs.getFirst();
    assertThat(webhookConfig.headers().get("Authorization")).isEqualTo("Bearer token123");
    assertThat(webhookConfig.headers().get("X-Custom-Header")).isEqualTo("custom-value");
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

    assertThat(configs).hasSize(1);
    assertThat(configs.getFirst().name()).isEqualTo("enabled-webhook");
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

    assertThat(configs).hasSize(1);
    assertThat(configs.getFirst().enabled()).isTrue();
  }
}
