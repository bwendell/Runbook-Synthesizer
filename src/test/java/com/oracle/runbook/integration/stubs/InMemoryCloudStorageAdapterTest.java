package com.oracle.runbook.integration.stubs;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link InMemoryCloudStorageAdapter}.
 *
 * <p>Verifies the in-memory adapter correctly implements CloudStorageAdapter contract for
 * cloud-free testing.
 */
class InMemoryCloudStorageAdapterTest {

  private static final String BUCKET = "test-bucket";
  private InMemoryCloudStorageAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new InMemoryCloudStorageAdapter();
  }

  @Nested
  @DisplayName("providerType tests")
  class ProviderTypeTests {

    @Test
    @DisplayName("providerType should return 'in-memory'")
    void providerTypeShouldReturnInMemory() {
      assertThat(adapter.providerType()).isEqualTo("in-memory");
    }
  }

  @Nested
  @DisplayName("listRunbooks tests")
  class ListRunbooksTests {

    @Test
    @DisplayName("listRunbooks from empty bucket should return empty list")
    void listRunbooksFromEmptyBucketShouldReturnEmptyList() throws Exception {
      List<String> result = adapter.listRunbooks(BUCKET).get();

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("listRunbooks should return only markdown files")
    void listRunbooksShouldReturnOnlyMarkdownFiles() throws Exception {
      // Arrange - seed with mixed file types
      adapter.seedBucket(BUCKET, "readme.md", "# README");
      adapter.seedBucket(BUCKET, "config.yaml", "key: value");
      adapter.seedBucket(BUCKET, "runbooks/memory.md", "# Memory Runbook");
      adapter.seedBucket(BUCKET, "script.sh", "#!/bin/bash");

      // Act
      List<String> result = adapter.listRunbooks(BUCKET).get();

      // Assert
      assertThat(result).hasSize(2).containsExactlyInAnyOrder("readme.md", "runbooks/memory.md");
    }

    @Test
    @DisplayName("listRunbooks from non-existent bucket should return empty list")
    void listRunbooksFromNonExistentBucketShouldReturnEmptyList() throws Exception {
      List<String> result = adapter.listRunbooks("non-existent").get();

      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("getRunbookContent tests")
  class GetRunbookContentTests {

    @Test
    @DisplayName("getRunbookContent should return content for existing object")
    void getRunbookContentShouldReturnContent() throws Exception {
      String expectedContent = "# Troubleshooting Guide\n\nStep 1: Check CPU";
      adapter.seedBucket(BUCKET, "guide.md", expectedContent);

      Optional<String> result = adapter.getRunbookContent(BUCKET, "guide.md").get();

      assertThat(result).isPresent().hasValue(expectedContent);
    }

    @Test
    @DisplayName("getRunbookContent for non-existent object should return empty")
    void getRunbookContentForNonExistentObjectShouldReturnEmpty() throws Exception {
      Optional<String> result = adapter.getRunbookContent(BUCKET, "missing.md").get();

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getRunbookContent from non-existent bucket should return empty")
    void getRunbookContentFromNonExistentBucketShouldReturnEmpty() throws Exception {
      Optional<String> result = adapter.getRunbookContent("missing-bucket", "file.md").get();

      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("seedBucket tests")
  class SeedBucketTests {

    @Test
    @DisplayName("seedBucket should add content that can be retrieved")
    void seedBucketShouldAddContent() throws Exception {
      adapter.seedBucket(BUCKET, "test.md", "content");

      Optional<String> result = adapter.getRunbookContent(BUCKET, "test.md").get();

      assertThat(result).isPresent().hasValue("content");
    }

    @Test
    @DisplayName("seedBucket with map should add multiple objects")
    void seedBucketWithMapShouldAddMultipleObjects() throws Exception {
      adapter.seedBucket(
          BUCKET,
          Map.of(
              "file1.md", "content1",
              "file2.md", "content2"));

      assertThat(adapter.getObjectCount(BUCKET)).isEqualTo(2);
      assertThat(adapter.getRunbookContent(BUCKET, "file1.md").get()).hasValue("content1");
      assertThat(adapter.getRunbookContent(BUCKET, "file2.md").get()).hasValue("content2");
    }
  }

  @Nested
  @DisplayName("clearAll tests")
  class ClearAllTests {

    @Test
    @DisplayName("clearAll should remove all data")
    void clearAllShouldRemoveAllData() throws Exception {
      adapter.seedBucket("bucket1", "file.md", "content");
      adapter.seedBucket("bucket2", "other.md", "content");

      adapter.clearAll();

      assertThat(adapter.listRunbooks("bucket1").get()).isEmpty();
      assertThat(adapter.listRunbooks("bucket2").get()).isEmpty();
    }
  }

  @Nested
  @DisplayName("clearBucket tests")
  class ClearBucketTests {

    @Test
    @DisplayName("clearBucket should remove only specified bucket")
    void clearBucketShouldRemoveOnlySpecifiedBucket() throws Exception {
      adapter.seedBucket("bucket1", "file.md", "content1");
      adapter.seedBucket("bucket2", "file.md", "content2");

      adapter.clearBucket("bucket1");

      assertThat(adapter.listRunbooks("bucket1").get()).isEmpty();
      assertThat(adapter.listRunbooks("bucket2").get()).hasSize(1);
    }
  }

  @Nested
  @DisplayName("getObjectCount tests")
  class GetObjectCountTests {

    @Test
    @DisplayName("getObjectCount should return correct count")
    void getObjectCountShouldReturnCorrectCount() {
      adapter.seedBucket(BUCKET, "file1.md", "c1");
      adapter.seedBucket(BUCKET, "file2.md", "c2");
      adapter.seedBucket(BUCKET, "file3.yaml", "c3");

      assertThat(adapter.getObjectCount(BUCKET)).isEqualTo(3);
    }

    @Test
    @DisplayName("getObjectCount for non-existent bucket should return 0")
    void getObjectCountForNonExistentBucketShouldReturn0() {
      assertThat(adapter.getObjectCount("missing")).isZero();
    }
  }
}
