package com.oracle.runbook.infrastructure.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Contract tests for {@link CloudStorageAdapter} interface.
 *
 * <p>Verifies the interface contract for cloud storage operations across providers.
 */
class CloudStorageAdapterTest {

  @Test
  @DisplayName("CloudStorageAdapter should be an interface")
  void cloudStorageAdapterShouldBeInterface() {
    assertThat(CloudStorageAdapter.class.isInterface())
        .as("CloudStorageAdapter must be an interface for polymorphic storage operations")
        .isTrue();
  }

  @Test
  @DisplayName("CloudStorageAdapter should declare providerType() method")
  void shouldDeclareProviderTypeMethod() throws NoSuchMethodException {
    var method = CloudStorageAdapter.class.getMethod("providerType");

    assertThat(method.getReturnType())
        .as("providerType() should return String")
        .isEqualTo(String.class);
    assertThat(method.getParameterCount()).as("providerType() should take no parameters").isZero();
  }

  @Test
  @DisplayName("CloudStorageAdapter should declare listRunbooks(String) method")
  void shouldDeclareListRunbooksMethod() throws NoSuchMethodException {
    var method = CloudStorageAdapter.class.getMethod("listRunbooks", String.class);

    assertThat(method.getReturnType())
        .as("listRunbooks() should return CompletableFuture")
        .isEqualTo(CompletableFuture.class);
    assertThat(method.getParameterCount())
        .as("listRunbooks() should take one parameter")
        .isEqualTo(1);
  }

  @Test
  @DisplayName("CloudStorageAdapter should declare getRunbookContent(String, String) method")
  void shouldDeclareGetRunbookContentMethod() throws NoSuchMethodException {
    var method =
        CloudStorageAdapter.class.getMethod("getRunbookContent", String.class, String.class);

    assertThat(method.getReturnType())
        .as("getRunbookContent() should return CompletableFuture")
        .isEqualTo(CompletableFuture.class);
    assertThat(method.getParameterCount())
        .as("getRunbookContent() should take two parameters")
        .isEqualTo(2);
  }
}
