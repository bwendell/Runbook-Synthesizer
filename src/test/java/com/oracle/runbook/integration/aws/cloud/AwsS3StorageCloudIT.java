/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0.
 */
package com.oracle.runbook.integration.aws.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import com.oracle.runbook.infrastructure.cloud.aws.AwsS3StorageAdapter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * E2E integration tests for {@link AwsS3StorageAdapter} against real AWS S3 cloud service.
 *
 * <p>These tests run against the CDK-provisioned S3 bucket and verify:
 *
 * <ul>
 *   <li>Listing markdown files in the bucket
 *   <li>Reading file content from the bucket
 *   <li>Handling non-existent keys gracefully
 * </ul>
 *
 * <p><strong>Prerequisites:</strong>
 *
 * <ul>
 *   <li>AWS credentials configured
 *   <li>CDK infrastructure deployed (infra/npm run cdk:deploy)
 *   <li>Run with -Pe2e-aws-cloud Maven profile
 * </ul>
 */
@DisplayName("AWS S3 Cloud E2E Tests")
@EnabledIfSystemProperty(named = "aws.cloud.enabled", matches = "true")
class AwsS3StorageCloudIT extends CloudAwsTestBase {

  /** Unique test run ID to prevent conflicts between parallel runs. */
  private static final String TEST_RUN_ID = UUID.randomUUID().toString().substring(0, 8);

  /** Prefix for all test files to facilitate cleanup. */
  private static final String TEST_PREFIX = "e2e-test-" + TEST_RUN_ID + "/";

  private static S3AsyncClient s3Client;

  /** Test file keys for cleanup tracking. */
  private static final String RUNBOOK1_KEY = TEST_PREFIX + "runbook1.md";

  private static final String RUNBOOK2_KEY = TEST_PREFIX + "runbook2.md";
  private static final String NON_MARKDOWN_KEY = TEST_PREFIX + "not-a-runbook.txt";

  @BeforeAll
  static void setupTestFiles() throws Exception {
    s3Client = S3AsyncClient.builder().region(AWS_REGION).build();

    System.out.printf("[AwsS3StorageCloudIT] Setting up test files with prefix: %s%n", TEST_PREFIX);
    System.out.printf("[AwsS3StorageCloudIT] Using bucket: %s%n", getBucketName());

    // Upload test runbooks
    uploadTestFile(RUNBOOK1_KEY, "# Runbook 1\nThis is test content for runbook 1.");
    uploadTestFile(RUNBOOK2_KEY, "# Runbook 2\nAnother runbook content.");
    uploadTestFile(NON_MARKDOWN_KEY, "This should not be listed as a runbook.");
  }

  private static void uploadTestFile(String key, String content) throws Exception {
    s3Client
        .putObject(
            PutObjectRequest.builder().bucket(getBucketName()).key(key).build(),
            AsyncRequestBody.fromString(content))
        .get();
    System.out.printf("[AwsS3StorageCloudIT] Uploaded test file: %s%n", key);
  }

  @AfterAll
  static void cleanupTestFiles() throws Exception {
    if (s3Client != null) {
      System.out.printf(
          "[AwsS3StorageCloudIT] Cleaning up test files with prefix: %s%n", TEST_PREFIX);

      // Delete test files
      deleteTestFile(RUNBOOK1_KEY);
      deleteTestFile(RUNBOOK2_KEY);
      deleteTestFile(NON_MARKDOWN_KEY);

      s3Client.close();
    }
  }

  private static void deleteTestFile(String key) {
    try {
      s3Client
          .deleteObject(DeleteObjectRequest.builder().bucket(getBucketName()).key(key).build())
          .get();
      System.out.printf("[AwsS3StorageCloudIT] Deleted test file: %s%n", key);
    } catch (Exception e) {
      System.err.printf("[AwsS3StorageCloudIT] Failed to delete %s: %s%n", key, e.getMessage());
    }
  }

  @Nested
  @DisplayName("listRunbooks()")
  class ListRunbooksTests {

    @Test
    @DisplayName("Should list markdown files from S3 bucket")
    void shouldListMarkdownFiles() throws Exception {
      AwsS3StorageAdapter adapter = new AwsS3StorageAdapter(s3Client);

      List<String> runbooks = adapter.listRunbooks(getBucketName()).get();

      assertThat(runbooks)
          .as("Should contain our test markdown files")
          .contains(RUNBOOK1_KEY, RUNBOOK2_KEY);
    }

    @Test
    @DisplayName("Should only list markdown files, not other file types")
    void shouldOnlyListMarkdownFiles() throws Exception {
      AwsS3StorageAdapter adapter = new AwsS3StorageAdapter(s3Client);

      List<String> runbooks = adapter.listRunbooks(getBucketName()).get();

      assertThat(runbooks)
          .as("Should not contain non-markdown files")
          .doesNotContain(NON_MARKDOWN_KEY);
    }
  }

  @Nested
  @DisplayName("getRunbookContent()")
  class GetRunbookContentTests {

    @Test
    @DisplayName("Should read runbook content from S3")
    void shouldReadRunbookContent() throws Exception {
      AwsS3StorageAdapter adapter = new AwsS3StorageAdapter(s3Client);

      Optional<String> content = adapter.getRunbookContent(getBucketName(), RUNBOOK1_KEY).get();

      assertThat(content).as("Should return content for existing file").isPresent();
      assertThat(content.get())
          .contains("# Runbook 1")
          .contains("This is test content for runbook 1.");
    }

    @Test
    @DisplayName("Should return empty Optional for non-existent key")
    void shouldReturnEmptyForNonExistentKey() throws Exception {
      AwsS3StorageAdapter adapter = new AwsS3StorageAdapter(s3Client);

      Optional<String> content =
          adapter.getRunbookContent(getBucketName(), TEST_PREFIX + "nonexistent.md").get();

      assertThat(content).as("Non-existent key should return empty Optional").isEmpty();
    }
  }
}
