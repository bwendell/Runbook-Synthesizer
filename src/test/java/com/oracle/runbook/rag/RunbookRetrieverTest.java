package com.oracle.runbook.rag;

import static org.junit.jupiter.api.Assertions.*;

import com.oracle.runbook.domain.Alert;
import com.oracle.runbook.domain.AlertSeverity;
import com.oracle.runbook.domain.EnrichedContext;
import com.oracle.runbook.domain.RetrievedChunk;
import com.oracle.runbook.domain.RunbookChunk;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for the RunbookRetriever interface contract. */
class RunbookRetrieverTest {

  @Test
  @DisplayName("retrieve accepts EnrichedContext and topK, returns List<RetrievedChunk>")
  void retrieve_acceptsContextAndTopK_returnsRetrievedChunks() {
    // Arrange
    RunbookRetriever retriever = new TestRunbookRetriever();
    EnrichedContext context = createTestContext();
    int topK = 5;

    // Act
    List<RetrievedChunk> chunks = retriever.retrieve(context, topK);

    // Assert
    assertNotNull(chunks);
    assertEquals(2, chunks.size());
    assertTrue(chunks.get(0).similarityScore() > 0);
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

  /** Test implementation of RunbookRetriever for verifying interface contract. */
  private static class TestRunbookRetriever implements RunbookRetriever {
    @Override
    public List<RetrievedChunk> retrieve(EnrichedContext context, int topK) {
      float[] embedding = new float[768];
      RunbookChunk chunk1 =
          new RunbookChunk(
              "chunk-001",
              "runbooks/memory/high-memory.md",
              "Step 1: Check memory",
              "Run free -h to check memory usage",
              List.of("memory", "linux"),
              List.of("VM.*"),
              embedding);
      RunbookChunk chunk2 =
          new RunbookChunk(
              "chunk-002",
              "runbooks/memory/high-memory.md",
              "Step 2: Find memory consumers",
              "Run ps aux --sort=-%mem | head -20",
              List.of("memory", "process"),
              List.of("VM.*"),
              embedding);
      return List.of(
          new RetrievedChunk(chunk1, 0.92, 0.1, 1.02),
          new RetrievedChunk(chunk2, 0.88, 0.05, 0.93));
    }
  }
}
