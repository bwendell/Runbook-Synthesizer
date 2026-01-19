package com.oracle.runbook.integration.stubs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oracle.runbook.domain.MetricSnapshot;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link StubMetricsSourceAdapter}.
 *
 * <p>Verifies the stub adapter correctly implements MetricsSourceAdapter contract for cloud-free
 * testing.
 */
class StubMetricsSourceAdapterTest {

  private StubMetricsSourceAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new StubMetricsSourceAdapter();
  }

  @Nested
  @DisplayName("sourceType tests")
  class SourceTypeTests {

    @Test
    @DisplayName("sourceType should return 'stub-metrics'")
    void sourceTypeShouldReturnStubMetrics() {
      assertThat(adapter.sourceType()).isEqualTo("stub-metrics");
    }
  }

  @Nested
  @DisplayName("fetchMetrics tests")
  class FetchMetricsTests {

    @Test
    @DisplayName("fetchMetrics should return empty list by default")
    void fetchMetricsShouldReturnEmptyListByDefault() throws Exception {
      List<MetricSnapshot> result =
          adapter.fetchMetrics("resource-id", Duration.ofMinutes(5)).get();

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("fetchMetrics should return configured metrics")
    void fetchMetricsShouldReturnConfiguredMetrics() throws Exception {
      MetricSnapshot metric =
          new MetricSnapshot("CpuUtilization", "oci_computeagent", 75.5, "percent", Instant.now());
      adapter.setMetrics(List.of(metric));

      List<MetricSnapshot> result =
          adapter.fetchMetrics("resource-id", Duration.ofMinutes(5)).get();

      assertThat(result).hasSize(1);
      assertThat(result.get(0).metricName()).isEqualTo("CpuUtilization");
    }

    @Test
    @DisplayName("fetchMetrics should track call parameters")
    void fetchMetricsShouldTrackCallParameters() throws Exception {
      String resourceId = "ocid1.instance.oc1..test";
      Duration lookback = Duration.ofHours(1);

      adapter.fetchMetrics(resourceId, lookback).get();

      assertThat(adapter.wasFetchMetricsCalled()).isTrue();
      assertThat(adapter.getLastResourceId()).isEqualTo(resourceId);
      assertThat(adapter.getLastLookback()).isEqualTo(lookback);
    }

    @Test
    @DisplayName("fetchMetrics should propagate configured exception")
    void fetchMetricsShouldPropagateException() {
      RuntimeException error = new RuntimeException("Connection timeout");
      adapter.setException(error);

      assertThatThrownBy(() -> adapter.fetchMetrics("id", Duration.ofMinutes(1)).get())
          .isInstanceOf(ExecutionException.class)
          .hasCauseInstanceOf(RuntimeException.class)
          .hasRootCauseMessage("Connection timeout");
    }
  }

  @Nested
  @DisplayName("reset tests")
  class ResetTests {

    @Test
    @DisplayName("reset should clear all state")
    void resetShouldClearAllState() throws Exception {
      adapter.setMetrics(List.of(new MetricSnapshot("test", "ns", 1.0, "unit", Instant.now())));
      adapter.fetchMetrics("id", Duration.ofMinutes(1)).get();

      adapter.reset();

      assertThat(adapter.wasFetchMetricsCalled()).isFalse();
      assertThat(adapter.getLastResourceId()).isNull();
      assertThat(adapter.fetchMetrics("new-id", Duration.ofMinutes(1)).get()).isEmpty();
    }
  }
}
