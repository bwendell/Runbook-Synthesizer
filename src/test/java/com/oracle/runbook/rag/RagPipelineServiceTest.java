package com.oracle.runbook.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oracle.runbook.domain.*;
import com.oracle.runbook.enrichment.ContextEnrichmentService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

  @Nested
  @DisplayName("Happy path tests")
  class HappyPathTests {

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
  }

  @Nested
  @DisplayName("Error handling tests")
  class ErrorHandlingTests {

    @Test
    @DisplayName("processAlert with null alert should throw NullPointerException")
    void processAlertWithNullAlertShouldThrow() {
      assertThatThrownBy(() -> pipelineService.processAlert(null, 5))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("alert");
    }

    @Test
    @DisplayName("processAlert should propagate enrichment service failures")
    void processAlertWhenEnrichmentFailsShouldPropagateException() {
      // Arrange
      Alert alert = createTestAlert();
      RuntimeException enrichmentError = new RuntimeException("Enrichment failed: timeout");
      enrichmentService.setException(enrichmentError);

      // Act & Assert
      assertThatThrownBy(() -> pipelineService.processAlert(alert, 5).get())
          .isInstanceOf(ExecutionException.class)
          .hasCauseInstanceOf(RuntimeException.class)
          .hasRootCauseMessage("Enrichment failed: timeout");
    }

    @Test
    @DisplayName("processAlert should propagate retriever failures")
    void processAlertWhenRetrieverFailsShouldPropagateException() throws Exception {
      // Arrange
      Alert alert = createTestAlert();
      EnrichedContext context = createTestContext(alert);
      enrichmentService.setResult(context);

      RuntimeException retrieverError = new RuntimeException("Vector store connection failed");
      retriever.setException(retrieverError);

      // Act & Assert
      assertThatThrownBy(() -> pipelineService.processAlert(alert, 5).get())
          .isInstanceOf(ExecutionException.class)
          .cause()
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Vector store connection failed");
    }

    @Test
    @DisplayName("processAlert should propagate generator failures")
    void processAlertWhenGeneratorFailsShouldPropagateException() throws Exception {
      // Arrange
      Alert alert = createTestAlert();
      EnrichedContext context = createTestContext(alert);
      enrichmentService.setResult(context);

      List<RetrievedChunk> chunks = List.of(createTestChunk());
      retriever.setResult(chunks);

      RuntimeException generatorError = new RuntimeException("LLM API rate limited");
      generator.setException(generatorError);

      // Act & Assert
      assertThatThrownBy(() -> pipelineService.processAlert(alert, 5).get())
          .isInstanceOf(ExecutionException.class)
          .cause()
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("LLM API rate limited");
    }
  }

  @Nested
  @DisplayName("Edge case tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("processAlert with empty chunks list should still generate checklist")
    void processAlertWithEmptyChunksListShouldStillGenerate() throws Exception {
      // Arrange
      Alert alert = createTestAlert();
      EnrichedContext context = createTestContext(alert);
      enrichmentService.setResult(context);

      // Retriever returns empty list (no relevant runbooks found)
      retriever.setResult(List.of());

      DynamicChecklist expectedChecklist = createTestChecklist(alert.id());
      generator.setResult(expectedChecklist);

      // Act
      CompletableFuture<DynamicChecklist> future = pipelineService.processAlert(alert, 5);
      DynamicChecklist actualChecklist = future.get();

      // Assert - generator should still be called even with empty chunks
      assertThat(generator.called).isTrue();
      assertThat(actualChecklist).isNotNull();
      assertThat(actualChecklist.alertId()).isEqualTo(alert.id());
    }

    @Test
    @DisplayName("processAlert with topK=0 should still complete pipeline")
    void processAlertWithZeroTopKShouldComplete() throws Exception {
      // Arrange
      Alert alert = createTestAlert();
      EnrichedContext context = createTestContext(alert);
      enrichmentService.setResult(context);
      retriever.setResult(List.of());

      DynamicChecklist expectedChecklist = createTestChecklist(alert.id());
      generator.setResult(expectedChecklist);

      // Act - use topK=0
      CompletableFuture<DynamicChecklist> future = pipelineService.processAlert(alert, 0);
      DynamicChecklist actualChecklist = future.get();

      // Assert - pipeline should complete
      assertThat(actualChecklist).isNotNull();
      assertThat(retriever.called).isTrue();
      assertThat(retriever.lastTopK).isZero();
    }

    @Test
    @DisplayName("processAlert with large topK should pass value to retriever")
    void processAlertWithLargeTopKShouldPassToRetriever() throws Exception {
      // Arrange
      Alert alert = createTestAlert();
      EnrichedContext context = createTestContext(alert);
      enrichmentService.setResult(context);
      retriever.setResult(List.of());
      generator.setResult(createTestChecklist(alert.id()));

      // Act
      pipelineService.processAlert(alert, 100).get();

      // Assert
      assertThat(retriever.lastTopK).isEqualTo(100);
    }
  }

  // ============ Test helpers ============

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

  // ============ Stub implementations ============

  private static class StubEnrichmentService implements ContextEnrichmentService {
    boolean called = false;
    EnrichedContext result;
    RuntimeException exception;

    void setResult(EnrichedContext r) {
      this.result = r;
      this.exception = null;
    }

    void setException(RuntimeException e) {
      this.exception = e;
      this.result = null;
    }

    @Override
    public CompletableFuture<EnrichedContext> enrich(Alert alert) {
      this.called = true;
      if (exception != null) {
        return CompletableFuture.failedFuture(exception);
      }
      return CompletableFuture.completedFuture(result);
    }
  }

  private static class StubRetriever implements RunbookRetriever {
    boolean called = false;
    int lastTopK = -1;
    List<RetrievedChunk> result;
    RuntimeException exception;

    void setResult(List<RetrievedChunk> r) {
      this.result = r;
      this.exception = null;
    }

    void setException(RuntimeException e) {
      this.exception = e;
      this.result = null;
    }

    @Override
    public List<RetrievedChunk> retrieve(EnrichedContext context, int topK) {
      this.called = true;
      this.lastTopK = topK;
      if (exception != null) {
        throw exception;
      }
      return result;
    }
  }

  private static class StubGenerator implements ChecklistGenerator {
    boolean called = false;
    DynamicChecklist result;
    RuntimeException exception;

    void setResult(DynamicChecklist r) {
      this.result = r;
      this.exception = null;
    }

    void setException(RuntimeException e) {
      this.exception = e;
      this.result = null;
    }

    @Override
    public DynamicChecklist generate(EnrichedContext context, List<RetrievedChunk> chunks) {
      this.called = true;
      if (exception != null) {
        throw exception;
      }
      return result;
    }
  }
}
