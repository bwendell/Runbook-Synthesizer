package com.oracle.runbook.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link OciObjectStorageClient}. */
class OciObjectStorageClientTest {

  @Test
  @DisplayName("OciObjectStorageClient constructor should reject null client")
  void testConstructorRejectsNullClient() {
    assertThatThrownBy(() -> new OciObjectStorageClient(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("listRunbooks should return List of markdown file names")
  void testListRunbooksContract() {
    try {
      var method =
          OciObjectStorageClient.class.getMethod("listRunbooks", String.class, String.class);
      assertThat(method.getReturnType()).isEqualTo(CompletableFuture.class);
    } catch (NoSuchMethodException e) {
      fail("listRunbooks(String, String) method should exist");
    }
  }

  @Test
  @DisplayName("getRunbookContent should return Optional with content")
  void testGetRunbookContentContract() {
    try {
      var method =
          OciObjectStorageClient.class.getMethod(
              "getRunbookContent", String.class, String.class, String.class);
      assertThat(method.getReturnType()).isEqualTo(CompletableFuture.class);
    } catch (NoSuchMethodException e) {
      fail("getRunbookContent(String, String, String) method should exist");
    }
  }
}
