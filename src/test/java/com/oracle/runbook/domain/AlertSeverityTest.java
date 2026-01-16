package com.oracle.runbook.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AlertSeverity} enum.
 */
class AlertSeverityTest {

    @Test
    @DisplayName("All severity values exist")
    void allSeverityValuesExist() {
        assertEquals(3, AlertSeverity.values().length);
        assertNotNull(AlertSeverity.CRITICAL);
        assertNotNull(AlertSeverity.WARNING);
        assertNotNull(AlertSeverity.INFO);
    }

    @ParameterizedTest
    @DisplayName("fromString handles case-insensitive input")
    @ValueSource(strings = {"critical", "CRITICAL", "Critical", "CrItIcAl"})
    void fromStringHandlesCaseInsensitiveInput(String input) {
        assertEquals(AlertSeverity.CRITICAL, AlertSeverity.fromString(input));
    }

    @Test
    @DisplayName("fromString returns WARNING for warning strings")
    void fromStringReturnsWarning() {
        assertEquals(AlertSeverity.WARNING, AlertSeverity.fromString("warning"));
        assertEquals(AlertSeverity.WARNING, AlertSeverity.fromString("WARNING"));
    }

    @Test
    @DisplayName("fromString returns INFO for info strings")
    void fromStringReturnsInfo() {
        assertEquals(AlertSeverity.INFO, AlertSeverity.fromString("info"));
        assertEquals(AlertSeverity.INFO, AlertSeverity.fromString("INFO"));
    }

    @Test
    @DisplayName("fromString throws IllegalArgumentException for unknown values")
    void fromStringThrowsForUnknownValues() {
        assertThrows(IllegalArgumentException.class, () -> AlertSeverity.fromString("unknown"));
        assertThrows(IllegalArgumentException.class, () -> AlertSeverity.fromString(""));
        assertThrows(IllegalArgumentException.class, () -> AlertSeverity.fromString(null));
    }
}
