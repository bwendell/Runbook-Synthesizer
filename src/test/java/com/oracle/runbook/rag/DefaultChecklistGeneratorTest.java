package com.oracle.runbook.rag;

import static org.junit.jupiter.api.Assertions.*;

import com.oracle.runbook.domain.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for DefaultChecklistGenerator implementation. */
class DefaultChecklistGeneratorTest {

  private DefaultChecklistGenerator generator;
  private StubLlmProvider llmProvider;

  @BeforeEach
  void setUp() {
    llmProvider = new StubLlmProvider();
    generator = new DefaultChecklistGenerator(llmProvider);
  }

  @Test
  @DisplayName("generate formats prompt and parses Markdown checklist")
  void generate_formatsPromptAndParsesChecklist() {
    // Arrange
    EnrichedContext context = createTestContext();
    List<RetrievedChunk> chunks = List.of(createRetrievedChunk("c1", "Restart the service"));

    String markdownResponse =
        """
				Step 1: Check logs
				Step 2: Restart the service if logs are clean
				Step 3: Verify service is up
				""";
    llmProvider.setNextResponse(markdownResponse);

    // Act
    DynamicChecklist checklist = generator.generate(context, chunks);

    // Assert
    assertNotNull(checklist);
    assertEquals("a1", checklist.alertId());
    assertEquals(3, checklist.steps().size());
    assertEquals("Check logs", checklist.steps().get(0).instruction());
    assertEquals("Verify service is up", checklist.steps().get(2).instruction());
    assertTrue(llmProvider.lastPrompt.contains("High CPU")); // Verify context was in prompt
    assertTrue(
        llmProvider.lastPrompt.contains("Restart the service")); // Verify chunk was in prompt
  }

  @Test
  @DisplayName("generate handles empty LLM response")
  void generate_handlesEmptyResponse() {
    // Arrange
    llmProvider.setNextResponse("");
    EnrichedContext context = createTestContext();

    // Act
    DynamicChecklist checklist = generator.generate(context, List.of());

    // Assert
    assertNotNull(checklist);
    assertTrue(checklist.steps().isEmpty());
  }

  private EnrichedContext createTestContext() {
    Alert alert =
        new Alert(
            "a1",
            "High CPU",
            "CPU spike",
            AlertSeverity.CRITICAL,
            "oci",
            Map.of(),
            Map.of(),
            Instant.now(),
            "{}");
    ResourceMetadata resource =
        new ResourceMetadata("r1", "web01", "comp1", "VM.Standard2.1", "AD1", Map.of(), Map.of());
    return new EnrichedContext(alert, resource, List.of(), List.of(), Map.of());
  }

  private RetrievedChunk createRetrievedChunk(String id, String content) {
    RunbookChunk chunk =
        new RunbookChunk(id, "path", "Title", content, List.of(), List.of(), new float[0]);
    return new RetrievedChunk(chunk, 0.9, 0.1, 1.0);
  }

  private static class StubLlmProvider implements LlmProvider {
    String nextResponse = "";
    String lastPrompt = "";

    void setNextResponse(String r) {
      this.nextResponse = r;
    }

    @Override
    public String providerId() {
      return "stub";
    }

    @Override
    public CompletableFuture<String> generateText(String prompt, GenerationConfig config) {
      this.lastPrompt = prompt;
      return CompletableFuture.completedFuture(nextResponse);
    }

    @Override
    public CompletableFuture<float[]> generateEmbedding(String text) {
      return null;
    }

    @Override
    public CompletableFuture<List<float[]>> generateEmbeddings(List<String> texts) {
      return null;
    }
  }
}
