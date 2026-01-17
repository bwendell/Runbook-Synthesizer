package com.oracle.runbook.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Semantically chunked runbook section with embeddings for vector search.
 *
 * @param id unique identifier for this chunk
 * @param runbookPath path to the source runbook file
 * @param sectionTitle the title of this section
 * @param content the actual text content
 * @param tags semantic tags for filtering (e.g., memory, oom, linux)
 * @param applicableShapes compute shapes this applies to (e.g., VM.*, GPU.*)
 * @param embedding vector embedding for similarity search
 */
public record RunbookChunk(
    String id,
    String runbookPath,
    String sectionTitle,
    String content,
    List<String> tags,
    List<String> applicableShapes,
    float[] embedding) {
  /** Compact constructor with validation and defensive copies. */
  public RunbookChunk {
    Objects.requireNonNull(id, "RunbookChunk id cannot be null");
    Objects.requireNonNull(content, "RunbookChunk content cannot be null");

    // Defensive copies for immutability
    tags = tags != null ? List.copyOf(tags) : List.of();
    applicableShapes = applicableShapes != null ? List.copyOf(applicableShapes) : List.of();
    embedding = embedding != null ? Arrays.copyOf(embedding, embedding.length) : new float[0];
  }

  /** Returns a defensive copy of the embedding array. */
  @Override
  public float[] embedding() {
    return Arrays.copyOf(embedding, embedding.length);
  }
}
