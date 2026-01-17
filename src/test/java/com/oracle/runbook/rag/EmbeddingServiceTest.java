package com.oracle.runbook.rag;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the EmbeddingService interface contract.
 */
class EmbeddingServiceTest {

	@Test
	@DisplayName("embed accepts text and returns CompletableFuture<float[]>")
	void embed_acceptsText_returnsEmbeddingFuture() throws Exception {
		// Arrange
		EmbeddingService service = new TestEmbeddingService();
		String text = "High memory utilization on server";

		// Act
		CompletableFuture<float[]> future = service.embed(text);
		float[] embedding = future.get();

		// Assert
		assertNotNull(future);
		assertNotNull(embedding);
		assertEquals(768, embedding.length);
	}

	@Test
	@DisplayName("embedBatch accepts list of texts and returns CompletableFuture<List<float[]>>")
	void embedBatch_acceptsTextList_returnsEmbeddingsFuture() throws Exception {
		// Arrange
		EmbeddingService service = new TestEmbeddingService();
		List<String> texts = List.of("Memory issue", "CPU spike", "Disk full");

		// Act
		CompletableFuture<List<float[]>> future = service.embedBatch(texts);
		List<float[]> embeddings = future.get();

		// Assert
		assertNotNull(future);
		assertNotNull(embeddings);
		assertEquals(3, embeddings.size());
		assertEquals(768, embeddings.get(0).length);
	}

	/**
	 * Test implementation of EmbeddingService for verifying interface contract.
	 */
	private static class TestEmbeddingService implements EmbeddingService {
		@Override
		public CompletableFuture<float[]> embed(String text) {
			float[] embedding = new float[768];
			for (int i = 0; i < 768; i++) {
				embedding[i] = 0.1f;
			}
			return CompletableFuture.completedFuture(embedding);
		}

		@Override
		public CompletableFuture<List<float[]>> embedBatch(List<String> texts) {
			return CompletableFuture.completedFuture(texts.stream().map(t -> {
				float[] embedding = new float[768];
				for (int i = 0; i < 768; i++) {
					embedding[i] = 0.1f;
				}
				return embedding;
			}).toList());
		}
	}
}
