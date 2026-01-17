package com.oracle.runbook.rag;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Port interface for text embedding generation.
 * <p>
 * This interface defines the contract for the embedding service in the RAG
 * pipeline. It acts as a facade over
 * {@link LlmProvider#generateEmbedding(String)}, separating the embedding
 * concern from text generation.
 * <p>
 * Implementations may add caching, normalization, or other optimizations. All
 * methods are non-blocking and return CompletableFuture.
 *
 * @see LlmProvider
 */
public interface EmbeddingService {

	/**
	 * Generates an embedding vector for the given text.
	 * <p>
	 * Used for query embedding during RAG retrieval.
	 *
	 * @param text
	 *            the text to embed
	 * @return a CompletableFuture containing the embedding vector, never null
	 */
	CompletableFuture<float[]> embed(String text);

	/**
	 * Generates embedding vectors for multiple texts in a single batch.
	 * <p>
	 * More efficient than calling {@link #embed(String)} repeatedly during document
	 * ingestion.
	 *
	 * @param texts
	 *            the list of texts to embed
	 * @return a CompletableFuture containing the list of embedding vectors, in the
	 *         same order as the input texts, never null
	 */
	CompletableFuture<List<float[]>> embedBatch(List<String> texts);
}
