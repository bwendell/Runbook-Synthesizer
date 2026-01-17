package com.oracle.runbook.rag;

import com.oracle.runbook.domain.DynamicChecklist;
import com.oracle.runbook.domain.EnrichedContext;
import com.oracle.runbook.domain.RetrievedChunk;
import java.util.List;

/**
 * Port interface for generating dynamic troubleshooting checklists.
 *
 * <p>This interface defines the contract for the checklist generation component in the RAG
 * pipeline. Implementations use an {@link LlmProvider} to synthesize context-aware checklists from
 * retrieved runbook chunks.
 *
 * @see DynamicChecklist
 * @see EnrichedContext
 * @see RetrievedChunk
 */
public interface ChecklistGenerator {

  /**
   * Generates a dynamic troubleshooting checklist from the enriched context and relevant runbook
   * chunks.
   *
   * <p>The implementation should:
   *
   * <ol>
   *   <li>Build a prompt combining context and retrieved chunks
   *   <li>Call the LLM provider for text generation
   *   <li>Parse the generated response into structured steps
   *   <li>Include current system values where available
   * </ol>
   *
   * @param context the enriched alert context
   * @param relevantChunks the top-K relevant runbook chunks from retrieval
   * @return a fully populated DynamicChecklist, never null
   */
  DynamicChecklist generate(EnrichedContext context, List<RetrievedChunk> relevantChunks);
}
