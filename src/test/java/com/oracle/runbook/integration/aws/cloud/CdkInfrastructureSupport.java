/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0.
 */
package com.oracle.runbook.integration.aws.cloud;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogGroupsRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

/**
 * Utility class that ensures CDK infrastructure is deployed before running AWS cloud E2E tests.
 *
 * <p>This class provides a centralized infrastructure check that:
 *
 * <ul>
 *   <li>Checks if required AWS resources exist (S3 bucket, CloudWatch log group)
 *   <li>Attempts to deploy CDK infrastructure if resources don't exist
 *   <li>Waits up to 5 minutes for deployment to complete
 *   <li>Fails fast with a clear error banner if deployment fails
 * </ul>
 *
 * <p>Usage: Call {@link #ensureInfrastructureDeployed(String, String, Region)} in
 * {@code @BeforeAll} of AWS cloud E2E tests.
 */
public final class CdkInfrastructureSupport {

  private static final int CDK_DEPLOY_TIMEOUT_SECONDS = 300;

  /** Flag to track if CDK deployment was already attempted this test run. */
  private static volatile boolean deploymentAttempted = false;

  /** Flag to track if infrastructure is verified as deployed. */
  private static volatile boolean infrastructureVerified = false;

  private CdkInfrastructureSupport() {
    // Utility class, no instantiation
  }

  /**
   * Ensures CDK infrastructure is deployed for AWS cloud E2E tests.
   *
   * <p>This method:
   *
   * <ol>
   *   <li>Checks if required AWS resources exist (fast path)
   *   <li>If not, attempts to deploy CDK infrastructure from infra/ directory
   *   <li>Waits for deployment to complete with timeout
   *   <li>Fails fast with a clear error banner if deployment fails
   * </ol>
   *
   * <p>This method is thread-safe and idempotent - multiple calls will only attempt deployment once
   * per test run.
   *
   * @param bucketName the expected S3 bucket name
   * @param logGroupName the expected CloudWatch log group name
   * @param region the AWS region
   * @throws AssertionError if infrastructure cannot be deployed
   */
  public static synchronized void ensureInfrastructureDeployed(
      String bucketName, String logGroupName, Region region) {
    // Fast path: Infrastructure already verified
    if (infrastructureVerified) {
      return;
    }

    // Check if resources exist
    if (isInfrastructureDeployed(bucketName, logGroupName, region)) {
      System.out.println("[CdkInfrastructureSupport] ✓ CDK infrastructure is deployed.");
      infrastructureVerified = true;
      return;
    }

    // Only attempt deployment once per test run
    if (deploymentAttempted) {
      failWithInfrastructureError(
          "CDK infrastructure not available and previous deployment attempt failed.");
    }

    deploymentAttempted = true;
    System.out.println(
        "[CdkInfrastructureSupport] CDK infrastructure not deployed. Attempting to deploy...");

    if (!attemptCdkDeploy()) {
      failWithInfrastructureError("Failed to deploy CDK infrastructure. See error output above.");
    }

    // Verify deployment succeeded
    if (isInfrastructureDeployed(bucketName, logGroupName, region)) {
      System.out.println("[CdkInfrastructureSupport] ✓ CDK infrastructure deployed successfully.");
      infrastructureVerified = true;
    } else {
      failWithInfrastructureError(
          "CDK deployment completed but resources are not accessible. "
              + "Check AWS Console for deployment status.");
    }
  }

  /**
   * Checks if the required CDK infrastructure resources exist.
   *
   * @param bucketName the S3 bucket name to check
   * @param logGroupName the CloudWatch log group name to check
   * @param region the AWS region
   * @return true if all resources exist, false otherwise
   */
  private static boolean isInfrastructureDeployed(
      String bucketName, String logGroupName, Region region) {
    return isBucketExists(bucketName, region) && isLogGroupExists(logGroupName, region);
  }

  /**
   * Checks if an S3 bucket exists.
   *
   * @param bucketName the bucket name
   * @param region the AWS region
   * @return true if bucket exists, false otherwise
   */
  private static boolean isBucketExists(String bucketName, Region region) {
    try (S3Client s3 = S3Client.builder().region(region).build()) {
      s3.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
      System.out.printf("[CdkInfrastructureSupport] ✓ S3 bucket exists: %s%n", bucketName);
      return true;
    } catch (NoSuchBucketException e) {
      System.out.printf("[CdkInfrastructureSupport] ✗ S3 bucket does not exist: %s%n", bucketName);
      return false;
    } catch (Exception e) {
      System.out.printf(
          "[CdkInfrastructureSupport] ✗ Error checking bucket %s: %s%n",
          bucketName, e.getMessage());
      return false;
    }
  }

