package com.oracle.runbook.api.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Unit tests for SyncRequest DTO. */
class SyncRequestTest {

  @Test
  void testCreation_WithNoFilters() {
    var request = new SyncRequest(null, null, false);
    assertNull(request.bucketName());
    assertNull(request.prefix());
    assertFalse(request.forceRefresh());
  }

  @Test
  void testCreation_WithBucketFilter() {
    var request = new SyncRequest("my-bucket", "runbooks/", true);
    assertEquals("my-bucket", request.bucketName());
    assertEquals("runbooks/", request.prefix());
    assertTrue(request.forceRefresh());
  }

  @Test
  void testCreation_AllOptional() {
    var request = new SyncRequest(null, null, false);
    assertNotNull(request);
  }
}
