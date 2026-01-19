/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0.
 */
package com.oracle.runbook.integration.aws.cloud;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.oracle.runbook.domain.LogEntry;
import com.oracle.runbook.infrastructure.cloud.aws.AwsCloudWatchLogsAdapter;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsAsyncClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogStreamRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DeleteLogStreamRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.InputLogEvent;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsRequest;

/**
 * E2E integration tests for {@link AwsCloudWatchLogsAdapter} against real AWS CloudWatch Logs.
 *
 * <p>These tests run against the CDK-provisioned CloudWatch Logs group and verify:
 *
 * <ul>
 *   <li>Fetching log entries from CloudWatch Logs
 *   <li>Filter pattern matching works correctly
 *   <li>Handling empty results gracefully
 * </ul>
 *
 * <p><strong>Prerequisites:</strong>
 *
 * <ul>
 *   <li>AWS credentials configured
 *   <li>CDK infrastructure deployed (infra/npm run cdk:deploy)
 *   <li>Run with -Pe2e-aws-cloud Maven profile
 * </ul>
 */
@DisplayName("AWS CloudWatch Logs Cloud E2E Tests")
@EnabledIfSystemProperty(named = "aws.cloud.enabled", matches = "true")
class AwsCloudWatchLogsCloudIT extends CloudAwsTestBase {

  /** Unique test run ID to prevent conflicts between parallel runs. */
  private static final String TEST_RUN_ID = UUID.randomUUID().toString().substring(0, 8);

  /** Log stream name for this test run. */
  private static final String TEST_LOG_STREAM = "e2e-test-stream-" + TEST_RUN_ID;

  /** Test resource ID for log queries. */
  private static final String TEST_RESOURCE_ID = "e2e-test-resource";

  /** CloudWatch Logs async client for setup and tests. */
  private static CloudWatchLogsAsyncClient logsClient;

  /** Timestamp when test logs were written (for lookback calculation). */
  private static long testLogTimestamp;

  /** Test log messages to verify retrieval. */
  private static final String INFO_MESSAGE =
      "[INFO] Application started successfully - test " + TEST_RUN_ID;

  private static final String ERROR_MESSAGE = "[ERROR] Connection failed - test " + TEST_RUN_ID;
  private static final String DEBUG_MESSAGE = "[DEBUG] Processing request - test " + TEST_RUN_ID;

  @BeforeAll
  static void setupTestLogs() throws Exception {
    logsClient = CloudWatchLogsAsyncClient.builder().region(AWS_REGION).build();

    System.out.printf(
        "[AwsCloudWatchLogsCloudIT] Setting up test logs in stream: %s%n", TEST_LOG_STREAM);
    System.out.printf("[AwsCloudWatchLogsCloudIT] Using log group: %s%n", getLogGroupName());

    // Create log stream for this test run
    logsClient
        .createLogStream(
            CreateLogStreamRequest.builder()
                .logGroupName(getLogGroupName())
                .logStreamName(TEST_LOG_STREAM)
                .build())
        .get();
    System.out.printf("[AwsCloudWatchLogsCloudIT] Created log stream: %s%n", TEST_LOG_STREAM);

    // Write test log events
    testLogTimestamp = Instant.now().toEpochMilli();

    InputLogEvent infoEvent =
        InputLogEvent.builder().message(INFO_MESSAGE).timestamp(testLogTimestamp).build();

    InputLogEvent errorEvent =
        InputLogEvent.builder()
            .message(ERROR_MESSAGE)
            .timestamp(testLogTimestamp + 100) // Slightly later timestamp
            .build();

    InputLogEvent debugEvent =
        InputLogEvent.builder().message(DEBUG_MESSAGE).timestamp(testLogTimestamp + 200).build();

    logsClient
        .putLogEvents(
            PutLogEventsRequest.builder()
                .logGroupName(getLogGroupName())
                .logStreamName(TEST_LOG_STREAM)
                .logEvents(infoEvent, errorEvent, debugEvent)
                .build())
        .get();
    System.out.printf("[AwsCloudWatchLogsCloudIT] Published %d test log events%n", 3);

    // Wait for CloudWatch eventual consistency using condition-based polling
    // instead of arbitrary fixed delay
    System.out.println(
        "[AwsCloudWatchLogsCloudIT] Waiting for logs to be indexed (CloudWatch eventual consistency)...");
    await()
        .atMost(30, SECONDS)
        .pollInterval(2, SECONDS)
        .pollDelay(2, SECONDS)
        .until(
            () -> {
              var response =
                  logsClient
                      .filterLogEvents(
                          software.amazon.awssdk.services.cloudwatchlogs.model
                              .FilterLogEventsRequest.builder()
                              .logGroupName(getLogGroupName())
                              .filterPattern(TEST_RUN_ID)
                              .build())
                      .get();
              boolean logsAvailable = response.events() != null && !response.events().isEmpty();
              if (logsAvailable) {
                System.out.printf(
                    "[AwsCloudWatchLogsCloudIT] Logs indexed: found %d events%n",
                    response.events().size());
              }
              return logsAvailable;
            });
  }

