# rag-pipeline Specification

## Purpose
TBD - created by archiving change implement-rag-pipeline. Update Purpose after archive.
## Requirements
### Requirement: EmbeddingService generates vector embeddings via LlmProvider

The system SHALL provide an EmbeddingService that generates vector embeddings for text content using the configured LlmProvider.

#### Scenario: Embed single text string
- **WHEN** `embedText("High memory usage on compute instance")` is called
- **THEN** the service calls `LlmProvider.generateEmbedding()` with the text
- **AND** returns the resulting `float[]` embedding vector

#### Scenario: Embed multiple text strings in batch
- **WHEN** `embedTexts(["text1", "text2", "text3"])` is called
- **THEN** the service calls `LlmProvider.generateEmbeddings()` with the list
- **AND** returns a `List<float[]>` with embeddings in the same order

#### Scenario: Embed EnrichedContext as query
- **WHEN** `embedContext(context)` is called on an EnrichedContext
- **THEN** the service formats context into a query string
- **AND** returns the embedding for that query string

---

### Requirement: VectorStoreRepository stores and retrieves chunks by similarity

The system SHALL provide a VectorStoreRepository interface for storing runbook chunks and retrieving similar chunks by vector similarity.

#### Scenario: Store a single runbook chunk
- **WHEN** `store(chunk)` is called with a RunbookChunk
- **THEN** the chunk is persisted to the vector store with all metadata

#### Scenario: Store batch of runbook chunks
- **WHEN** `storeBatch(chunks)` is called with a list of chunks
- **THEN** all chunks are persisted atomically

#### Scenario: Find similar chunks by embedding
- **WHEN** `findSimilar(embedding, 5)` is called
- **THEN** returns the top 5 most similar chunks ordered by cosine similarity
- **AND** each result includes the similarity score

---

### Requirement: OracleVectorStoreRepository uses Oracle 23ai

The system SHALL provide an Oracle 23ai implementation of VectorStoreRepository using LangChain4j OracleEmbeddingStore.

#### Scenario: Store chunk to Oracle 23ai
- **WHEN** `store(chunk)` is called on OracleVectorStoreRepository
- **THEN** the chunk is stored using LangChain4j OracleEmbeddingStore
- **AND** metadata (tags, shapes, path, title) is preserved

#### Scenario: Retrieve similar chunks from Oracle 23ai
- **WHEN** `findSimilar(embedding, topK)` is called
- **THEN** Oracle 23ai Vector Search returns top-K similar embeddings
- **AND** results are mapped back to RunbookChunk domain objects

---

### Requirement: RunbookRetriever combines similarity with metadata re-ranking

The system SHALL provide a RunbookRetriever that combines vector similarity with metadata-based re-ranking to improve relevance.

#### Scenario: Retrieve chunks for enriched context
- **WHEN** `retrieve(context, 5)` is called with an EnrichedContext
- **THEN** the context is embedded into a query vector
- **AND** similar chunks are retrieved and re-ranked
- **AND** the top 5 `RetrievedChunk` objects are returned

#### Scenario: Boost chunks with matching tags
- **WHEN** retrieval is performed for an alert with "cpu" label
- **AND** a chunk has tags ["cpu", "linux"]
- **THEN** the chunk receives a metadata boost
- **AND** chunks with more matching tags rank higher

#### Scenario: Boost chunks with matching shape patterns
- **WHEN** retrieval is performed for resource shape "VM.Standard.E4.Flex"
- **AND** a chunk has applicableShapes ["VM.*"]
- **THEN** the chunk receives a shape match boost

---

### Requirement: PromptTemplates provides reusable prompts

The system SHALL provide reusable prompt templates for LLM-based checklist generation with placeholder substitution.

#### Scenario: Format checklist generation prompt
- **WHEN** `formatPrompt(context, chunks)` is called
- **THEN** the prompt contains the alert title, resource details, metrics, and logs
- **AND** the prompt contains the retrieved chunk content
- **AND** the prompt requests JSON-formatted output

---

### Requirement: ChecklistGenerator produces DynamicChecklist

The system SHALL provide a ChecklistGenerator that produces structured troubleshooting checklists using the LLM provider.

#### Scenario: Generate checklist from context and chunks
- **WHEN** `generate(context, chunks)` is called
- **THEN** the LLM is called with a formatted prompt
- **AND** the response is parsed into a DynamicChecklist
- **AND** the checklist contains prioritized ChecklistStep objects

#### Scenario: Include source runbooks in checklist
- **WHEN** a checklist is generated from multiple chunks
- **THEN** `DynamicChecklist.sourceRunbooks()` contains all unique runbook paths

#### Scenario: Handle malformed LLM response gracefully
- **WHEN** the LLM returns invalid JSON
- **THEN** a fallback checklist is returned with an error step
- **AND** the error is logged for debugging

---

### Requirement: RagPipelineService orchestrates the pipeline

The system SHALL provide a top-level RagPipelineService that coordinates retrieval and generation steps.

#### Scenario: Process enriched context through full pipeline
- **WHEN** `process(context)` is called
- **THEN** the retriever fetches relevant chunks (default topK=5)
- **AND** the generator produces a DynamicChecklist
- **AND** the checklist is returned

#### Scenario: Process with custom topK parameter
- **WHEN** `process(context, 10)` is called
- **THEN** retrieval uses topK=10
- **AND** all 10 chunks are passed to the generator

