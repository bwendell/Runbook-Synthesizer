## ADDED Requirements

### Requirement: True E2E Runbook Ingestion Tests

The system SHALL have integration tests that verify the complete runbook ingestion pipeline from cloud storage to vector store.

#### Scenario: Ingest runbooks from S3 into vector store
- **WHEN** runbooks are uploaded to S3 (via LocalStack)
- **AND** `RunbookIngestionService.ingestAll(bucket)` is invoked
- **THEN** runbook chunks SHALL be stored in the vector store
- **AND** the stored chunks SHALL be searchable via `VectorStoreRepository.search()`
- **AND** search results SHALL return semantically similar chunks

#### Scenario: Verify chunk embedding quality
- **WHEN** a memory-related runbook is ingested
- **AND** a query about memory is searched
- **THEN** the memory runbook chunk SHALL be returned with high similarity score

---

### Requirement: Startup Ingestion Integration Tests

The system SHALL have integration tests that verify runbook ingestion at application startup.

#### Scenario: Startup ingestion populates vector store
- **WHEN** the application initializes with `runbooks.ingestOnStartup=true`
- **AND** runbooks exist in the configured S3 bucket
- **THEN** `RunbookRetriever.retrieve()` SHALL return relevant chunks for matching alerts

#### Scenario: Application runs without ingestion when disabled
- **WHEN** the application initializes with `runbooks.ingestOnStartup=false`
- **THEN** the vector store SHALL remain empty
- **AND** `RunbookRetriever.retrieve()` SHALL return an empty list

---

## MODIFIED Requirements

### Requirement: End-to-End Flow Tests

The full pipeline E2E tests SHALL use true ingestion from cloud storage instead of manual vector store seeding.

> **Note:** This modifies the existing `FullPipelineE2EIT` tests to use `RunbookIngestionService` instead of `PipelineTestHarness.seedRunbooks()`.

#### Scenario: Memory alert through full pipeline with real ingestion
- **WHEN** runbooks are uploaded to LocalStack S3
- **AND** `harness.ingestRunbooksFromS3(bucket)` is called
- **AND** a memory alert is processed through the pipeline
- **THEN** the generated checklist SHALL reference the memory runbook
- **AND** the steps SHALL contain memory-related instructions

#### Scenario: CPU alert through full pipeline with real ingestion
- **WHEN** runbooks are uploaded to LocalStack S3
- **AND** `harness.ingestRunbooksFromS3(bucket)` is called
- **AND** a CPU alert is processed through the pipeline
- **THEN** the generated checklist SHALL reference the CPU runbook
- **AND** the steps SHALL contain CPU-related instructions
