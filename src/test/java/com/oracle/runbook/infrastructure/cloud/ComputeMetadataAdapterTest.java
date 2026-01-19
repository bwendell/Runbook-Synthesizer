package com.oracle.runbook.infrastructure.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Contract tests for {@link ComputeMetadataAdapter} interface.
 *
 * <p>Verifies the interface contract for compute instance metadata retrieval across providers.
 */
class ComputeMetadataAdapterTest {

  @Test
  @DisplayName("ComputeMetadataAdapter should be an interface")
  void computeMetadataAdapterShouldBeInterface() {
    assertThat(ComputeMetadataAdapter.class.isInterface())
        .as("ComputeMetadataAdapter must be an interface for polymorphic metadata operations")
        .isTrue();
  }

  @Test
  @DisplayName("ComputeMetadataAdapter should declare providerType() method")
  void shouldDeclareProviderTypeMethod() throws NoSuchMethodException {
    var method = ComputeMetadataAdapter.class.getMethod("providerType");

    assertThat(method.getReturnType())
        .as("providerType() should return String")
        .isEqualTo(String.class);
    assertThat(method.getParameterCount()).as("providerType() should take no parameters").isZero();
  }

  @Test
  @DisplayName("ComputeMetadataAdapter should declare getInstance(String) method")
  void shouldDeclareGetInstanceMethod() throws NoSuchMethodException {
    var method = ComputeMetadataAdapter.class.getMethod("getInstance", String.class);

    assertThat(method.getReturnType())
        .as("getInstance() should return CompletableFuture")
        .isEqualTo(CompletableFuture.class);
    assertThat(method.getParameterCount())
        .as("getInstance() should take one parameter")
        .isEqualTo(1);
  }
}
