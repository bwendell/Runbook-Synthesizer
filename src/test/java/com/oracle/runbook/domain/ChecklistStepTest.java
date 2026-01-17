package com.oracle.runbook.domain;

import static org.junit.jupiter.api.Assertions.*;

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

    assertEquals(1, step.order());
    assertEquals("Check current memory usage", step.instruction());
    assertEquals("Memory issues often manifest as high utilization", step.rationale());
    assertEquals("92%", step.currentValue());
    assertEquals("<80%", step.expectedValue());
    assertEquals(StepPriority.HIGH, step.priority());
    assertEquals(commands, step.commands());
  }

  @Test
  @DisplayName("ChecklistStep throws NullPointerException for null instruction")
  void throwsForNullInstruction() {
    assertThrows(
        NullPointerException.class,
        () ->
            new ChecklistStep(
                1, null, "rationale", "current", "expected", StepPriority.MEDIUM, List.of()));
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
    assertEquals(1, step.commands().size());

    // Step's list should be unmodifiable
    assertThrows(UnsupportedOperationException.class, () -> step.commands().add("newCommand"));
  }

  @Test
  @DisplayName("ChecklistStep accepts zero order")
  void acceptsZeroOrder() {
    assertDoesNotThrow(
        () ->
            new ChecklistStep(0, "instruction", null, null, null, StepPriority.MEDIUM, List.of()));
  }
}
