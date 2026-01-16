package com.oracle.runbook.domain;

/**
 * Severity levels for alerts from OCI Monitoring Alarms and other sources.
 * Used to categorize alert urgency for prioritization in checklist generation.
 */
public enum AlertSeverity {
    /**
     * Critical severity - requires immediate attention.
     */
    CRITICAL,

    /**
     * Warning severity - potential issue that needs investigation.
     */
    WARNING,

    /**
     * Informational severity - for awareness, no immediate action required.
     */
    INFO;

    /**
     * Parses a severity string into an AlertSeverity enum value.
     * Case-insensitive matching.
     *
     * @param severity the severity string to parse (e.g., "critical", "CRITICAL", "Critical")
     * @return the matching AlertSeverity enum value
     * @throws IllegalArgumentException if the severity string is null or does not match any known value
     */
    public static AlertSeverity fromString(String severity) {
        if (severity == null) {
            throw new IllegalArgumentException("Severity cannot be null");
        }
        try {
            return valueOf(severity.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown severity: " + severity, e);
        }
    }
}
