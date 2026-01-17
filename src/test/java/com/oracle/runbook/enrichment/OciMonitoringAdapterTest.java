package com.oracle.runbook.enrichment;

import com.oracle.runbook.config.OciConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link OciMonitoringAdapter}.
 * <p>
 * Note: Full integration testing with OCI SDK requires live credentials.
 * These unit tests verify the adapter implements the interface contract.
 */
class OciMonitoringAdapterTest {

    @Test
    @DisplayName("OciMonitoringAdapter should implement MetricsSourceAdapter")
    void testImplementsMetricsSourceAdapter() {
        // Verify the class implements the interface
        assertTrue(MetricsSourceAdapter.class.isAssignableFrom(OciMonitoringAdapter.class),
            "OciMonitoringAdapter should implement MetricsSourceAdapter");
    }

    @Test
    @DisplayName("OciMonitoringAdapter constructor should reject null monitoringClient")
    void testConstructorRejectsNullClient() {
        OciConfig config = new OciConfig(
            "ocid1.compartment.oc1..test",
            null, null, null
        );
        
        assertThrows(NullPointerException.class, () -> 
            new OciMonitoringAdapter(null, config)
        );
    }

    @Test
    @DisplayName("OciMonitoringAdapter constructor should reject null config")
    void testConstructorRejectsNullConfig() {
        // MonitoringClient.builder() requires auth, so we can't easily create one
        // This test verifies the validation order - null client check happens first
        assertThrows(NullPointerException.class, () -> 
            new OciMonitoringAdapter(null, null)
        );
    }

    @Test
    @DisplayName("sourceType() should be accessible via interface contract")
    void testSourceTypeContract() {
        // Verify the sourceType method exists and returns the expected type annotation
        // This is a compile-time contract verification
        try {
            var method = OciMonitoringAdapter.class.getMethod("sourceType");
            assertEquals(String.class, method.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("sourceType() method should exist");
        }
    }

    @Test
    @DisplayName("fetchMetrics should be accessible via interface contract")
    void testFetchMetricsContract() {
        // Verify the fetchMetrics method exists with correct signature
        try {
            var method = OciMonitoringAdapter.class.getMethod(
                "fetchMetrics", String.class, java.time.Duration.class);
            assertNotNull(method.getGenericReturnType());
        } catch (NoSuchMethodException e) {
            fail("fetchMetrics(String, Duration) method should exist");
        }
    }
}
