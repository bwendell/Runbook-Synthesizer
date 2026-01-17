package com.oracle.runbook.rag;

import com.oracle.runbook.domain.GenerationConfig;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Port interface for pluggable LLM (Large Language Model) providers.
 *
 * <p>This interface defines the contract for LLM backends in the Hexagonal Architecture.
 * Implementations provide concrete integrations with specific LLM services like OCI Generative AI
 * (Cohere), OpenAI, or Ollama.
 *
 * <p>All implementations must be non-blocking and return CompletableFuture to support Helidon SE's
 * reactive patterns. The interface supports both text generation (for checklist synthesis) and
 * embeddings (for RAG retrieval).
 *
 * @see GenerationConfig
 */
public interface LlmProvider {

  /**
   * Returns the identifier for this LLM provider.
   *
   * <p>Examples: "oci-genai", "openai", "ollama"
   *
   * @return the provider identifier, never null
   */
  String providerId();

  /**
   * Generates text based on the provided prompt and configuration.
   *
   * <p>This method is used for checklist synthesis, combining alert context and retrieved runbook
   * chunks into troubleshooting instructions.
   *
   * @param prompt the input prompt for text generation
   * @param config generation parameters (temperature, max tokens, model override)
   * @return a CompletableFuture containing the generated text, never null
   */
  CompletableFuture<String> generateText(String prompt, GenerationConfig config);

  /**
   * Generates an embedding vector for the given text.
   *
   * <p>Used for query embedding during RAG retrieval to find similar runbook chunks in the vector
   * store.
   *
   * @param text the text to embed
   * @return a CompletableFuture containing the embedding vector, never null
   */
  CompletableFuture<float[]> generateEmbedding(String text);

  /**
   * Generates embedding vectors for multiple texts in a single batch.
   *
   * <p>Used during document ingestion for efficient embedding of multiple runbook chunks. More
   * efficient than calling {@link #generateEmbedding(String)} repeatedly.
   *
   * @param texts the list of texts to embed
   * @return a CompletableFuture containing the list of embedding vectors, in the same order as the
   *     input texts, never null
   */
  CompletableFuture<List<float[]>> generateEmbeddings(List<String> texts);
}
