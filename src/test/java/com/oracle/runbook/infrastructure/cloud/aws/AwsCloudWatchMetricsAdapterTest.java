package com.oracle.runbook.infrastructure.cloud.aws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.oracle.runbook.domain.MetricSnapshot;
import com.oracle.runbook.enrichment.MetricsSourceAdapter;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.model.Datapoint;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResponse;

/**
 * Unit tests for {@link AwsCloudWatchMetricsAdapter}.
 *
 * <p>Uses mocked CloudWatchAsyncClient per testing-patterns-java.
 */
class AwsCloudWatchMetricsAdapterTest {

  @Nested
  @DisplayName("MetricsSourceAdapter interface implementation")
  class InterfaceImplementationTests {

    @Test
    @DisplayName("AwsCloudWatchMetricsAdapter should implement MetricsSourceAdapter")
    void shouldImplementMetricsSourceAdapter() {
      CloudWatchAsyncClient mockClient = mock(CloudWatchAsyncClient.class);
      AwsCloudWatchMetricsAdapter adapter = new AwsCloudWatchMetricsAdapter(mockClient);

      assertThat(adapter)
          .as("AwsCloudWatchMetricsAdapter must implement MetricsSourceAdapter")
          .isInstanceOf(MetricsSourceAdapter.class);
    }

    @Test
    @DisplayName("sourceType() should return 'aws-cloudwatch'")
    void sourceTypeShouldReturnAwsCloudWatch() {
      CloudWatchAsyncClient mockClient = mock(CloudWatchAsyncClient.class);
      AwsCloudWatchMetricsAdapter adapter = new AwsCloudWatchMetricsAdapter(mockClient);

      assertThat(adapter.sourceType())
          .as("sourceType() must return 'aws-cloudwatch'")
          .isEqualTo("aws-cloudwatch");
    }
  }

  @Nested
  @DisplayName("Constructor validation")
  class ConstructorValidationTests {

    @Test
    @DisplayName("Should reject null CloudWatchAsyncClient")
    void shouldRejectNullClient() {
      assertThatThrownBy(() -> new AwsCloudWatchMetricsAdapter(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("cloudWatchClient");
    }
  }

  @Nested
  @DisplayName("fetchMetrics()")
  class FetchMetricsTests {

    @Test
    @DisplayName("Should return metrics for resource")
    void shouldReturnMetricsForResource() throws Exception {
      CloudWatchAsyncClient mockClient = mock(CloudWatchAsyncClient.class);

      Instant now = Instant.now();
      GetMetricStatisticsResponse mockResponse =
          GetMetricStatisticsResponse.builder()
              .datapoints(Datapoint.builder().timestamp(now).average(50.0).unit("Percent").build())
              .build();

      when(mockClient.getMetricStatistics(any(GetMetricStatisticsRequest.class)))
          .thenReturn(CompletableFuture.completedFuture(mockResponse));

      AwsCloudWatchMetricsAdapter adapter = new AwsCloudWatchMetricsAdapter(mockClient);

      List<MetricSnapshot> metrics =
          adapter.fetchMetrics("i-1234567890abcdef0", Duration.ofHours(1)).get();

      assertThat(metrics).isNotEmpty();
    }

    @Test
    @DisplayName("Should return empty list when no metrics found")
    void shouldReturnEmptyListWhenNoMetrics() throws Exception {
      CloudWatchAsyncClient mockClient = mock(CloudWatchAsyncClient.class);

      GetMetricStatisticsResponse mockResponse =
          GetMetricStatisticsResponse.builder().datapoints(List.of()).build();

      when(mockClient.getMetricStatistics(any(GetMetricStatisticsRequest.class)))
          .thenReturn(CompletableFuture.completedFuture(mockResponse));

      AwsCloudWatchMetricsAdapter adapter = new AwsCloudWatchMetricsAdapter(mockClient);

      List<MetricSnapshot> metrics =
          adapter.fetchMetrics("i-nonexistent", Duration.ofHours(1)).get();

      assertThat(metrics).isEmpty();
    }
  }

  @Nested
  @DisplayName("Exception handling")
  class ExceptionHandlingTests {

    @Test
    @DisplayName("Should wrap CloudWatchException in CompletionException")
    void shouldWrapCloudWatchExceptionInCompletionException() {
      CloudWatchAsyncClient mockClient = mock(CloudWatchAsyncClient.class);

      CompletableFuture<GetMetricStatisticsResponse> failedFuture = new CompletableFuture<>();
      failedFuture.completeExceptionally(
          software.amazon.awssdk.services.cloudwatch.model.CloudWatchException.builder()
              .message("Access Denied")
              .statusCode(403)
              .awsErrorDetails(
                  software.amazon.awssdk.awscore.exception.AwsErrorDetails.builder()
                      .errorCode("AccessDenied")
                      .errorMessage("User is not authorized")
                      .build())
              .build());

      when(mockClient.getMetricStatistics(any(GetMetricStatisticsRequest.class)))
          .thenReturn(failedFuture);

      AwsCloudWatchMetricsAdapter adapter = new AwsCloudWatchMetricsAdapter(mockClient);

      // Following aws-sdk-java pattern: async exceptions wrapped in CompletionException
      assertThatThrownBy(() -> adapter.fetchMetrics("i-test", Duration.ofHours(1)).get())
          .isInstanceOf(java.util.concurrent.ExecutionException.class)
          .hasRootCauseInstanceOf(
              software.amazon.awssdk.services.cloudwatch.model.CloudWatchException.class);
    }

    @Test
    @DisplayName("Should handle null datapoints in response gracefully")
    void shouldHandleNullDatapointsGracefully() throws Exception {
      CloudWatchAsyncClient mockClient = mock(CloudWatchAsyncClient.class);

      // Response with no datapoints set (null internally)
      GetMetricStatisticsResponse mockResponse =
          GetMetricStatisticsResponse.builder().build(); // No datapoints

      when(mockClient.getMetricStatistics(any(GetMetricStatisticsRequest.class)))
          .thenReturn(CompletableFuture.completedFuture(mockResponse));

      AwsCloudWatchMetricsAdapter adapter = new AwsCloudWatchMetricsAdapter(mockClient);

      List<MetricSnapshot> metrics = adapter.fetchMetrics("i-test", Duration.ofHours(1)).get();

      assertThat(metrics).as("Should return empty list when no datapoints").isEmpty();
    }

    @Test
    @DisplayName("Should handle null values in datapoint gracefully")
    void shouldHandleNullValuesInDatapointGracefully() throws Exception {
      CloudWatchAsyncClient mockClient = mock(CloudWatchAsyncClient.class);

      // Datapoint with null average and unit (edge case)
      GetMetricStatisticsResponse mockResponse =
          GetMetricStatisticsResponse.builder()
              .datapoints(Datapoint.builder().timestamp(Instant.now()).build()) // No average/unit
              .build();

      when(mockClient.getMetricStatistics(any(GetMetricStatisticsRequest.class)))
          .thenReturn(CompletableFuture.completedFuture(mockResponse));

      AwsCloudWatchMetricsAdapter adapter = new AwsCloudWatchMetricsAdapter(mockClient);

      List<MetricSnapshot> metrics = adapter.fetchMetrics("i-test", Duration.ofHours(1)).get();

      assertThat(metrics).hasSize(1);
      assertThat(metrics.get(0).value()).isEqualTo(0.0); // Null defaults to 0.0
      assertThat(metrics.get(0).unit()).isEqualTo("None"); // Null defaults to "None"
    }
  }
}
