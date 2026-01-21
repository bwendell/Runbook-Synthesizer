package com.oracle.runbook.rag;

import static org.assertj.core.api.Assertions.assertThat;

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
  @DisplayName("generate formats prompt and parses JSON checklist")
  void generate_formatsPromptAndParsesChecklist() {
    // Arrange
    EnrichedContext context = createTestContext();
    List<RetrievedChunk> chunks = List.of(createRetrievedChunk("c1", "Restart the service"));

    String jsonResponse =
        """
        {
          "summary": "Check logs and restart service",
          "steps": [
            {
              "order": 1,
              "instruction": "Check logs",
              "rationale": "To identify the root cause",
              "priority": "HIGH",
              "commands": ["tail -f /var/log/syslog"]
            },
            {
              "order": 2,
              "instruction": "Restart the service if logs are clean",
              "rationale": "To recover the service",
              "priority": "MEDIUM",
              "commands": ["systemctl restart service"]
            },
            {
              "order": 3,
              "instruction": "Verify service is up",
              "rationale": "To ensure stability",
              "priority": "LOW",
              "commands": []
            }
          ]
        }
        """;
    llmProvider.setNextResponse(jsonResponse);

    // Act
    DynamicChecklist checklist = generator.generate(context, chunks);

    // Assert
    assertThat(checklist).isNotNull();
    assertThat(checklist.alertId()).isEqualTo("a1");
    assertThat(checklist.summary()).isEqualTo("Check logs and restart service");
    assertThat(checklist.steps()).hasSize(3);

    ChecklistStep step1 = checklist.steps().get(0);
    assertThat(step1.instruction()).isEqualTo("Check logs");
    assertThat(step1.rationale()).isEqualTo("To identify the root cause");
    assertThat(step1.priority()).isEqualTo(StepPriority.HIGH);
    assertThat(step1.commands()).containsExactly("tail -f /var/log/syslog");

    assertThat(checklist.steps().get(2).instruction()).isEqualTo("Verify service is up");
    assertThat(llmProvider.lastPrompt).contains("High CPU"); // Verify context was in prompt
    assertThat(llmProvider.lastPrompt)
        .contains("Restart the service"); // Verify chunk was in prompt
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
    assertThat(checklist).isNotNull();
    assertThat(checklist.steps()).isEmpty();
  }

  @Test
  @DisplayName("generate falls back to Markdown parsing on invalid JSON")
  void generate_fallsBackToMarkdownOnInvalidJson() {
    // Arrange
    EnrichedContext context = createTestContext();
    List<RetrievedChunk> chunks = List.of(createRetrievedChunk("c1", "Restart"));
    // Invalid JSON but valid Markdown
    String response =
        """
        This is not JSON but it is a checklist:
        Step 1: Check logs
        Step 2: Fix it
        """;
    llmProvider.setNextResponse(response);

    // Act
    DynamicChecklist checklist = generator.generate(context, chunks);

    // Assert
    assertThat(checklist).isNotNull();
    assertThat(checklist.steps()).hasSize(2);
    assertThat(checklist.steps().get(0).instruction()).isEqualTo("Check logs");
  }

  @Test
  @DisplayName("generate handles JSON with code fences")
  void generate_handlesJsonWithCodeFences() {
    // Arrange
    EnrichedContext context = createTestContext();
    List<RetrievedChunk> chunks = List.of(createRetrievedChunk("c1", "Restart"));
    String response =
        """
         ```json
         {
           "summary": "Summary",
           "steps": [
             {
               "order": 1,
               "instruction": "Do simple thing",
               "priority": "LOW",
               "commands": []
             }
           ]
         }
         ```
         """;
    llmProvider.setNextResponse(response);

    // Act
    DynamicChecklist checklist = generator.generate(context, chunks);

    // Assert
    assertThat(checklist.steps()).hasSize(1);
    assertThat(checklist.steps().get(0).instruction()).isEqualTo("Do simple thing");
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
