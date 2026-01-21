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

The system SHALL provide an Oracle 23ai implementation of VectorStoreRepository, ensuring compatibility with both cloud-hosted (ADB-S) and containerized (Testcontainers) Oracle 23ai instances.

#### Scenario: Initialize repository with dynamic JDBC URL
- **GIVEN** a dynamically provided JDBC URL and credentials (from Testcontainers or Cloud)
- **WHEN** OracleVectorStoreRepository is initialized
- **THEN** it successfully connects to the instance
- **AND** performs vector operations as expected

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

The system SHALL provide a top-level RagPipelineService that coordinates retrieval and generation steps, including proper error handling and input validation.

#### Scenario: Process enriched context through full pipeline
- **WHEN** `process(context)` is called
- **THEN** the retriever fetches relevant chunks (default topK=5)
- **AND** the generator produces a DynamicChecklist
- **AND** the checklist is returned

#### Scenario: Process with custom topK parameter
- **WHEN** `process(context, 10)` is called
- **THEN** retrieval uses topK=10
- **AND** all 10 chunks are passed to the generator

#### Scenario: Process with null alert throws exception
- **WHEN** `processAlert(null, 5)` is called
- **THEN** a `NullPointerException` or `IllegalArgumentException` is thrown
- **AND** the exception message indicates null input

#### Scenario: Enrichment service failure propagates to caller
- **GIVEN** the enrichment service throws a RuntimeException
- **WHEN** `processAlert(alert, 5)` is called
- **THEN** the exception is propagated wrapped in CompletionException
- **AND** the original exception is accessible as the cause

#### Scenario: Retriever failure propagates to caller
- **GIVEN** the runbook retriever throws a RuntimeException
- **WHEN** `processAlert(alert, 5)` is called
- **THEN** the exception is propagated wrapped in CompletionException

#### Scenario: Generator failure propagates to caller
- **GIVEN** the checklist generator throws a RuntimeException
- **WHEN** `processAlert(alert, 5)` is called
- **THEN** the exception is propagated wrapped in CompletionException

#### Scenario: Empty retrieval results still generates checklist
- **GIVEN** the retriever returns an empty list
- **WHEN** `processAlert(alert, 5)` is called
- **THEN** the generator is still invoked with empty chunks list
- **AND** a checklist is returned (potentially with fallback content)

---

### Requirement: OciObjectStorageClient Behavior Tests

The system SHALL provide comprehensive unit tests for `OciObjectStorageClient` covering all method behaviors and error conditions.

#### Scenario: listRunbooks returns only markdown files
- **GIVEN** an OCI Object Storage bucket containing files: `runbook.md`, `image.png`, `notes.txt`
- **WHEN** `listRunbooks(namespace, bucketName)` is called
- **THEN** only `["runbook.md"]` is returned

#### Scenario: listRunbooks from empty bucket returns empty list
- **GIVEN** an OCI Object Storage bucket with no objects
- **WHEN** `listRunbooks(namespace, bucketName)` is called
- **THEN** an empty list is returned

#### Scenario: listRunbooks propagates BmcException
- **GIVEN** the OCI SDK throws a BmcException (e.g., 403 Forbidden)
- **WHEN** `listRunbooks(namespace, bucketName)` is called
- **THEN** the exception is propagated to the caller

#### Scenario: getRunbookContent returns content for existing object
- **GIVEN** an object exists in OCI Object Storage with content "# Runbook Content"
- **WHEN** `getRunbookContent(namespace, bucketName, objectName)` is called
- **THEN** `Optional.of("# Runbook Content")` is returned

#### Scenario: getRunbookContent returns empty for 404
- **GIVEN** the OCI SDK throws a BmcException with status code 404
- **WHEN** `getRunbookContent(namespace, bucketName, objectName)` is called
- **THEN** `Optional.empty()` is returned

#### Scenario: getRunbookContent wraps non-404 exceptions
- **GIVEN** the OCI SDK throws a BmcException with status code 500
- **WHEN** `getRunbookContent(namespace, bucketName, objectName)` is called
- **THEN** a RuntimeException is thrown with the original exception as cause

### Requirement: Startup Runbook Ingestion

The system SHALL ingest runbooks from cloud storage into the vector store at application startup when configured.

#### Scenario: Ingestion enabled and runbooks exist
- **WHEN** `runbooks.ingestOnStartup` is `true`
- **AND** the application starts in non-stub mode
- **AND** runbooks exist in the configured S3 bucket
- **THEN** the system SHALL invoke `RunbookIngestionService.ingestAll(bucket)`
- **AND** all runbook chunks SHALL be stored in the vector store
- **AND** a log message SHALL indicate the number of chunks ingested

#### Scenario: Ingestion disabled
- **WHEN** `runbooks.ingestOnStartup` is `false`
- **AND** the application starts in non-stub mode
- **THEN** the system SHALL NOT invoke ingestion
- **AND** the vector store SHALL remain empty

#### Scenario: Ingestion fails gracefully
- **WHEN** `runbooks.ingestOnStartup` is `true`
- **AND** ingestion fails (S3 unavailable, network error, etc.)
- **THEN** the system SHALL log a warning
- **AND** the application SHALL continue startup
- **AND** the vector store SHALL remain empty

---

### Requirement: ServiceFactory Runbook Ingestion Support

The `ServiceFactory` class SHALL provide methods to create runbook ingestion components.

#### Scenario: Create CloudStorageAdapter
- **WHEN** `createCloudStorageAdapter()` is called
- **AND** cloud provider is `aws`
- **THEN** the factory SHALL return an `AwsS3StorageAdapter` instance
- **AND** the adapter SHALL be configured with the correct S3 client and region

#### Scenario: Create RunbookChunker
- **WHEN** `createRunbookChunker()` is called
- **THEN** the factory SHALL return a `RunbookChunker` instance

#### Scenario: Create RunbookIngestionService
- **WHEN** `createRunbookIngestionService()` is called
- **THEN** the factory SHALL return a `RunbookIngestionService` instance
- **AND** the service SHALL be wired with `CloudStorageAdapter`, `RunbookChunker`, `EmbeddingService`, and `VectorStoreRepository`

#### Scenario: Cache ingestion service instance
- **WHEN** `createRunbookIngestionService()` is called multiple times
- **THEN** the same cached instance SHALL be returned

---

### Requirement: Runbook Ingestion Configuration

The application SHALL support configuration for runbook ingestion.

#### Scenario: Default bucket configuration
- **WHEN** `runbooks.bucket` is not explicitly set
- **THEN** the system SHALL use `runbook-synthesizer-runbooks` as the default bucket name

#### Scenario: Custom bucket configuration
- **WHEN** `runbooks.bucket` is set to a custom value
- **THEN** the system SHALL use the custom bucket name for ingestion

#### Scenario: Default ingestOnStartup configuration
- **WHEN** `runbooks.ingestOnStartup` is not explicitly set
- **THEN** the system SHALL default to `true` (ingestion enabled)

