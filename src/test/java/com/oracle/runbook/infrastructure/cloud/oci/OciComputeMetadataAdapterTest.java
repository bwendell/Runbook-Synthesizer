package com.oracle.runbook.infrastructure.cloud.oci;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.oracle.bmc.core.ComputeClient;
import com.oracle.runbook.infrastructure.cloud.ComputeMetadataAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OciComputeMetadataAdapter}.
 *
 * <p>Tests follow testing-patterns-java skill patterns with mocked OCI SDK.
 */
class OciComputeMetadataAdapterTest {

  @Nested
  @DisplayName("ComputeMetadataAdapter interface implementation")
  class InterfaceImplementationTests {

    @Test
    @DisplayName("OciComputeMetadataAdapter should implement ComputeMetadataAdapter")
    void shouldImplementComputeMetadataAdapter() {
      ComputeClient mockClient = mock(ComputeClient.class);
      OciComputeMetadataAdapter adapter = new OciComputeMetadataAdapter(mockClient);

      assertThat(adapter)
          .as("OciComputeMetadataAdapter must implement ComputeMetadataAdapter")
          .isInstanceOf(ComputeMetadataAdapter.class);
    }

    @Test
    @DisplayName("providerType() should return 'oci'")
    void providerTypeShouldReturnOci() {
      ComputeClient mockClient = mock(ComputeClient.class);
      OciComputeMetadataAdapter adapter = new OciComputeMetadataAdapter(mockClient);

      assertThat(adapter.providerType())
          .as("providerType() must return 'oci' for OCI adapter")
          .isEqualTo("oci");
    }
  }

  @Nested
  @DisplayName("Constructor validation")
  class ConstructorValidationTests {

    @Test
    @DisplayName("Should reject null ComputeClient")
    void shouldRejectNullClient() {
      assertThatThrownBy(() -> new OciComputeMetadataAdapter(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("computeClient");
    }
  }
}
