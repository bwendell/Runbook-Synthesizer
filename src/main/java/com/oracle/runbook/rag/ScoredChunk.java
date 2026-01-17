package com.oracle.runbook.rag;

import com.oracle.runbook.domain.RunbookChunk;
import java.util.Objects;

/**
 * Internal result record for vector search results with similarity scores.
 * Used by {@link VectorStoreRepository} to return results before re-ranking.
 *
 * @param chunk the retrieved runbook chunk
 * @param similarityScore the raw similarity score from the vector database
 */
public record ScoredChunk(RunbookChunk chunk, double similarityScore) {
    public ScoredChunk {
        Objects.requireNonNull(chunk, "chunk cannot be null");
    }
}
