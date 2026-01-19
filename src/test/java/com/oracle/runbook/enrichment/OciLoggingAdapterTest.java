package com.oracle.runbook.enrichment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.oracle.bmc.loggingsearch.LogSearchClient;
import com.oracle.runbook.infrastructure.cloud.oci.OciConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OciLoggingAdapter}.
 *
 * <p>Note: Full integration testing with OCI SDK requires live credentials. These unit tests verify
 * the adapter's public API contract via behavioral tests.
 */
class OciLoggingAdapterTest {

  @Test
  @DisplayName("OciLoggingAdapter should implement LogSourceAdapter")
  void testImplementsLogSourceAdapter() {
    assertThat(LogSourceAdapter.class.isAssignableFrom(OciLoggingAdapter.class))
        .as("OciLoggingAdapter should implement LogSourceAdapter")
        .isTrue();
  }

  @Test
  @DisplayName("OciLoggingAdapter constructor should reject null client")
  void testConstructorRejectsNullClient() {
    OciConfig config =
        new OciConfig(
            "ocid1.compartment.oc1..test", null, null, null, null, null, null, null, null);

    assertThatThrownBy(() -> new OciLoggingAdapter(null, config))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("OciLoggingAdapter constructor should reject null config")
  void testConstructorRejectsNullConfig() {
    assertThatThrownBy(() -> new OciLoggingAdapter(null, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("sourceType() returns 'oci-logging'")
  void sourceType_ReturnsOciLogging() {
    // Create a mock client to construct the adapter
    LogSearchClient mockClient = mock(LogSearchClient.class);
    OciConfig config =
        new OciConfig(
            "ocid1.compartment.oc1..test", null, null, null, null, null, null, null, null);

    OciLoggingAdapter adapter = new OciLoggingAdapter(mockClient, config);

    assertThat(adapter.sourceType())
        .as("sourceType should return the expected identifier")
        .isEqualTo("oci-logging");
  }
}
