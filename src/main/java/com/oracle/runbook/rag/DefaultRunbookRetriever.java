package com.oracle.runbook.rag;

import com.oracle.runbook.domain.EnrichedContext;
import com.oracle.runbook.domain.RetrievedChunk;
import com.oracle.runbook.domain.RunbookChunk;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link RunbookRetriever} that combines vector similarity search with
 * metadata-based re-ranking.
 *
 * <p>This implementation boosts chunks based on tag overlap and shape matching to improve relevance
 * for specific resource contexts.
 *
 * @see RunbookRetriever
 * @see EmbeddingService
 * @see VectorStoreRepository
 */
public class DefaultRunbookRetriever implements RunbookRetriever {

  private static final double TAG_BOOST_WEIGHT = 0.1;
  private static final double MAX_TAG_BOOST = 0.3;
  private static final double SHAPE_BOOST_WEIGHT = 0.2;

  private final EmbeddingService embeddingService;
  private final VectorStoreRepository vectorStore;

  /**
   * Creates a new DefaultRunbookRetriever with the given services.
   *
   * @param embeddingService the service to generate embeddings for query context
   * @param vectorStore the repository to search for similar chunks
   */
  public DefaultRunbookRetriever(
      EmbeddingService embeddingService, VectorStoreRepository vectorStore) {
    this.embeddingService =
        Objects.requireNonNull(embeddingService, "embeddingService cannot be null");
    this.vectorStore = Objects.requireNonNull(vectorStore, "vectorStore cannot be null");
  }

  /** {@inheritDoc} */
  @Override
  public List<RetrievedChunk> retrieve(EnrichedContext context, int topK) {
    Objects.requireNonNull(context, "context cannot be null");
    if (topK <= 0) {
      throw new IllegalArgumentException("topK must be positive");
    }

    // 1. Embed the enriched context (alert + resource metadata)
    float[] queryEmbedding = embeddingService.embedContext(context).join();

    // 2. Fetch candidates (over-fetch by 2x for re-ranking)
    List<ScoredChunk> candidates = vectorStore.search(queryEmbedding, topK * 2);

    // 3. Apply metadata boosting and re-rank
    return candidates.stream()
        .map(scoredChunk -> calculateRetrievedChunk(scoredChunk, context))
        .sorted(Comparator.comparingDouble(RetrievedChunk::finalScore).reversed())
        .limit(topK)
        .collect(Collectors.toList());
  }

  private RetrievedChunk calculateRetrievedChunk(ScoredChunk scoredChunk, EnrichedContext context) {
    RunbookChunk chunk = scoredChunk.chunk();
    double similarityScore = scoredChunk.similarityScore();

    double tagBoost = calculateTagBoost(chunk, context);
    double shapeBoost = calculateShapeBoost(chunk, context);
    double metadataBoost = tagBoost + shapeBoost;

    double finalScore = similarityScore + metadataBoost;

    return new RetrievedChunk(chunk, similarityScore, metadataBoost, finalScore);
  }

  private double calculateTagBoost(RunbookChunk chunk, EnrichedContext context) {
    if (chunk.tags() == null || chunk.tags().isEmpty()) {
      return 0.0;
    }

    // Match tags against alert dimensions and labels
    long matchCount =
        chunk.tags().stream()
            .filter(
                tag ->
                    context.alert().dimensions().containsKey(tag)
                        || context.alert().labels().containsKey(tag)
                        || context.alert().title().toLowerCase().contains(tag.toLowerCase()))
            .count();

    return Math.min(matchCount * TAG_BOOST_WEIGHT, MAX_TAG_BOOST);
  }

  private double calculateShapeBoost(RunbookChunk chunk, EnrichedContext context) {
    if (chunk.applicableShapes() == null
        || chunk.applicableShapes().isEmpty()
        || context.resource() == null
        || context.resource().shape() == null) {
      return 0.0;
    }

    String resourceShape = context.resource().shape();
    boolean matches =
        chunk.applicableShapes().stream().anyMatch(pattern -> matchesShape(pattern, resourceShape));

    return matches ? SHAPE_BOOST_WEIGHT : 0.0;
  }

  private boolean matchesShape(String pattern, String shape) {
    if (pattern.equals("*") || pattern.equalsIgnoreCase("all")) {
      return true;
    }
    // Convert simple glob pattern to regex
    String regex = pattern.replace(".", "\\.").replace("*", ".*");
    try {
      return Pattern.compile("^" + regex + "$", Pattern.CASE_INSENSITIVE).matcher(shape).matches();
    } catch (Exception e) {
      return false;
    }
  }
}
