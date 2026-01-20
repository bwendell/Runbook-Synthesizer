package com.oracle.runbook.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.oracle.runbook.infrastructure.cloud.CloudStorageAdapter;
import com.oracle.runbook.infrastructure.cloud.VectorStoreRepository;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link RunbookIngestionService}.
 *
 * <p>Tests follow TDD red-green-refactor cycle. Written FIRST before implementation.
 */
@ExtendWith(MockitoExtension.class)
class RunbookIngestionServiceTest {

  @Mock private CloudStorageAdapter storageAdapter;
  @Mock private EmbeddingService embeddingService;
  @Mock private VectorStoreRepository vectorStore;

  private RunbookChunker chunker;
  private RunbookIngestionService service;

  @BeforeEach
  void setUp() {
    chunker = new RunbookChunker();
    service = new RunbookIngestionService(storageAdapter, chunker, embeddingService, vectorStore);
  }

  @Nested
  @DisplayName("ingest()")
  class IngestSingleRunbook {

    @Test
    @DisplayName("should fetch content from storage")
    void shouldFetchContentFromStorage() {
      String bucketName = "test-bucket";
      String runbookPath = "runbooks/memory.md";
      String content =
          """
          ---
          title: Memory Guide
          ---

          ## Section One

          Content here is long enough to create a chunk for testing purposes.
          """;

      when(storageAdapter.getRunbookContent(bucketName, runbookPath))
          .thenReturn(CompletableFuture.completedFuture(Optional.of(content)));
      when(embeddingService.embedBatch(anyList()))
          .thenReturn(CompletableFuture.completedFuture(List.of(new float[] {1.0f, 0.0f})));

      service.ingest(bucketName, runbookPath).join();

      verify(storageAdapter).getRunbookContent(bucketName, runbookPath);
    }

    @Test
    @DisplayName("should chunk fetched content")
    void shouldChunkFetchedContent() {
      // Content must have sections longer than minChunkSize (100 chars) to produce separate chunks
      String content =
          """
          ---
          title: Test Guide
          tags:
            - memory
          ---

          ## Section One

          This is the first section with enough content to meet minimum chunk size requirements.
          It includes detailed information about the topic that spans multiple lines to ensure
          we exceed the minimum chunk size threshold of 100 characters for proper chunking.

          ## Section Two

          This is the second section with enough content to meet minimum chunk size requirements.
          It also includes detailed information about a different topic that spans multiple lines
          to ensure we exceed the minimum chunk size threshold of 100 characters for chunking.
          """;

      when(storageAdapter.getRunbookContent(any(), any()))
          .thenReturn(CompletableFuture.completedFuture(Optional.of(content)));
      when(embeddingService.embedBatch(anyList()))
          .thenReturn(
              CompletableFuture.completedFuture(
                  List.of(new float[] {1.0f, 0.0f}, new float[] {0.0f, 1.0f})));

      int chunkCount = service.ingest("bucket", "path.md").join();

      // Chunker should produce at least 2 chunks
      assertThat(chunkCount).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("should generate embeddings for chunks")
    void shouldGenerateEmbeddingsForChunks() {
      String content =
          """
          ---
          title: Guide
          ---

          ## Section

          Content here is long enough to create a chunk for testing purposes.
          """;

      when(storageAdapter.getRunbookContent(any(), any()))
          .thenReturn(CompletableFuture.completedFuture(Optional.of(content)));
      when(embeddingService.embedBatch(anyList()))
          .thenReturn(CompletableFuture.completedFuture(List.of(new float[] {1.0f})));

      service.ingest("bucket", "path.md").join();

      verify(embeddingService).embedBatch(anyList());
    }

    @Test
    @DisplayName("should store chunks in vector store")
    void shouldStoreChunksInVectorStore() {
      String content =
          """
          ---
          title: Guide
          ---

          ## Section

          Content here is long enough to create a chunk for testing purposes.
          """;

      when(storageAdapter.getRunbookContent(any(), any()))
          .thenReturn(CompletableFuture.completedFuture(Optional.of(content)));
      when(embeddingService.embedBatch(anyList()))
          .thenReturn(CompletableFuture.completedFuture(List.of(new float[] {1.0f})));

      service.ingest("bucket", "path.md").join();

      verify(vectorStore).storeBatch(anyList());
    }

    @Test
    @DisplayName("should return chunk count on successful ingestion")
    void shouldReturnChunkCount() {
      String content =
          """
          ## Section

          Content here is long enough to create a chunk for testing purposes.
          """;

      when(storageAdapter.getRunbookContent(any(), any()))
          .thenReturn(CompletableFuture.completedFuture(Optional.of(content)));
      when(embeddingService.embedBatch(anyList()))
          .thenReturn(CompletableFuture.completedFuture(List.of(new float[] {1.0f})));

      int chunkCount = service.ingest("bucket", "path.md").join();

      assertThat(chunkCount).isEqualTo(1);
    }

    @Test
    @DisplayName("should delete existing chunks before re-ingesting")
    void shouldDeleteExistingChunksBeforeReingesting() {
      String runbookPath = "runbooks/memory.md";
      String content =
          """
          ## Section

          Content here is long enough to create a chunk for testing purposes.
          """;

      when(storageAdapter.getRunbookContent(any(), eq(runbookPath)))
          .thenReturn(CompletableFuture.completedFuture(Optional.of(content)));
      when(embeddingService.embedBatch(anyList()))
          .thenReturn(CompletableFuture.completedFuture(List.of(new float[] {1.0f})));

      service.ingest("bucket", runbookPath).join();

      verify(vectorStore).delete(runbookPath);
    }

    @Test
    @DisplayName("should return 0 when storage returns empty")
    void shouldHandleStorageNotFound() {
      when(storageAdapter.getRunbookContent(any(), any()))
          .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

      int chunkCount = service.ingest("bucket", "nonexistent.md").join();

      assertThat(chunkCount).isEqualTo(0);
      verify(vectorStore, never()).storeBatch(anyList());
    }
  }

