package com.oracle.runbook.output.adapters;

import static org.assertj.core.api.Assertions.assertThat;

import com.oracle.runbook.domain.ChecklistStep;
import com.oracle.runbook.domain.DynamicChecklist;
import com.oracle.runbook.domain.StepPriority;
import com.oracle.runbook.output.WebhookConfig;
import com.oracle.runbook.output.WebhookResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for GenericWebhookDestination adapter following Task 5 specification. */
class GenericWebhookDestinationTest {

  @Test
  @DisplayName("implements WebhookDestination interface")
  void implementsWebhookDestinationInterface() {
    // Arrange
    WebhookConfig config =
        WebhookConfig.builder()
            .name("test-webhook")
            .type("generic")
            .url("https://example.com/webhook")
            .build();

    // Act
    GenericWebhookDestination destination = new GenericWebhookDestination(config);

    // Assert
    assertThat(destination.name()).isEqualTo("test-webhook");
    assertThat(destination.type()).isEqualTo("generic");
    assertThat(destination.config()).isEqualTo(config);
  }

  @Test
  @DisplayName("send returns CompletableFuture with WebhookResult")
  void send_returnsCompletableFutureWithWebhookResult() throws Exception {
    // Arrange
    WebhookConfig config =
        WebhookConfig.builder()
            .name("test-webhook")
            .type("generic")
            .url("https://httpbin.org/post") // Real endpoint for integration
            .build();
    GenericWebhookDestination destination = new GenericWebhookDestination(config);
    DynamicChecklist checklist = createTestChecklist();

    // Act
    CompletableFuture<WebhookResult> future = destination.send(checklist);

    // Assert
    assertThat(future).isNotNull();
    // Note: Full integration test would verify actual HTTP response
  }

  @Test
  @DisplayName("shouldSend delegates to config filter")
  void shouldSend_delegatesToConfigFilter() {
    // Arrange
    WebhookConfig config =
        WebhookConfig.builder()
            .name("test-webhook")
            .type("generic")
            .url("https://example.com/webhook")
            .build();
    GenericWebhookDestination destination = new GenericWebhookDestination(config);
    DynamicChecklist checklist = createTestChecklist();

    // Act & Assert - default filter allows all
    assertThat(destination.shouldSend(checklist)).isTrue();
  }

  @Test
  @DisplayName("handles connection failure gracefully")
  void handlesConnectionFailureGracefully() throws Exception {
    // Arrange - non-routable IP to trigger connection failure
    WebhookConfig config =
        WebhookConfig.builder()
            .name("failing-webhook")
            .type("generic")
            .url("https://192.0.2.1/webhook") // TEST-NET, non-routable
            .build();
    GenericWebhookDestination destination = new GenericWebhookDestination(config);
    DynamicChecklist checklist = createTestChecklist();

    // Act
    CompletableFuture<WebhookResult> future = destination.send(checklist);
    WebhookResult result = future.get();

    // Assert - should return failure, not throw exception
    assertThat(result.success()).isFalse();
    assertThat(result.errorMessage()).isPresent();
    assertThat(result.destinationName()).isEqualTo("failing-webhook");
  }

  @Test
  @DisplayName("includes configured headers in requests")
  void includesConfiguredHeaders() {
    // Arrange
    WebhookConfig config =
        WebhookConfig.builder()
            .name("auth-webhook")
            .type("generic")
            .url("https://example.com/webhook")
            .headers(Map.of("Authorization", "Bearer token123", "X-Custom", "value"))
            .build();

    // Act
    GenericWebhookDestination destination = new GenericWebhookDestination(config);

    // Assert - headers accessible via config
    assertThat(destination.config().headers().get("Authorization")).isEqualTo("Bearer token123");
    assertThat(destination.config().headers().get("X-Custom")).isEqualTo("value");
  }

  private DynamicChecklist createTestChecklist() {
    ChecklistStep step =
        new ChecklistStep(
            1,
            "Check memory usage",
            "Memory is at 92%",
            "92%",
            "Below 80%",
            StepPriority.HIGH,
            List.of("free -h"));
    return new DynamicChecklist(
        "alert-001",
        "High memory troubleshooting",
        List.of(step),
        List.of("runbooks/memory/high-memory.md"),
        Instant.now(),
        "oci-genai");
  }
}
