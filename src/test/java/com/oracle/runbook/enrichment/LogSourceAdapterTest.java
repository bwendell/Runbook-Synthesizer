package com.oracle.runbook.enrichment;

import com.oracle.runbook.domain.LogEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the LogSourceAdapter interface contract.
 */
class LogSourceAdapterTest {

    @Test
    @DisplayName("Mock implementation returns expected source type identifier")
    void sourceType_returnsExpectedIdentifier() {
        // Arrange
        LogSourceAdapter adapter = new TestLogSourceAdapter("oci-logging");
        
        // Act
        String sourceType = adapter.sourceType();
        
        // Assert
        assertEquals("oci-logging", sourceType);
    }

    @Test
    @DisplayName("fetchLogs accepts resourceId, lookback, and query, returns CompletableFuture<List<LogEntry>>")
    void fetchLogs_acceptsParameters_returnsCompletableFuture() throws Exception {
        // Arrange
        LogSourceAdapter adapter = new TestLogSourceAdapter("loki");
        String resourceId = "ocid1.instance.oc1..example";
        Duration lookback = Duration.ofMinutes(30);
        String query = "{job=\"varlogs\"}";
        
        // Act
        CompletableFuture<List<LogEntry>> future = adapter.fetchLogs(resourceId, lookback, query);
        List<LogEntry> logs = future.get();
        
        // Assert
        assertNotNull(future);
        assertNotNull(logs);
        assertEquals(1, logs.size());
        assertEquals("ERROR", logs.get(0).level());
    }

    /**
     * Test implementation of LogSourceAdapter for verifying interface contract.
     */
    private static class TestLogSourceAdapter implements LogSourceAdapter {
        private final String sourceType;

        TestLogSourceAdapter(String sourceType) {
            this.sourceType = sourceType;
        }

        @Override
        public String sourceType() {
            return sourceType;
        }

        @Override
        public CompletableFuture<List<LogEntry>> fetchLogs(String resourceId, Duration lookback, String query) {
            LogEntry entry = new LogEntry(
                "log-001",
                Instant.now(),
                "ERROR",
                "Out of memory error occurred",
                Map.of("host", "server-1")
            );
            return CompletableFuture.completedFuture(List.of(entry));
        }
    }
}
