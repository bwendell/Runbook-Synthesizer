package com.oracle.runbook.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oracle.runbook.domain.RunbookChunk;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ScoredChunk}.
 *
 * <p>Tests constructor validation and accessor behavior for the vector search result record.
 */
class ScoredChunkTest {

  @Nested
  @DisplayName("Constructor validation")
  class ConstructorValidationTests {

    @Test
    @DisplayName("Constructor rejects null chunk with NullPointerException")
    void constructorRejectsNullChunk() {
      assertThatThrownBy(() -> new ScoredChunk(null, 0.9))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("chunk");
    }

    @Test
    @DisplayName("Constructor accepts valid chunk and score")
    void constructorAcceptsValidChunk() {
      RunbookChunk chunk = createTestChunk();

      ScoredChunk scored = new ScoredChunk(chunk, 0.85);

      assertThat(scored).isNotNull();
      assertThat(scored.chunk()).isEqualTo(chunk);
      assertThat(scored.similarityScore()).isEqualTo(0.85);
    }
  }

  @Nested
  @DisplayName("Accessor methods")
  class AccessorTests {

    @Test
    @DisplayName("chunk() returns the correct chunk")
    void accessorsReturnCorrectChunk() {
      RunbookChunk chunk = createTestChunk();
      ScoredChunk scored = new ScoredChunk(chunk, 0.75);

      assertThat(scored.chunk()).isNotNull().isEqualTo(chunk);
    }

    @Test
    @DisplayName("similarityScore() returns the correct score")
    void accessorsReturnCorrectScore() {
      RunbookChunk chunk = createTestChunk();
      double expectedScore = 0.92;
      ScoredChunk scored = new ScoredChunk(chunk, expectedScore);

      assertThat(scored.similarityScore()).isEqualTo(expectedScore);
    }

    @Test
    @DisplayName("Accessors return expected values for various scores")
    void accessorsReturnCorrectValues() {
      RunbookChunk chunk = createTestChunk();

      // Test with various valid scores
      ScoredChunk lowScore = new ScoredChunk(chunk, 0.0);
      assertThat(lowScore.similarityScore()).isEqualTo(0.0);

      ScoredChunk highScore = new ScoredChunk(chunk, 1.0);
      assertThat(highScore.similarityScore()).isEqualTo(1.0);

      ScoredChunk midScore = new ScoredChunk(chunk, 0.5);
      assertThat(midScore.similarityScore()).isEqualTo(0.5);
    }
  }

  /**
   * Creates a minimal test RunbookChunk for testing.
   *
   * @return a valid RunbookChunk instance
   */
  private RunbookChunk createTestChunk() {
    return new RunbookChunk(
        "chunk-id-1",
        "runbooks/troubleshooting.md",
        "Memory Troubleshooting",
        "Check memory usage with `free -h`",
        List.of("memory", "linux"),
        List.of("VM.Standard.*"),
        new float[] {0.1f, 0.2f, 0.3f});
  }
}
