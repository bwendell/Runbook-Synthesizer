package com.oracle.runbook.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link StepPriority} enum.
 */
class StepPriorityTest {

	@Test
	@DisplayName("All priority values exist")
	void allPriorityValuesExist() {
		assertEquals(3, StepPriority.values().length);
		assertNotNull(StepPriority.HIGH);
		assertNotNull(StepPriority.MEDIUM);
		assertNotNull(StepPriority.LOW);
	}

	@Test
	@DisplayName("Priority ordinal ordering is HIGH < MEDIUM < LOW")
	void priorityOrdinalOrdering() {
		// HIGH should have lowest ordinal (most urgent)
		assertTrue(StepPriority.HIGH.ordinal() < StepPriority.MEDIUM.ordinal());
		assertTrue(StepPriority.MEDIUM.ordinal() < StepPriority.LOW.ordinal());
	}

	@Test
	@DisplayName("Priority comparison works correctly")
	void priorityComparisonWorks() {
		// Using compareTo for ordering
		assertTrue(StepPriority.HIGH.compareTo(StepPriority.MEDIUM) < 0);
		assertTrue(StepPriority.MEDIUM.compareTo(StepPriority.LOW) < 0);
		assertTrue(StepPriority.HIGH.compareTo(StepPriority.LOW) < 0);
		assertEquals(0, StepPriority.HIGH.compareTo(StepPriority.HIGH));
	}
}
