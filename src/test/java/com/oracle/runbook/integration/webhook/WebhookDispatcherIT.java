package com.oracle.runbook.integration.webhook;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.oracle.runbook.domain.*;
import com.oracle.runbook.integration.IntegrationTestBase;
import com.oracle.runbook.output.*;
import com.oracle.runbook.output.adapters.GenericWebhookDestination;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.WebServerConfig;
import io.helidon.webserver.testing.junit5.SetUpServer;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for WebhookDispatcher multi-channel delivery.
 *
 * <p>Tests verify that checklists are correctly dispatched to multiple webhook destinations with
 * proper filtering and WireMock captures the HTTP requests.
 */
class WebhookDispatcherIT extends IntegrationTestBase {

  WebhookDispatcherIT(WebServer server) {
    super(server);
  }

  @SetUpServer
  static void setup(WebServerConfig.Builder builder) {
    builder.routing(routing -> routing.get("/health", (req, res) -> res.send("OK")));
  }

  @BeforeEach
  void setUp() {
    resetWireMock();
  }

  @Test
  void dispatch_ToMultipleChannels_AllReceiveChecklist() {
    // Given: Stub two webhook endpoints (Slack and PagerDuty)
    wireMockServer.stubFor(
        post(urlPathEqualTo("/slack-webhook"))
            .willReturn(aResponse().withStatus(200).withBody("ok")));

    wireMockServer.stubFor(
        post(urlPathEqualTo("/pagerduty-webhook"))
            .willReturn(aResponse().withStatus(202).withBody("{\"status\": \"accepted\"}")));

    // Given: Create two webhook destinations
    WebhookConfig slackConfig =
        WebhookConfig.builder()
            .name("slack-oncall")
            .type("generic")
            .url(wireMockBaseUrl() + "/slack-webhook")
            .build();

    WebhookConfig pagerDutyConfig =
        WebhookConfig.builder()
            .name("pagerduty-incidents")
            .type("generic")
            .url(wireMockBaseUrl() + "/pagerduty-webhook")
            .build();

    List<WebhookDestination> destinations =
        List.of(
            new GenericWebhookDestination(slackConfig),
            new GenericWebhookDestination(pagerDutyConfig));

    WebhookDispatcher dispatcher = new WebhookDispatcher(destinations);

    // Given: A critical severity checklist
    DynamicChecklist checklist = createTestChecklist(AlertSeverity.CRITICAL);

    // When: Dispatch the checklist
    List<WebhookResult> results = dispatcher.dispatchSync(checklist);

    // Then: Both webhooks received the checklist
    assertThat(results).hasSize(2);
    assertThat(results).allMatch(WebhookResult::isSuccess);

    // Then: Verify WireMock received both requests
    wireMockServer.verify(postRequestedFor(urlPathEqualTo("/slack-webhook")));
    wireMockServer.verify(postRequestedFor(urlPathEqualTo("/pagerduty-webhook")));
  }

  @Test
  void dispatch_WithSeverityFilter_OnlySendsToMatchingDestinations() {
    // Given: Stub webhook endpoint
    wireMockServer.stubFor(
        post(urlPathEqualTo("/critical-only")).willReturn(aResponse().withStatus(200)));

    // Given: Webhook configured for CRITICAL severity only
    WebhookFilter criticalOnlyFilter = new WebhookFilter(Set.of(AlertSeverity.CRITICAL), Map.of());

    WebhookConfig criticalConfig =
        WebhookConfig.builder()
            .name("critical-alerts")
            .type("generic")
            .url(wireMockBaseUrl() + "/critical-only")
            .filter(criticalOnlyFilter)
            .build();

    // Given: Use a test destination that respects the filter
    WebhookDestination destination = new FilterAwareWebhookDestination(criticalConfig);
    WebhookDispatcher dispatcher = new WebhookDispatcher(List.of(destination));

    // When: Dispatch a WARNING severity checklist
    DynamicChecklist warningChecklist = createTestChecklist(AlertSeverity.WARNING);
    List<WebhookResult> results = dispatcher.dispatchSync(warningChecklist);

    // Then: No results returned (filtered out)
    assertThat(results).isEmpty();

    // Then: Verify webhook was NOT called
    wireMockServer.verify(0, postRequestedFor(urlPathEqualTo("/critical-only")));
  }

