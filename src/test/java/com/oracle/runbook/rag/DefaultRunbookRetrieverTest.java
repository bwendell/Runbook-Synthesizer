package com.oracle.runbook.rag;

import com.oracle.runbook.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DefaultRunbookRetriever implementation.
 */
class DefaultRunbookRetrieverTest {

	private DefaultRunbookRetriever retriever;
	private StubEmbeddingService embeddingService;
	private StubVectorStoreRepository vectorStore;

	@BeforeEach
	void setUp() {
		embeddingService = new StubEmbeddingService();
		vectorStore = new StubVectorStoreRepository();
		retriever = new DefaultRunbookRetriever(embeddingService, vectorStore);
	}

	@Test
	@DisplayName("retrieve embeds context and searches vector store")
	void retrieve_embedsContextAndSearches() {
		// Arrange
		EnrichedContext context = createTestContext("High Memory", "Memory is at 95%", "VM.Standard2.1");
		float[] queryEmbedding = new float[] { 0.1f };
		embeddingService.setNextEmbedding(queryEmbedding);

		RunbookChunk chunk = createTestChunk("c1", "Check memory", List.of(), List.of());
		vectorStore.setSearchResults(List.of(new ScoredChunk(chunk, 0.9)));

		// Act
		List<RetrievedChunk> results = retriever.retrieve(context, 1);

		// Assert
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals("c1", results.get(0).chunk().id());
		// Verify it used the context (stub captures it)
		assertNotNull(embeddingService.lastContextEmbedded);
		assertEquals("High Memory", embeddingService.lastContextEmbedded.alert().title());
	}

	@Test
	@DisplayName("retrieve applies metadata boost for matching tags")
	void retrieve_appliesTagBoost() {
		// Arrange
		EnrichedContext context = createTestContext("High Memory", "Memory issue", "VM.Standard2.1");
		// Alert has dimensions/labels that could match tags.
		// Let's assume tags "memory" and "oom" match.
		
		RunbookChunk chunkWithTags = createTestChunk("c1", "Memory fix", List.of("memory", "oom"), List.of());
		RunbookChunk chunkWithoutTags = createTestChunk("c2", "General fix", List.of("network"), List.of());
		
		vectorStore.setSearchResults(List.of(
				new ScoredChunk(chunkWithTags, 0.5), 
				new ScoredChunk(chunkWithoutTags, 0.5)));
		// Mock similarity scores as 0.5 for both
		
		// Act
		List<RetrievedChunk> results = retriever.retrieve(context, 2);

		// Assert
		assertEquals(2, results.size());
		// c1 should have higher final score due to tag boost
		assertTrue(results.get(0).finalScore() > results.get(1).finalScore(), 
				"Chunk with matching tags should have higher score");
		assertEquals("c1", results.get(0).chunk().id());
	}

	@Test
	@DisplayName("retrieve applies metadata boost for matching shape patterns")
	void retrieve_appliesShapeBoost() {
		// Arrange
		EnrichedContext context = createTestContext("Compute slow", "CPU spike", "VM.Standard2.1");
		
		RunbookChunk chunkMatchShape = createTestChunk("c1", "VM fix", List.of(), List.of("VM.*"));
		RunbookChunk chunkNoMatchShape = createTestChunk("c2", "BM fix", List.of(), List.of("BM.*"));
		
		vectorStore.setSearchResults(List.of(
				new ScoredChunk(chunkMatchShape, 0.6), 
				new ScoredChunk(chunkNoMatchShape, 0.6)));

		// Act
		List<RetrievedChunk> results = retriever.retrieve(context, 2);

		// Assert
		assertEquals(2, results.size());
		assertTrue(results.get(0).metadataBoost() > results.get(1).metadataBoost(),
				"Matching shape should get boost");
		assertEquals("c1", results.get(0).chunk().id());
	}

	private EnrichedContext createTestContext(String title, String message, String shape) {
		Alert alert = new Alert("a1", title, message, AlertSeverity.CRITICAL, "oci",
				Map.of("resourceId", "r1"), Map.of("app", "web"), Instant.now(), "{}");
		ResourceMetadata resource = new ResourceMetadata("r1", "web01", "comp1", shape, "AD1", Map.of(), Map.of());
		return new EnrichedContext(alert, resource, List.of(), List.of(), Map.of());
	}

	private RunbookChunk createTestChunk(String id, String content, List<String> tags, List<String> shapes) {
		return new RunbookChunk(id, "path", "Title", content, tags, shapes, new float[] { 0.1f });
	}

	private static class StubEmbeddingService implements EmbeddingService {
		float[] nextEmbedding;
		EnrichedContext lastContextEmbedded;

		void setNextEmbedding(float[] e) { this.nextEmbedding = e; }

		@Override
		public CompletableFuture<float[]> embed(String text) { return CompletableFuture.completedFuture(new float[768]); }

		@Override
		public CompletableFuture<List<float[]>> embedBatch(List<String> texts) {
			return CompletableFuture.completedFuture(texts.stream().map(t -> new float[768]).toList());
		}

		@Override
		public CompletableFuture<float[]> embedContext(EnrichedContext context) {
			this.lastContextEmbedded = context;
			return CompletableFuture.completedFuture(nextEmbedding);
		}
	}

	private static class StubVectorStoreRepository implements VectorStoreRepository {
		List<ScoredChunk> results = List.of();

		void setSearchResults(List<ScoredChunk> r) { this.results = r; }

		@Override public void store(RunbookChunk chunk) {}
		@Override public void storeBatch(List<RunbookChunk> chunks) {}
		@Override public void delete(String path) {}

		@Override
		public List<ScoredChunk> search(float[] embedding, int topK) {
			return results;
		}
	}
}
