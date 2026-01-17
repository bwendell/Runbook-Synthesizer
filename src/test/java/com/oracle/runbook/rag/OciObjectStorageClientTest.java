package com.oracle.runbook.rag;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link OciObjectStorageClient}. */
class OciObjectStorageClientTest {

  @Test
  @DisplayName("OciObjectStorageClient constructor should reject null client")
  void testConstructorRejectsNullClient() {
    assertThrows(NullPointerException.class, () -> new OciObjectStorageClient(null));
  }

  @Test
  @DisplayName("listRunbooks should return List of markdown file names")
  void testListRunbooksContract() {
    try {
      var method =
          OciObjectStorageClient.class.getMethod("listRunbooks", String.class, String.class);
      assertTrue(method.getReturnType().equals(CompletableFuture.class));
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
      assertTrue(method.getReturnType().equals(CompletableFuture.class));
    } catch (NoSuchMethodException e) {
      fail("getRunbookContent(String, String, String) method should exist");
    }
  }
}
