package com.oracle.runbook.integration.rag;

import static org.assertj.core.api.Assertions.assertThat;

import com.oracle.runbook.domain.*;
import com.oracle.runbook.integration.IntegrationTestBase;
import com.oracle.runbook.rag.RunbookRetriever;
import com.oracle.runbook.rag.ScoredChunk;
import com.oracle.runbook.rag.VectorStoreRepository;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.WebServerConfig;
import io.helidon.webserver.testing.junit5.SetUpServer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for runbook retrieval with top-K semantics.
 *
 * <p>
 * Tests verify that the retriever correctly ranks and returns chunks based on
 * similarity.
 */
class RetrievalIT extends IntegrationTestBase {

    private InMemoryVectorStore vectorStore;
    private TestRunbookRetriever retriever;

    RetrievalIT(WebServer server) {
        super(server);
    }

    @SetUpServer
    static void setup(WebServerConfig.Builder builder) {
        builder.routing(routing -> routing.get("/health", (req, res) -> res.send("OK")));
    }

    @BeforeEach
    void setUp() {
        vectorStore = new InMemoryVectorStore();
        retriever = new TestRunbookRetriever(vectorStore);
    }

    @Test
    void retrieve_WithSeededChunks_ReturnsTopK() {
        // Given: 10 runbook chunks with varied embeddings
        for (int i = 0; i < 10; i++) {
            float[] embedding = new float[4];
            embedding[i % 4] = 1.0f; // Each chunk emphasizes different dimension
            vectorStore.store(
                    new RunbookChunk(
                            "chunk-" + i,
                            "runbooks/test.md",
                            "Section " + i,
                            "Content " + i,
                            List.of("tag" + i),
                            List.of(),
                            embedding));
        }

        // When: Query for chunks similar to dimension 0
        EnrichedContext context = createTestContext();
        List<RetrievedChunk> results = retriever.retrieve(context, 3);

        // Then: Top 3 results returned
        assertThat(results).hasSize(3);

        // Then: Results are ordered by similarity (highest first)
        assertThat(results.get(0).finalScore()).isGreaterThanOrEqualTo(results.get(1).finalScore());
        assertThat(results.get(1).finalScore()).isGreaterThanOrEqualTo(results.get(2).finalScore());
    }

    @Test
    void retrieve_WithDifferentTopK_RespectsLimit() {
        // Given: 5 chunks
        for (int i = 0; i < 5; i++) {
            vectorStore.store(
                    new RunbookChunk(
                            "chunk-" + i,
                            "runbooks/test.md",
                            "Section",
                            "Content",
                            List.of(),
                            List.of(),
                            new float[] { 0.5f, 0.5f, 0.0f, 0.0f }));
        }

        // When: Query with topK = 2
        EnrichedContext context = createTestContext();
        List<RetrievedChunk> results = retriever.retrieve(context, 2);

        // Then: Only 2 results returned
        assertThat(results).hasSize(2);
    }

    @Test
    void retrieve_EmptyStore_ReturnsEmptyList() {
        // Given: Empty vector store

        // When: Query
        EnrichedContext context = createTestContext();
        List<RetrievedChunk> results = retriever.retrieve(context, 5);

        // Then: Empty list, not null
        assertThat(results).isEmpty();
    }

    private EnrichedContext createTestContext() {
        Alert alert = new Alert(
                "alert-001",
                "High Memory Usage",
                "Memory above threshold",
                AlertSeverity.WARNING,
                "oci-monitoring",
                Map.of("resourceId", "ocid1.instance.oc1..example"),
                Map.of(),
                Instant.now(),
                "{}");
        return new EnrichedContext(alert, null, List.of(), List.of(), Map.of());
    }

    /** Test implementation of RunbookRetriever using in-memory vector store. */
    private static class TestRunbookRetriever implements RunbookRetriever {
        private final InMemoryVectorStore vectorStore;

        TestRunbookRetriever(InMemoryVectorStore vectorStore) {
            this.vectorStore = vectorStore;
        }

        @Override
        public List<RetrievedChunk> retrieve(EnrichedContext context, int topK) {
            // Simulate embedding generation (fixed for test determinism)
            float[] queryEmbedding = { 1.0f, 0.1f, 0.1f, 0.1f };

            List<ScoredChunk> scored = vectorStore.search(queryEmbedding, topK);

            return scored.stream()
                    .map(
                            sc -> new RetrievedChunk(
                                    sc.chunk(), sc.similarityScore(), 0.0, sc.similarityScore()))
                    .toList();
        }
    }

    /** In-memory vector store for testing. */
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
