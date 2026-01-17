package com.oracle.runbook.rag;

import com.oracle.runbook.domain.RunbookChunk;

import java.util.List;

/**
 * Port interface for vector store operations on runbook chunks.
 * <p>
 * This interface defines the contract for vector storage in the
 * Hexagonal Architecture. Implementations provide concrete integrations
 * with vector databases like Oracle Database 23ai with AI Vector Search.
 * <p>
 * Note: Methods are synchronous for simplicity. Implementations may use
 * internal async operations but present a blocking API.
 *
 * @see RunbookChunk
 */
public interface VectorStoreRepository {

    /**
     * Stores a single runbook chunk with its embedding.
     *
     * @param chunk the runbook chunk to store
     */
    void store(RunbookChunk chunk);

    /**
     * Stores multiple runbook chunks in a single batch operation.
     * <p>
     * More efficient than calling {@link #store(RunbookChunk)} repeatedly
     * during document ingestion.
     *
     * @param chunks the list of runbook chunks to store
     */
    void storeBatch(List<RunbookChunk> chunks);

    /**
     * Searches for the top-K most similar runbook chunks.
     * <p>
     * The implementation should use vector similarity search
     * (e.g., Oracle 23ai AI_VECTOR_SEARCH functions) to find
     * chunks with embeddings closest to the query.
     *
     * @param queryEmbedding the query vector to search with
     * @param topK           the maximum number of results to return
     * @return ordered list of matching chunks (most similar first), never null
     */
    List<RunbookChunk> search(float[] queryEmbedding, int topK);

    /**
     * Deletes all chunks associated with a runbook path.
     * <p>
     * Enables re-indexing when a runbook is updated - delete old chunks
     * before storing new ones.
     *
     * @param runbookPath the path of the runbook whose chunks should be deleted
     */
    void delete(String runbookPath);
}
