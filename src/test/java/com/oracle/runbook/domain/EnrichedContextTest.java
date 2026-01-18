package com.oracle.runbook.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link EnrichedContext} record. */
class EnrichedContextTest {

  private Alert createTestAlert() {
    return new Alert(
        "alert-123",
        "High CPU",
        "CPU exceeded threshold",
        AlertSeverity.WARNING,
        "oci-monitoring",
        Map.of(),
        Map.of(),
        Instant.now(),
        "{}");
  }

  private ResourceMetadata createTestMetadata() {
    return new ResourceMetadata(
        "ocid1.instance.oc1..xxx",
        "web-01",
        "ocid1.compartment..xxx",
        "VM.Standard2.4",
        "AD-1",
        Map.of(),
        Map.of());
  }

  @Test
  @DisplayName("EnrichedContext construction with all components succeeds")
  void constructionWithAllComponentsSucceeds() {
    Alert alert = createTestAlert();
    ResourceMetadata resource = createTestMetadata();
    List<MetricSnapshot> metrics =
        List.of(new MetricSnapshot("CpuUtilization", "oci", 85.0, "%", Instant.now()));
    List<LogEntry> logs =
        List.of(new LogEntry("log-1", Instant.now(), "ERROR", "error msg", Map.of()));
    Map<String, Object> customProps = Map.of("gpuStatus", "healthy");

    EnrichedContext context = new EnrichedContext(alert, resource, metrics, logs, customProps);

    assertThat(context.alert()).isEqualTo(alert);
    assertThat(context.resource()).isEqualTo(resource);
    assertThat(context.recentMetrics()).isEqualTo(metrics);
    assertThat(context.recentLogs()).isEqualTo(logs);
    assertThat(context.customProperties()).isEqualTo(customProps);
  }

  @Test
  @DisplayName("EnrichedContext throws NullPointerException for null alert")
  void throwsForNullAlert() {
    assertThatThrownBy(
            () -> new EnrichedContext(null, createTestMetadata(), List.of(), List.of(), Map.of()))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("EnrichedContext metrics list is immutable")
  void metricsListIsImmutable() {
    List<MetricSnapshot> mutableMetrics = new ArrayList<>();
    mutableMetrics.add(new MetricSnapshot("cpu", "ns", 50.0, "%", Instant.now()));

    EnrichedContext context =
        new EnrichedContext(
            createTestAlert(), createTestMetadata(), mutableMetrics, List.of(), Map.of());

    // Modifying original should not affect context
    mutableMetrics.add(new MetricSnapshot("mem", "ns", 60.0, "%", Instant.now()));
    assertThat(context.recentMetrics()).hasSize(1);

    // Context's list should be unmodifiable
    assertThatThrownBy(() -> context.recentMetrics().add(null))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("EnrichedContext logs list is immutable")
  void logsListIsImmutable() {
    List<LogEntry> mutableLogs = new ArrayList<>();
    mutableLogs.add(new LogEntry("1", Instant.now(), "INFO", "msg", Map.of()));

    EnrichedContext context =
        new EnrichedContext(
            createTestAlert(), createTestMetadata(), List.of(), mutableLogs, Map.of());

    // Modifying original should not affect context
    mutableLogs.add(new LogEntry("2", Instant.now(), "ERROR", "msg2", Map.of()));
    assertThat(context.recentLogs()).hasSize(1);

    // Context's list should be unmodifiable
    assertThatThrownBy(() -> context.recentLogs().add(null))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("EnrichedContext customProperties map is immutable")
  void customPropertiesMapIsImmutable() {
    Map<String, Object> mutableProps = new HashMap<>();
    mutableProps.put("key", "value");

    EnrichedContext context =
        new EnrichedContext(
            createTestAlert(), createTestMetadata(), List.of(), List.of(), mutableProps);

    // Modifying original should not affect context
    mutableProps.put("newKey", "newValue");
    assertThat(context.customProperties()).doesNotContainKey("newKey");

    // Context's map should be unmodifiable
    assertThatThrownBy(() -> context.customProperties().put("another", "value"))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
