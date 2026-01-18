package com.oracle.runbook.output;

import static org.assertj.core.api.Assertions.assertThat;

import com.oracle.runbook.domain.ChecklistStep;
import com.oracle.runbook.domain.DynamicChecklist;
import com.oracle.runbook.domain.StepPriority;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for the WebhookDestination interface and WebhookResult record contracts. */
class WebhookDestinationTest {

  @Test
  @DisplayName("name returns the webhook destination name")
  void name_returnsWebhookName() {
    // Arrange
    WebhookDestination webhook = new TestWebhookDestination("slack-oncall", "slack");

    // Act & Assert
    assertThat(webhook.name()).isEqualTo("slack-oncall");
  }

  @Test
  @DisplayName("type returns the webhook type")
  void type_returnsWebhookType() {
    // Arrange
    WebhookDestination webhook = new TestWebhookDestination("pd-incidents", "pagerduty");

    // Act & Assert
    assertThat(webhook.type()).isEqualTo("pagerduty");
  }

  @Test
  @DisplayName("send accepts DynamicChecklist and returns CompletableFuture<WebhookResult>")
  void send_acceptsChecklist_returnsWebhookResultFuture() throws Exception {
    // Arrange
    WebhookDestination webhook = new TestWebhookDestination("slack-oncall", "slack");
    DynamicChecklist checklist = createTestChecklist();

    // Act
    CompletableFuture<WebhookResult> future = webhook.send(checklist);
    WebhookResult result = future.get();

    // Assert
    assertThat(future).isNotNull();
    assertThat(result).isNotNull();
    assertThat(result.success()).isTrue();
    assertThat(result.statusCode()).isEqualTo(200);
  }

  @Test
  @DisplayName("shouldSend returns boolean based on filter logic")
  void shouldSend_returnsBoolean() {
    // Arrange
    WebhookDestination webhook = new TestWebhookDestination("slack-oncall", "slack");
    DynamicChecklist checklist = createTestChecklist();

    // Act & Assert
    assertThat(webhook.shouldSend(checklist)).isTrue();
  }

  @Test
  @DisplayName("config returns the underlying WebhookConfig")
  void config_returnsWebhookConfig() {
    // Arrange
    WebhookConfig config =
        WebhookConfig.builder()
            .name("slack-oncall")
            .type("slack")
            .url("https://hooks.slack.com/test")
            .build();
    WebhookDestination webhook = new TestWebhookDestination(config);

    // Act & Assert
    assertThat(webhook.config()).isEqualTo(config);
    assertThat(webhook.name()).isEqualTo("slack-oncall");
    assertThat(webhook.type()).isEqualTo("slack");
  }

  @Test
  @DisplayName("WebhookResult captures HTTP response details")
  void webhookResult_capturesResponseDetails() {
    // Arrange & Act
    WebhookResult successResult = WebhookResult.success("test-webhook", 200);
    WebhookResult failResult = WebhookResult.failure("test-webhook", "Internal server error");

    // Assert
    assertThat(successResult.success()).isTrue();
    assertThat(successResult.statusCode()).isEqualTo(200);
    assertThat(successResult.errorMessage()).isEmpty();
    assertThat(successResult.destinationName()).isEqualTo("test-webhook");

    assertThat(failResult.success()).isFalse();
    assertThat(failResult.errorMessage()).isPresent();
    assertThat(failResult.errorMessage().get()).isEqualTo("Internal server error");
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

  /** Test implementation of WebhookDestination for verifying interface contract. */
  private static class TestWebhookDestination implements WebhookDestination {
    private final WebhookConfig config;

    TestWebhookDestination(String name, String type) {
      this.config =
          WebhookConfig.builder().name(name).type(type).url("https://example.com/webhook").build();
    }

    TestWebhookDestination(WebhookConfig config) {
      this.config = config;
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
    public CompletableFuture<WebhookResult> send(DynamicChecklist checklist) {
      WebhookResult result = WebhookResult.success(config.name(), 200);
      return CompletableFuture.completedFuture(result);
    }

    @Override
    public boolean shouldSend(DynamicChecklist checklist) {
      return true;
    }
  }
}
