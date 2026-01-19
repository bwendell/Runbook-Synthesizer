/**
 * Amazon Web Services (AWS) adapter implementations.
 *
 * <p>This package contains AWS-specific implementations of the cloud abstraction interfaces:
 *
 * <ul>
 *   <li>{@code AwsConfig} - AWS configuration implementing {@code CloudConfig}
 *   <li>{@code AwsS3StorageAdapter} - AWS S3 implementing {@code CloudStorageAdapter}
 *   <li>{@code AwsEc2MetadataAdapter} - AWS EC2 implementing {@code ComputeMetadataAdapter}
 *   <li>{@code AwsCloudWatchMetricsAdapter} - AWS CloudWatch implementing {@code
 *       MetricsSourceAdapter}
 *   <li>{@code AwsCloudWatchLogsAdapter} - AWS CloudWatch Logs implementing {@code
 *       LogSourceAdapter}
 * </ul>
 *
 * @see com.oracle.runbook.infrastructure.cloud.CloudConfig
 * @see com.oracle.runbook.infrastructure.cloud.CloudStorageAdapter
 * @see com.oracle.runbook.infrastructure.cloud.ComputeMetadataAdapter
 */
package com.oracle.runbook.infrastructure.cloud.aws;
