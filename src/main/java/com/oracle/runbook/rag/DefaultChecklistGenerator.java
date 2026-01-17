package com.oracle.runbook.rag;

import com.oracle.runbook.domain.ChecklistStep;
import com.oracle.runbook.domain.DynamicChecklist;
import com.oracle.runbook.domain.EnrichedContext;
import com.oracle.runbook.domain.GenerationConfig;
import com.oracle.runbook.domain.RetrievedChunk;
import com.oracle.runbook.domain.StepPriority;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Default implementation of {@link ChecklistGenerator} using an LLM.
 *
 * <p>This class builds a rich prompt from alert context and retrieved runbook chunks, sends it to
 * the LLM, and parses the Markdown response into a structured {@link DynamicChecklist}.
 */
public class DefaultChecklistGenerator implements ChecklistGenerator {

  private final LlmProvider llmProvider;

  public DefaultChecklistGenerator(LlmProvider llmProvider) {
    this.llmProvider = Objects.requireNonNull(llmProvider, "llmProvider cannot be null");
  }

  /** {@inheritDoc} */
  @Override
  public DynamicChecklist generate(EnrichedContext context, List<RetrievedChunk> relevantChunks) {
    Objects.requireNonNull(context, "context cannot be null");
    Objects.requireNonNull(relevantChunks, "relevantChunks cannot be null");

    // 1. Construct prompt
    String prompt = buildPrompt(context, relevantChunks);

    // 2. Call LLM (synchronous for simplicity in this port)
    GenerationConfig config = new GenerationConfig(0.7, 1000, java.util.Optional.empty());
    String response = llmProvider.generateText(prompt, config).join();

    // 3. Parse Markdown checklist
    List<ChecklistStep> steps = parseMarkdownChecklist(response);
    String summary = response.split("\n")[0]; // Simple first line as summary
    if (summary.length() > 200) {
      summary = summary.substring(0, 197) + "...";
    }

    List<String> sourceRunbooks =
        relevantChunks.stream().map(rc -> rc.chunk().runbookPath()).distinct().toList();

    return new DynamicChecklist(
        context.alert().id(),
        summary,
        steps,
        sourceRunbooks,
        Instant.now(),
        llmProvider.providerId());
  }

  private String buildPrompt(EnrichedContext context, List<RetrievedChunk> relevantChunks) {
    StringBuilder sb = new StringBuilder();
    sb.append(PromptTemplates.SYSTEM_PROMPT).append("\n\n");

    // Alert Context
    String resourceName = context.resource() != null ? context.resource().displayName() : "N/A";
    String resourceShape = context.resource() != null ? context.resource().shape() : "N/A";
    sb.append(
        String.format(
            PromptTemplates.CONTEXT_TEMPLATE,
            context.alert().title(),
            context.alert().severity(),
            context.alert().message(),
            resourceName,
            resourceShape));

    // Runbook Chunks
    sb.append("\n### RELEVANT RUNBOOK SECTIONS\n");
    if (relevantChunks.isEmpty()) {
      sb.append("No specific runbook sections were found for this alert.\n");
    } else {
      for (RetrievedChunk rc : relevantChunks) {
        sb.append(
            String.format(
                PromptTemplates.CHUNK_TEMPLATE,
                rc.chunk().runbookPath(),
                rc.chunk().sectionTitle(),
                rc.chunk().content()));
      }
    }

    sb.append(PromptTemplates.GENERATE_INSTRUCTION);
    return sb.toString();
  }

  /** Parses Markdown lists or numbered steps into ChecklistStep objects. */
  private List<ChecklistStep> parseMarkdownChecklist(String markdown) {
    List<ChecklistStep> steps = new ArrayList<>();
    if (markdown == null || markdown.isBlank()) {
      return steps;
    }

    // Look for lines starting with "Step X:", "-", "*", or "1."
    Pattern stepPattern =
        Pattern.compile("^(?:Step\\s+\\d+:|[-*]|\\d+\\.)\\s*(.*)$", Pattern.MULTILINE);
    Matcher matcher = stepPattern.matcher(markdown);

    int order = 1;
    while (matcher.find()) {
      String instruction = matcher.group(1).trim();
      if (!instruction.isEmpty()) {
        // Simple heuristic for priority
        StepPriority priority =
            instruction.toLowerCase().contains("urgent")
                    || instruction.toLowerCase().contains("critical")
                ? StepPriority.HIGH
                : StepPriority.MEDIUM;

        steps.add(
            new ChecklistStep(
                order++,
                instruction,
                "Generated based on alert context",
                null,
                null,
                priority,
                List.of()));
      }
    }

    return steps;
  }
}
