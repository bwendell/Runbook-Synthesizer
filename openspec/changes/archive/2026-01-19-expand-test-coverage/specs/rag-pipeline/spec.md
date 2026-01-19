# rag-pipeline Spec Delta

## ADDED Requirements

### Requirement: PromptTemplates Unit Test Coverage

The system SHALL provide unit tests for `PromptTemplates` ensuring prompt template structure and completeness.

#### Scenario: SYSTEM_PROMPT contains required sections
- **WHEN** `PromptTemplates.SYSTEM_PROMPT` is examined
- **THEN** it contains text referencing "ALERT CONTEXT"
- **AND** it contains text referencing "RUNBOOK"
- **AND** it contains instructions for checklist generation

#### Scenario: CONTEXT_TEMPLATE has correct placeholder count
- **WHEN** `PromptTemplates.CONTEXT_TEMPLATE` is examined
- **THEN** it contains exactly 5 `%s` placeholders for: title, severity, message, resource, shape

#### Scenario: CHUNK_TEMPLATE has correct placeholder count
- **WHEN** `PromptTemplates.CHUNK_TEMPLATE` is examined
- **THEN** it contains exactly 3 `%s` placeholders for: runbook name, section, content

#### Scenario: GENERATE_INSTRUCTION is non-empty
- **WHEN** `PromptTemplates.GENERATE_INSTRUCTION` is examined
- **THEN** it is a non-blank string
- **AND** it contains instruction text for checklist generation

---

### Requirement: ScoredChunk Record Validation

The system SHALL validate `ScoredChunk` record constructor rejects null chunk values.

#### Scenario: ScoredChunk rejects null chunk
- **WHEN** `new ScoredChunk(null, 0.9)` is constructed
- **THEN** a `NullPointerException` is thrown
- **AND** the exception message contains "chunk"

#### Scenario: ScoredChunk accepts valid parameters
- **WHEN** `new ScoredChunk(validChunk, 0.85)` is constructed
- **THEN** the record is created successfully
- **AND** `chunk()` returns the provided chunk
- **AND** `similarityScore()` returns 0.85

---

## MODIFIED Requirements

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

## ADDED Requirements

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
