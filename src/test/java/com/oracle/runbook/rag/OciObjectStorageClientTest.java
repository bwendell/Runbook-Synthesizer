package com.oracle.runbook.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.ListObjects;
import com.oracle.bmc.objectstorage.model.ObjectSummary;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import com.oracle.bmc.objectstorage.responses.ListObjectsResponse;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link OciObjectStorageClient}. */
class OciObjectStorageClientTest {

  private static final String NAMESPACE = "test-namespace";
  private static final String BUCKET_NAME = "test-bucket";

  private ObjectStorageClient mockClient;
  private OciObjectStorageClient ociObjectStorageClient;

  @BeforeEach
  void setUp() {
    mockClient = mock(ObjectStorageClient.class);
    ociObjectStorageClient = new OciObjectStorageClient(mockClient);
  }

  @Test
  @DisplayName("OciObjectStorageClient constructor should reject null client")
  void testConstructorRejectsNullClient() {
    assertThatThrownBy(() -> new OciObjectStorageClient(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Nested
  @DisplayName("listRunbooks tests")
  class ListRunbooksTests {

    @Test
    @DisplayName("listRunbooks should return only markdown files from bucket")
    void listRunbooksShouldReturnMarkdownFilesOnly() throws Exception {
      // Arrange - bucket contains mixed files
      List<ObjectSummary> objects =
          List.of(
              createObjectSummary("readme.md"),
              createObjectSummary("runbook-memory.md"),
              createObjectSummary("config.yaml"),
              createObjectSummary("script.sh"),
              createObjectSummary("troubleshooting.md"));

      mockListObjectsResponse(objects);

      // Act
      List<String> result = ociObjectStorageClient.listRunbooks(NAMESPACE, BUCKET_NAME).get();

      // Assert
      assertThat(result)
          .hasSize(3)
          .containsExactlyInAnyOrder("readme.md", "runbook-memory.md", "troubleshooting.md");
    }

    @Test
    @DisplayName("listRunbooks from empty bucket should return empty list")
    void listRunbooksFromEmptyBucketShouldReturnEmptyList() throws Exception {
      // Arrange - empty bucket
      mockListObjectsResponse(List.of());

      // Act
      List<String> result = ociObjectStorageClient.listRunbooks(NAMESPACE, BUCKET_NAME).get();

      // Assert
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("listRunbooks should propagate BmcException from SDK")
    void listRunbooksShouldPropagateExceptions() {
      // Arrange - SDK throws exception
      BmcException bmcException =
          new BmcException(500, "ServiceError", "Internal Server Error", "request-id");
      when(mockClient.listObjects(any(ListObjectsRequest.class))).thenThrow(bmcException);

      // Act & Assert
      assertThatThrownBy(() -> ociObjectStorageClient.listRunbooks(NAMESPACE, BUCKET_NAME).get())
          .isInstanceOf(ExecutionException.class)
          .hasCauseInstanceOf(BmcException.class);
    }

    private void mockListObjectsResponse(List<ObjectSummary> objects) {
      ListObjects listObjects = ListObjects.builder().objects(objects).build();
      ListObjectsResponse response = ListObjectsResponse.builder().listObjects(listObjects).build();
      when(mockClient.listObjects(any(ListObjectsRequest.class))).thenReturn(response);
    }

    private ObjectSummary createObjectSummary(String name) {
      return ObjectSummary.builder().name(name).build();
    }
  }

  @Nested
  @DisplayName("getRunbookContent tests")
  class GetRunbookContentTests {

    private static final String OBJECT_NAME = "runbook.md";

    @Test
    @DisplayName("getRunbookContent should return content from SDK response")
    void getRunbookContentShouldReturnContent() throws Exception {
      // Arrange
      String expectedContent = "# Troubleshooting\n\nCheck CPU usage with `top`";
      mockGetObjectResponse(expectedContent);

      // Act
      Optional<String> result =
          ociObjectStorageClient.getRunbookContent(NAMESPACE, BUCKET_NAME, OBJECT_NAME).get();

      // Assert
      assertThat(result).isPresent().hasValue(expectedContent);
    }

    @Test
    @DisplayName("getRunbookContent should return empty Optional for 404 response")
    void getRunbookContentFor404ShouldReturnEmpty() throws Exception {
      // Arrange - SDK throws 404 BmcException
      BmcException notFoundException =
          new BmcException(404, "ObjectNotFound", "Object not found", "request-id");
      when(mockClient.getObject(any(GetObjectRequest.class))).thenThrow(notFoundException);

      // Act
      Optional<String> result =
          ociObjectStorageClient.getRunbookContent(NAMESPACE, BUCKET_NAME, OBJECT_NAME).get();

      // Assert
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getRunbookContent should propagate non-404 BmcException")
    void getRunbookContentShouldPropagateNon404Exceptions() {
      // Arrange - SDK throws 500 BmcException
      BmcException serverException =
          new BmcException(500, "ServiceError", "Internal Server Error", "request-id");
      when(mockClient.getObject(any(GetObjectRequest.class))).thenThrow(serverException);

      // Act & Assert
      assertThatThrownBy(
              () ->
                  ociObjectStorageClient
                      .getRunbookContent(NAMESPACE, BUCKET_NAME, OBJECT_NAME)
                      .get())
          .isInstanceOf(ExecutionException.class)
          .hasCauseInstanceOf(BmcException.class);
    }

    @Test
    @DisplayName("getRunbookContent should wrap non-BmcException in RuntimeException")
    void getRunbookContentShouldWrapNon404ExceptionsInRuntime() {
      // Arrange - SDK throws unexpected exception that becomes IO error
      // Note: This tests the catch (Exception e) block which wraps in RuntimeException
      RuntimeException ioException = new RuntimeException("Connection reset");
      when(mockClient.getObject(any(GetObjectRequest.class))).thenThrow(ioException);

      // Act & Assert
      assertThatThrownBy(
              () ->
                  ociObjectStorageClient
                      .getRunbookContent(NAMESPACE, BUCKET_NAME, OBJECT_NAME)
                      .get())
          .isInstanceOf(ExecutionException.class)
          .hasCauseInstanceOf(RuntimeException.class);
    }

    private void mockGetObjectResponse(String content) {
      byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
      ByteArrayInputStream inputStream = new ByteArrayInputStream(contentBytes);

      GetObjectResponse response =
          GetObjectResponse.builder()
              .inputStream(inputStream)
              .contentLength((long) contentBytes.length)
              .build();

      when(mockClient.getObject(any(GetObjectRequest.class))).thenReturn(response);
    }
  }
}
