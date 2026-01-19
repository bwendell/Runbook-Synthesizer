package com.oracle.runbook.infrastructure.cloud.oci;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.ListObjects;
import com.oracle.bmc.objectstorage.model.ObjectSummary;
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest;
import com.oracle.bmc.objectstorage.responses.ListObjectsResponse;
import com.oracle.runbook.infrastructure.cloud.CloudStorageAdapter;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OciObjectStorageAdapter}.
 *
 * <p>Tests follow testing-patterns-java skill patterns with mocked OCI SDK.
 */
class OciObjectStorageAdapterTest {

  private static final String TEST_NAMESPACE = "test-namespace";
  private static final String TEST_BUCKET = "test-bucket";

  @Nested
  @DisplayName("CloudStorageAdapter interface implementation")
  class InterfaceImplementationTests {

    @Test
    @DisplayName("OciObjectStorageAdapter should implement CloudStorageAdapter")
    void shouldImplementCloudStorageAdapter() {
      ObjectStorageClient mockClient = mock(ObjectStorageClient.class);
      OciObjectStorageAdapter adapter = new OciObjectStorageAdapter(mockClient, TEST_NAMESPACE);

      assertThat(adapter)
          .as("OciObjectStorageAdapter must implement CloudStorageAdapter")
          .isInstanceOf(CloudStorageAdapter.class);
    }

    @Test
    @DisplayName("providerType() should return 'oci'")
    void providerTypeShouldReturnOci() {
      ObjectStorageClient mockClient = mock(ObjectStorageClient.class);
      OciObjectStorageAdapter adapter = new OciObjectStorageAdapter(mockClient, TEST_NAMESPACE);

      assertThat(adapter.providerType())
          .as("providerType() must return 'oci' for OCI adapter")
          .isEqualTo("oci");
    }
  }

  @Nested
  @DisplayName("Constructor validation")
  class ConstructorValidationTests {

    @Test
    @DisplayName("Should reject null ObjectStorageClient")
    void shouldRejectNullClient() {
      assertThatThrownBy(() -> new OciObjectStorageAdapter(null, TEST_NAMESPACE))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("objectStorageClient");
    }

    @Test
    @DisplayName("Should reject null namespace")
    void shouldRejectNullNamespace() {
      ObjectStorageClient mockClient = mock(ObjectStorageClient.class);

      assertThatThrownBy(() -> new OciObjectStorageAdapter(mockClient, null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("namespace");
    }
  }

  @Nested
  @DisplayName("listRunbooks()")
  class ListRunbooksTests {

    @Test
    @DisplayName("Should return list of markdown file names")
    void shouldReturnMarkdownFileNames() throws Exception {
      ObjectStorageClient mockClient = mock(ObjectStorageClient.class);

      ListObjects listObjects =
          ListObjects.builder()
              .objects(
                  List.of(
                      ObjectSummary.builder().name("runbook1.md").build(),
                      ObjectSummary.builder().name("runbook2.md").build(),
                      ObjectSummary.builder().name("other.txt").build()))
              .build();

      ListObjectsResponse response = ListObjectsResponse.builder().listObjects(listObjects).build();

      when(mockClient.listObjects(any(ListObjectsRequest.class))).thenReturn(response);

      OciObjectStorageAdapter adapter = new OciObjectStorageAdapter(mockClient, TEST_NAMESPACE);

      List<String> runbooks = adapter.listRunbooks(TEST_BUCKET).get();

      assertThat(runbooks)
          .as("Should only include .md files")
          .containsExactly("runbook1.md", "runbook2.md");
    }
  }
}
