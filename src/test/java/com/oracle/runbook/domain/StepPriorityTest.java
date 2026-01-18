package com.oracle.runbook.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link StepPriority} enum. */
class StepPriorityTest {

  @Test
  @DisplayName("All priority values exist")
  void allPriorityValuesExist() {
    assertThat(StepPriority.values()).hasSize(3);
    assertThat(StepPriority.HIGH).isNotNull();
    assertThat(StepPriority.MEDIUM).isNotNull();
    assertThat(StepPriority.LOW).isNotNull();
  }

  @Test
  @DisplayName("Priority ordinal ordering is HIGH < MEDIUM < LOW")
  void priorityOrdinalOrdering() {
    // HIGH should have lowest ordinal (most urgent)
    assertThat(StepPriority.HIGH.ordinal()).isLessThan(StepPriority.MEDIUM.ordinal());
    assertThat(StepPriority.MEDIUM.ordinal()).isLessThan(StepPriority.LOW.ordinal());
  }

  @Test
  @DisplayName("Priority comparison works correctly")
  void priorityComparisonWorks() {
    // Using compareTo for ordering
    assertThat(StepPriority.HIGH.compareTo(StepPriority.MEDIUM)).isLessThan(0);
    assertThat(StepPriority.MEDIUM.compareTo(StepPriority.LOW)).isLessThan(0);
    assertThat(StepPriority.HIGH.compareTo(StepPriority.LOW)).isLessThan(0);
    assertThat(StepPriority.HIGH.compareTo(StepPriority.HIGH)).isEqualTo(0);
  }
}
