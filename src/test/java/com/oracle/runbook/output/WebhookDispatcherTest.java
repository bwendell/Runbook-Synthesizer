package com.oracle.runbook.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.oracle.runbook.domain.DynamicChecklist;
import com.oracle.runbook.integration.TestFixtures;
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

    checklist = TestFixtures.loadAs("checklists/sample-checklist.json", DynamicChecklist.class);
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

    assertThat(results).hasSize(2);
    assertThat(results).allMatch(WebhookResult::isSuccess);
    assertThat(results)
        .extracting(WebhookResult::destinationName)
        .containsExactlyInAnyOrder("dest-1", "dest-2");
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

    assertThat(results).hasSize(1);
    assertThat(results.getFirst().destinationName()).isEqualTo("dest-1");
    // verify dest-2 was NOT dispatched by confirming it's not in results
    assertThat(results)
        .extracting(WebhookResult::destinationName)
        .containsExactly("dest-1")
        .doesNotContain("dest-2");
  }

  @Test
  @DisplayName("dispatch() returns empty list when no destinations match")
  void dispatchReturnsEmptyWhenNoMatches() throws Exception {
    when(destination1.shouldSend(checklist)).thenReturn(false);
    when(destination2.shouldSend(checklist)).thenReturn(false);

    WebhookDispatcher dispatcher = new WebhookDispatcher(List.of(destination1, destination2));

    List<WebhookResult> results = dispatcher.dispatch(checklist).get();

    assertThat(results).isEmpty();
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

    assertThat(results).hasSize(2);
    assertThat(results).filteredOn(WebhookResult::isSuccess).hasSize(1);
    assertThat(results).filteredOn(r -> !r.isSuccess()).hasSize(1);
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

    assertThat(results).hasSize(3);
    assertThat(results)
        .extracting(WebhookResult::destinationName)
        .containsExactlyInAnyOrder("dest-1", "dest-2", "dest-3");
  }

  @Test
  @DisplayName("dispatchSync() returns results synchronously")
  void dispatchSyncReturnsResults() {
    when(destination1.shouldSend(checklist)).thenReturn(true);
    when(destination1.send(checklist))
        .thenReturn(CompletableFuture.completedFuture(WebhookResult.success("dest-1", 200)));

    WebhookDispatcher dispatcher = new WebhookDispatcher(List.of(destination1));

    List<WebhookResult> results = dispatcher.dispatchSync(checklist);

    assertThat(results).hasSize(1);
    assertThat(results.getFirst().isSuccess()).isTrue();
  }

  @Test
  @DisplayName("empty dispatcher returns empty results")
  void emptyDispatcherReturnsEmpty() throws Exception {
    WebhookDispatcher dispatcher = new WebhookDispatcher(List.of());

    List<WebhookResult> results = dispatcher.dispatch(checklist).get();

    assertThat(results).isEmpty();
  }
}
