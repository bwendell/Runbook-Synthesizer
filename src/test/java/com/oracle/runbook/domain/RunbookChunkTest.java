package com.oracle.runbook.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RunbookChunk} record.
 */
class RunbookChunkTest {

    @Test
    @DisplayName("RunbookChunk construction with all fields succeeds")
    void constructionWithAllFieldsSucceeds() {
        float[] embedding = new float[]{0.1f, 0.2f, 0.3f};
        List<String> tags = List.of("memory", "oom", "linux");
        List<String> shapes = List.of("VM.*", "BM.*");

        RunbookChunk chunk = new RunbookChunk(
            "chunk-123",
            "runbooks/memory/high-memory.md",
            "Step 3: Check for OOM",
            "Run dmesg to check for OOM killer events...",
            tags,
            shapes,
            embedding
        );

        assertEquals("chunk-123", chunk.id());
        assertEquals("runbooks/memory/high-memory.md", chunk.runbookPath());
        assertEquals("Step 3: Check for OOM", chunk.sectionTitle());
        assertEquals("Run dmesg to check for OOM killer events...", chunk.content());
        assertEquals(tags, chunk.tags());
        assertEquals(shapes, chunk.applicableShapes());
        assertArrayEquals(embedding, chunk.embedding());
    }

    @Test
    @DisplayName("RunbookChunk throws NullPointerException for null id")
    void throwsForNullId() {
        assertThrows(NullPointerException.class, () -> new RunbookChunk(
            null, "path", "title", "content",
            List.of(), List.of(), new float[0]
        ));
    }

    @Test
    @DisplayName("RunbookChunk embedding array is defensively copied")
    void embeddingArrayIsDefensivelyCopied() {
        float[] originalEmbedding = new float[]{0.1f, 0.2f, 0.3f};

        RunbookChunk chunk = new RunbookChunk(
            "id", "path", "title", "content",
            List.of(), List.of(), originalEmbedding
        );

        // Mutating original should not affect chunk
        originalEmbedding[0] = 999.0f;
        assertEquals(0.1f, chunk.embedding()[0]);

        // Mutating returned array should not affect chunk
        float[] returnedEmbedding = chunk.embedding();
        returnedEmbedding[0] = 888.0f;
        assertEquals(0.1f, chunk.embedding()[0]);
    }

    @Test
    @DisplayName("RunbookChunk tags list is immutable")
    void tagsListIsImmutable() {
        List<String> mutableTags = new ArrayList<>();
        mutableTags.add("memory");

        RunbookChunk chunk = new RunbookChunk(
            "id", "path", "title", "content",
            mutableTags, List.of(), new float[0]
        );

        // Modifying original should not affect chunk
        mutableTags.add("cpu");
        assertEquals(1, chunk.tags().size());

        // Chunk's list should be unmodifiable
        assertThrows(UnsupportedOperationException.class, 
            () -> chunk.tags().add("newTag"));
    }

    @Test
    @DisplayName("RunbookChunk applicableShapes list is immutable")
    void applicableShapesListIsImmutable() {
        List<String> mutableShapes = new ArrayList<>();
        mutableShapes.add("VM.*");

        RunbookChunk chunk = new RunbookChunk(
            "id", "path", "title", "content",
            List.of(), mutableShapes, new float[0]
        );

        // Modifying original should not affect chunk
        mutableShapes.add("BM.*");
        assertEquals(1, chunk.applicableShapes().size());

        // Chunk's list should be unmodifiable
        assertThrows(UnsupportedOperationException.class, 
            () -> chunk.applicableShapes().add("GPU.*"));
    }
}
