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

  /** Environment variable names for AWS configuration. */
  public static final String ENV_REGION = "AWS_REGION";

  public static final String ENV_S3_BUCKET = "AWS_S3_BUCKET";
  public static final String ENV_ACCESS_KEY_ID = "AWS_ACCESS_KEY_ID";
  public static final String ENV_SECRET_ACCESS_KEY = "AWS_SECRET_ACCESS_KEY";

  /**
   * Creates an AwsConfig from environment variables if all required variables are present.
   *
   * <p>Requires the following environment variables:
   *
   * <ul>
   *   <li>{@code AWS_REGION} - AWS region (required)
   *   <li>{@code AWS_S3_BUCKET} - S3 bucket for runbook storage (required)
   *   <li>{@code AWS_ACCESS_KEY_ID} - AWS access key ID (optional, uses default chain if null)
   *   <li>{@code AWS_SECRET_ACCESS_KEY} - AWS secret access key (optional, uses default chain if
   *       null)
   * </ul>
   *
   * @param envLookup function to look up environment variable values (e.g., System::getenv)
   * @return Optional containing AwsConfig if required vars present, empty otherwise
   */
  public static java.util.Optional<AwsConfig> fromEnvironment(
      java.util.function.Function<String, String> envLookup) {
    String region = envLookup.apply(ENV_REGION);
    String bucket = envLookup.apply(ENV_S3_BUCKET);
    String accessKeyId = envLookup.apply(ENV_ACCESS_KEY_ID);
    String secretAccessKey = envLookup.apply(ENV_SECRET_ACCESS_KEY);

    // Required fields must be present
    if (isBlank(region) || isBlank(bucket)) {
      return java.util.Optional.empty();
    }

    return java.util.Optional.of(new AwsConfig(region, bucket, accessKeyId, secretAccessKey));
  }

  /**
   * Creates an AwsConfig from system environment variables.
   *
   * @return Optional containing AwsConfig if required vars present, empty otherwise
   */
  public static java.util.Optional<AwsConfig> fromEnvironment() {
    return fromEnvironment(System::getenv);
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}
