package com.oracle.runbook.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link MetricSnapshot} record. */
class MetricSnapshotTest {

  @Test
  @DisplayName("MetricSnapshot construction with valid data succeeds")
  void constructionWithValidDataSucceeds() {
    Instant now = Instant.now();

    MetricSnapshot snapshot =
        new MetricSnapshot("CpuUtilization", "oci_computeagent", 92.5, "percent", now);

    assertThat(snapshot.metricName()).isEqualTo("CpuUtilization");
    assertThat(snapshot.namespace()).isEqualTo("oci_computeagent");
    assertThat(snapshot.value()).isEqualTo(92.5);
    assertThat(snapshot.unit()).isEqualTo("percent");
    assertThat(snapshot.timestamp()).isEqualTo(now);
  }

  @Test
  @DisplayName("MetricSnapshot throws NullPointerException for null metricName")
  void throwsForNullMetricName() {
    assertThatThrownBy(() -> new MetricSnapshot(null, "namespace", 0.0, "unit", Instant.now()))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("MetricSnapshot throws NullPointerException for null timestamp")
  void throwsForNullTimestamp() {
    assertThatThrownBy(() -> new MetricSnapshot("metricName", "namespace", 0.0, "unit", null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("MetricSnapshot allows zero and negative values")
  void allowsZeroAndNegativeValues() {
    Instant now = Instant.now();

    MetricSnapshot zero = new MetricSnapshot("metric", "ns", 0.0, "unit", now);
    MetricSnapshot negative = new MetricSnapshot("metric", "ns", -10.5, "unit", now);

    assertThat(zero.value()).isEqualTo(0.0);
    assertThat(negative.value()).isEqualTo(-10.5);
  }
}
