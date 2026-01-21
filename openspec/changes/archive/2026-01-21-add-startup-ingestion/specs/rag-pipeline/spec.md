## ADDED Requirements

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
