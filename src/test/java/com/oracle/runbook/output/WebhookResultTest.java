package com.oracle.runbook.output;

import static org.assertj.core.api.Assertions.assertThat;

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
    assertThat(result.destinationName()).isEqualTo("slack-oncall");
    assertThat(result.success()).isTrue();
    assertThat(result.statusCode()).isEqualTo(200);
    assertThat(result.errorMessage()).isEmpty();
    assertThat(result.sentAt()).isEqualTo(sentAt);
  }

  @Test
  @DisplayName("success factory method creates successful result with statusCode")
  void successFactoryCreatesSuccessfulResult() {
    // Act
    WebhookResult result = WebhookResult.success("pagerduty-incidents", 201);

    // Assert
    assertThat(result.destinationName()).isEqualTo("pagerduty-incidents");
    assertThat(result.success()).isTrue();
    assertThat(result.statusCode()).isEqualTo(201);
    assertThat(result.errorMessage()).isEmpty();
    assertThat(result.sentAt()).isNotNull();
  }

  @Test
  @DisplayName("failure factory method creates failed result with errorMessage")
  void failureFactoryCreatesFailedResult() {
    // Act
    WebhookResult result = WebhookResult.failure("slack-oncall", "Connection timeout");

    // Assert
    assertThat(result.destinationName()).isEqualTo("slack-oncall");
    assertThat(result.success()).isFalse();
    assertThat(result.statusCode()).isEqualTo(0);
    assertThat(result.errorMessage()).isPresent();
    assertThat(result.errorMessage().get()).isEqualTo("Connection timeout");
    assertThat(result.sentAt()).isNotNull();
  }

  @Test
  @DisplayName("isSuccess convenience method returns success field value")
  void isSuccessReturnsSuccessFieldValue() {
    // Arrange
    WebhookResult successResult = WebhookResult.success("test", 200);
    WebhookResult failureResult = WebhookResult.failure("test", "error");

    // Act & Assert
    assertThat(successResult.isSuccess()).isTrue();
    assertThat(failureResult.isSuccess()).isFalse();
  }

  @Test
  @DisplayName("record is immutable")
  void recordIsImmutable() {
    // Arrange
    Instant sentAt = Instant.now();
    WebhookResult result = new WebhookResult("webhook", true, 200, Optional.empty(), sentAt);

    // Assert - verify no setters exist (would be compilation error if attempted)
    assertThat(result.destinationName()).isEqualTo("webhook");
    assertThat(result.statusCode()).isEqualTo(200);
  }
}
