package com.oracle.runbook.rag;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Default implementation of {@link EmbeddingService} that delegates to
 * {@link LlmProvider} for embedding generation.
 * <p>
 * This service provides a simplified facade over the LlmProvider's embedding
 * capabilities, decoupling embedding concerns from text generation concerns.
 *
 * @see EmbeddingService
 * @see LlmProvider
 */
public class DefaultEmbeddingService implements EmbeddingService {

	private final LlmProvider llmProvider;

	/**
	 * Creates a new DefaultEmbeddingService with the given LlmProvider.
	 *
	 * @param llmProvider
	 *            the LLM provider for embedding generation
	 * @throws NullPointerException
	 *             if llmProvider is null
	 */
	public DefaultEmbeddingService(LlmProvider llmProvider) {
		this.llmProvider = Objects.requireNonNull(llmProvider, "llmProvider cannot be null");
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws NullPointerException
	 *             if text is null
	 */
	@Override
	public CompletableFuture<float[]> embed(String text) {
		Objects.requireNonNull(text, "text cannot be null");
		return llmProvider.generateEmbedding(text);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws NullPointerException
	 *             if texts is null
	 */
	@Override
	public CompletableFuture<List<float[]>> embedBatch(List<String> texts) {
		Objects.requireNonNull(texts, "texts cannot be null");
		return llmProvider.generateEmbeddings(texts);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws NullPointerException
	 *             if context is null
	 */
	@Override
	public CompletableFuture<float[]> embedContext(com.oracle.runbook.domain.EnrichedContext context) {
		Objects.requireNonNull(context, "context cannot be null");
		String query = formatContextQuery(context);
		return llmProvider.generateEmbedding(query);
	}

	private String formatContextQuery(com.oracle.runbook.domain.EnrichedContext context) {
		StringBuilder sb = new StringBuilder();
		sb.append("Alert Title: ").append(context.alert().title()).append("\n");
		sb.append("Alert Message: ").append(context.alert().message()).append("\n");
		if (context.resource() != null) {
			sb.append("Resource: ").append(context.resource().displayName())
					.append(" (Shape: ").append(context.resource().shape()).append(")\n");
		}
		return sb.toString();
	}
}
