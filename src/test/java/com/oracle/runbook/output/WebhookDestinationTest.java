package com.oracle.runbook.output;

import static org.junit.jupiter.api.Assertions.*;

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
    assertEquals("slack-oncall", webhook.name());
  }

  @Test
  @DisplayName("type returns the webhook type")
  void type_returnsWebhookType() {
    // Arrange
    WebhookDestination webhook = new TestWebhookDestination("pd-incidents", "pagerduty");

    // Act & Assert
    assertEquals("pagerduty", webhook.type());
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
    assertNotNull(future);
    assertNotNull(result);
    assertTrue(result.success());
    assertEquals(200, result.statusCode());
  }

  @Test
  @DisplayName("shouldSend returns boolean based on filter logic")
  void shouldSend_returnsBoolean() {
    // Arrange
    WebhookDestination webhook = new TestWebhookDestination("slack-oncall", "slack");
    DynamicChecklist checklist = createTestChecklist();

    // Act & Assert
    assertTrue(webhook.shouldSend(checklist));
  }

  @Test
  @DisplayName("WebhookResult captures HTTP response details")
  void webhookResult_capturesResponseDetails() {
    // Arrange & Act
    WebhookResult successResult = WebhookResult.success("test-webhook", 200);
    WebhookResult failResult = WebhookResult.failure("test-webhook", "Internal server error");

    // Assert
    assertTrue(successResult.success());
    assertEquals(200, successResult.statusCode());
    assertTrue(successResult.errorMessage().isEmpty());
    assertEquals("test-webhook", successResult.destinationName());

    assertFalse(failResult.success());
    assertTrue(failResult.errorMessage().isPresent());
    assertEquals("Internal server error", failResult.errorMessage().get());
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
    private final String name;
    private final String type;

    TestWebhookDestination(String name, String type) {
      this.name = name;
      this.type = type;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public String type() {
      return type;
    }

    @Override
    public CompletableFuture<WebhookResult> send(DynamicChecklist checklist) {
      WebhookResult result = WebhookResult.success(name, 200);
      return CompletableFuture.completedFuture(result);
    }

    @Override
    public boolean shouldSend(DynamicChecklist checklist) {
      return true;
    }
  }
}
