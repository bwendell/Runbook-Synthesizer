package com.oracle.runbook.rag;

import static org.junit.jupiter.api.Assertions.*;

import com.oracle.runbook.domain.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for the ChecklistGenerator interface contract. */
class ChecklistGeneratorTest {

  @Test
  @DisplayName("generate accepts EnrichedContext and relevant chunks, returns DynamicChecklist")
  void generate_acceptsContextAndChunks_returnsDynamicChecklist() {
    // Arrange
    ChecklistGenerator generator = new TestChecklistGenerator();
    EnrichedContext context = createTestContext();
    List<RetrievedChunk> chunks = createTestChunks();

    // Act
    DynamicChecklist checklist = generator.generate(context, chunks);

    // Assert
    assertNotNull(checklist);
    assertEquals("alert-001", checklist.alertId());
    assertFalse(checklist.steps().isEmpty());
    assertFalse(checklist.sourceRunbooks().isEmpty());
  }

  private EnrichedContext createTestContext() {
    Alert alert =
        new Alert(
            "alert-001",
            "High Memory",
            "Memory utilization exceeded threshold",
            AlertSeverity.WARNING,
            "oci-monitoring",
            Map.of("resourceId", "ocid1.instance.oc1..example"),
            Map.of(),
            Instant.now(),
            "{}");
    return new EnrichedContext(alert, null, List.of(), List.of(), Map.of());
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
