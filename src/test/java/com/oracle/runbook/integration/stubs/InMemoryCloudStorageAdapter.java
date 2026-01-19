package com.oracle.runbook.integration.stubs;

import com.oracle.runbook.infrastructure.cloud.CloudStorageAdapter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of {@link CloudStorageAdapter} for testing.
 *
 * <p>This stub allows tests to run without any cloud dependencies by storing runbook content in
 * memory. Use {@link #seedBucket(String, String, String)} to populate test data and {@link
 * #clearAll()} to reset between tests.
 */
public class InMemoryCloudStorageAdapter implements CloudStorageAdapter {

  private static final String MARKDOWN_SUFFIX = ".md";
  private static final String PROVIDER_TYPE = "in-memory";

  /** Storage structure: bucketName -> (objectKey -> content) */
  private final Map<String, Map<String, String>> buckets = new ConcurrentHashMap<>();

  @Override
  public String providerType() {
    return PROVIDER_TYPE;
  }

  @Override
  public CompletableFuture<List<String>> listRunbooks(String containerName) {
    return CompletableFuture.supplyAsync(
        () -> {
          Map<String, String> bucket = buckets.getOrDefault(containerName, Map.of());
          return bucket.keySet().stream()
              .filter(key -> key.endsWith(MARKDOWN_SUFFIX))
              .collect(Collectors.toList());
        });
  }

  @Override
  public CompletableFuture<Optional<String>> getRunbookContent(
      String containerName, String objectPath) {
    return CompletableFuture.supplyAsync(
        () -> {
          Map<String, String> bucket = buckets.get(containerName);
          if (bucket == null) {
            return Optional.empty();
          }
          return Optional.ofNullable(bucket.get(objectPath));
        });
  }

  /**
   * Seeds a bucket with content for testing.
   *
   * @param bucketName the bucket/container name
   * @param objectName the object key (file path)
   * @param content the file content
   */
  public void seedBucket(String bucketName, String objectName, String content) {
    buckets.computeIfAbsent(bucketName, k -> new ConcurrentHashMap<>()).put(objectName, content);
  }

  /**
   * Seeds multiple objects in a bucket.
   *
   * @param bucketName the bucket/container name
   * @param objects map of object key to content
   */
  public void seedBucket(String bucketName, Map<String, String> objects) {
    objects.forEach((key, content) -> seedBucket(bucketName, key, content));
  }

  /** Clears all stored data. Call this in test setup/teardown to ensure isolation. */
  public void clearAll() {
    buckets.clear();
  }

  /**
   * Clears a specific bucket.
   *
   * @param bucketName the bucket to clear
   */
  public void clearBucket(String bucketName) {
    buckets.remove(bucketName);
  }

  /**
   * Returns the number of objects in a bucket.
   *
   * @param bucketName the bucket to check
   * @return number of objects, or 0 if bucket doesn't exist
   */
  public int getObjectCount(String bucketName) {
    Map<String, String> bucket = buckets.get(bucketName);
    return bucket != null ? bucket.size() : 0;
  }
}
