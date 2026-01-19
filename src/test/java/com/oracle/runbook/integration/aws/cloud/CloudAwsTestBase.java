/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0.
 */
package com.oracle.runbook.integration.aws.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sts.StsClient;

/**
 * Base class for E2E tests that run against real AWS cloud services.
 *
 * <p>This class provides:
 *
 * <ul>
 *   <li>AWS credential validation before any tests run
 *   <li>Conditional test execution via {@code aws.cloud.enabled} system property
 *   <li>Helper methods for creating AWS service clients
 *   <li>Access to CDK-provisioned resource identifiers
 * </ul>
 *
 * <p>Tests extending this class will only run when the {@code -Pe2e-aws-cloud} Maven profile is
 * active, which sets the required system property.
 *
 * <p><strong>Prerequisites:</strong>
 *
 * <ul>
 *   <li>AWS credentials configured (via environment variables, ~/.aws/credentials, or IAM role)
 *   <li>CDK infrastructure deployed (see infra/README.md)
 *   <li>Docker Desktop running (for future containerized test support)
 * </ul>
 */
@EnabledIfSystemProperty(named = "aws.cloud.enabled", matches = "true")
public abstract class CloudAwsTestBase {

  /** The AWS region to use for all tests. Must match CDK stack deployment region. */
  protected static final Region AWS_REGION = Region.US_WEST_2;

  /** S3 bucket name from CDK outputs. Format: runbook-synthesizer-e2e-{accountId} */
  protected static String bucketName;

  /** CloudWatch log group name from CDK outputs. */
  protected static String logGroupName;

  /**
   * Validates AWS credentials are properly configured before any tests run.
   *
   * <p>Uses STS GetCallerIdentity to verify credentials without requiring specific permissions. If
   * credentials are not available, the test fails fast with a clear error message.
   *
   * @throws AssertionError if AWS credentials are not available
   */
  @BeforeAll
  static void validateAwsCredentials() {
    try (StsClient stsClient = StsClient.builder().region(AWS_REGION).build()) {
      var identity = stsClient.getCallerIdentity();
      assertThat(identity.account()).as("AWS account should not be null").isNotNull();
      System.out.printf("[CloudAwsTestBase] ✓ Authenticated: %s%n", identity.arn());

      // Set resource names based on account (matching CDK construct naming in e2e-test-stack.ts)
      bucketName = "runbook-synthesizer-e2e-" + identity.account();
      logGroupName = "/runbook-synthesizer/e2e";

      // Ensure CDK infrastructure is deployed (auto-deploy if needed)
      CdkInfrastructureSupport.ensureInfrastructureDeployed(bucketName, logGroupName, AWS_REGION);
    } catch (Exception e) {
      failWithAwsCredentialsError(e.getMessage());
    }
  }

  /**
   * Fails the test with a prominent AWS credentials error message.
   *
   * @param reason the specific reason for the failure
   */
  private static void failWithAwsCredentialsError(String reason) {
    String errorMessage =
        """

        ╔══════════════════════════════════════════════════════════════════════════════╗
        ║                 🔐 AWS CREDENTIALS NOT AVAILABLE 🔐                           ║
        ╠══════════════════════════════════════════════════════════════════════════════╣
        ║                                                                              ║
        ║  Reason: %s
        ║                                                                              ║
        ║  These E2E tests require valid AWS credentials to run.                       ║
        ║                                                                              ║
        ║  To fix this:                                                                ║
        ║    1. Configure AWS credentials using one of these methods:                  ║
        ║       - Environment variables: AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY      ║
        ║       - AWS credentials file: ~/.aws/credentials                             ║
        ║       - AWS SSO: aws sso login --profile <profile>                           ║
        ║    2. Verify credentials work: aws sts get-caller-identity                   ║
        ║    3. Re-run the tests with -Pe2e-aws-cloud profile                          ║
        ║                                                                              ║
        ║  To skip these tests:                                                        ║
        ║    Run without -Pe2e-aws-cloud or -Daws.cloud.enabled=true                   ║
        ║                                                                              ║
        ╚══════════════════════════════════════════════════════════════════════════════╝

        """
            .formatted(reason);

    System.err.println(errorMessage);
    throw new AssertionError("AWS CREDENTIALS NOT AVAILABLE: " + reason);
  }

  /**
   * Creates a new S3 client configured for the E2E test region.
   *
   * @return a new S3Client instance (caller is responsible for closing)
   */
  protected static S3Client createS3Client() {
    return S3Client.builder().region(AWS_REGION).build();
  }

  /**
   * Creates a new CloudWatch Logs client configured for the E2E test region.
   *
   * @return a new CloudWatchLogsClient instance (caller is responsible for closing)
   */
  protected static CloudWatchLogsClient createCloudWatchLogsClient() {
    return CloudWatchLogsClient.builder().region(AWS_REGION).build();
  }

  /**
   * Gets the S3 bucket name provisioned by CDK.
   *
   * @return the bucket name, or null if credentials validation hasn't run
   */
  protected static String getBucketName() {
    return bucketName;
  }

  /**
   * Gets the CloudWatch log group name provisioned by CDK.
   *
   * @return the log group name, or null if credentials validation hasn't run
   */
  protected static String getLogGroupName() {
    return logGroupName;
  }

  /**
   * Ensures Docker is available for container-based tests.
   *
   * <p>Delegates to the centralized {@link com.oracle.runbook.integration.DockerSupport} utility
   * which handles Docker detection, auto-start, and fail-fast behavior.
   *
   * @throws AssertionError if Docker is not available and cannot be started
   * @see com.oracle.runbook.integration.DockerSupport#ensureDockerAvailable()
   */
  protected static void ensureDockerAvailable() {
    com.oracle.runbook.integration.DockerSupport.ensureDockerAvailable();
  }
}
