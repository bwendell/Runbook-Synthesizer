package com.oracle.runbook.output;

import com.oracle.runbook.domain.DynamicChecklist;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Orchestrates delivery of checklists to multiple webhook destinations.
 *
 * <p>The dispatcher routes checklists to all configured destinations that pass their filtering
 * criteria, executing deliveries in parallel for optimal performance.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * List<WebhookDestination> destinations = List.of(
 *         new GenericWebhookDestination(config1),
 *         new GenericWebhookDestination(config2));
 * WebhookDispatcher dispatcher = new WebhookDispatcher(destinations);
 *
 * // Async dispatch
 * CompletableFuture<List<WebhookResult>> resultsFuture = dispatcher.dispatch(checklist);
 *
 * // Sync dispatch for testing
 * List<WebhookResult> results = dispatcher.dispatchSync(checklist);
 * }</pre>
 *
 * @see WebhookDestination
 * @see WebhookResult
 */
public class WebhookDispatcher {

  private static final Logger LOGGER = Logger.getLogger(WebhookDispatcher.class.getName());

  private final List<WebhookDestination> destinations;

  /**
   * Creates a new WebhookDispatcher with the given destinations.
   *
   * @param destinations the list of webhook destinations to dispatch to
   */
  public WebhookDispatcher(List<WebhookDestination> destinations) {
    this.destinations = List.copyOf(destinations);
    LOGGER.info("WebhookDispatcher initialized with " + destinations.size() + " destination(s)");
  }

  /**
   * Dispatches the checklist to all matching destinations asynchronously.
   *
   * <p>Only destinations where {@link WebhookDestination#shouldSend(DynamicChecklist)} returns
   * {@code true} will receive the checklist. All matching destinations are notified in parallel.
   *
   * @param checklist the checklist to dispatch
   * @return a future containing the list of results from all destinations
   */
  public CompletableFuture<List<WebhookResult>> dispatch(DynamicChecklist checklist) {
    List<WebhookDestination> matchingDestinations =
        destinations.stream().filter(dest -> shouldSendTo(dest, checklist)).toList();

    if (matchingDestinations.isEmpty()) {
      LOGGER.fine("No matching destinations for checklist");
      return CompletableFuture.completedFuture(List.of());
    }

    LOGGER.info(
        "Dispatching checklist to "
            + matchingDestinations.size()
            + " destination(s): "
            + matchingDestinations.stream().map(WebhookDestination::name).toList());

    // Create futures for all matching destinations
    List<CompletableFuture<WebhookResult>> futures =
        matchingDestinations.stream().map(dest -> dest.send(checklist)).toList();

    // Wait for all to complete and collect results
    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .thenApply(v -> futures.stream().map(CompletableFuture::join).toList());
  }

  /**
   * Dispatches the checklist synchronously and returns the results.
   *
   * <p>This is a convenience method for testing and situations where async behavior is not needed.
   *
   * @param checklist the checklist to dispatch
   * @return the list of results from all destinations
   */
  public List<WebhookResult> dispatchSync(DynamicChecklist checklist) {
    return dispatch(checklist).join();
  }

  /**
   * Returns the number of configured destinations.
   *
   * @return the destination count
   */
  public int destinationCount() {
    return destinations.size();
  }

  private boolean shouldSendTo(WebhookDestination destination, DynamicChecklist checklist) {
    try {
      boolean shouldSend = destination.shouldSend(checklist);
      if (!shouldSend) {
        LOGGER.fine("Skipping destination '" + destination.name() + "' - filter criteria not met");
      }
      return shouldSend;
    } catch (Exception e) {
      LOGGER.log(
          Level.WARNING,
          "Error checking shouldSend for destination '" + destination.name() + "'",
          e);
      return false;
    }
  }
}
