package com.oracle.runbook.infrastructure.cloud;

import com.oracle.runbook.domain.RunbookChunk;
import com.oracle.runbook.rag.ScoredChunk;
import java.util.List;

/**
 * Port interface for vector store operations on runbook chunks.
 *
 * <p>This interface defines the contract for vector storage in the Hexagonal Architecture.
 * Implementations provide concrete integrations with vector databases like Oracle Database 23ai
 * with AI Vector Search, AWS OpenSearch, or in-memory stores for testing.
 *
 * <p>Note: Methods are synchronous for simplicity. Implementations may use internal async
 * operations but present a blocking API.
 *
 * @see RunbookChunk
 */
public interface VectorStoreRepository {

  /**
   * Returns the provider type identifier for this vector store implementation.
   *
   * @return the provider type (e.g., "local", "oci", "aws")
   */
  String providerType();

  /**
   * Stores a single runbook chunk with its embedding.
   *
   * @param chunk the runbook chunk to store
   */
  void store(RunbookChunk chunk);

  /**
   * Stores multiple runbook chunks in a single batch operation.
   *
   * <p>More efficient than calling {@link #store(RunbookChunk)} repeatedly during document
   * ingestion.
   *
   * @param chunks the list of runbook chunks to store
   */
  void storeBatch(List<RunbookChunk> chunks);

  /**
   * Searches for the top-K most similar runbook chunks.
   *
   * <p>The implementation should use vector similarity search (e.g., cosine similarity) to find
   * chunks with embeddings closest to the query.
   *
   * @param queryEmbedding the query vector to search with
   * @param topK the maximum number of results to return
   * @return ordered list of matching chunks with similarity scores (most similar first), never null
   */
  List<ScoredChunk> search(float[] queryEmbedding, int topK);

  /**
   * Deletes all chunks associated with a runbook path.
   *
   * <p>Enables re-indexing when a runbook is updated - delete old chunks before storing new ones.
   *
   * @param runbookPath the path of the runbook whose chunks should be deleted
   */
  void delete(String runbookPath);
}