  /**
   * Checks if a CloudWatch log group exists.
   *
   * @param logGroupName the log group name
   * @param region the AWS region
   * @return true if log group exists, false otherwise
   */
  private static boolean isLogGroupExists(String logGroupName, Region region) {
    try (CloudWatchLogsClient logs = CloudWatchLogsClient.builder().region(region).build()) {
      var response =
          logs.describeLogGroups(
              DescribeLogGroupsRequest.builder().logGroupNamePrefix(logGroupName).build());

      boolean exists =
          response.logGroups().stream().anyMatch(lg -> lg.logGroupName().equals(logGroupName));

      if (exists) {
        System.out.printf(
            "[CdkInfrastructureSupport] ✓ CloudWatch log group exists: %s%n", logGroupName);
      } else {
        System.out.printf(
            "[CdkInfrastructureSupport] ✗ CloudWatch log group does not exist: %s%n", logGroupName);
      }
      return exists;
    } catch (Exception e) {
      System.out.printf(
          "[CdkInfrastructureSupport] ✗ Error checking log group %s: %s%n",
          logGroupName, e.getMessage());
      return false;
    }
  }

  /**
   * Attempts to deploy CDK infrastructure from the infra/ directory.
   *
   * @return true if deployment command completed successfully, false otherwise
   */
  private static boolean attemptCdkDeploy() {
    // Find project root (look for infra/ directory)
    File projectRoot = findProjectRoot();
    if (projectRoot == null) {
      System.err.println(
          "[CdkInfrastructureSupport] ✗ Could not find project root with infra/ directory.");
      return false;
    }

    File infraDir = new File(projectRoot, "infra");
    System.out.printf("[CdkInfrastructureSupport] Deploying CDK from: %s%n", infraDir);

    try {
      // First ensure npm dependencies are installed
      if (!runNpmCommand(infraDir, "install")) {
        System.err.println("[CdkInfrastructureSupport] ✗ npm install failed.");
        return false;
      }

      // Run CDK deploy
      System.out.println("[CdkInfrastructureSupport] Running: npm run cdk:deploy");
      return runNpmCommand(infraDir, "run", "cdk:deploy");
    } catch (Exception e) {
      System.err.printf("[CdkInfrastructureSupport] ✗ CDK deployment failed: %s%n", e.getMessage());
      return false;
    }
  }

  /**
   * Runs an npm command in the specified directory.
   *
   * @param workingDir the working directory
   * @param args npm arguments
   * @return true if command succeeded, false otherwise
   */
  private static boolean runNpmCommand(File workingDir, String... args) throws Exception {
    String os = System.getProperty("os.name", "").toLowerCase();
    String npm = os.contains("win") ? "npm.cmd" : "npm";

    String[] command = new String[args.length + 1];
    command[0] = npm;
    System.arraycopy(args, 0, command, 1, args.length);

    ProcessBuilder pb = new ProcessBuilder(command);
    pb.directory(workingDir);
    pb.redirectErrorStream(true);

    Process process = pb.start();

    // Stream output to console
    try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        System.out.println("[CDK] " + line);
      }
    }

    boolean completed = process.waitFor(CDK_DEPLOY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    if (!completed) {
      process.destroyForcibly();
      System.err.println("[CdkInfrastructureSupport] ✗ CDK deploy timed out.");
      return false;
    }

    return process.exitValue() == 0;
  }

  /**
   * Finds the project root directory by looking for a directory containing "infra/".
   *
   * @return the project root directory, or null if not found
   */
  private static File findProjectRoot() {
    // Start from current working directory
    File current = new File(System.getProperty("user.dir"));

    // Walk up to find infra/ directory (max 5 levels)
    for (int i = 0; i < 5 && current != null; i++) {
      File infraDir = new File(current, "infra");
      if (infraDir.exists() && infraDir.isDirectory()) {
        return current;
      }
      current = current.getParentFile();
    }

    return null;
  }

  /**
   * Fails the test with a prominent infrastructure error message.
   *
   * @param reason the specific reason for the failure
   */
  private static void failWithInfrastructureError(String reason) {
    String errorMessage =
        """

        ╔══════════════════════════════════════════════════════════════════════════════╗
        ║              ☁️ CDK INFRASTRUCTURE NOT AVAILABLE ☁️                         ║
        ╠══════════════════════════════════════════════════════════════════════════════╣
        ║                                                                              ║
        ║  Reason: %s
        ║                                                                              ║
        ║  AWS Cloud E2E tests require CDK-provisioned resources.                      ║
        ║                                                                              ║
        ║  To fix this manually:                                                       ║
        ║    1. cd infra                                                               ║
        ║    2. npm install                                                            ║
        ║    3. npm run cdk:deploy                                                     ║
        ║    4. Re-run the tests with -Pe2e-aws-cloud                                  ║
        ║                                                                              ║
        ║  Common issues:                                                              ║
        ║    - AWS credentials not configured (run: aws configure)                     ║
        ║    - Insufficient IAM permissions (need CloudFormation, S3, CloudWatch)      ║
        ║    - CDK not bootstrapped (run: npx cdk bootstrap)                           ║
        ║                                                                              ║
        ║  To skip these tests:                                                        ║
        ║    Run without -Pe2e-aws-cloud profile                                       ║
        ║                                                                              ║
        ╚══════════════════════════════════════════════════════════════════════════════╝

        """
            .formatted(reason);

    System.err.println(errorMessage);
    throw new AssertionError("CDK INFRASTRUCTURE NOT AVAILABLE: " + reason);
  }

  /** Resets the deployment state. For testing purposes only. */
  static void resetState() {
    deploymentAttempted = false;
    infrastructureVerified = false;
  }
}
