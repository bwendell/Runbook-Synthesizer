package com.oracle.runbook.infrastructure.cloud;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Port interface for cloud storage operations.
 *
 * <p>This interface defines the contract for cloud storage adapters in the Hexagonal Architecture.
 * Implementations provide concrete integrations with specific cloud storage services like OCI
 * Object Storage, AWS S3, etc.
 *
 * <p>All implementations must be non-blocking and return CompletableFuture to support Helidon SE's
 * reactive patterns.
 *
 * @see com.oracle.runbook.infrastructure.cloud.oci.OciObjectStorageAdapter
 * @see com.oracle.runbook.infrastructure.cloud.aws.AwsS3StorageAdapter
 */
public interface CloudStorageAdapter {

  /**
   * Returns the cloud provider type for this adapter.
   *
   * <p>Examples: "oci", "aws"
   *
   * @return the provider type identifier, never null
   */
  String providerType();

  /**
   * Lists all markdown runbook files in the specified container/bucket.
   *
   * <p>The implementation should filter for markdown files (.md extension) and return fully
   * qualified object paths within the container.
   *
   * @param containerName the storage container or bucket name
   * @return a CompletableFuture containing the list of runbook file paths, never null (may complete
   *     with empty list)
   */
  CompletableFuture<List<String>> listRunbooks(String containerName);

  /**
   * Gets the content of a runbook file from cloud storage.
   *
   * @param containerName the storage container or bucket name
   * @param objectPath the path to the object within the container
   * @return a CompletableFuture containing the file content, or empty if not found
   */
  CompletableFuture<Optional<String>> getRunbookContent(String containerName, String objectPath);
}
