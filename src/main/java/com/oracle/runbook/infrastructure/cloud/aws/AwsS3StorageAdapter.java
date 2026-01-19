package com.oracle.runbook.infrastructure.cloud.aws;

import com.oracle.runbook.infrastructure.cloud.CloudStorageAdapter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * AWS S3 implementation of {@link CloudStorageAdapter}.
 *
 * <p>Provides cloud storage operations using AWS S3AsyncClient for non-blocking operations
 * compatible with Helidon SE's reactive patterns.
 */
public class AwsS3StorageAdapter implements CloudStorageAdapter {

  private static final String MARKDOWN_SUFFIX = ".md";

  private final S3AsyncClient s3Client;

  /**
   * Creates a new AwsS3StorageAdapter.
   *
   * @param s3Client the AWS S3 async client
   * @throws NullPointerException if s3Client is null
   */
  public AwsS3StorageAdapter(S3AsyncClient s3Client) {
    this.s3Client = Objects.requireNonNull(s3Client, "s3Client cannot be null");
  }

  @Override
  public String providerType() {
    return "aws";
  }

  @Override
  public CompletableFuture<List<String>> listRunbooks(String containerName) {
    var request = ListObjectsV2Request.builder().bucket(containerName).build();

    return s3Client
        .listObjectsV2(request)
        .thenApply(
            response ->
                response.contents().stream()
                    .map(S3Object::key)
                    .filter(key -> key.endsWith(MARKDOWN_SUFFIX))
                    .toList());
  }

  @Override
  public CompletableFuture<Optional<String>> getRunbookContent(
      String containerName, String objectPath) {
    var request =
        software.amazon.awssdk.services.s3.model.GetObjectRequest.builder()
            .bucket(containerName)
            .key(objectPath)
            .build();

    return s3Client
        .getObject(request, AsyncResponseTransformer.toBytes())
        .thenApply(responseBytes -> Optional.of(responseBytes.asUtf8String()))
        .exceptionally(
            throwable -> {
              // Unwrap CompletionException to get the actual cause
              Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
              if (cause instanceof NoSuchKeyException) {
                return Optional.empty();
              }
              throw new RuntimeException("Failed to get runbook content: " + objectPath, cause);
            });
  }
}
