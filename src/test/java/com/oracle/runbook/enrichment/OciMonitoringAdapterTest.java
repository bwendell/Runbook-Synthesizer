package com.oracle.runbook.enrichment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.oracle.bmc.monitoring.MonitoringClient;
import com.oracle.runbook.infrastructure.cloud.oci.OciConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OciMonitoringAdapter}.
 *
 * <p>Note: Full integration testing with OCI SDK requires live credentials. These unit tests verify
 * the adapter's public API contract via behavioral tests.
 */
class OciMonitoringAdapterTest {

  @Test
  @DisplayName("OciMonitoringAdapter should implement MetricsSourceAdapter")
  void testImplementsMetricsSourceAdapter() {
    assertThat(MetricsSourceAdapter.class.isAssignableFrom(OciMonitoringAdapter.class))
        .as("OciMonitoringAdapter should implement MetricsSourceAdapter")
        .isTrue();
  }

  @Test
  @DisplayName("OciMonitoringAdapter constructor should reject null monitoringClient")
  void testConstructorRejectsNullClient() {
    OciConfig config =
        new OciConfig(
            "ocid1.compartment.oc1..test", null, null, null, null, null, null, null, null);

    assertThatThrownBy(() -> new OciMonitoringAdapter(null, config))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("OciMonitoringAdapter constructor should reject null config")
  void testConstructorRejectsNullConfig() {
    assertThatThrownBy(() -> new OciMonitoringAdapter(null, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("sourceType() returns 'oci-monitoring'")
  void sourceType_ReturnsOciMonitoring() {
    // Create a mock client to construct the adapter
    MonitoringClient mockClient = mock(MonitoringClient.class);
    OciConfig config =
        new OciConfig(
            "ocid1.compartment.oc1..test", null, null, null, null, null, null, null, null);

    OciMonitoringAdapter adapter = new OciMonitoringAdapter(mockClient, config);

    assertThat(adapter.sourceType())
        .as("sourceType should return the expected identifier")
        .isEqualTo("oci-monitoring");
  }
}
