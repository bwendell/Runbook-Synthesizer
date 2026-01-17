package com.oracle.runbook.output;

import java.time.Instant;
import java.util.Optional;

/**
 * Result of sending a checklist to a webhook destination.
 *
 * <p>Captures delivery outcome including HTTP response details for logging, retry logic, and error
 * handling.
 *
 * @param destinationName the name of the webhook destination
 * @param success whether the webhook call succeeded
 * @param statusCode the HTTP status code returned (0 if no response)
 * @param errorMessage optional error message if the call failed
 * @param sentAt timestamp when the delivery was attempted
 */
public record WebhookResult(
    String destinationName,
    boolean success,
    int statusCode,
    Optional<String> errorMessage,
    Instant sentAt) {

  /**
   * Convenience method to check if delivery was successful.
   *
   * @return true if success is true
   */
  public boolean isSuccess() {
    return success;
  }

  /**
   * Factory method for a successful result.
   *
   * @param destinationName the webhook destination name
   * @param statusCode the HTTP status code
   * @return a successful WebhookResult with current timestamp
   */
  public static WebhookResult success(String destinationName, int statusCode) {
    return new WebhookResult(destinationName, true, statusCode, Optional.empty(), Instant.now());
  }

  /**
   * Factory method for a failed result.
   *
   * @param destinationName the webhook destination name
   * @param errorMessage the error message
   * @return a failed WebhookResult with current timestamp and status code 0
   */
  public static WebhookResult failure(String destinationName, String errorMessage) {
    return new WebhookResult(destinationName, false, 0, Optional.of(errorMessage), Instant.now());
  }

  /**
   * Factory method for a failed result with HTTP status code.
   *
   * @param destinationName the webhook destination name
   * @param statusCode the HTTP status code (e.g., 500 for server error)
   * @param errorMessage the error message
   * @return a failed WebhookResult with current timestamp and status code
   */
  public static WebhookResult failure(String destinationName, int statusCode, String errorMessage) {
    return new WebhookResult(
        destinationName, false, statusCode, Optional.of(errorMessage), Instant.now());
  }
}
