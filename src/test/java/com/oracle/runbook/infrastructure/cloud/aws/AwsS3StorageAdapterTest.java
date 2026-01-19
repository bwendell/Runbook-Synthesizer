package com.oracle.runbook.infrastructure.cloud.aws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Unit tests for {@link AwsS3StorageAdapter}.
 *
 * <p>Uses mocked S3AsyncClient per testing-patterns-java for external SDK dependencies.
 */
class AwsS3StorageAdapterTest {

  @Nested
  @DisplayName("CloudStorageAdapter interface implementation")
  class InterfaceImplementationTests {

    @Test
    @DisplayName("providerType() should return 'aws'")
    void providerTypeShouldReturnAws() {
      S3AsyncClient mockClient = mock(S3AsyncClient.class);
      AwsS3StorageAdapter adapter = new AwsS3StorageAdapter(mockClient);

      assertThat(adapter.providerType())
          .as("providerType() must return 'aws' for S3 adapter")
          .isEqualTo("aws");
    }
  }

  @Nested
  @DisplayName("Constructor validation")
  class ConstructorValidationTests {

    @Test
    @DisplayName("Should reject null S3AsyncClient")
    void shouldRejectNullClient() {
      assertThatThrownBy(() -> new AwsS3StorageAdapter(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("s3Client");
    }
  }

  @Nested
  @DisplayName("listRunbooks()")
  class ListRunbooksTests {

    @Test
    @DisplayName("Should return list of markdown file names")
    void shouldReturnMarkdownFileNames() throws Exception {
      S3AsyncClient mockClient = mock(S3AsyncClient.class);

      ListObjectsV2Response mockResponse =
          ListObjectsV2Response.builder()
              .contents(
                  S3Object.builder().key("runbook1.md").build(),
                  S3Object.builder().key("runbook2.md").build(),
                  S3Object.builder().key("other-file.txt").build())
              .build();

      when(mockClient.listObjectsV2(any(ListObjectsV2Request.class)))
          .thenReturn(CompletableFuture.completedFuture(mockResponse));

      AwsS3StorageAdapter adapter = new AwsS3StorageAdapter(mockClient);

      List<String> runbooks = adapter.listRunbooks("test-bucket").get();

      assertThat(runbooks)
          .as("Should only include .md files")
          .containsExactly("runbook1.md", "runbook2.md");
    }

    @Test
    @DisplayName("Should return empty list when bucket is empty")
    void shouldReturnEmptyListWhenBucketEmpty() throws Exception {
      S3AsyncClient mockClient = mock(S3AsyncClient.class);

      ListObjectsV2Response mockResponse =
          ListObjectsV2Response.builder().contents(List.of()).build();

      when(mockClient.listObjectsV2(any(ListObjectsV2Request.class)))
          .thenReturn(CompletableFuture.completedFuture(mockResponse));

      AwsS3StorageAdapter adapter = new AwsS3StorageAdapter(mockClient);

      List<String> runbooks = adapter.listRunbooks("test-bucket").get();

      assertThat(runbooks).as("Should return empty list for empty bucket").isEmpty();
    }
  }

  @Nested
  @DisplayName("getRunbookContent()")
  class GetRunbookContentTests {

    @Test
    @DisplayName("Should return content when object exists")
    void shouldReturnContentWhenObjectExists() throws Exception {
      S3AsyncClient mockClient = mock(S3AsyncClient.class);
      String expectedContent = "# Runbook Title\n\nThis is a runbook.";

      @SuppressWarnings("unchecked")
      ResponseBytes<GetObjectResponse> mockResponseBytes = mock(ResponseBytes.class);
      when(mockResponseBytes.asUtf8String()).thenReturn(expectedContent);

      when(mockClient.getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class)))
          .thenReturn(CompletableFuture.completedFuture(mockResponseBytes));

      AwsS3StorageAdapter adapter = new AwsS3StorageAdapter(mockClient);

      Optional<String> content = adapter.getRunbookContent("test-bucket", "runbook1.md").get();

      assertThat(content)
          .as("Should return content for existing object")
          .isPresent()
          .hasValue(expectedContent);
    }

    @Test
    @DisplayName("Should return empty when object does not exist")
    void shouldReturnEmptyWhenObjectNotExists() throws Exception {
      S3AsyncClient mockClient = mock(S3AsyncClient.class);

      CompletableFuture<ResponseBytes<GetObjectResponse>> failedFuture = new CompletableFuture<>();
      failedFuture.completeExceptionally(
          NoSuchKeyException.builder().message("Key not found").build());

      when(mockClient.getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class)))
          .thenReturn(failedFuture);

      AwsS3StorageAdapter adapter = new AwsS3StorageAdapter(mockClient);

      Optional<String> content = adapter.getRunbookContent("test-bucket", "nonexistent.md").get();

      assertThat(content).as("Should return empty for non-existent object").isEmpty();
    }
  }

  @Nested
  @DisplayName("Exception handling")
  class ExceptionHandlingTests {

    @Test
    @DisplayName("Should wrap S3Exception in RuntimeException")
    void shouldWrapS3ExceptionInRuntimeException() {
      S3AsyncClient mockClient = mock(S3AsyncClient.class);

      CompletableFuture<ResponseBytes<GetObjectResponse>> failedFuture = new CompletableFuture<>();
      failedFuture.completeExceptionally(
          software.amazon.awssdk.services.s3.model.S3Exception.builder()
              .message("Access Denied")
              .statusCode(403)
              .build());

      when(mockClient.getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class)))
          .thenReturn(failedFuture);

      AwsS3StorageAdapter adapter = new AwsS3StorageAdapter(mockClient);

      assertThatThrownBy(() -> adapter.getRunbookContent("test-bucket", "protected.md").get())
          .hasCauseInstanceOf(RuntimeException.class)
          .hasMessageContaining("protected.md");
    }

    @Test
    @DisplayName("Should return empty list when contents is null in response")
    void shouldHandleNullContentsInListResponse() throws Exception {
      S3AsyncClient mockClient = mock(S3AsyncClient.class);

      // When AWS SDK has null contents, it internally converts to empty list
      ListObjectsV2Response mockResponse =
          ListObjectsV2Response.builder().build(); // No contents set

      when(mockClient.listObjectsV2(any(ListObjectsV2Request.class)))
          .thenReturn(CompletableFuture.completedFuture(mockResponse));

      AwsS3StorageAdapter adapter = new AwsS3StorageAdapter(mockClient);

      List<String> runbooks = adapter.listRunbooks("test-bucket").get();

      assertThat(runbooks).as("Should return empty list when no contents").isEmpty();
    }
  }
}
