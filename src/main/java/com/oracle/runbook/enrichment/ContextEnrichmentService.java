package com.oracle.runbook.enrichment;

import com.oracle.runbook.domain.Alert;
import com.oracle.runbook.domain.EnrichedContext;

import java.util.concurrent.CompletableFuture;

/**
 * Port interface for context enrichment orchestration.
 * <p>
 * This interface defines the contract for the context enrichment service
 * in the Hexagonal Architecture. Implementations orchestrate calls to
 * {@link MetricsSourceAdapter} and {@link LogSourceAdapter} to gather
 * real-time infrastructure state for a given alert.
 * <p>
 * All operations are async to parallelize external API calls and
 * support Helidon SE's reactive patterns.
 *
 * @see EnrichedContext
 * @see MetricsSourceAdapter
 * @see LogSourceAdapter
 */
public interface ContextEnrichmentService {

    /**
     * Enriches an alert with real-time infrastructure context.
     * <p>
     * The implementation should:
     * <ol>
     *   <li>Resolve resource metadata from the alert dimensions</li>
     *   <li>Fetch recent metrics from configured sources</li>
     *   <li>Fetch recent logs from configured sources</li>
     *   <li>Gather custom properties (GPU status, DB health, etc.)</li>
     * </ol>
     * <p>
     * These operations should be parallelized for efficiency.
     *
     * @param alert the alert to enrich with context
     * @return a CompletableFuture containing the enriched context, never null
     */
    CompletableFuture<EnrichedContext> enrich(Alert alert);
}
