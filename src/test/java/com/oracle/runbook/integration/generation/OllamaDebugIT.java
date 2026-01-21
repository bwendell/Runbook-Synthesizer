package com.oracle.runbook.integration.generation;

import static org.assertj.core.api.Assertions.assertThat;

import com.oracle.runbook.domain.Alert;
import com.oracle.runbook.domain.AlertSeverity;
import com.oracle.runbook.domain.DynamicChecklist;
import com.oracle.runbook.domain.EnrichedContext;
import com.oracle.runbook.domain.ResourceMetadata;
import com.oracle.runbook.domain.RetrievedChunk;
import com.oracle.runbook.domain.RunbookChunk;
import com.oracle.runbook.infrastructure.llm.OllamaConfig;
import com.oracle.runbook.infrastructure.llm.OllamaLlmProvider;
import com.oracle.runbook.rag.DefaultChecklistGenerator;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Debug test to reproduce empty checklist issue with real Ollama instance.
 *
 * <p>This test allows identifying if the issue is with the LLM response format, retrieval, or the
 * JSON parsing logic by hitting the local Ollama instance directly.
 */
@Tag("manual") // Tagged manual to avoid running in standard CI/CD unless explicit
class OllamaDebugIT {

  @Test
  @DisplayName("Debug: Generate checklist with real Ollama")
  void debugGenerateWithRealOllama() {
    // 1. Setup Real Ollama Provider
    // Config matches application.yaml defaults
    OllamaConfig config =
        new OllamaConfig("http://localhost:11434", "llama3.2:1b", "nomic-embed-text");
    OllamaLlmProvider llmProvider = new OllamaLlmProvider(config);

    DefaultChecklistGenerator generator = new DefaultChecklistGenerator(llmProvider);

    // 2. Create Dummy Context
    EnrichedContext context = createMemoryAlertContext();

    // 3. Create Dummy Retrieved Chunks (Simulating successful retrieval)
    List<RetrievedChunk> chunks =
        List.of(
            createRetrievedChunk("c1", "Restart the service if memory is high", "Restart Service"),
            createRetrievedChunk(
                "c2", "Check logs for OOM errors at /var/log/syslog", "Check Logs"));

    System.out.println("---------- STARTING OLLAMA GENERATION DEBUG ----------");

    // 4. Generate
    DynamicChecklist checklist = generator.generate(context, chunks);

    System.out.println("---------- OLLAMA GENERATION COMPLETE ----------");
    System.out.println("Summary: " + checklist.summary());
    System.out.println("Steps count: " + checklist.steps().size());
    checklist
        .steps()
        .forEach(
            step -> {
              System.out.println("Step " + step.order() + ": " + step.instruction());
              System.out.println("  Rationale: " + step.rationale());
              System.out.println("  Commands: " + step.commands());
            });
    System.out.println("------------------------------------------------");

    // 5. Assertions
    assertThat(checklist).isNotNull();
    assertThat(checklist.steps())
        .as("Checklist steps should not be empty. Check Raw LLM Response logs.")
        .isNotEmpty();
  }

  private EnrichedContext createMemoryAlertContext() {
    Alert alert =
        new Alert(
            "alert-debug-1",
            "High Memory Utilization",
            "Memory utilization exceeded 90% threshold",
            AlertSeverity.WARNING,
            "aws-cloudwatch",
            Map.of("instanceId", "i-1234567890abcdef0"),
            Map.of(),
            Instant.now(),
            "{}");

    ResourceMetadata resource =
        new ResourceMetadata(
            "i-1234567890abcdef0",
            "prod-web-server",
            "vpc-12345",
            "t3.medium",
            "us-west-2a",
            Map.of(),
            Map.of());

    return new EnrichedContext(alert, resource, List.of(), List.of(), Map.of());
  }

  private RetrievedChunk createRetrievedChunk(String id, String content, String title) {
    RunbookChunk chunk =
        new RunbookChunk(
            id,
            "runbooks/debug-runbook.md",
            title,
            content,
            List.of("debug"),
            List.of("t3.*"),
            new float[0]);
    return new RetrievedChunk(chunk, 0.95, 0.0, 0.95);
  }
}
