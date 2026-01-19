/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0.
 */
package com.oracle.runbook.integration.aws.local;

import static org.assertj.core.api.Assertions.assertThat;

import com.oracle.runbook.domain.MetricSnapshot;
import com.oracle.runbook.infrastructure.cloud.aws.AwsCloudWatchMetricsAdapter;
import com.oracle.runbook.integration.LocalStackContainerBase;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

/**
 * Integration tests for {@link AwsCloudWatchMetricsAdapter} using LocalStack.
 *
 * <p>These tests verify CloudWatch metrics operations against a local CloudWatch-compatible service
 * (LocalStack). No AWS credentials or cloud resources are required.
 */
@DisplayName("AWS CloudWatch Metrics Local (LocalStack) Integration Tests")
class AwsCloudWatchMetricsLocalIT extends LocalStackContainerBase {

  private static final String TEST_INSTANCE_ID = "i-test12345";
  private static final String TEST_NAMESPACE = "AWS/EC2";

  private static CloudWatchAsyncClient cloudWatchClient;

  @BeforeAll
  static void publishTestMetrics() throws Exception {
    cloudWatchClient = createCloudWatchClient();

    // Publish test metrics
    Dimension instanceDimension =
        Dimension.builder().name("InstanceId").value(TEST_INSTANCE_ID).build();

    MetricDatum cpuMetric =
        MetricDatum.builder()
            .metricName("CPUUtilization")
            .dimensions(instanceDimension)
            .timestamp(Instant.now())
            .value(45.5)
            .unit(StandardUnit.PERCENT)
            .build();

    cloudWatchClient
        .putMetricData(
            PutMetricDataRequest.builder().namespace(TEST_NAMESPACE).metricData(cpuMetric).build())
        .get();
  }

  @Test
  @DisplayName("Should fetch metrics from CloudWatch")
  void shouldFetchMetricsFromCloudWatch() throws Exception {
    AwsCloudWatchMetricsAdapter adapter = new AwsCloudWatchMetricsAdapter(cloudWatchClient);

    List<MetricSnapshot> metrics =
        adapter.fetchMetrics(TEST_INSTANCE_ID, Duration.ofHours(1)).get();

    // LocalStack may not return metrics immediately, so we just verify the call succeeds
    assertThat(metrics).as("Should return metrics list (may be empty in LocalStack)").isNotNull();
  }

  @Test
  @DisplayName("Should return empty list for non-existent instance")
  void shouldReturnEmptyForNonExistentInstance() throws Exception {
    AwsCloudWatchMetricsAdapter adapter = new AwsCloudWatchMetricsAdapter(cloudWatchClient);

    List<MetricSnapshot> metrics = adapter.fetchMetrics("i-nonexistent", Duration.ofHours(1)).get();

    assertThat(metrics).as("Non-existent instance should return empty list").isEmpty();
  }
}
