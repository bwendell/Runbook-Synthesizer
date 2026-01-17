package com.oracle.runbook.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RetrievedChunk} record.
 */
class RetrievedChunkTest {

	private RunbookChunk createTestChunk() {
		return new RunbookChunk("chunk-123", "runbooks/memory/high-memory.md", "Step 1", "content", List.of("memory"),
				List.of("VM.*"), new float[]{0.1f, 0.2f});
	}

	@Test
	@DisplayName("RetrievedChunk construction with valid data succeeds")
	void constructionWithValidDataSucceeds() {
		RunbookChunk chunk = createTestChunk();

		RetrievedChunk retrieved = new RetrievedChunk(chunk, 0.95, 0.1, 1.05);

		assertEquals(chunk, retrieved.chunk());
		assertEquals(0.95, retrieved.similarityScore());
		assertEquals(0.1, retrieved.metadataBoost());
		assertEquals(1.05, retrieved.finalScore());
	}

	@Test
	@DisplayName("RetrievedChunk throws NullPointerException for null chunk")
	void throwsForNullChunk() {
		assertThrows(NullPointerException.class, () -> new RetrievedChunk(null, 0.9, 0.1, 1.0));
	}

	@Test
	@DisplayName("RetrievedChunk allows various score values")
	void allowsVariousScoreValues() {
		RunbookChunk chunk = createTestChunk();

		// Zero scores
		assertDoesNotThrow(() -> new RetrievedChunk(chunk, 0.0, 0.0, 0.0));

		// Perfect similarity
		assertDoesNotThrow(() -> new RetrievedChunk(chunk, 1.0, 0.0, 1.0));

		// Negative boost (penalty)
		assertDoesNotThrow(() -> new RetrievedChunk(chunk, 0.8, -0.1, 0.7));
	}
}
