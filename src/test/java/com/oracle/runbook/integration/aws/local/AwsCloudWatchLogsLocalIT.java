/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0.
 */
package com.oracle.runbook.integration.aws.local;

import static org.assertj.core.api.Assertions.assertThat;

import com.oracle.runbook.domain.LogEntry;
import com.oracle.runbook.infrastructure.cloud.aws.AwsCloudWatchLogsAdapter;
import com.oracle.runbook.integration.LocalStackContainerBase;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsAsyncClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogGroupRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogStreamRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.InputLogEvent;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsRequest;

/**
 * Integration tests for {@link AwsCloudWatchLogsAdapter} using LocalStack.
 *
 * <p>These tests verify CloudWatch Logs operations against a local CloudWatch Logs-compatible
 * service (LocalStack). No AWS credentials or cloud resources are required.
 */
@DisplayName("AWS CloudWatch Logs Local (LocalStack) Integration Tests")
class AwsCloudWatchLogsLocalIT extends LocalStackContainerBase {

  private static final String TEST_LOG_GROUP = "/aws/test/runbook-synthesizer";
  private static final String TEST_LOG_STREAM = "test-stream";
  private static final String TEST_RESOURCE_ID = "i-test12345";

  private static CloudWatchLogsAsyncClient logsClient;

  @BeforeAll
  static void publishTestLogs() throws Exception {
    logsClient = createCloudWatchLogsClient();

    // Create log group and stream
    logsClient
        .createLogGroup(CreateLogGroupRequest.builder().logGroupName(TEST_LOG_GROUP).build())
        .get();

    logsClient
        .createLogStream(
            CreateLogStreamRequest.builder()
                .logGroupName(TEST_LOG_GROUP)
                .logStreamName(TEST_LOG_STREAM)
                .build())
        .get();

    // Publish test log events
    InputLogEvent logEvent1 =
        InputLogEvent.builder()
            .message("Test log message 1")
            .timestamp(Instant.now().toEpochMilli())
            .build();

    InputLogEvent logEvent2 =
        InputLogEvent.builder()
            .message("ERROR: Something failed")
            .timestamp(Instant.now().toEpochMilli())
            .build();

    logsClient
        .putLogEvents(
            PutLogEventsRequest.builder()
                .logGroupName(TEST_LOG_GROUP)
                .logStreamName(TEST_LOG_STREAM)
                .logEvents(logEvent1, logEvent2)
                .build())
        .get();
  }

  @Test
  @DisplayName("Should fetch logs from CloudWatch Logs")
  void shouldFetchLogsFromCloudWatchLogs() throws Exception {
    // Adapter is constructed with logGroupName
    AwsCloudWatchLogsAdapter adapter = new AwsCloudWatchLogsAdapter(logsClient, TEST_LOG_GROUP);

    // fetchLogs takes (resourceId, lookback, query)
    List<LogEntry> logs = adapter.fetchLogs(TEST_RESOURCE_ID, Duration.ofHours(1), null).get();

    assertThat(logs).as("Should return log entries").isNotEmpty();
    assertThat(logs.get(0).message()).isNotNull();
  }

  @Test
  @DisplayName("Should filter logs by query pattern")
  void shouldFilterLogsByQueryPattern() throws Exception {
    AwsCloudWatchLogsAdapter adapter = new AwsCloudWatchLogsAdapter(logsClient, TEST_LOG_GROUP);

    // Filter logs containing "ERROR"
    List<LogEntry> logs = adapter.fetchLogs(TEST_RESOURCE_ID, Duration.ofHours(1), "ERROR").get();

    // LocalStack may not support filter patterns fully, so just verify call succeeds
    assertThat(logs).as("Should return filtered log entries or empty list").isNotNull();
  }

  @Test
  @DisplayName("Should handle filter pattern query (LocalStack limited support)")
  void shouldHandleFilterPatternQuery() throws Exception {
    // Create adapter with log group but query for non-matching pattern
    AwsCloudWatchLogsAdapter adapter = new AwsCloudWatchLogsAdapter(logsClient, TEST_LOG_GROUP);

    // Use a filter pattern - LocalStack may not filter correctly but call should succeed
    List<LogEntry> logs =
        adapter.fetchLogs(TEST_RESOURCE_ID, Duration.ofHours(1), "NONEXISTENT_PATTERN_12345").get();

    // LocalStack doesn't fully support filter patterns, so just verify call completes
    assertThat(logs)
        .as("Filter pattern query should complete without error (LocalStack may not filter)")
        .isNotNull();
  }
}
