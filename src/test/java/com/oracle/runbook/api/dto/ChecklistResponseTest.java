package com.oracle.runbook.api.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oracle.runbook.domain.ChecklistStep;
import com.oracle.runbook.domain.DynamicChecklist;
import com.oracle.runbook.domain.StepPriority;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for ChecklistResponse DTO. */
class ChecklistResponseTest {

  @Test
  void testFromDomain_CreatesCorrectResponse() {
    var steps =
        List.of(
            new ChecklistStep(
                1,
                "Check logs",
                "First step",
                "error",
                "none",
                StepPriority.HIGH,
                List.of("kubectl logs")),
            new ChecklistStep(
                2, "Restart pod", "Second step", null, null, StepPriority.MEDIUM, List.of()));

    var checklist =
        new DynamicChecklist(
            "alert-123",
            "Database troubleshooting guide",
            steps,
            List.of("runbook1.md", "runbook2.md"),
            Instant.parse("2025-01-16T12:00:00Z"),
            "oci-genai");

    var response = ChecklistResponse.fromDomain(checklist);

    assertThat(response.alertId()).isEqualTo("alert-123");
    assertThat(response.summary()).isEqualTo("Database troubleshooting guide");
    assertThat(response.steps()).hasSize(2);
    assertThat(response.steps().get(0).instruction()).isEqualTo("Check logs");
    assertThat(response.llmProviderUsed()).isEqualTo("oci-genai");
  }

  @Test
  void testFromDomain_ContainsSourceRunbooks() {
    var checklist =
        new DynamicChecklist(
            "alert-456", "Summary", List.of(), List.of("runbook-a.md"), Instant.now(), "openai");

    var response = ChecklistResponse.fromDomain(checklist);

    assertThat(response.sourceRunbooks()).hasSize(1);
    assertThat(response.sourceRunbooks().get(0)).isEqualTo("runbook-a.md");
  }

  @Test
  void testFromDomain_NullChecklist_ThrowsException() {
    assertThatThrownBy(() -> ChecklistResponse.fromDomain(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void testStepResponse_IncludesAllFields() {
    var step =
        new ChecklistStep(
            1,
            "Check CPU",
            "CPU is high",
            "95%",
            "below 80%",
            StepPriority.HIGH,
            List.of("top -n 1"));

    var checklist =
        new DynamicChecklist(
            "alert-789", "CPU Guide", List.of(step), List.of(), Instant.now(), "ollama");

    var response = ChecklistResponse.fromDomain(checklist);
    var stepResponse = response.steps().get(0);

    assertThat(stepResponse.order()).isEqualTo(1);
    assertThat(stepResponse.instruction()).isEqualTo("Check CPU");
    assertThat(stepResponse.rationale()).isEqualTo("CPU is high");
    assertThat(stepResponse.currentValue()).isEqualTo("95%");
    assertThat(stepResponse.expectedValue()).isEqualTo("below 80%");
    assertThat(stepResponse.priority()).isEqualTo("HIGH");
    assertThat(stepResponse.commands()).isEqualTo(List.of("top -n 1"));
  }
}
