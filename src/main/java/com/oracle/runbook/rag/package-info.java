/**
 * RAG pipeline for dynamic runbook generation.
 *
 * <p>Components for Retrieval Augmented Generation:
 *
 * <ul>
 *   <li>Embedding service for runbook indexing
 *   <li>Content retrieval from Vector Store
 *   <li>Checklist generation via LLM providers
 * </ul>
 *
 * <h2>Ports/Interfaces (Hexagonal Architecture)</h2>
 *
 * <ul>
 *   <li>{@link com.oracle.runbook.rag.LlmProvider} - Pluggable LLM backends for text/embedding
 *       generation
 *   <li>{@link com.oracle.runbook.rag.EmbeddingService} - Facade for embedding generation
 *   <li>{@link com.oracle.runbook.rag.VectorStoreRepository} - Vector storage operations
 *   <li>{@link com.oracle.runbook.rag.RunbookRetriever} - RAG retrieval of relevant chunks
 *   <li>{@link com.oracle.runbook.rag.ChecklistGenerator} - Checklist synthesis from context
 * </ul>
 */
package com.oracle.runbook.rag;
