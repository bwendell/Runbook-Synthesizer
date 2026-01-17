package com.oracle.runbook.output;

import java.util.Optional;

/**
 * Result of sending a checklist to a webhook destination.
 *
 * <p>Captures HTTP response details for logging, retry logic, and error handling.
 *
 * @param success whether the webhook call succeeded
 * @param statusCode the HTTP status code returned
 * @param responseBody the response body content
 * @param errorMessage optional error message if the call failed
 */
public record WebhookResult(
    boolean success, int statusCode, String responseBody, Optional<String> errorMessage) {
  /**
   * Factory method for a successful result.
   *
   * @param statusCode the HTTP status code
   * @param responseBody the response body
   * @return a successful WebhookResult
   */
  public static WebhookResult success(int statusCode, String responseBody) {
    return new WebhookResult(true, statusCode, responseBody, Optional.empty());
  }

  /**
   * Factory method for a failed result.
   *
   * @param statusCode the HTTP status code
   * @param errorMessage the error message
   * @return a failed WebhookResult
   */
  public static WebhookResult failure(int statusCode, String errorMessage) {
    return new WebhookResult(false, statusCode, "", Optional.of(errorMessage));
  }
}
