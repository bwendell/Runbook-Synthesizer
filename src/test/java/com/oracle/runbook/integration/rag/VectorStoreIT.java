package com.oracle.runbook.integration.rag;

import static org.assertj.core.api.Assertions.assertThat;

import com.oracle.runbook.domain.RunbookChunk;
import com.oracle.runbook.integration.IntegrationTestBase;
import com.oracle.runbook.rag.ScoredChunk;
import com.oracle.runbook.rag.VectorStoreRepository;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.WebServerConfig;
import io.helidon.webserver.testing.junit5.SetUpServer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for VectorStoreRepository operations.
 *
 * <p>
 * Tests store, retrieve, and search functionality using an in-memory
 * implementation.
 */
class VectorStoreIT extends IntegrationTestBase {

    private InMemoryVectorStore vectorStore;

    VectorStoreIT(WebServer server) {
        super(server);
    }

    @SetUpServer
    static void setup(WebServerConfig.Builder builder) {
        builder.routing(routing -> routing.get("/health", (req, res) -> res.send("OK")));
    }

    @BeforeEach
    void setUp() {
        resetWireMock();
        vectorStore = new InMemoryVectorStore();
    }

    @Test
    void store_ThenSearch_ReturnsMatchingChunk() {
        // Given: A runbook chunk with embedding
        float[] embedding = { 0.1f, 0.2f, 0.3f, 0.4f };
        RunbookChunk chunk = new RunbookChunk(
                "chunk-001",
                "runbooks/memory-troubleshooting.md",
                "Memory Investigation",
                "Check free memory with: free -h",
                List.of("memory", "linux"),
                List.of("VM.*"),
                embedding);

        // When: Store the chunk
        vectorStore.store(chunk);

        // When: Search with similar embedding
        float[] queryEmbedding = { 0.1f, 0.2f, 0.3f, 0.4f };
        List<ScoredChunk> results = vectorStore.search(queryEmbedding, 5);

        // Then: Chunk is returned with high similarity score
        assertThat(results).hasSize(1);
        assertThat(results.get(0).chunk().id()).isEqualTo("chunk-001");
        assertThat(results.get(0).similarityScore()).isGreaterThan(0.9);
    }

    @Test
    void storeBatch_ThenSearch_ReturnsTopKChunks() {
        // Given: Multiple runbook chunks
        List<RunbookChunk> chunks = List.of(
                createChunk("chunk-cpu-1", "CPU troubleshooting", new float[] { 0.8f, 0.1f, 0.0f, 0.0f }),
                createChunk("chunk-mem-1", "Memory troubleshooting", new float[] { 0.1f, 0.8f, 0.0f, 0.0f }),
                createChunk("chunk-disk-1", "Disk troubleshooting", new float[] { 0.0f, 0.1f, 0.8f, 0.0f }));

        // When: Store batch
        vectorStore.storeBatch(chunks);

        // When: Search for memory-related content
        float[] memoryQuery = { 0.1f, 0.9f, 0.0f, 0.0f };
        List<ScoredChunk> results = vectorStore.search(memoryQuery, 2);

        // Then: Memory chunk is top result
        assertThat(results).hasSize(2);
        assertThat(results.get(0).chunk().id()).isEqualTo("chunk-mem-1");
    }

    @Test
    void delete_RemovesChunksByRunbookPath() {
        // Given: Chunks from two different runbooks
        vectorStore.store(
                createChunk("chunk-a1", "Runbook A content", new float[] { 0.5f, 0.5f, 0.0f, 0.0f }));
        vectorStore.store(
                createChunk("chunk-b1", "Runbook B content", new float[] { 0.5f, 0.5f, 0.0f, 0.0f }));

        // Verify both are stored
        assertThat(vectorStore.search(new float[] { 0.5f, 0.5f, 0.0f, 0.0f }, 10)).hasSize(2);

        // When: Delete runbook A chunks
        vectorStore.delete("runbooks/runbook-a.md");

        // Then: Only runbook B chunks remain
        List<ScoredChunk> results = vectorStore.search(new float[] { 0.5f, 0.5f, 0.0f, 0.0f }, 10);
        assertThat(results).hasSize(1);
    }

    private RunbookChunk createChunk(String id, String content, float[] embedding) {
        String runbookPath = id.startsWith("chunk-a") ? "runbooks/runbook-a.md" : "runbooks/runbook-b.md";
        return new RunbookChunk(id, runbookPath, "Section", content, List.of(), List.of(), embedding);
    }

    /** In-memory vector store for testing. Uses cosine similarity for search. */
    private static class InMemoryVectorStore implements VectorStoreRepository {
        private final Map<String, RunbookChunk> chunks = new HashMap<>();

        @Override
        public void store(RunbookChunk chunk) {
            chunks.put(chunk.id(), chunk);
        }

        @Override
        public void storeBatch(List<RunbookChunk> chunkList) {
            chunkList.forEach(this::store);
        }

        @Override
        public List<ScoredChunk> search(float[] queryEmbedding, int topK) {
            List<ScoredChunk> scored = new ArrayList<>();
            for (RunbookChunk chunk : chunks.values()) {
                double score = cosineSimilarity(queryEmbedding, chunk.embedding());
                scored.add(new ScoredChunk(chunk, score));
            }
            scored.sort((a, b) -> Double.compare(b.similarityScore(), a.similarityScore()));
            return scored.subList(0, Math.min(topK, scored.size()));
        }

        @Override
        public void delete(String runbookPath) {
            chunks.entrySet().removeIf(e -> e.getValue().runbookPath().equals(runbookPath));
        }

        private double cosineSimilarity(float[] a, float[] b) {
            double dotProduct = 0.0;
            double normA = 0.0;
            double normB = 0.0;
            for (int i = 0; i < Math.min(a.length, b.length); i++) {
                dotProduct += a[i] * b[i];
                normA += a[i] * a[i];
                normB += b[i] * b[i];
            }
            return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
        }
    }
}