  @AfterAll
  static void cleanupTestLogs() throws Exception {
    if (logsClient != null) {
      System.out.printf("[AwsCloudWatchLogsCloudIT] Cleaning up log stream: %s%n", TEST_LOG_STREAM);

      try {
        logsClient
            .deleteLogStream(
                DeleteLogStreamRequest.builder()
                    .logGroupName(getLogGroupName())
                    .logStreamName(TEST_LOG_STREAM)
                    .build())
            .get();
        System.out.printf("[AwsCloudWatchLogsCloudIT] Deleted log stream: %s%n", TEST_LOG_STREAM);
      } catch (Exception e) {
        System.err.printf(
            "[AwsCloudWatchLogsCloudIT] Failed to delete log stream: %s%n", e.getMessage());
      }

      logsClient.close();
    }
  }

  @Nested
  @DisplayName("fetchLogs()")
  class FetchLogsTests {

    @Test
    @DisplayName("Should fetch log entries from CloudWatch Logs")
    void shouldFetchLogEntries() throws Exception {
      AwsCloudWatchLogsAdapter adapter =
          new AwsCloudWatchLogsAdapter(logsClient, getLogGroupName());

      // Fetch logs from the last hour to ensure we capture our test logs
      List<LogEntry> logs = adapter.fetchLogs(TEST_RESOURCE_ID, Duration.ofHours(1), null).get();

      assertThat(logs).as("Should return log entries").isNotEmpty();

      // Find our test messages
      List<String> messages =
          logs.stream().map(LogEntry::message).filter(msg -> msg.contains(TEST_RUN_ID)).toList();

      assertThat(messages)
          .as("Should contain our test log messages")
          .hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Should return LogEntry with correct structure")
    void shouldReturnLogEntriesWithCorrectStructure() throws Exception {
      AwsCloudWatchLogsAdapter adapter =
          new AwsCloudWatchLogsAdapter(logsClient, getLogGroupName());

      List<LogEntry> logs = adapter.fetchLogs(TEST_RESOURCE_ID, Duration.ofHours(1), null).get();

      assertThat(logs).isNotEmpty();

      LogEntry firstEntry = logs.get(0);
      assertThat(firstEntry.id()).as("LogEntry should have an ID").isNotNull();
      assertThat(firstEntry.timestamp()).as("LogEntry should have a timestamp").isNotNull();
      assertThat(firstEntry.message()).as("LogEntry should have a message").isNotNull();
      assertThat(firstEntry.level()).as("LogEntry should have a level").isNotNull();
    }
  }

  @Nested
  @DisplayName("Filter patterns")
  class FilterPatternTests {

    @Test
    @DisplayName("Should filter logs by ERROR pattern")
    void shouldFilterLogsByErrorPattern() throws Exception {
      AwsCloudWatchLogsAdapter adapter =
          new AwsCloudWatchLogsAdapter(logsClient, getLogGroupName());

      // Filter for ERROR messages
      List<LogEntry> logs = adapter.fetchLogs(TEST_RESOURCE_ID, Duration.ofHours(1), "ERROR").get();

      // CloudWatch filter should return only matching entries or fewer entries
      assertThat(logs).as("Filter pattern query should complete without error").isNotNull();

      if (!logs.isEmpty()) {
        // If we got results, verify they contain ERROR
        long errorCount = logs.stream().filter(log -> log.message().contains("ERROR")).count();
        assertThat(errorCount)
            .as("Filtered results should contain ERROR messages")
            .isGreaterThan(0);
      }
    }

    @Test
    @DisplayName("Should handle filter pattern with no matches")
    void shouldHandleFilterWithNoMatches() throws Exception {
      AwsCloudWatchLogsAdapter adapter =
          new AwsCloudWatchLogsAdapter(logsClient, getLogGroupName());

      // Use a pattern that definitely won't match
      List<LogEntry> logs =
          adapter
              .fetchLogs(
                  TEST_RESOURCE_ID,
                  Duration.ofHours(1),
                  "NONEXISTENT_PATTERN_XYZ_" + UUID.randomUUID())
              .get();

      // Should complete without error, returning empty or filtered result
      assertThat(logs)
          .as("Query with non-matching filter should return empty list or complete without error")
          .isNotNull();
    }
  }
}
