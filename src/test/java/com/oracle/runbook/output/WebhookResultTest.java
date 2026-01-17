package com.oracle.runbook.output;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for WebhookResult record following the specification in tasks.md Task 1. */
class WebhookResultTest {

  @Test
  @DisplayName("record has destinationName, success, statusCode, errorMessage, sentAt fields")
  void recordHasRequiredFields() {
    // Arrange
    Instant sentAt = Instant.now();

    // Act
    WebhookResult result = new WebhookResult("slack-oncall", true, 200, Optional.empty(), sentAt);

    // Assert
    assertEquals("slack-oncall", result.destinationName());
    assertTrue(result.success());
    assertEquals(200, result.statusCode());
    assertTrue(result.errorMessage().isEmpty());
    assertEquals(sentAt, result.sentAt());
  }

  @Test
  @DisplayName("success factory method creates successful result with statusCode")
  void successFactoryCreatesSuccessfulResult() {
    // Act
    WebhookResult result = WebhookResult.success("pagerduty-incidents", 201);

    // Assert
    assertEquals("pagerduty-incidents", result.destinationName());
    assertTrue(result.success());
    assertEquals(201, result.statusCode());
    assertTrue(result.errorMessage().isEmpty());
    assertNotNull(result.sentAt());
  }

  @Test
  @DisplayName("failure factory method creates failed result with errorMessage")
  void failureFactoryCreatesFailedResult() {
    // Act
    WebhookResult result = WebhookResult.failure("slack-oncall", "Connection timeout");

    // Assert
    assertEquals("slack-oncall", result.destinationName());
    assertFalse(result.success());
    assertEquals(0, result.statusCode());
    assertTrue(result.errorMessage().isPresent());
    assertEquals("Connection timeout", result.errorMessage().get());
    assertNotNull(result.sentAt());
  }

  @Test
  @DisplayName("isSuccess convenience method returns success field value")
  void isSuccessReturnsSuccessFieldValue() {
    // Arrange
    WebhookResult successResult = WebhookResult.success("test", 200);
    WebhookResult failureResult = WebhookResult.failure("test", "error");

    // Act & Assert
    assertTrue(successResult.isSuccess());
    assertFalse(failureResult.isSuccess());
  }

  @Test
  @DisplayName("record is immutable")
  void recordIsImmutable() {
    // Arrange
    Instant sentAt = Instant.now();
    WebhookResult result = new WebhookResult("webhook", true, 200, Optional.empty(), sentAt);

    // Assert - verify no setters exist (would be compilation error if attempted)
    assertEquals("webhook", result.destinationName());
    assertEquals(200, result.statusCode());
  }
}
