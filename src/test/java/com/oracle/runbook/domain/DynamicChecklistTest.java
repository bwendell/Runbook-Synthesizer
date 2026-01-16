package com.oracle.runbook.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DynamicChecklist} record.
 */
class DynamicChecklistTest {

    private ChecklistStep createTestStep(int order) {
        return new ChecklistStep(
            order,
            "Step " + order + " instruction",
            "Rationale for step " + order,
            "current",
            "expected",
            StepPriority.MEDIUM,
            List.of("command-" + order)
        );
    }

    @Test
    @DisplayName("DynamicChecklist construction with all fields succeeds")
    void constructionWithAllFieldsSucceeds() {
        Instant now = Instant.now();
        List<ChecklistStep> steps = List.of(createTestStep(1), createTestStep(2));
        List<String> sourceRunbooks = List.of("runbooks/memory/high-memory.md");

        DynamicChecklist checklist = new DynamicChecklist(
            "alert-123",
            "This checklist addresses high memory usage on web-server-01",
            steps,
            sourceRunbooks,
            now,
            "oci-genai"
        );

        assertEquals("alert-123", checklist.alertId());
        assertEquals("This checklist addresses high memory usage on web-server-01", checklist.summary());
        assertEquals(steps, checklist.steps());
        assertEquals(sourceRunbooks, checklist.sourceRunbooks());
        assertEquals(now, checklist.generatedAt());
        assertEquals("oci-genai", checklist.llmProviderUsed());
    }

    @Test
    @DisplayName("DynamicChecklist throws NullPointerException for null alertId")
    void throwsForNullAlertId() {
        assertThrows(NullPointerException.class, () -> new DynamicChecklist(
            null, "summary", List.of(), List.of(), Instant.now(), "provider"
        ));
    }

    @Test
    @DisplayName("DynamicChecklist steps list is immutable")
    void stepsListIsImmutable() {
        List<ChecklistStep> mutableSteps = new ArrayList<>();
        mutableSteps.add(createTestStep(1));

        DynamicChecklist checklist = new DynamicChecklist(
            "alert-123", "summary", mutableSteps, List.of(),
            Instant.now(), "provider"
        );

        // Modifying original should not affect checklist
        mutableSteps.add(createTestStep(2));
        assertEquals(1, checklist.steps().size());

        // Checklist's list should be unmodifiable
        assertThrows(UnsupportedOperationException.class, 
            () -> checklist.steps().add(null));
    }

    @Test
    @DisplayName("DynamicChecklist sourceRunbooks list is immutable")
    void sourceRunbooksListIsImmutable() {
        List<String> mutableRunbooks = new ArrayList<>();
        mutableRunbooks.add("runbook1.md");

        DynamicChecklist checklist = new DynamicChecklist(
            "alert-123", "summary", List.of(), mutableRunbooks,
            Instant.now(), "provider"
        );

        // Modifying original should not affect checklist
        mutableRunbooks.add("runbook2.md");
        assertEquals(1, checklist.sourceRunbooks().size());

        // Checklist's list should be unmodifiable
        assertThrows(UnsupportedOperationException.class, 
            () -> checklist.sourceRunbooks().add("another"));
    }
}
