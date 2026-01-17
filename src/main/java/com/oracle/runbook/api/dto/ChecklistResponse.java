package com.oracle.runbook.api.dto;

import com.oracle.runbook.domain.ChecklistStep;
import com.oracle.runbook.domain.DynamicChecklist;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Response body for POST /api/v1/alerts endpoint.
 *
 * @param alertId the ID of the triggering alert
 * @param summary high-level summary of what this checklist addresses
 * @param steps ordered list of troubleshooting steps
 * @param sourceRunbooks paths to runbooks that contributed to this checklist
 * @param generatedAt when this checklist was generated
 * @param llmProviderUsed which LLM provider was used for generation
 */
public record ChecklistResponse(
    String alertId,
    String summary,
    List<ChecklistStepResponse> steps,
    List<String> sourceRunbooks,
    Instant generatedAt,
    String llmProviderUsed) {

  /**
   * Factory method to create a ChecklistResponse from a domain DynamicChecklist.
   *
   * @param checklist the domain checklist
   * @return the API response DTO
   */
  public static ChecklistResponse fromDomain(DynamicChecklist checklist) {
    Objects.requireNonNull(checklist, "checklist cannot be null");

    var stepResponses = checklist.steps().stream().map(ChecklistStepResponse::fromDomain).toList();

    return new ChecklistResponse(
        checklist.alertId(),
        checklist.summary(),
        stepResponses,
        checklist.sourceRunbooks(),
        checklist.generatedAt(),
        checklist.llmProviderUsed());
  }

  /**
   * Single step in a generated troubleshooting checklist.
   *
   * @param order the step number for ordering
   * @param instruction the actionable instruction text
   * @param rationale why this step matters for this specific alert
   * @param currentValue the current observed value
   * @param expectedValue the expected/healthy value
   * @param priority the urgency of this step
   * @param commands executable commands to run
   */
  public record ChecklistStepResponse(
      int order,
      String instruction,
      String rationale,
      String currentValue,
      String expectedValue,
      String priority,
      List<String> commands) {

    /**
     * Factory method to create a ChecklistStepResponse from a domain ChecklistStep.
     *
     * @param step the domain step
     * @return the API response DTO
     */
    public static ChecklistStepResponse fromDomain(ChecklistStep step) {
      return new ChecklistStepResponse(
          step.order(),
          step.instruction(),
          step.rationale(),
          step.currentValue(),
          step.expectedValue(),
          step.priority() != null ? step.priority().name() : null,
          step.commands());
    }
  }
}
