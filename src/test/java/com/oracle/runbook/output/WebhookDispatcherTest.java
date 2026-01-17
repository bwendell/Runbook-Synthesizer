package com.oracle.runbook.output;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.oracle.runbook.domain.DynamicChecklist;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link WebhookDispatcher}. */
@DisplayName("WebhookDispatcher")
class WebhookDispatcherTest {

  private WebhookDestination destination1;
  private WebhookDestination destination2;
  private WebhookDestination destination3;
  private DynamicChecklist checklist;

  @BeforeEach
  void setUp() {
    destination1 = mock(WebhookDestination.class);
    destination2 = mock(WebhookDestination.class);
    destination3 = mock(WebhookDestination.class);

    when(destination1.name()).thenReturn("dest-1");
    when(destination2.name()).thenReturn("dest-2");
    when(destination3.name()).thenReturn("dest-3");

    checklist = createTestChecklist();
  }

  @Test
  @DisplayName("dispatch() sends to all destinations that should receive")
  void dispatchSendsToAllMatchingDestinations() throws Exception {
    when(destination1.shouldSend(checklist)).thenReturn(true);
    when(destination2.shouldSend(checklist)).thenReturn(true);
    when(destination1.send(checklist))
        .thenReturn(CompletableFuture.completedFuture(WebhookResult.success("dest-1", 200)));
    when(destination2.send(checklist))
        .thenReturn(CompletableFuture.completedFuture(WebhookResult.success("dest-2", 200)));

    WebhookDispatcher dispatcher = new WebhookDispatcher(List.of(destination1, destination2));

    List<WebhookResult> results = dispatcher.dispatch(checklist).get();

    assertEquals(2, results.size());
    assertTrue(results.stream().allMatch(WebhookResult::isSuccess));
    verify(destination1).send(checklist);
    verify(destination2).send(checklist);
  }

  @Test
  @DisplayName("dispatch() skips destinations where shouldSend returns false")
  void dispatchSkipsFilteredDestinations() throws Exception {
    when(destination1.shouldSend(checklist)).thenReturn(true);
    when(destination2.shouldSend(checklist)).thenReturn(false);
    when(destination1.send(checklist))
        .thenReturn(CompletableFuture.completedFuture(WebhookResult.success("dest-1", 200)));

    WebhookDispatcher dispatcher = new WebhookDispatcher(List.of(destination1, destination2));

    List<WebhookResult> results = dispatcher.dispatch(checklist).get();

    assertEquals(1, results.size());
    assertEquals("dest-1", results.getFirst().destinationName());
    verify(destination1).send(checklist);
    verify(destination2, never()).send(checklist);
  }

  @Test
  @DisplayName("dispatch() returns empty list when no destinations match")
  void dispatchReturnsEmptyWhenNoMatches() throws Exception {
    when(destination1.shouldSend(checklist)).thenReturn(false);
    when(destination2.shouldSend(checklist)).thenReturn(false);

    WebhookDispatcher dispatcher = new WebhookDispatcher(List.of(destination1, destination2));

    List<WebhookResult> results = dispatcher.dispatch(checklist).get();

    assertTrue(results.isEmpty());
    verify(destination1, never()).send(checklist);
    verify(destination2, never()).send(checklist);
  }

  @Test
  @DisplayName("dispatch() collects both success and failure results")
  void dispatchCollectsMixedResults() throws Exception {
    when(destination1.shouldSend(checklist)).thenReturn(true);
    when(destination2.shouldSend(checklist)).thenReturn(true);
    when(destination1.send(checklist))
        .thenReturn(CompletableFuture.completedFuture(WebhookResult.success("dest-1", 200)));
    when(destination2.send(checklist))
        .thenReturn(
            CompletableFuture.completedFuture(
                WebhookResult.failure("dest-2", "Connection refused")));

    WebhookDispatcher dispatcher = new WebhookDispatcher(List.of(destination1, destination2));

    List<WebhookResult> results = dispatcher.dispatch(checklist).get();

    assertEquals(2, results.size());
    long successCount = results.stream().filter(WebhookResult::isSuccess).count();
    long failureCount = results.stream().filter(r -> !r.isSuccess()).count();
    assertEquals(1, successCount);
    assertEquals(1, failureCount);
  }

  @Test
  @DisplayName("dispatch() dispatches to all destinations in parallel")
  void dispatchExecutesInParallel() throws Exception {
    // All destinations should send
    when(destination1.shouldSend(checklist)).thenReturn(true);
    when(destination2.shouldSend(checklist)).thenReturn(true);
    when(destination3.shouldSend(checklist)).thenReturn(true);

    when(destination1.send(checklist))
        .thenReturn(CompletableFuture.completedFuture(WebhookResult.success("dest-1", 200)));
    when(destination2.send(checklist))
        .thenReturn(CompletableFuture.completedFuture(WebhookResult.success("dest-2", 200)));
    when(destination3.send(checklist))
        .thenReturn(CompletableFuture.completedFuture(WebhookResult.success("dest-3", 200)));

    WebhookDispatcher dispatcher =
        new WebhookDispatcher(List.of(destination1, destination2, destination3));

    List<WebhookResult> results = dispatcher.dispatch(checklist).get();

    assertEquals(3, results.size());
    verify(destination1).send(checklist);
    verify(destination2).send(checklist);
    verify(destination3).send(checklist);
  }

  @Test
  @DisplayName("dispatchSync() returns results synchronously")
  void dispatchSyncReturnsResults() {
    when(destination1.shouldSend(checklist)).thenReturn(true);
    when(destination1.send(checklist))
        .thenReturn(CompletableFuture.completedFuture(WebhookResult.success("dest-1", 200)));

    WebhookDispatcher dispatcher = new WebhookDispatcher(List.of(destination1));

    List<WebhookResult> results = dispatcher.dispatchSync(checklist);

    assertEquals(1, results.size());
    assertTrue(results.getFirst().isSuccess());
  }

  @Test
  @DisplayName("empty dispatcher returns empty results")
  void emptyDispatcherReturnsEmpty() throws Exception {
    WebhookDispatcher dispatcher = new WebhookDispatcher(List.of());

    List<WebhookResult> results = dispatcher.dispatch(checklist).get();

    assertTrue(results.isEmpty());
  }

  private DynamicChecklist createTestChecklist() {
    return new DynamicChecklist(
        "alert-123",
        "Test Checklist Summary",
        List.of(),
        List.of("runbook-1"),
        Instant.now(),
        "test-llm");
  }
}
