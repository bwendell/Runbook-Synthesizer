package com.oracle.runbook.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for SyncResponse DTO. */
class SyncResponseTest {

  @Test
  void testCreation_Started() {
    var response = new SyncResponse(SyncResponse.SyncStatus.STARTED, 0, List.of(), "req-123");

    assertThat(response.status()).isEqualTo(SyncResponse.SyncStatus.STARTED);
    assertThat(response.documentsProcessed()).isEqualTo(0);
    assertThat(response.errors()).isEmpty();
    assertThat(response.requestId()).isEqualTo("req-123");
  }

  @Test
  void testCreation_Completed() {
    var response = new SyncResponse(SyncResponse.SyncStatus.COMPLETED, 42, List.of(), "req-456");

    assertThat(response.status()).isEqualTo(SyncResponse.SyncStatus.COMPLETED);
    assertThat(response.documentsProcessed()).isEqualTo(42);
  }

  @Test
  void testCreation_WithErrors() {
    var errors = List.of("Failed to parse doc1.md", "Doc2.md not found");
    var response = new SyncResponse(SyncResponse.SyncStatus.FAILED, 10, errors, "req-789");

    assertThat(response.status()).isEqualTo(SyncResponse.SyncStatus.FAILED);
    assertThat(response.errors()).hasSize(2);
  }

  @Test
  void testNullErrors_DefaultsToEmptyList() {
    var response = new SyncResponse(SyncResponse.SyncStatus.STARTED, 0, null, "req-000");

    assertThat(response.errors()).isNotNull().isEmpty();
  }
}
