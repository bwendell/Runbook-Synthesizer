/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0.
 */
package com.oracle.runbook.integration.aws.local;

import static org.assertj.core.api.Assertions.assertThat;

import com.oracle.runbook.infrastructure.cloud.aws.AwsS3StorageAdapter;
import com.oracle.runbook.integration.LocalStackContainerBase;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.ec2.Ec2AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Integration tests for {@link AwsS3StorageAdapter} using LocalStack.
 *
 * <p>These tests verify S3 storage operations against a local S3-compatible service (LocalStack).
 * No AWS credentials or cloud resources are required.
 */
@DisplayName("AWS S3 Local (LocalStack) Integration Tests")
class AwsS3StorageLocalIT extends LocalStackContainerBase {

  private static final String TEST_BUCKET = "test-runbook-bucket";

  private static S3AsyncClient s3Client;

  @BeforeAll
  static void setupBucket() throws Exception {
    s3Client = createS3Client();

    // Create test bucket
    s3Client.createBucket(CreateBucketRequest.builder().bucket(TEST_BUCKET).build()).get();

    // Upload test runbooks
    uploadTestFile("runbook1.md", "# Runbook 1\nThis is test content for runbook 1.");
    uploadTestFile("runbook2.md", "# Runbook 2\nAnother runbook content.");
    uploadTestFile("not-a-runbook.txt", "This should not be listed as a runbook.");
  }

  private static void uploadTestFile(String key, String content) throws Exception {
    s3Client
        .putObject(
            PutObjectRequest.builder().bucket(TEST_BUCKET).key(key).build(),
            AsyncRequestBody.fromString(content))
        .get();
  }

  @Nested
  @DisplayName("listRunbooks()")
  class ListRunbooksTests {

    @Test
    @DisplayName("Should list all runbooks from S3 bucket")
    void shouldListAllRunbooks() throws Exception {
      AwsS3StorageAdapter adapter = new AwsS3StorageAdapter(s3Client);

      List<String> runbooks = adapter.listRunbooks(TEST_BUCKET).get();

      assertThat(runbooks)
          .as("Should list markdown files in bucket")
          .containsExactlyInAnyOrder("runbook1.md", "runbook2.md");
    }

    @Test
    @DisplayName("Should return empty list for empty bucket")
    void shouldReturnEmptyListForEmptyBucket() throws Exception {
      // Create empty bucket
      String emptyBucket = "empty-bucket";
      s3Client.createBucket(CreateBucketRequest.builder().bucket(emptyBucket).build()).get();

      AwsS3StorageAdapter adapter = new AwsS3StorageAdapter(s3Client);

      List<String> runbooks = adapter.listRunbooks(emptyBucket).get();

      assertThat(runbooks).as("Empty bucket should return empty list").isEmpty();
    }
  }

  @Nested
  @DisplayName("getRunbookContent()")
  class GetRunbookContentTests {

    @Test
    @DisplayName("Should read runbook content from S3")
    void shouldReadRunbookContent() throws Exception {
      AwsS3StorageAdapter adapter = new AwsS3StorageAdapter(s3Client);

      Optional<String> content = adapter.getRunbookContent(TEST_BUCKET, "runbook1.md").get();

      assertThat(content).isPresent();
      assertThat(content.get())
          .contains("# Runbook 1")
          .contains("This is test content for runbook 1.");
    }

    @Test
    @DisplayName("Should return empty for non-existent object")
    void shouldReturnEmptyForNonExistentObject() throws Exception {
      AwsS3StorageAdapter adapter = new AwsS3StorageAdapter(s3Client);

      Optional<String> content =
          adapter.getRunbookContent(TEST_BUCKET, "nonexistent-runbook.md").get();

      assertThat(content).as("Non-existent object should return empty Optional").isEmpty();
    }
  }

  @Nested
  @DisplayName("EC2 Client Factory")
  class Ec2ClientFactoryTests {

    @Test
    @DisplayName("Should create EC2 client pointing to LocalStack")
    void shouldCreateEc2ClientPointingToLocalStack() throws Exception {
      Ec2AsyncClient ec2Client = createEc2Client();

      assertThat(ec2Client).as("EC2 client should not be null").isNotNull();

      // Verify client can communicate with LocalStack by describing regions
      var regionsResponse = ec2Client.describeRegions().get();
      assertThat(regionsResponse.regions())
          .as("LocalStack EC2 should return at least one region")
          .isNotEmpty();
    }
  }
}
