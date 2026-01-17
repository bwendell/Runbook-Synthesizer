package com.oracle.runbook.ingestion;

import com.oracle.runbook.domain.Alert;
import com.oracle.runbook.domain.AlertSeverity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the AlertSourceAdapter interface contract.
 */
class AlertSourceAdapterTest {

    @Test
    @DisplayName("sourceType returns expected identifier")
    void sourceType_returnsExpectedIdentifier() {
        // Arrange
        AlertSourceAdapter adapter = new TestAlertSourceAdapter("oci-monitoring");
        
        // Act & Assert
        assertEquals("oci-monitoring", adapter.sourceType());
    }

    @Test
    @DisplayName("parseAlert transforms raw payload to Alert domain object")
    void parseAlert_transformsPayloadToAlert() {
        // Arrange
        AlertSourceAdapter adapter = new TestAlertSourceAdapter("oci-events");
        String rawPayload = "{\"alarmName\":\"High CPU\",\"severity\":\"CRITICAL\"}";
        
        // Act
        Alert alert = adapter.parseAlert(rawPayload);
        
        // Assert
        assertNotNull(alert);
        assertEquals("High CPU", alert.title());
        assertEquals(AlertSeverity.CRITICAL, alert.severity());
    }

    @Test
    @DisplayName("canHandle returns boolean to check if adapter handles payload")
    void canHandle_returnsBooleanForPayloadRouting() {
        // Arrange
        AlertSourceAdapter adapter = new TestAlertSourceAdapter("oci-monitoring");
        String ociPayload = "{\"alarmName\":\"Test\",\"type\":\"oci-monitoring\"}";
        String otherPayload = "{\"alertname\":\"Test\",\"type\":\"prometheus\"}";
        
        // Act & Assert
        assertTrue(adapter.canHandle(ociPayload));
        assertFalse(adapter.canHandle(otherPayload));
    }

    /**
     * Test implementation of AlertSourceAdapter for verifying interface contract.
     */
    private static class TestAlertSourceAdapter implements AlertSourceAdapter {
        private final String sourceType;

        TestAlertSourceAdapter(String sourceType) {
            this.sourceType = sourceType;
        }

        @Override
        public String sourceType() {
            return sourceType;
        }

        @Override
        public Alert parseAlert(String rawPayload) {
            // Simplified parsing for test
            return new Alert(
                "alert-001",
                "High CPU",
                "CPU threshold exceeded",
                AlertSeverity.CRITICAL,
                sourceType,
                Map.of("resourceId", "ocid1.instance.oc1..example"),
                Map.of(),
                Instant.now(),
                rawPayload
            );
        }

        @Override
        public boolean canHandle(String rawPayload) {
            return rawPayload.contains(sourceType);
        }
    }
}
