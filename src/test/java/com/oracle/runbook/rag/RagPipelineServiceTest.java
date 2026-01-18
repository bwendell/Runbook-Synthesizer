package com.oracle.runbook.rag;

import static org.assertj.core.api.Assertions.assertThat;

import com.oracle.runbook.domain.*;
import com.oracle.runbook.enrichment.ContextEnrichmentService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for RagPipelineService implementation. */
class RagPipelineServiceTest {

  private RagPipelineService pipelineService;
  private StubEnrichmentService enrichmentService;
  private StubRetriever retriever;
  private StubGenerator generator;

  @BeforeEach
  void setUp() {
    enrichmentService = new StubEnrichmentService();
    retriever = new StubRetriever();
    generator = new StubGenerator();
    pipelineService = new RagPipelineService(enrichmentService, retriever, generator);
  }

  @Test
  @DisplayName("processAlert orchestrates the full RAG pipeline")
  void processAlert_orchestratesFullPipeline() throws Exception {
    // Arrange
    Alert alert = createTestAlert();
    EnrichedContext context = createTestContext(alert);
    enrichmentService.setResult(context);

    List<RetrievedChunk> chunks = List.of(createTestChunk());
    retriever.setResult(chunks);

    DynamicChecklist expectedChecklist = createTestChecklist(alert.id());
    generator.setResult(expectedChecklist);

    // Act
    CompletableFuture<DynamicChecklist> future = pipelineService.processAlert(alert, 5);
    DynamicChecklist actualChecklist = future.get();

    // Assert
    assertThat(actualChecklist).isNotNull();
    assertThat(actualChecklist.alertId()).isEqualTo(expectedChecklist.alertId());
    assertThat(enrichmentService.called).isTrue();
    assertThat(retriever.called).isTrue();
    assertThat(generator.called).isTrue();
  }

  private Alert createTestAlert() {
    return new Alert(
        "a1",
        "Title",
        "Msg",
        AlertSeverity.CRITICAL,
        "oci",
        Map.of(),
        Map.of(),
        Instant.now(),
        "{}");
  }

  private EnrichedContext createTestContext(Alert alert) {
    return new EnrichedContext(alert, null, List.of(), List.of(), Map.of());
  }

  private RetrievedChunk createTestChunk() {
    RunbookChunk chunk = new RunbookChunk("c1", "p", "T", "C", List.of(), List.of(), new float[0]);
    return new RetrievedChunk(chunk, 0.9, 0.1, 1.0);
  }

  private DynamicChecklist createTestChecklist(String alertId) {
    return new DynamicChecklist(alertId, "Summary", List.of(), List.of(), Instant.now(), "stub");
  }

  private static class StubEnrichmentService implements ContextEnrichmentService {
    boolean called = false;
    EnrichedContext result;

    void setResult(EnrichedContext r) {
      this.result = r;
    }

    @Override
    public CompletableFuture<EnrichedContext> enrich(Alert alert) {
      this.called = true;
      return CompletableFuture.completedFuture(result);
    }
  }

  private static class StubRetriever implements RunbookRetriever {
    boolean called = false;
    List<RetrievedChunk> result;

    void setResult(List<RetrievedChunk> r) {
      this.result = r;
    }

    @Override
    public List<RetrievedChunk> retrieve(EnrichedContext context, int topK) {
      this.called = true;
      return result;
    }
  }

  private static class StubGenerator implements ChecklistGenerator {
    boolean called = false;
    DynamicChecklist result;

    void setResult(DynamicChecklist r) {
      this.result = r;
    }

    @Override
    public DynamicChecklist generate(EnrichedContext context, List<RetrievedChunk> chunks) {
      this.called = true;
      return result;
    }
  }
}
