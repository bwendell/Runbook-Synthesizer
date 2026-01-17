package com.oracle.runbook.rag;

import com.oracle.runbook.domain.EnrichedContext;
import com.oracle.runbook.domain.RetrievedChunk;

import java.util.List;

/**
 * Port interface for retrieving relevant runbook chunks based on enriched context.
 * <p>
 * This interface defines the contract for the RAG retrieval component.
 * Implementations combine vector similarity search with metadata filtering
 * to find the most relevant runbook sections for a given alert context.
 *
 * @see RetrievedChunk
 * @see EnrichedContext
 */
public interface RunbookRetriever {

    /**
     * Retrieves the top-K most relevant runbook chunks for the given context.
     * <p>
     * The implementation should:
     * <ol>
     *   <li>Build a query from the EnrichedContext (alert + metrics + logs)</li>
     *   <li>Perform vector similarity search against the runbook store</li>
     *   <li>Apply metadata filtering/boosting (tags, applicable shapes)</li>
     *   <li>Return ranked results with similarity scores</li>
     * </ol>
     *
     * @param context the enriched alert context to search with
     * @param topK    the maximum number of chunks to retrieve
     * @return ordered list of retrieved chunks with scores, never null
     */
    List<RetrievedChunk> retrieve(EnrichedContext context, int topK);
}
