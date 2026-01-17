package com.oracle.runbook.api.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for SyncResponse DTO. */
class SyncResponseTest {

  @Test
  void testCreation_Started() {
    var response = new SyncResponse(SyncResponse.SyncStatus.STARTED, 0, List.of(), "req-123");

    assertEquals(SyncResponse.SyncStatus.STARTED, response.status());
    assertEquals(0, response.documentsProcessed());
    assertTrue(response.errors().isEmpty());
    assertEquals("req-123", response.requestId());
  }

  @Test
  void testCreation_Completed() {
    var response = new SyncResponse(SyncResponse.SyncStatus.COMPLETED, 42, List.of(), "req-456");

    assertEquals(SyncResponse.SyncStatus.COMPLETED, response.status());
    assertEquals(42, response.documentsProcessed());
  }

  @Test
  void testCreation_WithErrors() {
    var errors = List.of("Failed to parse doc1.md", "Doc2.md not found");
    var response = new SyncResponse(SyncResponse.SyncStatus.FAILED, 10, errors, "req-789");

    assertEquals(SyncResponse.SyncStatus.FAILED, response.status());
    assertEquals(2, response.errors().size());
  }

  @Test
  void testNullErrors_DefaultsToEmptyList() {
    var response = new SyncResponse(SyncResponse.SyncStatus.STARTED, 0, null, "req-000");

    assertNotNull(response.errors());
    assertTrue(response.errors().isEmpty());
  }
}
