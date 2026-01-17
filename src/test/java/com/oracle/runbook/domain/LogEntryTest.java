package com.oracle.runbook.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LogEntry} record.
 */
class LogEntryTest {

	@Test
	@DisplayName("LogEntry construction with all fields succeeds")
	void constructionWithAllFieldsSucceeds() {
		Instant now = Instant.now();
		Map<String, String> metadata = Map.of("source", "syslog", "host", "web-01");

		LogEntry entry = new LogEntry("log-123", now, "ERROR", "Connection refused to database server", metadata);

		assertEquals("log-123", entry.id());
		assertEquals(now, entry.timestamp());
		assertEquals("ERROR", entry.level());
		assertEquals("Connection refused to database server", entry.message());
		assertEquals(metadata, entry.metadata());
	}

	@Test
	@DisplayName("LogEntry throws NullPointerException for null id")
	void throwsForNullId() {
		assertThrows(NullPointerException.class, () -> new LogEntry(null, Instant.now(), "INFO", "message", Map.of()));
	}

	@Test
	@DisplayName("LogEntry throws NullPointerException for null timestamp")
	void throwsForNullTimestamp() {
		assertThrows(NullPointerException.class, () -> new LogEntry("id", null, "INFO", "message", Map.of()));
	}

	@Test
	@DisplayName("LogEntry metadata map is immutable")
	void metadataMapIsImmutable() {
		Map<String, String> mutableMetadata = new HashMap<>();
		mutableMetadata.put("key", "value");

		LogEntry entry = new LogEntry("log-123", Instant.now(), "INFO", "message", mutableMetadata);

		// Modifying original should not affect entry
		mutableMetadata.put("newKey", "newValue");
		assertFalse(entry.metadata().containsKey("newKey"));

		// Entry's metadata should be unmodifiable
		assertThrows(UnsupportedOperationException.class, () -> entry.metadata().put("another", "value"));
	}

	@Test
	@DisplayName("LogEntry accepts various log levels")
	void acceptsVariousLogLevels() {
		Instant now = Instant.now();

		assertDoesNotThrow(() -> new LogEntry("1", now, "DEBUG", "msg", Map.of()));
		assertDoesNotThrow(() -> new LogEntry("2", now, "INFO", "msg", Map.of()));
		assertDoesNotThrow(() -> new LogEntry("3", now, "WARN", "msg", Map.of()));
		assertDoesNotThrow(() -> new LogEntry("4", now, "ERROR", "msg", Map.of()));
	}
}
