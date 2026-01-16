package com.oracle.runbook.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Complete generated troubleshooting guide for an alert.
 *
 * @param alertId the ID of the triggering alert
 * @param summary high-level summary of what this checklist addresses
 * @param steps ordered list of troubleshooting steps
 * @param sourceRunbooks paths to runbooks that contributed to this checklist
 * @param generatedAt when this checklist was generated
 * @param llmProviderUsed which LLM provider was used for generation
 */
public record DynamicChecklist(
    String alertId,
    String summary,
    List<ChecklistStep> steps,
    List<String> sourceRunbooks,
    Instant generatedAt,
    String llmProviderUsed
) {
    /**
     * Compact constructor with validation and defensive copies.
     */
    public DynamicChecklist {
        Objects.requireNonNull(alertId, "DynamicChecklist alertId cannot be null");
        Objects.requireNonNull(generatedAt, "DynamicChecklist generatedAt cannot be null");
        
        // Defensive copies for immutability
        steps = steps != null ? List.copyOf(steps) : List.of();
        sourceRunbooks = sourceRunbooks != null ? List.copyOf(sourceRunbooks) : List.of();
    }
}
