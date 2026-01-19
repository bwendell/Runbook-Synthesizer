package com.oracle.runbook.integration.stubs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oracle.runbook.domain.LogEntry;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link StubLogSourceAdapter}.
 *
 * <p>Verifies the stub adapter correctly implements LogSourceAdapter contract for cloud-free
 * testing.
 */
class StubLogSourceAdapterTest {

  private StubLogSourceAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new StubLogSourceAdapter();
  }

  @Nested
  @DisplayName("sourceType tests")
  class SourceTypeTests {

    @Test
    @DisplayName("sourceType should return 'stub-logs'")
    void sourceTypeShouldReturnStubLogs() {
      assertThat(adapter.sourceType()).isEqualTo("stub-logs");
    }
  }

  @Nested
  @DisplayName("fetchLogs tests")
  class FetchLogsTests {

    @Test
    @DisplayName("fetchLogs should return empty list by default")
    void fetchLogsShouldReturnEmptyListByDefault() throws Exception {
      List<LogEntry> result =
          adapter.fetchLogs("resource-id", Duration.ofMinutes(5), "query").get();

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("fetchLogs should return configured logs")
    void fetchLogsShouldReturnConfiguredLogs() throws Exception {
      LogEntry log = new LogEntry("log-1", Instant.now(), "ERROR", "Out of memory", Map.of());
      adapter.setLogs(List.of(log));

      List<LogEntry> result = adapter.fetchLogs("resource-id", Duration.ofMinutes(5), null).get();

      assertThat(result).hasSize(1);
      assertThat(result.get(0).message()).isEqualTo("Out of memory");
    }

    @Test
    @DisplayName("fetchLogs should track call parameters")
    void fetchLogsShouldTrackCallParameters() throws Exception {
      String resourceId = "ocid1.instance.oc1..test";
      Duration lookback = Duration.ofHours(2);
      String query = "level=ERROR";

      adapter.fetchLogs(resourceId, lookback, query).get();

      assertThat(adapter.wasFetchLogsCalled()).isTrue();
      assertThat(adapter.getLastResourceId()).isEqualTo(resourceId);
      assertThat(adapter.getLastLookback()).isEqualTo(lookback);
      assertThat(adapter.getLastQuery()).isEqualTo(query);
    }

    @Test
    @DisplayName("fetchLogs should propagate configured exception")
    void fetchLogsShouldPropagateException() {
      RuntimeException error = new RuntimeException("Log service unavailable");
      adapter.setException(error);

      assertThatThrownBy(() -> adapter.fetchLogs("id", Duration.ofMinutes(1), null).get())
          .isInstanceOf(ExecutionException.class)
          .hasCauseInstanceOf(RuntimeException.class)
          .hasRootCauseMessage("Log service unavailable");
    }
  }

  @Nested
  @DisplayName("reset tests")
  class ResetTests {

    @Test
    @DisplayName("reset should clear all state")
    void resetShouldClearAllState() throws Exception {
      adapter.setLogs(List.of(new LogEntry("1", Instant.now(), "INFO", "msg", Map.of())));
      adapter.fetchLogs("id", Duration.ofMinutes(1), "q").get();

      adapter.reset();

      assertThat(adapter.wasFetchLogsCalled()).isFalse();
      assertThat(adapter.getLastResourceId()).isNull();
      assertThat(adapter.getLastQuery()).isNull();
      assertThat(adapter.fetchLogs("new-id", Duration.ofMinutes(1), null).get()).isEmpty();
    }
  }
}
