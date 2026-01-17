package com.oracle.runbook.rag;

import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.ObjectSummary;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import com.oracle.bmc.objectstorage.responses.ListObjectsResponse;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * OCI Object Storage client wrapper for runbook bucket operations.
 *
 * <p>Provides methods to list and retrieve markdown runbook files from OCI Object Storage buckets.
 */
public class OciObjectStorageClient {

  private static final String MARKDOWN_SUFFIX = ".md";

  private final ObjectStorageClient objectStorageClient;

  /**
   * Creates a new OciObjectStorageClient.
   *
   * @param objectStorageClient the OCI Object Storage client
   */
  public OciObjectStorageClient(ObjectStorageClient objectStorageClient) {
    this.objectStorageClient =
        Objects.requireNonNull(objectStorageClient, "objectStorageClient cannot be null");
  }

  /**
   * Lists all markdown runbook files in the specified bucket.
   *
   * @param namespace the Object Storage namespace
   * @param bucketName the bucket name
   * @return a CompletableFuture containing the list of markdown file names
   */
  public CompletableFuture<List<String>> listRunbooks(String namespace, String bucketName) {
    return CompletableFuture.supplyAsync(
        () -> {
          ListObjectsRequest request =
              ListObjectsRequest.builder().namespaceName(namespace).bucketName(bucketName).build();

          ListObjectsResponse response = objectStorageClient.listObjects(request);

          return response.getListObjects().getObjects().stream()
              .map(ObjectSummary::getName)
              .filter(name -> name.endsWith(MARKDOWN_SUFFIX))
              .collect(Collectors.toList());
        });
  }

  /**
   * Gets the content of a runbook file.
   *
   * @param namespace the Object Storage namespace
   * @param bucketName the bucket name
   * @param objectName the object name (file path)
   * @return a CompletableFuture containing the file content, or empty if not found
   */
  public CompletableFuture<Optional<String>> getRunbookContent(
      String namespace, String bucketName, String objectName) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            GetObjectRequest request =
                GetObjectRequest.builder()
                    .namespaceName(namespace)
                    .bucketName(bucketName)
                    .objectName(objectName)
                    .build();

            GetObjectResponse response = objectStorageClient.getObject(request);

            try (InputStream inputStream = response.getInputStream();
                BufferedReader reader =
                    new BufferedReader(
                        new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
              String content = reader.lines().collect(Collectors.joining("\n"));
              return Optional.of(content);
            }
          } catch (BmcException e) {
            if (e.getStatusCode() == 404) {
              return Optional.empty();
            }
            throw e;
          } catch (Exception e) {
            throw new RuntimeException("Failed to read runbook content: " + objectName, e);
          }
        });
  }
}
