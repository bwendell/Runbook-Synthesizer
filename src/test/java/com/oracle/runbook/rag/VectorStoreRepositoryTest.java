package com.oracle.runbook.rag;

import com.oracle.runbook.domain.RunbookChunk;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the VectorStoreRepository interface contract.
 */
class VectorStoreRepositoryTest {

    @Test
    @DisplayName("store accepts a RunbookChunk")
    void store_acceptsRunbookChunk() {
        // Arrange
        VectorStoreRepository repository = new TestVectorStoreRepository();
        RunbookChunk chunk = createTestChunk("chunk-001");
        
        // Act & Assert - should not throw
        assertDoesNotThrow(() -> repository.store(chunk));
    }

    @Test
    @DisplayName("storeBatch accepts list of RunbookChunks")
    void storeBatch_acceptsChunkList() {
        // Arrange
        VectorStoreRepository repository = new TestVectorStoreRepository();
        List<RunbookChunk> chunks = List.of(
            createTestChunk("chunk-001"),
            createTestChunk("chunk-002")
        );
        
        // Act & Assert - should not throw
        assertDoesNotThrow(() -> repository.storeBatch(chunks));
    }

    @Test
    @DisplayName("search accepts embedding and topK, returns List<RunbookChunk>")
    void search_acceptsEmbeddingAndTopK_returnsChunks() {
        // Arrange
        VectorStoreRepository repository = new TestVectorStoreRepository();
        float[] queryEmbedding = new float[768];
        int topK = 5;
        
        // Act
        List<RunbookChunk> results = repository.search(queryEmbedding, topK);
        
        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());
    }

    @Test
    @DisplayName("delete removes all chunks for a runbook path")
    void delete_removesChunksForPath() {
        // Arrange
        VectorStoreRepository repository = new TestVectorStoreRepository();
        String runbookPath = "runbooks/memory/high-memory.md";
        
        // Act & Assert - should not throw
        assertDoesNotThrow(() -> repository.delete(runbookPath));
    }

    private RunbookChunk createTestChunk(String id) {
        return new RunbookChunk(
            id,
            "runbooks/memory/high-memory.md",
            "Step 1: Check memory",
            "Run free -h to check memory",
            List.of("memory"),
            List.of("VM.*"),
            new float[768]
        );
    }

    /**
     * Test implementation of VectorStoreRepository for verifying interface contract.
     */
    private static class TestVectorStoreRepository implements VectorStoreRepository {
        @Override
        public void store(RunbookChunk chunk) {
            // No-op for test
        }

        @Override
        public void storeBatch(List<RunbookChunk> chunks) {
            // No-op for test
        }

        @Override
        public List<RunbookChunk> search(float[] queryEmbedding, int topK) {
            return List.of(
                createTestChunk("chunk-001"),
                createTestChunk("chunk-002")
            );
        }

        @Override
        public void delete(String runbookPath) {
            // No-op for test
        }

        private RunbookChunk createTestChunk(String id) {
            return new RunbookChunk(
                id,
                "runbooks/memory/high-memory.md",
                "Step 1: Check memory",
                "Run free -h to check memory",
                List.of("memory"),
                List.of("VM.*"),
                new float[768]
            );
        }
    }
}
