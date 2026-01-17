package com.oracle.runbook.api.dto;

import static org.junit.jupiter.api.Assertions.*;

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

    assertEquals("alert-123", response.alertId());
    assertEquals("Database troubleshooting guide", response.summary());
    assertEquals(2, response.steps().size());
    assertEquals("Check logs", response.steps().get(0).instruction());
    assertEquals("oci-genai", response.llmProviderUsed());
  }

  @Test
  void testFromDomain_ContainsSourceRunbooks() {
    var checklist =
        new DynamicChecklist(
            "alert-456", "Summary", List.of(), List.of("runbook-a.md"), Instant.now(), "openai");

    var response = ChecklistResponse.fromDomain(checklist);

    assertEquals(1, response.sourceRunbooks().size());
    assertEquals("runbook-a.md", response.sourceRunbooks().get(0));
  }

  @Test
  void testFromDomain_NullChecklist_ThrowsException() {
    assertThrows(NullPointerException.class, () -> ChecklistResponse.fromDomain(null));
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

    assertEquals(1, stepResponse.order());
    assertEquals("Check CPU", stepResponse.instruction());
    assertEquals("CPU is high", stepResponse.rationale());
    assertEquals("95%", stepResponse.currentValue());
    assertEquals("below 80%", stepResponse.expectedValue());
    assertEquals("HIGH", stepResponse.priority());
    assertEquals(List.of("top -n 1"), stepResponse.commands());
  }
}
