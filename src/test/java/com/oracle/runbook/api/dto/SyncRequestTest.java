package com.oracle.runbook.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Unit tests for SyncRequest DTO. */
class SyncRequestTest {

  @Test
  void testCreation_WithNoFilters() {
    var request = new SyncRequest(null, null, false);
    assertThat(request.bucketName()).isNull();
    assertThat(request.prefix()).isNull();
    assertThat(request.forceRefresh()).isFalse();
  }

  @Test
  void testCreation_WithBucketFilter() {
    var request = new SyncRequest("my-bucket", "runbooks/", true);
    assertThat(request.bucketName()).isEqualTo("my-bucket");
    assertThat(request.prefix()).isEqualTo("runbooks/");
    assertThat(request.forceRefresh()).isTrue();
  }

  @Test
  void testCreation_AllOptional() {
    var request = new SyncRequest(null, null, false);
    assertThat(request).isNotNull();
  }
}
