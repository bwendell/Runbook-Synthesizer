package com.oracle.runbook.output;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.oracle.runbook.config.WebhookConfigLoader;
import com.oracle.runbook.domain.DynamicChecklist;
import com.oracle.runbook.output.adapters.GenericWebhookDestination;
import io.helidon.common.media.type.MediaTypes;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Integration tests for the webhook output framework using WireMock. */
@DisplayName("WebhookIntegrationTest")
class WebhookIntegrationTest {

  private WireMockServer wireMockServer;

  @BeforeEach
  void setUp() {
    wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    wireMockServer.start();
    WireMock.configureFor("localhost", wireMockServer.port());
  }

  @AfterEach
  void tearDown() {
    wireMockServer.stop();
  }

  @Test
  @DisplayName("GenericWebhookDestination successfully sends checklist to endpoint")
  void genericWebhookSendsChecklistSuccessfully() throws Exception {
    // Setup WireMock stub
    stubFor(post(urlEqualTo("/webhook")).willReturn(aResponse().withStatus(200).withBody("OK")));

    // Create config pointing to WireMock
    WebhookConfig config =
        WebhookConfig.builder()
            .name("test-webhook")
            .type("generic")
            .url(wireMockServer.baseUrl() + "/webhook")
            .build();

    GenericWebhookDestination destination = new GenericWebhookDestination(config);
    DynamicChecklist checklist = createTestChecklist();

    // Send the checklist
    WebhookResult result = destination.send(checklist).get();

    // Verify success
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.statusCode()).isEqualTo(200);

    // Verify the request was made
    verify(
        postRequestedFor(urlEqualTo("/webhook"))
            .withHeader("Content-Type", containing("application/json")));
  }

  @Test
  @DisplayName("GenericWebhookDestination handles 500 error gracefully")
  void genericWebhookHandlesServerError() throws Exception {
    // Setup WireMock to return 500
    stubFor(
        post(urlEqualTo("/webhook"))
            .willReturn(aResponse().withStatus(500).withBody("Internal Server Error")));

    WebhookConfig config =
        WebhookConfig.builder()
            .name("test-webhook")
            .type("generic")
            .url(wireMockServer.baseUrl() + "/webhook")
            .build();

    GenericWebhookDestination destination = new GenericWebhookDestination(config);
    DynamicChecklist checklist = createTestChecklist();

    WebhookResult result = destination.send(checklist).get();

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errorMessage()).isPresent();
  }

  @Test
  @DisplayName("WebhookDispatcher dispatches to multiple destinations")
  void dispatcherSendsToMultipleDestinations() throws Exception {
    // Setup two WireMock stubs
    stubFor(post(urlEqualTo("/webhook1")).willReturn(aResponse().withStatus(200).withBody("OK")));
    stubFor(post(urlEqualTo("/webhook2")).willReturn(aResponse().withStatus(200).withBody("OK")));

    WebhookConfig config1 =
        WebhookConfig.builder()
            .name("webhook-1")
            .type("generic")
            .url(wireMockServer.baseUrl() + "/webhook1")
            .build();

    WebhookConfig config2 =
        WebhookConfig.builder()
            .name("webhook-2")
            .type("generic")
            .url(wireMockServer.baseUrl() + "/webhook2")
            .build();

    List<WebhookDestination> destinations =
        List.of(new GenericWebhookDestination(config1), new GenericWebhookDestination(config2));

    WebhookDispatcher dispatcher = new WebhookDispatcher(destinations);
    DynamicChecklist checklist = createTestChecklist();

    List<WebhookResult> results = dispatcher.dispatch(checklist).get();

    assertThat(results).hasSize(2);
    assertThat(results).allMatch(WebhookResult::isSuccess);

    // Verify both endpoints were called
    verify(postRequestedFor(urlEqualTo("/webhook1")));
    verify(postRequestedFor(urlEqualTo("/webhook2")));
  }

  @Test
  @DisplayName("Full flow: Config loading to dispatch")
  void fullFlowFromConfigToDispatch() throws Exception {
    // Setup WireMock
    stubFor(
        post(urlEqualTo("/configured-webhook"))
            .willReturn(aResponse().withStatus(200).withBody("OK")));

    // Create config
    String yaml =
        String.format(
            """
                        output:
                          webhooks:
                            - name: configured-webhook
                              type: generic
                              url: %s/configured-webhook
                              enabled: true
                        """,
            wireMockServer.baseUrl());

    ByteArrayInputStream stream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
    Config config = Config.just(ConfigSources.create(stream, MediaTypes.APPLICATION_X_YAML));

    // Load configs
    WebhookConfigLoader loader = new WebhookConfigLoader(config);
    List<WebhookConfig> webhookConfigs = loader.loadEnabledWebhookConfigs();

    // Create destinations via factory
    WebhookDestinationFactory factory = new WebhookDestinationFactory();
    List<WebhookDestination> destinations = webhookConfigs.stream().map(factory::create).toList();

    // Create dispatcher and send
    WebhookDispatcher dispatcher = new WebhookDispatcher(destinations);
    DynamicChecklist checklist = createTestChecklist();

    List<WebhookResult> results = dispatcher.dispatch(checklist).get();

    assertThat(results).hasSize(1);
    assertThat(results.getFirst().isSuccess()).isTrue();

    verify(postRequestedFor(urlEqualTo("/configured-webhook")));
  }

  private DynamicChecklist createTestChecklist() {
    return new DynamicChecklist(
        "alert-123",
        "Test Checklist Summary",
        List.of(),
        List.of("runbook-1"),
        Instant.now(),
        "test-llm");
  }
}
