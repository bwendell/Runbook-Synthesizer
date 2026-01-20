package com.oracle.runbook.rag;

import com.oracle.runbook.domain.RunbookChunk;
import com.oracle.runbook.infrastructure.cloud.CloudStorageAdapter;
import com.oracle.runbook.infrastructure.cloud.VectorStoreRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for ingesting runbooks from cloud storage into the vector store.
 *
 * <p>Orchestrates the ingestion pipeline: fetch → chunk → embed → store.
 *
 * @see RunbookChunker
 * @see EmbeddingService
 * @see VectorStoreRepository
 */
public class RunbookIngestionService {

  private final CloudStorageAdapter storageAdapter;
  private final RunbookChunker chunker;
  private final EmbeddingService embeddingService;
  private final VectorStoreRepository vectorStore;

  /**
   * Creates a new RunbookIngestionService.
   *
   * @param storageAdapter adapter for fetching runbook content
   * @param chunker parses runbooks into semantic chunks
   * @param embeddingService generates embeddings for chunks
   * @param vectorStore stores chunks with embeddings
   * @throws NullPointerException if any argument is null
   */
  public RunbookIngestionService(
      CloudStorageAdapter storageAdapter,
      RunbookChunker chunker,
      EmbeddingService embeddingService,
      VectorStoreRepository vectorStore) {
    this.storageAdapter = Objects.requireNonNull(storageAdapter, "storageAdapter cannot be null");
    this.chunker = Objects.requireNonNull(chunker, "chunker cannot be null");
    this.embeddingService =
        Objects.requireNonNull(embeddingService, "embeddingService cannot be null");
    this.vectorStore = Objects.requireNonNull(vectorStore, "vectorStore cannot be null");
  }

  /**
   * Ingests a single runbook from storage and stores its chunks in the vector store.
   *
   * @param containerName the S3 bucket or OCI container name
   * @param runbookPath the path to the runbook file
   * @return a CompletableFuture containing the number of chunks stored
   */
  public CompletableFuture<Integer> ingest(String containerName, String runbookPath) {
    // Delete existing chunks for this runbook (support re-indexing)
    vectorStore.delete(runbookPath);

    return storageAdapter
        .getRunbookContent(containerName, runbookPath)
        .thenCompose(
            optionalContent -> {
              if (optionalContent.isEmpty()) {
                return CompletableFuture.completedFuture(0);
              }

              String content = optionalContent.get();

              // Parse content into chunks
              List<RunbookChunker.ParsedChunk> parsedChunks = chunker.chunk(content, runbookPath);

              if (parsedChunks.isEmpty()) {
                return CompletableFuture.completedFuture(0);
              }

              // Generate embeddings for all chunk contents
              List<String> chunkTexts =
                  parsedChunks.stream().map(RunbookChunker.ParsedChunk::content).toList();

              return embeddingService
                  .embedBatch(chunkTexts)
                  .thenApply(
                      embeddings -> {
                        // Create RunbookChunk domain objects with embeddings
                        List<RunbookChunk> chunks = new ArrayList<>();
                        for (int i = 0; i < parsedChunks.size(); i++) {
                          RunbookChunker.ParsedChunk parsed = parsedChunks.get(i);
                          float[] embedding = embeddings.get(i);

                          RunbookChunk chunk =
                              new RunbookChunk(
                                  UUID.randomUUID().toString(),
                                  runbookPath,
                                  parsed.sectionTitle(),
                                  parsed.content(),
                                  parsed.tags(),
                                  parsed.applicableShapes(),
                                  embedding);
                          chunks.add(chunk);
                        }

                        // Store chunks in vector store
                        vectorStore.storeBatch(chunks);
                        return chunks.size();
                      });
            });
  }

  /**
   * Ingests all runbooks from a storage container.
   *
   * @param containerName the S3 bucket or OCI container name
   * @return a CompletableFuture containing the total number of chunks stored
   */
  public CompletableFuture<Integer> ingestAll(String containerName) {
    return storageAdapter
        .listRunbooks(containerName)
        .thenCompose(
            paths -> {
              if (paths.isEmpty()) {
                return CompletableFuture.completedFuture(0);
              }

              // Ingest each runbook and sum up the chunk counts
              List<CompletableFuture<Integer>> futures =
                  paths.stream().map(path -> ingest(containerName, path)).toList();

              return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                  .thenApply(v -> futures.stream().mapToInt(CompletableFuture::join).sum());
            });
  }
}
