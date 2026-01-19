package com.oracle.runbook.infrastructure.cloud.aws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.oracle.runbook.domain.LogEntry;
import com.oracle.runbook.enrichment.LogSourceAdapter;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsAsyncClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.FilterLogEventsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.FilterLogEventsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.FilteredLogEvent;

/**
 * Unit tests for {@link AwsCloudWatchLogsAdapter}.
 *
 * <p>Uses mocked CloudWatchLogsAsyncClient per testing-patterns-java.
 */
class AwsCloudWatchLogsAdapterTest {

  private static final String TEST_LOG_GROUP = "/aws/ec2/test";

  @Nested
  @DisplayName("LogSourceAdapter interface implementation")
  class InterfaceImplementationTests {

    @Test
    @DisplayName("AwsCloudWatchLogsAdapter should implement LogSourceAdapter")
    void shouldImplementLogSourceAdapter() {
      CloudWatchLogsAsyncClient mockClient = mock(CloudWatchLogsAsyncClient.class);
      AwsCloudWatchLogsAdapter adapter = new AwsCloudWatchLogsAdapter(mockClient, TEST_LOG_GROUP);

      assertThat(adapter)
          .as("AwsCloudWatchLogsAdapter must implement LogSourceAdapter")
          .isInstanceOf(LogSourceAdapter.class);
    }

    @Test
    @DisplayName("sourceType() should return 'aws-cloudwatch-logs'")
    void sourceTypeShouldReturnAwsCloudWatchLogs() {
      CloudWatchLogsAsyncClient mockClient = mock(CloudWatchLogsAsyncClient.class);
      AwsCloudWatchLogsAdapter adapter = new AwsCloudWatchLogsAdapter(mockClient, TEST_LOG_GROUP);

      assertThat(adapter.sourceType())
          .as("sourceType() must return 'aws-cloudwatch-logs'")
          .isEqualTo("aws-cloudwatch-logs");
    }
  }

  @Nested
  @DisplayName("Constructor validation")
  class ConstructorValidationTests {

    @Test
    @DisplayName("Should reject null CloudWatchLogsAsyncClient")
    void shouldRejectNullClient() {
      assertThatThrownBy(() -> new AwsCloudWatchLogsAdapter(null, TEST_LOG_GROUP))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("cloudWatchLogsClient");
    }

    @Test
    @DisplayName("Should reject null logGroupName")
    void shouldRejectNullLogGroup() {
      CloudWatchLogsAsyncClient mockClient = mock(CloudWatchLogsAsyncClient.class);

      assertThatThrownBy(() -> new AwsCloudWatchLogsAdapter(mockClient, null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("logGroupName");
    }
  }

  @Nested
  @DisplayName("fetchLogs()")
  class FetchLogsTests {

    @Test
    @DisplayName("Should return logs for resource")
    void shouldReturnLogsForResource() throws Exception {
      CloudWatchLogsAsyncClient mockClient = mock(CloudWatchLogsAsyncClient.class);

      FilterLogEventsResponse mockResponse =
          FilterLogEventsResponse.builder()
              .events(
                  FilteredLogEvent.builder()
                      .timestamp(1705636800000L) // 2024-01-19T00:00:00Z
                      .message("Test log message")
                      .logStreamName("test-stream")
                      .build())
              .build();

      when(mockClient.filterLogEvents(any(FilterLogEventsRequest.class)))
          .thenReturn(CompletableFuture.completedFuture(mockResponse));

      AwsCloudWatchLogsAdapter adapter = new AwsCloudWatchLogsAdapter(mockClient, TEST_LOG_GROUP);

      List<LogEntry> logs =
          adapter.fetchLogs("i-1234567890abcdef0", Duration.ofHours(1), null).get();

      assertThat(logs).hasSize(1);
      assertThat(logs.get(0).message()).isEqualTo("Test log message");
    }

    @Test
    @DisplayName("Should return empty list when no logs found")
    void shouldReturnEmptyListWhenNoLogs() throws Exception {
      CloudWatchLogsAsyncClient mockClient = mock(CloudWatchLogsAsyncClient.class);

      FilterLogEventsResponse mockResponse =
          FilterLogEventsResponse.builder().events(List.of()).build();

      when(mockClient.filterLogEvents(any(FilterLogEventsRequest.class)))
          .thenReturn(CompletableFuture.completedFuture(mockResponse));

      AwsCloudWatchLogsAdapter adapter = new AwsCloudWatchLogsAdapter(mockClient, TEST_LOG_GROUP);

      List<LogEntry> logs = adapter.fetchLogs("i-nonexistent", Duration.ofHours(1), null).get();

      assertThat(logs).isEmpty();
    }
  }
}
