package com.oracle.runbook.domain;

import java.util.List;
import java.util.Objects;

/**
 * Single step in a generated troubleshooting checklist.
 *
 * @param order
 *            the step number for ordering
 * @param instruction
 *            the actionable instruction text
 * @param rationale
 *            why this step matters for this specific alert
 * @param currentValue
 *            the current observed value (e.g., "Memory: 92%")
 * @param expectedValue
 *            the expected/healthy value (e.g., "Should be below 80%")
 * @param priority
 *            the urgency of this step
 * @param commands
 *            executable commands to run
 */
public record ChecklistStep(int order, String instruction, String rationale, String currentValue, String expectedValue,
		StepPriority priority, List<String> commands) {
	/**
	 * Compact constructor with validation and defensive copies.
	 */
	public ChecklistStep {
		Objects.requireNonNull(instruction, "ChecklistStep instruction cannot be null");

		// Defensive copy for immutability
		commands = commands != null ? List.copyOf(commands) : List.of();
	}
}
