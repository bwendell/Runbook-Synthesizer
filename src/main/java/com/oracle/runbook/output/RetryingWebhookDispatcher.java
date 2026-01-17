package com.oracle.runbook.output;

import com.oracle.runbook.domain.DynamicChecklist;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A webhook dispatcher with retry support and exponential backoff.
 *
 * <p>This dispatcher wraps individual webhook sends with retry logic. Retries are only attempted
 * for server errors (5xx status codes) and connection failures. Client errors (4xx) are not
 * retried.
 *
 * <h2>Retry Behavior</h2>
 *
 * <ul>
 *   <li>Uses exponential backoff: delay = initialDelay * 2^attemptNumber
 *   <li>Only retries on 5xx errors and connection failures
 *   <li>Does not retry on 4xx client errors
 *   <li>Configurable via retryCount and retryDelayMs in WebhookConfig
 * </ul>
 *
 * @see WebhookDispatcher
 * @see WebhookConfig
 */
public class RetryingWebhookDispatcher {

  private static final Logger LOGGER = Logger.getLogger(RetryingWebhookDispatcher.class.getName());

  private final List<WebhookDestination> destinations;

  /**
   * Creates a new RetryingWebhookDispatcher with the given destinations.
   *
   * @param destinations the list of webhook destinations to dispatch to
   */
  public RetryingWebhookDispatcher(List<WebhookDestination> destinations) {
    this.destinations = List.copyOf(destinations);
    LOGGER.info(
        "RetryingWebhookDispatcher initialized with " + destinations.size() + " destination(s)");
  }

  /**
   * Dispatches the checklist to all matching destinations with retry support.
   *
   * <p>Each destination is handled independently - failures in one destination don't affect others.
   *
   * @param checklist the checklist to dispatch
   * @return a future containing the final results (after retries)
   */
  public CompletableFuture<List<WebhookResult>> dispatch(DynamicChecklist checklist) {
    List<WebhookDestination> matchingDestinations =
        destinations.stream().filter(dest -> dest.shouldSend(checklist)).toList();

    if (matchingDestinations.isEmpty()) {
      LOGGER.fine("No matching destinations for checklist");
      return CompletableFuture.completedFuture(List.of());
    }

    List<CompletableFuture<WebhookResult>> futures =
        matchingDestinations.stream().map(dest -> sendWithRetry(dest, checklist)).toList();

    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .thenApply(v -> futures.stream().map(CompletableFuture::join).toList());
  }

  private CompletableFuture<WebhookResult> sendWithRetry(
      WebhookDestination destination, DynamicChecklist checklist) {
    WebhookConfig config = destination.config();
    int maxAttempts = config.retryCount() + 1; // initial attempt + retries
    int initialDelayMs = config.retryDelayMs();

    return attemptSend(destination, checklist, 0, maxAttempts, initialDelayMs);
  }

  private CompletableFuture<WebhookResult> attemptSend(
      WebhookDestination destination,
      DynamicChecklist checklist,
      int currentAttempt,
      int maxAttempts,
      int initialDelayMs) {

    return destination
        .send(checklist)
        .thenCompose(
            result -> {
              if (result.isSuccess()) {
                return CompletableFuture.completedFuture(result);
              }

              // Check if we should retry
              if (!shouldRetry(result) || currentAttempt >= maxAttempts - 1) {
                if (!shouldRetry(result)) {
                  LOGGER.fine(
                      () ->
                          "Not retrying " + destination.name() + ": client error or non-retryable");
                } else {
                  LOGGER.warning(
                      "Max retries exhausted for "
                          + destination.name()
                          + " after "
                          + (currentAttempt + 1)
                          + " attempt(s)");
                }
                return CompletableFuture.completedFuture(result);
              }

              // Calculate exponential backoff delay
              int nextAttempt = currentAttempt + 1;
              long delayMs = (long) (initialDelayMs * Math.pow(2, currentAttempt));

              LOGGER.info(
                  "Retrying "
                      + destination.name()
                      + " (attempt "
                      + (nextAttempt + 1)
                      + "/"
                      + maxAttempts
                      + ") after "
                      + delayMs
                      + "ms");

              // Use delayed executor for async delay
              Executor delayedExecutor =
                  CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS);

              return CompletableFuture.supplyAsync(() -> null, delayedExecutor)
                  .thenCompose(
                      v ->
                          attemptSend(
                              destination, checklist, nextAttempt, maxAttempts, initialDelayMs));
            })
        .exceptionally(
            ex -> {
              // Handle exceptions as failures
              LOGGER.log(
                  Level.WARNING,
                  "Exception sending to " + destination.name() + ": " + ex.getMessage(),
                  ex);
              return WebhookResult.failure(
                  destination.name(), "Connection error: " + ex.getMessage());
            });
  }

  private boolean shouldRetry(WebhookResult result) {
    // Only retry on 5xx errors or connection failures (no status code)
    // Don't retry on 4xx client errors
    int statusCode = result.statusCode();
    if (statusCode >= 500 && statusCode < 600) {
      return true;
    }
    if (statusCode == 0 && result.errorMessage().isPresent()) {
      // Connection error (no status code means we didn't get a response)
      String error = result.errorMessage().orElse("");
      return error.contains("Connection") || error.contains("timeout");
    }
    return false;
  }
}
