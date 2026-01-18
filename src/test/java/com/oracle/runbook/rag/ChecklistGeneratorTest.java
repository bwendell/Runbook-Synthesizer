package com.oracle.runbook.rag;

import static org.assertj.core.api.Assertions.assertThat;

import com.oracle.runbook.domain.*;
import com.oracle.runbook.integration.TestFixtures;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for the ChecklistGenerator interface contract. */
class ChecklistGeneratorTest {

  @Test
  @DisplayName("generate accepts EnrichedContext and relevant chunks, returns DynamicChecklist")
  void generate_acceptsContextAndChunks_returnsDynamicChecklist() {
    // Arrange
    ChecklistGenerator generator = new TestChecklistGenerator();
    EnrichedContext context =
        TestFixtures.loadAs("contexts/enriched-context-memory.json", EnrichedContext.class);
    List<RetrievedChunk> chunks = createTestChunks();

    // Act
    DynamicChecklist checklist = generator.generate(context, chunks);

    // Assert
    assertThat(checklist).isNotNull();
    assertThat(checklist.alertId()).isEqualTo("alert-001");
    assertThat(checklist.steps()).isNotEmpty();
    assertThat(checklist.sourceRunbooks()).isNotEmpty();
  }

  private List<RetrievedChunk> createTestChunks() {
    float[] embedding = new float[768];
    RunbookChunk chunk =
        new RunbookChunk(
            "chunk-001",
            "runbooks/memory/high-memory.md",
            "Step 1: Check memory",
            "Run free -h to check memory usage",
            List.of("memory"),
            List.of("VM.*"),
            embedding);
    return List.of(new RetrievedChunk(chunk, 0.92, 0.1, 1.02));
  }

  /** Test implementation of ChecklistGenerator for verifying interface contract. */
  private static class TestChecklistGenerator implements ChecklistGenerator {
    @Override
    public DynamicChecklist generate(EnrichedContext context, List<RetrievedChunk> relevantChunks) {
      ChecklistStep step =
          new ChecklistStep(
              1,
              "Check memory usage with free -h",
              "Memory is at 92%",
              "92%",
              "Below 80%",
              StepPriority.HIGH,
              List.of("free -h"));
      return new DynamicChecklist(
          context.alert().id(),
          "High memory troubleshooting checklist",
          List.of(step),
          List.of("runbooks/memory/high-memory.md"),
          Instant.now(),
          "oci-genai");
    }
  }
}