  @Nested
  @DisplayName("ingestAll()")
  class IngestAllRunbooks {

    @Test
    @DisplayName("should ingest all markdown files in bucket")
    void shouldIngestAllMarkdownFiles() {
      when(storageAdapter.listRunbooks("test-bucket"))
          .thenReturn(
              CompletableFuture.completedFuture(List.of("runbooks/memory.md", "runbooks/cpu.md")));

      String memoryContent =
          """
          ## Memory

          Content here is long enough to create a chunk for testing purposes.
          """;
      String cpuContent =
          """
          ## CPU

          Content here is long enough to create a chunk for testing purposes.
          """;

      when(storageAdapter.getRunbookContent("test-bucket", "runbooks/memory.md"))
          .thenReturn(CompletableFuture.completedFuture(Optional.of(memoryContent)));
      when(storageAdapter.getRunbookContent("test-bucket", "runbooks/cpu.md"))
          .thenReturn(CompletableFuture.completedFuture(Optional.of(cpuContent)));
      when(embeddingService.embedBatch(anyList()))
          .thenReturn(CompletableFuture.completedFuture(List.of(new float[] {1.0f})));

      int totalChunks = service.ingestAll("test-bucket").join();

      assertThat(totalChunks).isEqualTo(2);
    }

    @Test
    @DisplayName("should return 0 when bucket is empty")
    void shouldReturnZeroForEmptyBucket() {
      when(storageAdapter.listRunbooks("empty-bucket"))
          .thenReturn(CompletableFuture.completedFuture(List.of()));

      int totalChunks = service.ingestAll("empty-bucket").join();

      assertThat(totalChunks).isEqualTo(0);
    }
  }

  @Nested
  @DisplayName("Constructor validation")
  class ConstructorValidation {

    @Test
    @DisplayName("should throw NullPointerException for null storage adapter")
    void shouldThrowForNullStorageAdapter() {
      assertThatThrownBy(
              () -> new RunbookIngestionService(null, chunker, embeddingService, vectorStore))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("storageAdapter");
    }

    @Test
    @DisplayName("should throw NullPointerException for null chunker")
    void shouldThrowForNullChunker() {
      assertThatThrownBy(
              () ->
                  new RunbookIngestionService(storageAdapter, null, embeddingService, vectorStore))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("chunker");
    }

    @Test
    @DisplayName("should throw NullPointerException for null embedding service")
    void shouldThrowForNullEmbeddingService() {
      assertThatThrownBy(
              () -> new RunbookIngestionService(storageAdapter, chunker, null, vectorStore))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("embeddingService");
    }

    @Test
    @DisplayName("should throw NullPointerException for null vector store")
    void shouldThrowForNullVectorStore() {
      assertThatThrownBy(
              () -> new RunbookIngestionService(storageAdapter, chunker, embeddingService, null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("vectorStore");
    }
  }
}
