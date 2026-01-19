package com.oracle.runbook.infrastructure.cloud.local;

import com.oracle.runbook.domain.RunbookChunk;
import com.oracle.runbook.infrastructure.cloud.VectorStoreRepository;
import com.oracle.runbook.rag.ScoredChunk;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link VectorStoreRepository} for local development and testing.
 *
 * <p>Uses a {@link ConcurrentHashMap} for thread-safe storage and implements cosine similarity
 * search for vector matching. This is suitable for MVP/E2E testing where persistence is not
 * required.
 *
 * @see VectorStoreRepository
 */
public class InMemoryVectorStoreRepository implements VectorStoreRepository {

  private final Map<String, RunbookChunk> chunks = new ConcurrentHashMap<>();

  @Override
  public String providerType() {
    return "local";
  }

  @Override
  public void store(RunbookChunk chunk) {
    Objects.requireNonNull(chunk, "chunk cannot be null");
    chunks.put(chunk.id(), chunk);
  }

  @Override
  public void storeBatch(List<RunbookChunk> chunks) {
    Objects.requireNonNull(chunks, "chunks cannot be null");
    chunks.forEach(this::store);
  }

  @Override
  public List<ScoredChunk> search(float[] queryEmbedding, int topK) {
    Objects.requireNonNull(queryEmbedding, "queryEmbedding cannot be null");
    if (topK <= 0) {
      throw new IllegalArgumentException("topK must be positive");
    }

    if (chunks.isEmpty()) {
      return List.of();
    }

    List<ScoredChunk> scoredChunks = new ArrayList<>();
    for (RunbookChunk chunk : chunks.values()) {
      double similarity = cosineSimilarity(queryEmbedding, chunk.embedding());
      scoredChunks.add(new ScoredChunk(chunk, similarity));
    }

    // Sort by similarity descending and limit to topK
    scoredChunks.sort(Comparator.comparingDouble(ScoredChunk::similarityScore).reversed());
    return scoredChunks.subList(0, Math.min(topK, scoredChunks.size()));
  }

  @Override
  public void delete(String runbookPath) {
    Objects.requireNonNull(runbookPath, "runbookPath cannot be null");
    chunks.entrySet().removeIf(entry -> runbookPath.equals(entry.getValue().runbookPath()));
  }

  /**
   * Computes cosine similarity between two vectors.
   *
   * @param a first vector
   * @param b second vector
   * @return cosine similarity score between -1 and 1
   */
  private double cosineSimilarity(float[] a, float[] b) {
    if (a.length != b.length) {
      throw new IllegalArgumentException(
          "Vectors must have same length: " + a.length + " vs " + b.length);
    }

    double dotProduct = 0.0;
    double normA = 0.0;
    double normB = 0.0;

    for (int i = 0; i < a.length; i++) {
      dotProduct += a[i] * b[i];
      normA += a[i] * a[i];
      normB += b[i] * b[i];
    }

    double denominator = Math.sqrt(normA) * Math.sqrt(normB);
    if (denominator == 0.0) {
      return 0.0;
    }
    return dotProduct / denominator;
  }
}
