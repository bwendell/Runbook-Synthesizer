package com.oracle.runbook.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link LogEntry} record. */
class LogEntryTest {

  @Test
  @DisplayName("LogEntry construction with all fields succeeds")
  void constructionWithAllFieldsSucceeds() {
    Instant now = Instant.now();
    Map<String, String> metadata = Map.of("source", "syslog", "host", "web-01");

    LogEntry entry =
        new LogEntry("log-123", now, "ERROR", "Connection refused to database server", metadata);

    assertThat(entry.id()).isEqualTo("log-123");
    assertThat(entry.timestamp()).isEqualTo(now);
    assertThat(entry.level()).isEqualTo("ERROR");
    assertThat(entry.message()).isEqualTo("Connection refused to database server");
    assertThat(entry.metadata()).isEqualTo(metadata);
  }

  @Test
  @DisplayName("LogEntry throws NullPointerException for null id")
  void throwsForNullId() {
    assertThatThrownBy(() -> new LogEntry(null, Instant.now(), "INFO", "message", Map.of()))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("LogEntry throws NullPointerException for null timestamp")
  void throwsForNullTimestamp() {
    assertThatThrownBy(() -> new LogEntry("id", null, "INFO", "message", Map.of()))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("LogEntry metadata map is immutable")
  void metadataMapIsImmutable() {
    Map<String, String> mutableMetadata = new HashMap<>();
    mutableMetadata.put("key", "value");

    LogEntry entry = new LogEntry("log-123", Instant.now(), "INFO", "message", mutableMetadata);

    // Modifying original should not affect entry
    mutableMetadata.put("newKey", "newValue");
    assertThat(entry.metadata()).doesNotContainKey("newKey");

    // Entry's metadata should be unmodifiable
    assertThatThrownBy(() -> entry.metadata().put("another", "value"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("LogEntry accepts various log levels")
  void acceptsVariousLogLevels() {
    Instant now = Instant.now();

    assertThatCode(() -> new LogEntry("1", now, "DEBUG", "msg", Map.of()))
        .doesNotThrowAnyException();
    assertThatCode(() -> new LogEntry("2", now, "INFO", "msg", Map.of()))
        .doesNotThrowAnyException();
    assertThatCode(() -> new LogEntry("3", now, "WARN", "msg", Map.of()))
        .doesNotThrowAnyException();
    assertThatCode(() -> new LogEntry("4", now, "ERROR", "msg", Map.of()))
        .doesNotThrowAnyException();
  }
}
