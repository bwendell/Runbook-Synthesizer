package com.oracle.runbook.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link RunbookChunk} record. */
class RunbookChunkTest {

  @Test
  @DisplayName("RunbookChunk construction with all fields succeeds")
  void constructionWithAllFieldsSucceeds() {
    float[] embedding = new float[] {0.1f, 0.2f, 0.3f};
    List<String> tags = List.of("memory", "oom", "linux");
    List<String> shapes = List.of("VM.*", "BM.*");

    RunbookChunk chunk =
        new RunbookChunk(
            "chunk-123",
            "runbooks/memory/high-memory.md",
            "Step 3: Check for OOM",
            "Run dmesg to check for OOM killer events...",
            tags,
            shapes,
            embedding);

    assertThat(chunk.id()).isEqualTo("chunk-123");
    assertThat(chunk.runbookPath()).isEqualTo("runbooks/memory/high-memory.md");
    assertThat(chunk.sectionTitle()).isEqualTo("Step 3: Check for OOM");
    assertThat(chunk.content()).isEqualTo("Run dmesg to check for OOM killer events...");
    assertThat(chunk.tags()).isEqualTo(tags);
    assertThat(chunk.applicableShapes()).isEqualTo(shapes);
    assertThat(chunk.embedding()).containsExactly(embedding);
  }

  @Test
  @DisplayName("RunbookChunk throws NullPointerException for null id")
  void throwsForNullId() {
    assertThatThrownBy(
            () ->
                new RunbookChunk(
                    null, "path", "title", "content", List.of(), List.of(), new float[0]))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("RunbookChunk embedding array is defensively copied")
  void embeddingArrayIsDefensivelyCopied() {
    float[] originalEmbedding = new float[] {0.1f, 0.2f, 0.3f};

    RunbookChunk chunk =
        new RunbookChunk("id", "path", "title", "content", List.of(), List.of(), originalEmbedding);

    // Mutating original should not affect chunk
    originalEmbedding[0] = 999.0f;
    assertThat(chunk.embedding()[0]).isEqualTo(0.1f);

    // Mutating returned array should not affect chunk
    float[] returnedEmbedding = chunk.embedding();
    returnedEmbedding[0] = 888.0f;
    assertThat(chunk.embedding()[0]).isEqualTo(0.1f);
  }

  @Test
  @DisplayName("RunbookChunk tags list is immutable")
  void tagsListIsImmutable() {
    List<String> mutableTags = new ArrayList<>();
    mutableTags.add("memory");

    RunbookChunk chunk =
        new RunbookChunk("id", "path", "title", "content", mutableTags, List.of(), new float[0]);

    // Modifying original should not affect chunk
    mutableTags.add("cpu");
    assertThat(chunk.tags()).hasSize(1);

    // Chunk's list should be unmodifiable
    assertThatThrownBy(() -> chunk.tags().add("newTag"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("RunbookChunk applicableShapes list is immutable")
  void applicableShapesListIsImmutable() {
    List<String> mutableShapes = new ArrayList<>();
    mutableShapes.add("VM.*");

    RunbookChunk chunk =
        new RunbookChunk("id", "path", "title", "content", List.of(), mutableShapes, new float[0]);

    // Modifying original should not affect chunk
    mutableShapes.add("BM.*");
    assertThat(chunk.applicableShapes()).hasSize(1);

    // Chunk's list should be unmodifiable
    assertThatThrownBy(() -> chunk.applicableShapes().add("GPU.*"))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
