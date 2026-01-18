package com.oracle.runbook.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link RetrievedChunk} record. */
class RetrievedChunkTest {

  private RunbookChunk createTestChunk() {
    return new RunbookChunk(
        "chunk-123",
        "runbooks/memory/high-memory.md",
        "Step 1",
        "content",
        List.of("memory"),
        List.of("VM.*"),
        new float[] {0.1f, 0.2f});
  }

  @Test
  @DisplayName("RetrievedChunk construction with valid data succeeds")
  void constructionWithValidDataSucceeds() {
    RunbookChunk chunk = createTestChunk();

    RetrievedChunk retrieved = new RetrievedChunk(chunk, 0.95, 0.1, 1.05);

    assertThat(retrieved.chunk()).isEqualTo(chunk);
    assertThat(retrieved.similarityScore()).isEqualTo(0.95);
    assertThat(retrieved.metadataBoost()).isEqualTo(0.1);
    assertThat(retrieved.finalScore()).isEqualTo(1.05);
  }

  @Test
  @DisplayName("RetrievedChunk throws NullPointerException for null chunk")
  void throwsForNullChunk() {
    assertThatThrownBy(() -> new RetrievedChunk(null, 0.9, 0.1, 1.0))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("RetrievedChunk allows various score values")
  void allowsVariousScoreValues() {
    RunbookChunk chunk = createTestChunk();

    // Zero scores
    assertThatCode(() -> new RetrievedChunk(chunk, 0.0, 0.0, 0.0)).doesNotThrowAnyException();

    // Perfect similarity
    assertThatCode(() -> new RetrievedChunk(chunk, 1.0, 0.0, 1.0)).doesNotThrowAnyException();

    // Negative boost (penalty)
    assertThatCode(() -> new RetrievedChunk(chunk, 0.8, -0.1, 0.7)).doesNotThrowAnyException();
  }
}
