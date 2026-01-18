package com.oracle.runbook.rag;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link OciObjectStorageClient}. */
class OciObjectStorageClientTest {

  @Test
  @DisplayName("OciObjectStorageClient constructor should reject null client")
  void testConstructorRejectsNullClient() {
    assertThatThrownBy(() -> new OciObjectStorageClient(null))
        .isInstanceOf(NullPointerException.class);
  }

  // Note: Removed reflection-based contract tests per testing-patterns-java skill.
  // Method signature verification is testing implementation details, not behavior.
  // Actual behavior tests require real OCI client mocks which are covered in integration tests.
}
