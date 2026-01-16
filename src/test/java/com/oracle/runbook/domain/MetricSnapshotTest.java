package com.oracle.runbook.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MetricSnapshot} record.
 */
class MetricSnapshotTest {

    @Test
    @DisplayName("MetricSnapshot construction with valid data succeeds")
    void constructionWithValidDataSucceeds() {
        Instant now = Instant.now();

        MetricSnapshot snapshot = new MetricSnapshot(
            "CpuUtilization",
            "oci_computeagent",
            92.5,
            "percent",
            now
        );

        assertEquals("CpuUtilization", snapshot.metricName());
        assertEquals("oci_computeagent", snapshot.namespace());
        assertEquals(92.5, snapshot.value());
        assertEquals("percent", snapshot.unit());
        assertEquals(now, snapshot.timestamp());
    }

    @Test
    @DisplayName("MetricSnapshot throws NullPointerException for null metricName")
    void throwsForNullMetricName() {
        assertThrows(NullPointerException.class, () -> new MetricSnapshot(
            null,
            "namespace",
            0.0,
            "unit",
            Instant.now()
        ));
    }

    @Test
    @DisplayName("MetricSnapshot throws NullPointerException for null timestamp")
    void throwsForNullTimestamp() {
        assertThrows(NullPointerException.class, () -> new MetricSnapshot(
            "metricName",
            "namespace",
            0.0,
            "unit",
            null
        ));
    }

    @Test
    @DisplayName("MetricSnapshot allows zero and negative values")
    void allowsZeroAndNegativeValues() {
        Instant now = Instant.now();

        MetricSnapshot zero = new MetricSnapshot("metric", "ns", 0.0, "unit", now);
        MetricSnapshot negative = new MetricSnapshot("metric", "ns", -10.5, "unit", now);

        assertEquals(0.0, zero.value());
        assertEquals(-10.5, negative.value());
    }
}
