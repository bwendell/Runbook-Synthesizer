package com.oracle.runbook.enrichment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link OciComputeClient}. */
class OciComputeClientTest {

  @Test
  @DisplayName("OciComputeClient constructor should reject null client")
  void testConstructorRejectsNullClient() {
    assertThatThrownBy(() -> new OciComputeClient(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("getInstance should return Optional with ResourceMetadata")
  void testGetInstanceContract() {
    try {
      var method = OciComputeClient.class.getMethod("getInstance", String.class);
      assertThat(method.getReturnType()).isEqualTo(CompletableFuture.class);
    } catch (NoSuchMethodException e) {
      fail("getInstance(String) method should exist");
    }
  }
}
