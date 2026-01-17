package com.oracle.runbook.rag;

import com.oracle.runbook.domain.GenerationConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DefaultEmbeddingService implementation.
 */
class DefaultEmbeddingServiceTest {

	@Test
	@DisplayName("embed delegates to LlmProvider.generateEmbedding")
	void embed_delegatesToLlmProvider() throws Exception {
		// Arrange
		float[] expectedEmbedding = new float[] { 0.1f, 0.2f, 0.3f };
		AtomicReference<String> capturedText = new AtomicReference<>();
		LlmProvider stubProvider = new StubLlmProvider() {
			@Override
			public CompletableFuture<float[]> generateEmbedding(String text) {
				capturedText.set(text);
				return CompletableFuture.completedFuture(expectedEmbedding);
			}
		};
		DefaultEmbeddingService embeddingService = new DefaultEmbeddingService(stubProvider);
		String text = "High memory utilization on server";

		// Act
		CompletableFuture<float[]> resultFuture = embeddingService.embed(text);
		float[] result = resultFuture.get();

		// Assert
		assertNotNull(result);
		assertArrayEquals(expectedEmbedding, result);
		assertEquals(text, capturedText.get());
	}

	@Test
	@DisplayName("embedBatch delegates to LlmProvider.generateEmbeddings")
	void embedBatch_delegatesToLlmProvider() throws Exception {
		// Arrange
		List<float[]> expectedEmbeddings = List.of(
				new float[] { 0.1f, 0.2f },
				new float[] { 0.3f, 0.4f },
				new float[] { 0.5f, 0.6f });
		AtomicReference<List<String>> capturedTexts = new AtomicReference<>();
		LlmProvider stubProvider = new StubLlmProvider() {
			@Override
			public CompletableFuture<List<float[]>> generateEmbeddings(List<String> texts) {
				capturedTexts.set(texts);
				return CompletableFuture.completedFuture(expectedEmbeddings);
			}
		};
		DefaultEmbeddingService embeddingService = new DefaultEmbeddingService(stubProvider);
		List<String> texts = List.of("Memory issue", "CPU spike", "Disk full");

		// Act
		CompletableFuture<List<float[]>> resultFuture = embeddingService.embedBatch(texts);
		List<float[]> result = resultFuture.get();

		// Assert
		assertNotNull(result);
		assertEquals(3, result.size());
		assertArrayEquals(expectedEmbeddings.get(0), result.get(0));
		assertEquals(texts, capturedTexts.get());
	}

	@Test
	@DisplayName("embed throws NullPointerException for null text")
	void embed_withNullText_throwsNullPointerException() {
		// Arrange
		LlmProvider stubProvider = new StubLlmProvider();
		DefaultEmbeddingService embeddingService = new DefaultEmbeddingService(stubProvider);

		// Act & Assert
		assertThrows(NullPointerException.class, () -> embeddingService.embed(null));
	}

	@Test
	@DisplayName("embedBatch throws NullPointerException for null list")
	void embedBatch_withNullList_throwsNullPointerException() {
		// Arrange
		LlmProvider stubProvider = new StubLlmProvider();
		DefaultEmbeddingService embeddingService = new DefaultEmbeddingService(stubProvider);

		// Act & Assert
		assertThrows(NullPointerException.class, () -> embeddingService.embedBatch(null));
	}

	@Test
	@DisplayName("embedBatch handles empty list")
	void embedBatch_withEmptyList_returnsEmptyList() throws Exception {
		// Arrange
		LlmProvider stubProvider = new StubLlmProvider() {
			@Override
			public CompletableFuture<List<float[]>> generateEmbeddings(List<String> texts) {
				return CompletableFuture.completedFuture(List.of());
			}
		};
		DefaultEmbeddingService embeddingService = new DefaultEmbeddingService(stubProvider);

		// Act
		CompletableFuture<List<float[]>> resultFuture = embeddingService.embedBatch(List.of());
		List<float[]> result = resultFuture.get();

		// Assert
		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	@DisplayName("constructor throws NullPointerException for null provider")
	void constructor_withNullProvider_throwsNullPointerException() {
		// Act & Assert
		assertThrows(NullPointerException.class, () -> new DefaultEmbeddingService(null));
	}

	/**
	 * Stub implementation of LlmProvider for testing.
	 */
	private static class StubLlmProvider implements LlmProvider {
		@Override
		public String providerId() {
			return "test-stub";
		}

		@Override
		public CompletableFuture<String> generateText(String prompt, GenerationConfig config) {
			return CompletableFuture.completedFuture("generated text");
		}

		@Override
		public CompletableFuture<float[]> generateEmbedding(String text) {
			return CompletableFuture.completedFuture(new float[] { 0.1f });
		}

		@Override
		public CompletableFuture<List<float[]>> generateEmbeddings(List<String> texts) {
			return CompletableFuture.completedFuture(List.of());
		}
	}
}
