package com.oracle.runbook.infrastructure.cloud.aws;

import com.oracle.runbook.infrastructure.cloud.CloudConfig;
import java.util.Objects;

/**
 * Configuration record for AWS connectivity settings.
 *
 * <p>Holds the necessary configuration for connecting to AWS services. Implements {@link
 * CloudConfig} to support polymorphic cloud configuration.
 *
 * <p>Authentication uses the AWS Default Credential Provider Chain when accessKeyId and
 * secretAccessKey are not provided. This supports:
 *
 * <ol>
 *   <li>Environment variables (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)
 *   <li>AWS credentials file (~/.aws/credentials)
 *   <li>ECS container credentials
 *   <li>EC2 instance profile / IAM role
 *   <li>Web identity token (EKS IRSA)
 * </ol>
 *
 * @param region the AWS region identifier (required, e.g., "us-east-1")
 * @param bucket the S3 bucket name for runbook storage (required)
 * @param accessKeyId the AWS access key ID (optional, uses default chain if null)
 * @param secretAccessKey the AWS secret access key (optional, uses default chain if null)
 */
public record AwsConfig(String region, String bucket, String accessKeyId, String secretAccessKey)
    implements CloudConfig {

  /** Compact constructor with validation. */
  public AwsConfig {
    Objects.requireNonNull(region, "AwsConfig region cannot be null");
    Objects.requireNonNull(bucket, "AwsConfig bucket cannot be null");
  }

  /**
   * Returns the cloud provider identifier.
   *
   * @return "aws" always
   */
  @Override
  public String provider() {
    return "aws";
  }
}
