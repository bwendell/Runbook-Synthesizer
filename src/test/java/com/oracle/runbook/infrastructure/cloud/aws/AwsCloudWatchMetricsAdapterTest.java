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
}
