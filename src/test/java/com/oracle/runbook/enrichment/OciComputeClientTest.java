package com.oracle.runbook.enrichment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link OciComputeClient}. */
class OciComputeClientTest {

  @Test
  @DisplayName("OciComputeClient constructor should reject null client")
  void testConstructorRejectsNullClient() {
    assertThatThrownBy(() -> new OciComputeClient(null)).isInstanceOf(NullPointerException.class);
  }

  // Note: Removed reflection-based contract tests per testing-patterns-java skill.
  // Method signature verification is testing implementation details, not behavior.
  // Actual behavior tests require real OCI client mocks which are covered in integration tests.
}