  @Test
  void dispatch_CriticalSeverity_SendsToCriticalFilteredWebhook() {
    // Given: Stub webhook endpoint
    wireMockServer.stubFor(
        post(urlPathEqualTo("/critical-only")).willReturn(aResponse().withStatus(200)));

    // Given: Webhook configured for CRITICAL severity only
    WebhookFilter criticalOnlyFilter = new WebhookFilter(Set.of(AlertSeverity.CRITICAL), Map.of());

    WebhookConfig criticalConfig =
        WebhookConfig.builder()
            .name("critical-alerts")
            .type("generic")
            .url(wireMockBaseUrl() + "/critical-only")
            .filter(criticalOnlyFilter)
            .build();

    WebhookDestination destination = new FilterAwareWebhookDestination(criticalConfig);
    WebhookDispatcher dispatcher = new WebhookDispatcher(List.of(destination));

    // When: Dispatch a CRITICAL severity checklist
    DynamicChecklist criticalChecklist = createTestChecklist(AlertSeverity.CRITICAL);
    List<WebhookResult> results = dispatcher.dispatchSync(criticalChecklist);

    // Then: One result returned (matched filter)
    assertThat(results).hasSize(1);
    assertThat(results.get(0).isSuccess()).isTrue();

    // Then: Verify webhook WAS called
    wireMockServer.verify(1, postRequestedFor(urlPathEqualTo("/critical-only")));
  }

  @Test
  void dispatch_WebhookFailure_ReturnsFailureResult() {
    // Given: Webhook returns 500 error
    wireMockServer.stubFor(
        post(urlPathEqualTo("/failing-webhook"))
            .willReturn(aResponse().withStatus(500).withBody("Internal Server Error")));

    WebhookConfig failingConfig =
        WebhookConfig.builder()
            .name("failing-webhook")
            .type("generic")
            .url(wireMockBaseUrl() + "/failing-webhook")
            .build();

    WebhookDispatcher dispatcher =
        new WebhookDispatcher(List.of(new GenericWebhookDestination(failingConfig)));

    // When: Dispatch checklist
    DynamicChecklist checklist = createTestChecklist(AlertSeverity.WARNING);
    List<WebhookResult> results = dispatcher.dispatchSync(checklist);

    // Then: Failure result returned
    assertThat(results).hasSize(1);
    assertThat(results.get(0).isSuccess()).isFalse();
    assertThat(results.get(0).statusCode()).isEqualTo(500);
  }

  // ========== Helper Methods ==========

  private DynamicChecklist createTestChecklist(AlertSeverity severity) {
    ChecklistStep step1 =
        new ChecklistStep(
            1,
            "Check system logs",
            "First diagnostic step",
            null,
            null,
            StepPriority.HIGH,
            List.of());

    ChecklistStep step2 =
        new ChecklistStep(
            2,
            "Review metrics dashboard",
            "Correlate with metrics",
            null,
            null,
            StepPriority.MEDIUM,
            List.of());

    // Store severity in the checklist summary for filter evaluation
    return new DynamicChecklist(
        "alert-test-001",
        severity.name() + ": High Memory Usage troubleshooting checklist",
        List.of(step1, step2),
        List.of("runbooks/memory.md"),
        Instant.now(),
        "test-llm");
  }

  /**
   * Test WebhookDestination that respects filter configuration. The GenericWebhookDestination
   * doesn't have access to severity info, so we use this test implementation.
   */
  private class FilterAwareWebhookDestination implements WebhookDestination {
    private final WebhookConfig config;
    private final GenericWebhookDestination delegate;

    FilterAwareWebhookDestination(WebhookConfig config) {
      this.config = config;
      this.delegate = new GenericWebhookDestination(config);
    }

    @Override
    public String name() {
      return config.name();
    }

    @Override
    public String type() {
      return config.type();
    }

    @Override
    public WebhookConfig config() {
      return config;
    }

    @Override
    public java.util.concurrent.CompletableFuture<WebhookResult> send(DynamicChecklist checklist) {
      return delegate.send(checklist);
    }

    @Override
    public boolean shouldSend(DynamicChecklist checklist) {
      // Extract severity from summary (test hack - real impl would have alert
      // context)
      AlertSeverity severity = extractSeverityFromSummary(checklist.summary());
      return config.filter().matches(severity, Map.of());
    }

    private AlertSeverity extractSeverityFromSummary(String summary) {
      for (AlertSeverity sev : AlertSeverity.values()) {
        if (summary.startsWith(sev.name())) {
          return sev;
        }
      }
      return AlertSeverity.INFO;
    }
  }
}
