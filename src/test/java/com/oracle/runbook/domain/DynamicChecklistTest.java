package com.oracle.runbook.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link DynamicChecklist} record. */
class DynamicChecklistTest {

  private ChecklistStep createTestStep(int order) {
    return new ChecklistStep(
        order,
        "Step " + order + " instruction",
        "Rationale for step " + order,
        "current",
        "expected",
        StepPriority.MEDIUM,
        List.of("command-" + order));
  }

  @Test
  @DisplayName("DynamicChecklist construction with all fields succeeds")
  void constructionWithAllFieldsSucceeds() {
    Instant now = Instant.now();
    List<ChecklistStep> steps = List.of(createTestStep(1), createTestStep(2));
    List<String> sourceRunbooks = List.of("runbooks/memory/high-memory.md");

    DynamicChecklist checklist =
        new DynamicChecklist(
            "alert-123",
            "This checklist addresses high memory usage on web-server-01",
            steps,
            sourceRunbooks,
            now,
            "oci-genai");

    assertThat(checklist.alertId()).isEqualTo("alert-123");
    assertThat(checklist.summary())
        .isEqualTo("This checklist addresses high memory usage on web-server-01");
    assertThat(checklist.steps()).isEqualTo(steps);
    assertThat(checklist.sourceRunbooks()).isEqualTo(sourceRunbooks);
    assertThat(checklist.generatedAt()).isEqualTo(now);
    assertThat(checklist.llmProviderUsed()).isEqualTo("oci-genai");
  }

  @Test
  @DisplayName("DynamicChecklist throws NullPointerException for null alertId")
  void throwsForNullAlertId() {
    assertThatThrownBy(
            () ->
                new DynamicChecklist(
                    null, "summary", List.of(), List.of(), Instant.now(), "provider"))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("DynamicChecklist steps list is immutable")
  void stepsListIsImmutable() {
    List<ChecklistStep> mutableSteps = new ArrayList<>();
    mutableSteps.add(createTestStep(1));

    DynamicChecklist checklist =
        new DynamicChecklist(
            "alert-123", "summary", mutableSteps, List.of(), Instant.now(), "provider");

    // Modifying original should not affect checklist
    mutableSteps.add(createTestStep(2));
    assertThat(checklist.steps()).hasSize(1);

    // Checklist's list should be unmodifiable
    assertThatThrownBy(() -> checklist.steps().add(null))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("DynamicChecklist sourceRunbooks list is immutable")
  void sourceRunbooksListIsImmutable() {
    List<String> mutableRunbooks = new ArrayList<>();
    mutableRunbooks.add("runbook1.md");

    DynamicChecklist checklist =
        new DynamicChecklist(
            "alert-123", "summary", List.of(), mutableRunbooks, Instant.now(), "provider");

    // Modifying original should not affect checklist
    mutableRunbooks.add("runbook2.md");
    assertThat(checklist.sourceRunbooks()).hasSize(1);

    // Checklist's list should be unmodifiable
    assertThatThrownBy(() -> checklist.sourceRunbooks().add("another"))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
