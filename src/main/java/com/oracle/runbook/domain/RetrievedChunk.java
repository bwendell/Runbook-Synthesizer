package com.oracle.runbook.domain;

import java.util.Objects;

/**
 * Retrieval result wrapping a RunbookChunk with similarity scoring.
 *
 * @param chunk the retrieved runbook chunk
 * @param similarityScore cosine similarity score from vector search
 * @param metadataBoost additional score boost from metadata matching
 * @param finalScore combined score for ranking
 */
public record RetrievedChunk(
    RunbookChunk chunk,
    double similarityScore,
    double metadataBoost,
    double finalScore
) {
    /**
     * Compact constructor with validation.
     */
    public RetrievedChunk {
        Objects.requireNonNull(chunk, "RetrievedChunk chunk cannot be null");
    }
}
