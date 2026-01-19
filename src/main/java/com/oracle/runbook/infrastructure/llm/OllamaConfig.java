package com.oracle.runbook.infrastructure.llm;

import java.util.Objects;

/**
 * Configuration for Ollama LLM provider.
 *
 * @param baseUrl the base URL for Ollama API (e.g., "http://localhost:11434")
 * @param textModel the model to use for text generation (e.g., "llama3.2:3b")
 * @param embeddingModel the model to use for embeddings (e.g., "nomic-embed-text")
 */
public record OllamaConfig(String baseUrl, String textModel, String embeddingModel) {
  /** Compact constructor with validation. */
  public OllamaConfig {
    Objects.requireNonNull(baseUrl, "baseUrl cannot be null");
    Objects.requireNonNull(textModel, "textModel cannot be null");
    Objects.requireNonNull(embeddingModel, "embeddingModel cannot be null");
  }
}
