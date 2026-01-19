package com.oracle.runbook.infrastructure.cloud.oci;

import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.ObjectSummary;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import com.oracle.bmc.objectstorage.responses.ListObjectsResponse;
import com.oracle.runbook.infrastructure.cloud.CloudStorageAdapter;
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
 * OCI Object Storage implementation of {@link CloudStorageAdapter}.
 *
 * <p>Provides cloud storage operations using OCI Object Storage SDK for runbook bucket operations.
 */
public class OciObjectStorageAdapter implements CloudStorageAdapter {

  private static final String MARKDOWN_SUFFIX = ".md";

  private final ObjectStorageClient objectStorageClient;
  private final String namespace;

  /**
   * Creates a new OciObjectStorageAdapter.
   *
   * @param objectStorageClient the OCI Object Storage client
   * @param namespace the OCI Object Storage namespace
   * @throws NullPointerException if objectStorageClient or namespace is null
   */
  public OciObjectStorageAdapter(ObjectStorageClient objectStorageClient, String namespace) {
    this.objectStorageClient =
        Objects.requireNonNull(objectStorageClient, "objectStorageClient cannot be null");
    this.namespace = Objects.requireNonNull(namespace, "namespace cannot be null");
  }

  @Override
  public String providerType() {
    return "oci";
  }

  @Override
  public CompletableFuture<List<String>> listRunbooks(String containerName) {
    return CompletableFuture.supplyAsync(
        () -> {
          ListObjectsRequest request =
              ListObjectsRequest.builder()
                  .namespaceName(namespace)
                  .bucketName(containerName)
                  .build();

          ListObjectsResponse response = objectStorageClient.listObjects(request);

          return response.getListObjects().getObjects().stream()
              .map(ObjectSummary::getName)
              .filter(name -> name.endsWith(MARKDOWN_SUFFIX))
              .collect(Collectors.toList());
        });
  }

  @Override
  public CompletableFuture<Optional<String>> getRunbookContent(
      String containerName, String objectPath) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            GetObjectRequest request =
                GetObjectRequest.builder()
                    .namespaceName(namespace)
                    .bucketName(containerName)
                    .objectName(objectPath)
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
            throw new RuntimeException("Failed to read runbook content: " + objectPath, e);
          }
        });
  }
}
