package com.oracle.runbook.output;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.oracle.runbook.domain.DynamicChecklist;
import com.oracle.runbook.output.adapters.GenericWebhookDestination;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for webhook retry functionality with exponential backoff. */
@DisplayName("WebhookRetryTest")
class WebhookRetryTest {

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
  @DisplayName("WebhookConfig builder supports retry configuration")
  void webhookConfigBuilderSupportsRetryConfig() {
    WebhookConfig config =
        WebhookConfig.builder()
            .name("test-webhook")
            .type("generic")
            .url("https://example.com/webhook")
            .retryCount(3)
            .retryDelayMs(1000)
            .build();

    assertThat(config.retryCount()).isEqualTo(3);
    assertThat(config.retryDelayMs()).isEqualTo(1000);
  }

  @Test
  @DisplayName("WebhookConfig defaults to 3 retries and 1000ms delay")
  void webhookConfigDefaultsRetryValues() {
    WebhookConfig config =
        WebhookConfig.builder()
            .name("test-webhook")
            .type("generic")
            .url("https://example.com/webhook")
            .build();

    assertThat(config.retryCount()).isEqualTo(3);
    assertThat(config.retryDelayMs()).isEqualTo(1000);
  }

  @Test
  @DisplayName("RetryingWebhookDispatcher retries on 5xx errors")
  void dispatcherRetriesOnServerError() throws Exception {
    // First two calls return 500, third succeeds
    stubFor(
        post(urlEqualTo("/webhook"))
            .inScenario("Retry")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withStatus(500))
            .willSetStateTo("First failure"));

    stubFor(
        post(urlEqualTo("/webhook"))
            .inScenario("Retry")
            .whenScenarioStateIs("First failure")
            .willReturn(aResponse().withStatus(500))
            .willSetStateTo("Second failure"));

    stubFor(
        post(urlEqualTo("/webhook"))
            .inScenario("Retry")
            .whenScenarioStateIs("Second failure")
            .willReturn(aResponse().withStatus(200)));

    WebhookConfig config =
        WebhookConfig.builder()
            .name("retry-webhook")
            .type("generic")
            .url(wireMockServer.baseUrl() + "/webhook")
            .retryCount(3)
            .retryDelayMs(50) // Short delay for testing
            .build();

    GenericWebhookDestination destination = new GenericWebhookDestination(config);
    RetryingWebhookDispatcher dispatcher = new RetryingWebhookDispatcher(List.of(destination));

    DynamicChecklist checklist = createTestChecklist();
    List<WebhookResult> results = dispatcher.dispatch(checklist).get();

    assertThat(results).hasSize(1);
    assertThat(results.getFirst().isSuccess()).isTrue();

    // Verify 3 attempts were made
    verify(3, postRequestedFor(urlEqualTo("/webhook")));
  }

  @Test
  @DisplayName("RetryingWebhookDispatcher does not retry on 4xx errors")
  void dispatcherDoesNotRetryOnClientError() throws Exception {
    stubFor(
        post(urlEqualTo("/webhook"))
            .willReturn(aResponse().withStatus(400).withBody("Bad Request")));

    WebhookConfig config =
        WebhookConfig.builder()
            .name("no-retry-webhook")
            .type("generic")
            .url(wireMockServer.baseUrl() + "/webhook")
            .retryCount(3)
            .retryDelayMs(50)
            .build();

    GenericWebhookDestination destination = new GenericWebhookDestination(config);
    RetryingWebhookDispatcher dispatcher = new RetryingWebhookDispatcher(List.of(destination));

    DynamicChecklist checklist = createTestChecklist();
    List<WebhookResult> results = dispatcher.dispatch(checklist).get();

    assertThat(results).hasSize(1);
    assertThat(results.getFirst().isSuccess()).isFalse();

    // Verify only 1 attempt was made (no retries)
    verify(1, postRequestedFor(urlEqualTo("/webhook")));
  }

  @Test
  @DisplayName("RetryingWebhookDispatcher returns failure after max retries exhausted")
  void dispatcherReturnsFailureAfterMaxRetries() throws Exception {
    stubFor(post(urlEqualTo("/webhook")).willReturn(aResponse().withStatus(500)));

    WebhookConfig config =
        WebhookConfig.builder()
            .name("failing-webhook")
            .type("generic")
            .url(wireMockServer.baseUrl() + "/webhook")
            .retryCount(2)
            .retryDelayMs(50)
            .build();

    GenericWebhookDestination destination = new GenericWebhookDestination(config);
    RetryingWebhookDispatcher dispatcher = new RetryingWebhookDispatcher(List.of(destination));

    DynamicChecklist checklist = createTestChecklist();
    List<WebhookResult> results = dispatcher.dispatch(checklist).get();

    assertThat(results).hasSize(1);
    assertThat(results.getFirst().isSuccess()).isFalse();

    // Verify retryCount + 1 attempts (initial + retries)
    verify(3, postRequestedFor(urlEqualTo("/webhook")));
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
