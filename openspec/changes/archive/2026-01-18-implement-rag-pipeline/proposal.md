# Change: Implement RAG Pipeline

## Why

The RAG (Retrieval Augmented Generation) pipeline is the core engine that transforms static runbooks into context-aware troubleshooting checklists. Without this component, the system cannot:
- Generate vector embeddings for semantic search
- Retrieve relevant runbook sections based on alert context
- Synthesize dynamic checklists tailored to specific hosts and alerts

This phase implements the complete retrieval-generation pipeline using Oracle 23ai Vector Search and the pluggable LlmProvider interface.

## What Changes

- Add LangChain4j Oracle dependency to `pom.xml` for Oracle 23ai vector store integration

- Create `EmbeddingService` that wraps `LlmProvider` for convenient embedding generation:
  - Single text embedding via `embedText()`
  - Batch embedding via `embedTexts()`
  - Context-aware embedding via `embedContext(EnrichedContext)`

- Create `VectorStoreRepository` interface and Oracle 23ai implementation:
  - Store `RunbookChunk` with embeddings and metadata
  - Retrieve similar chunks by cosine similarity
  - Map between domain types and LangChain4j types

- Create `RunbookRetriever` interface and default implementation:
  - Combine vector similarity with metadata-based re-ranking
  - Boost chunks with matching tags (+0.1 per tag, max +0.3)
  - Boost chunks with matching shape patterns (+0.2)

- Create `PromptTemplates` with checklist generation prompts:
  - JSON-formatted output for reliable parsing
  - Include alert context, resource details, metrics, and logs

- Create `ChecklistGenerator` interface and default implementation:
  - Format prompts using `PromptTemplates`
  - Call `LlmProvider.generateText()` with optimized config
  - Parse JSON response into `DynamicChecklist`

- Create `RagPipelineService` as top-level orchestrator:
  - Coordinate retrieval and generation steps
  - Default topK=5 with configurable override

## Impact

- **Affected specs**: New `rag-pipeline` capability
- **Affected code**: New files in `src/main/java/com/oracle/runbook/rag/`
- **Dependencies**: 
  - Requires `implement-domain-models` (completed) for `RunbookChunk`, `RetrievedChunk`, `EnrichedContext`, `DynamicChecklist`, `GenerationConfig`
  - Requires `implement-ports-interfaces` for `LlmProvider` interface
- **Downstream phases**: REST API (Phase 6) will consume `RagPipelineService`
