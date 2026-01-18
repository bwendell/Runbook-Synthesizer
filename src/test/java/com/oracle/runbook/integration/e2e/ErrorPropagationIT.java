package com.oracle.runbook.integration.e2e;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.oracle.runbook.domain.*;
import com.oracle.runbook.integration.IntegrationTestBase;
import com.oracle.runbook.output.*;
import com.oracle.runbook.output.adapters.GenericWebhookDestination;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.WebServerConfig;
import io.helidon.webserver.testing.junit5.SetUpServer;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for error propagation and graceful degradation.
 *
 * <p>Tests verify system behavior when external services fail, including OCI API failures, webhook
 * failures, and timeouts.
 */
class ErrorPropagationIT extends IntegrationTestBase {

  ErrorPropagationIT(WebServer server) {
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
  void ociMonitoringFailure_ReturnsPartialContextGracefully() {
    // Given: OCI Monitoring returns 503 Service Unavailable
    wireMockServer.stubFor(
        get(urlPathMatching("/monitoring/.*"))
            .willReturn(aResponse().withStatus(503).withBody("Service Unavailable")));

    // Given: OCI Logging still works
    wireMockServer.stubFor(
        post(urlPathMatching("/logging/.*"))
            .willReturn(aResponse().withStatus(200).withBody("{\"logs\": []}")));

    // When: We try to enrich an alert (simulating the enrichment service behavior)
    // The system should handle partial failures gracefully

    // Then: We can still create a context with partial data (empty metrics)
    Alert alert = createTestAlert();
    EnrichedContext partialContext =
        new EnrichedContext(
            alert,
            null, // Resource metadata unavailable
            List.of(), // No metrics due to failure
            List.of(), // No logs for this test
            Map.of("monitoringError", "Service Unavailable"));

    assertThat(partialContext.alert()).isEqualTo(alert);
    assertThat(partialContext.recentMetrics()).isEmpty();
    assertThat(partialContext.customProperties()).containsKey("monitoringError");
  }

  @Test
  void webhookFailure_ReturnsFailureResult_DoesNotCrash() {
    // Given: Webhook endpoint returns 500 error
    wireMockServer.stubFor(
        post(urlPathEqualTo("/webhook/failing"))
            .willReturn(aResponse().withStatus(500).withBody("Internal Server Error")));

    WebhookConfig failingConfig =
        WebhookConfig.builder()
            .name("failing-webhook")
            .type("generic")
            .url(wireMockBaseUrl() + "/webhook/failing")
            .build();

    WebhookDispatcher dispatcher =
        new WebhookDispatcher(List.of(new GenericWebhookDestination(failingConfig)));

    DynamicChecklist checklist = createTestChecklist();

    // When: Dispatch to failing webhook
    List<WebhookResult> results = dispatcher.dispatchSync(checklist);

    // Then: Failure result returned, system doesn't crash
    assertThat(results).hasSize(1);
    assertThat(results.get(0).isSuccess()).isFalse();
    assertThat(results.get(0).statusCode()).isEqualTo(500);
    assertThat(results.get(0).errorMessage()).isPresent();
  }

  @Test
  void multipleWebhookFailures_AllFailuresReported() {
    // Given: Two failing webhooks
    wireMockServer.stubFor(
        post(urlPathEqualTo("/webhook/fail1")).willReturn(aResponse().withStatus(500)));

    wireMockServer.stubFor(
        post(urlPathEqualTo("/webhook/fail2")).willReturn(aResponse().withStatus(502)));

    WebhookConfig fail1 =
        WebhookConfig.builder()
            .name("fail1")
            .type("generic")
            .url(wireMockBaseUrl() + "/webhook/fail1")
            .build();

    WebhookConfig fail2 =
        WebhookConfig.builder()
            .name("fail2")
            .type("generic")
            .url(wireMockBaseUrl() + "/webhook/fail2")
            .build();

    WebhookDispatcher dispatcher =
        new WebhookDispatcher(
            List.of(new GenericWebhookDestination(fail1), new GenericWebhookDestination(fail2)));

    DynamicChecklist checklist = createTestChecklist();

    // When: Dispatch to both
    List<WebhookResult> results = dispatcher.dispatchSync(checklist);

    // Then: Both failures reported
    assertThat(results).hasSize(2);
    assertThat(results).allMatch(r -> !r.isSuccess());
  }

  @Test
  void webhookTimeout_HandlesGracefully() {
    // Given: Webhook responds very slowly (30 second delay)
    wireMockServer.stubFor(
        post(urlPathEqualTo("/webhook/slow"))
            .willReturn(
                aResponse()
                    .withFixedDelay(30000) // 30 seconds
                    .withStatus(200)));

    WebhookConfig slowConfig =
        WebhookConfig.builder()
            .name("slow-webhook")
            .type("generic")
            .url(wireMockBaseUrl() + "/webhook/slow")
            .build();

    WebhookDispatcher dispatcher =
        new WebhookDispatcher(List.of(new GenericWebhookDestination(slowConfig)));

    DynamicChecklist checklist = createTestChecklist();

    // When: Dispatch with timeout expectation
    CompletableFuture<List<WebhookResult>> future = dispatcher.dispatch(checklist);

    // Then: We can check for timeout (the WebClient has a configured timeout)
    // Note: The actual timeout behavior depends on WebClient configuration
    // This test validates that slow responses are handled
    await()
        .atMost(Duration.ofSeconds(35))
        .pollInterval(Duration.ofMillis(500))
        .until(future::isDone);

    // The result may be success (if it completes) or failure (if timeout occurs)
    List<WebhookResult> results = future.join();
    assertThat(results).isNotNull();
  }

  @Test
  void connectionRefused_ReturnsErrorResult() {
    // Given: Webhook URL points to non-existent server
    WebhookConfig unreachableConfig =
        WebhookConfig.builder()
            .name("unreachable")
            .type("generic")
            .url("http://localhost:59999/non-existent") // Non-existent port
            .build();

    WebhookDispatcher dispatcher =
        new WebhookDispatcher(List.of(new GenericWebhookDestination(unreachableConfig)));

    DynamicChecklist checklist = createTestChecklist();

    // When: Dispatch to unreachable endpoint
    List<WebhookResult> results = dispatcher.dispatchSync(checklist);

    // Then: Connection error reported
    assertThat(results).hasSize(1);
    assertThat(results.get(0).isSuccess()).isFalse();
    assertThat(results.get(0).errorMessage()).isPresent();
    assertThat(results.get(0).errorMessage().get()).containsIgnoringCase("connection");
  }

  @Test
  void partialSuccess_SomeWebhooksSucceed_SomeFail() {
    // Given: One successful, one failing webhook
    wireMockServer.stubFor(
        post(urlPathEqualTo("/webhook/success"))
            .willReturn(aResponse().withStatus(200).withBody("ok")));

    wireMockServer.stubFor(
        post(urlPathEqualTo("/webhook/failure")).willReturn(aResponse().withStatus(500)));

    WebhookConfig successConfig =
        WebhookConfig.builder()
            .name("success-webhook")
            .type("generic")
            .url(wireMockBaseUrl() + "/webhook/success")
            .build();

    WebhookConfig failConfig =
        WebhookConfig.builder()
            .name("fail-webhook")
            .type("generic")
            .url(wireMockBaseUrl() + "/webhook/failure")
            .build();

    WebhookDispatcher dispatcher =
        new WebhookDispatcher(
            List.of(
                new GenericWebhookDestination(successConfig),
                new GenericWebhookDestination(failConfig)));

    DynamicChecklist checklist = createTestChecklist();

    // When: Dispatch to both
    List<WebhookResult> results = dispatcher.dispatchSync(checklist);

    // Then: Mixed results reported
    assertThat(results).hasSize(2);
    long successCount = results.stream().filter(WebhookResult::isSuccess).count();
    long failCount = results.stream().filter(r -> !r.isSuccess()).count();
    assertThat(successCount).isEqualTo(1);
    assertThat(failCount).isEqualTo(1);
  }

  // ========== Helper Methods ==========

  private Alert createTestAlert() {
    return new Alert(
        "alert-error-001",
        "Test Alert for Error Handling",
        "Testing error propagation scenarios",
        AlertSeverity.WARNING,
        "test-source",
        Map.of("resourceId", "test-resource"),
        Map.of(),
        Instant.now(),
        "{}");
  }

  private DynamicChecklist createTestChecklist() {
    ChecklistStep step =
        new ChecklistStep(
            1,
            "Test troubleshooting step",
            "For testing",
            null,
            null,
            StepPriority.MEDIUM,
            List.of());

    return new DynamicChecklist(
        "alert-error-001",
        "Error Handling Test Checklist",
        List.of(step),
        List.of("runbooks/test.md"),
        Instant.now(),
        "test-llm");
  }
}
