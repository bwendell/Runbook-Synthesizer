package com.oracle.runbook.rag;

/**
 * Centrally managed prompt templates for RAG-based checklist synthesis.
 *
 * <p>These templates define the structure of the LLM prompt, including how to present the alert
 * context and retrieved runbook chunks to the model.
 */
public final class PromptTemplates {

  private PromptTemplates() {
    // Private constructor to prevent instantiation
  }

  /** Main system prompt for the checklist generator. */
  public static final String SYSTEM_PROMPT =
      """
      You are an expert Reliability Engineer for Amazon Web Services (AWS).
      Your task is to generate a dynamic, context-aware troubleshooting checklist for
      an active alert.

      You will be provided with:
      1. ALERT CONTEXT: Details about the triggered alarm and the affected resource.
      2. RELEVANT RUNBOOK SECTIONS: Snippets from technical documentation that may apply.

      INSTRUCTIONS:
      - Synthesize the information into a concise, step-by-step checklist.
      - Prioritize safety (e.g., check backups before destructive actions).
      - If the runbook references specific AWS CLI commands, include them in the 'commands' array.
      - Provide a specific 'rationale' for each step, explaining WHY it is necessary based on the alert context.
      - RETURN ONLY VALID JSON. Do not include introductory text, markdown formatting, or code fences.
      - The output must be a valid JSON object matching the structure:

      {
        "summary": "A brief summary of the checklist",
        "steps": [
          {
            "order": 1,
            "instruction": "Step instruction here",
            "rationale": "Explanation of why this step is needed",
            "priority": "HIGH|MEDIUM|LOW",
            "commands": ["aws command 1", "aws command 2"]
          }
        ]
      }
      """;

  /** Template for formatting the enriched alert context. */
  public static final String CONTEXT_TEMPLATE =
      """
      ### ALERT CONTEXT
      Title: %s
      Severity: %s
      Message: %s
      Resource: %s (Shape: %s)
      """;

  /** Template for formatting a single runbook chunk. */
  public static final String CHUNK_TEMPLATE =
      """
      ---
      Runbook: %s (Section: %s)
      Content:
      %s
      """;

  /** Final instruction to start the checklist generation. */
  public static final String GENERATE_INSTRUCTION =
      "\nBased on the context and runbook chunks above, generate the troubleshooting checklist:";
}
