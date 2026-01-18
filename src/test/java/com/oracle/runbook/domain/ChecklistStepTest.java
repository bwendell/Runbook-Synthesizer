package com.oracle.runbook.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link ChecklistStep} record. */
class ChecklistStepTest {

  @Test
  @DisplayName("ChecklistStep construction with all fields succeeds")
  void constructionWithAllFieldsSucceeds() {
    List<String> commands = List.of("free -h", "ps aux --sort=-%mem | head -20");

    ChecklistStep step =
        new ChecklistStep(
            1,
            "Check current memory usage",
            "Memory issues often manifest as high utilization",
            "92%",
            "<80%",
            StepPriority.HIGH,
            commands);

    assertThat(step.order()).isEqualTo(1);
    assertThat(step.instruction()).isEqualTo("Check current memory usage");
    assertThat(step.rationale()).isEqualTo("Memory issues often manifest as high utilization");
    assertThat(step.currentValue()).isEqualTo("92%");
    assertThat(step.expectedValue()).isEqualTo("<80%");
    assertThat(step.priority()).isEqualTo(StepPriority.HIGH);
    assertThat(step.commands()).isEqualTo(commands);
  }

  @Test
  @DisplayName("ChecklistStep throws NullPointerException for null instruction")
  void throwsForNullInstruction() {
    assertThatThrownBy(
            () ->
                new ChecklistStep(
                    1, null, "rationale", "current", "expected", StepPriority.MEDIUM, List.of()))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("ChecklistStep commands list is immutable")
  void commandsListIsImmutable() {
    List<String> mutableCommands = new ArrayList<>();
    mutableCommands.add("free -h");

    ChecklistStep step =
        new ChecklistStep(
            1,
            "instruction",
            "rationale",
            "current",
            "expected",
            StepPriority.LOW,
            mutableCommands);

    // Modifying original should not affect step
    mutableCommands.add("ps aux");
    assertThat(step.commands()).hasSize(1);

    // Step's list should be unmodifiable
    assertThatThrownBy(() -> step.commands().add("newCommand"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("ChecklistStep accepts zero order")
  void acceptsZeroOrder() {
    assertThatCode(
            () ->
                new ChecklistStep(
                    0, "instruction", null, null, null, StepPriority.MEDIUM, List.of()))
        .doesNotThrowAnyException();
  }
}
