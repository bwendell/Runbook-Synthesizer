package com.oracle.runbook.rag;

import com.oracle.runbook.domain.Alert;
import com.oracle.runbook.domain.DynamicChecklist;
import com.oracle.runbook.domain.RetrievedChunk;
import com.oracle.runbook.enrichment.ContextEnrichmentService;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Domain service that orchestrates the Retrieval Augmented Generation (RAG) pipeline to produce
 * context-aware troubleshooting checklists.
 *
 * <p>The pipeline follows these steps:
 *
 * <ol>
 *   <li>Enrich the alert with real-time infrastructure context
 *   <li>Retrieve relevant runbook chunks based on the enriched context
 *   <li>Generate a dynamic checklist using the LLM and retrieved chunks
 * </ol>
 */
public class RagPipelineService {

  private final ContextEnrichmentService enrichmentService;
  private final RunbookRetriever retriever;
  private final ChecklistGenerator generator;

  /**
   * Creates a new RagPipelineService with required dependencies.
   *
   * @param enrichmentService the service used to gather infrastructure context
   * @param retriever the service used to fetch relevant runbook snippets
   * @param generator the service used to synthesize the final checklist
   */
  public RagPipelineService(
      ContextEnrichmentService enrichmentService,
      RunbookRetriever retriever,
      ChecklistGenerator generator) {
    this.enrichmentService =
        Objects.requireNonNull(enrichmentService, "enrichmentService cannot be null");
    this.retriever = Objects.requireNonNull(retriever, "retriever cannot be null");
    this.generator = Objects.requireNonNull(generator, "generator cannot be null");
  }

  /**
   * Processes an incoming alert through the RAG pipeline to generate a troubleshooting guide.
   *
   * @param alert the alert to process
   * @param topK the number of runbook chunks to retrieve for context
   * @return a CompletableFuture containing the generated DynamicChecklist
   */
  public CompletableFuture<DynamicChecklist> processAlert(Alert alert, int topK) {
    Objects.requireNonNull(alert, "alert cannot be null");

    // Step 1: Enrich context (Async)
    return enrichmentService
        .enrich(alert)
        .thenApply(
            context -> {
              // Step 2: Retrieve relevant chunks (Sync based on retriever interface)
              List<RetrievedChunk> relevantChunks = retriever.retrieve(context, topK);

              // Step 3: Generate checklist (Sync based on generator interface)
              return generator.generate(context, relevantChunks);
            });
  }
}
