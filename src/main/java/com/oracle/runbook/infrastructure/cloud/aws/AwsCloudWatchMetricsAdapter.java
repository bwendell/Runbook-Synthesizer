package com.oracle.runbook.infrastructure.cloud.aws;

import com.oracle.runbook.domain.MetricSnapshot;
import com.oracle.runbook.enrichment.MetricsSourceAdapter;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.model.Datapoint;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsRequest;
import software.amazon.awssdk.services.cloudwatch.model.Statistic;

/**
 * AWS CloudWatch implementation of {@link MetricsSourceAdapter}.
 *
 * <p>Provides metrics retrieval using AWS CloudWatchAsyncClient for non-blocking operations
 * compatible with Helidon SE's reactive patterns.
 */
public class AwsCloudWatchMetricsAdapter implements MetricsSourceAdapter {

  private static final String NAMESPACE = "AWS/EC2";
  private static final String METRIC_NAME = "CPUUtilization";

  private final CloudWatchAsyncClient cloudWatchClient;

  /**
   * Creates a new AwsCloudWatchMetricsAdapter.
   *
   * @param cloudWatchClient the AWS CloudWatch async client
   * @throws NullPointerException if cloudWatchClient is null
   */
  public AwsCloudWatchMetricsAdapter(CloudWatchAsyncClient cloudWatchClient) {
    this.cloudWatchClient =
        Objects.requireNonNull(cloudWatchClient, "cloudWatchClient cannot be null");
  }

  @Override
  public String sourceType() {
    return "aws-cloudwatch";
  }

  @Override
  public CompletableFuture<List<MetricSnapshot>> fetchMetrics(
      String resourceId, Duration lookback) {
    Instant endTime = Instant.now();
    Instant startTime = endTime.minus(lookback);

    var request =
        GetMetricStatisticsRequest.builder()
            .namespace(NAMESPACE)
            .metricName(METRIC_NAME)
            .dimensions(Dimension.builder().name("InstanceId").value(resourceId).build())
            .startTime(startTime)
            .endTime(endTime)
            .period(300) // 5 minutes
            .statistics(Statistic.AVERAGE)
            .build();

    return cloudWatchClient
        .getMetricStatistics(request)
        .thenApply(
            response -> {
              List<Datapoint> datapoints = response.datapoints();
              if (datapoints == null || datapoints.isEmpty()) {
                return List.of();
              }

              return datapoints.stream()
                  .map(
                      dp ->
                          new MetricSnapshot(
                              METRIC_NAME,
                              NAMESPACE,
                              dp.average() != null ? dp.average() : 0.0,
                              dp.unit() != null ? dp.unit().toString() : "None",
                              dp.timestamp()))
                  .toList();
            });
  }
}
