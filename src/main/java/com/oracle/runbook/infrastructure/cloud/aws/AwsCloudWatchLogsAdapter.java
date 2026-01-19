package com.oracle.runbook.infrastructure.cloud.aws;

import com.oracle.runbook.domain.LogEntry;
import com.oracle.runbook.enrichment.LogSourceAdapter;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsAsyncClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.FilterLogEventsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.FilteredLogEvent;

/**
 * AWS CloudWatch Logs implementation of {@link LogSourceAdapter}.
 *
 * <p>Provides log retrieval using AWS CloudWatchLogsAsyncClient for non-blocking operations
 * compatible with Helidon SE's reactive patterns.
 */
public class AwsCloudWatchLogsAdapter implements LogSourceAdapter {

  private final CloudWatchLogsAsyncClient cloudWatchLogsClient;
  private final String logGroupName;

  /**
   * Creates a new AwsCloudWatchLogsAdapter.
   *
   * @param cloudWatchLogsClient the AWS CloudWatch Logs async client
   * @param logGroupName the log group name to query
   * @throws NullPointerException if cloudWatchLogsClient or logGroupName is null
   */
  public AwsCloudWatchLogsAdapter(
      CloudWatchLogsAsyncClient cloudWatchLogsClient, String logGroupName) {
    this.cloudWatchLogsClient =
        Objects.requireNonNull(cloudWatchLogsClient, "cloudWatchLogsClient cannot be null");
    this.logGroupName = Objects.requireNonNull(logGroupName, "logGroupName cannot be null");
  }

  @Override
  public String sourceType() {
    return "aws-cloudwatch-logs";
  }

  @Override
  public CompletableFuture<List<LogEntry>> fetchLogs(
      String resourceId, Duration lookback, String query) {
    long endTime = Instant.now().toEpochMilli();
    long startTime = Instant.now().minus(lookback).toEpochMilli();

    var requestBuilder =
        FilterLogEventsRequest.builder()
            .logGroupName(logGroupName)
            .startTime(startTime)
            .endTime(endTime);

    // Add filter pattern if query is provided
    if (query != null && !query.isBlank()) {
      requestBuilder.filterPattern(query);
    }

    return cloudWatchLogsClient
        .filterLogEvents(requestBuilder.build())
        .thenApply(
            response -> {
              List<FilteredLogEvent> events = response.events();
              if (events == null || events.isEmpty()) {
                return List.of();
              }

              return events.stream()
                  .map(
                      event ->
                          new LogEntry(
                              event.eventId() != null ? event.eventId() : "unknown",
                              Instant.ofEpochMilli(event.timestamp()),
                              "INFO", // CloudWatch doesn't have built-in log levels
                              event.message(),
                              java.util.Map.of(
                                  "logStream",
                                  event.logStreamName() != null ? event.logStreamName() : "")))
                  .toList();
            });
  }
}
