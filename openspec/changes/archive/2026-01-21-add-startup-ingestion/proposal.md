# Change: Add Startup Runbook Ingestion

## Why

The application fails to send relevant runbook sections to the LLM because the vector store is never populated with runbooks at startup. When running in non-stub mode, `ServiceFactory` creates an empty `InMemoryVectorStoreRepository` but never calls `RunbookIngestionService.ingestAll()` to load runbooks from S3.

**Root cause:** The existing E2E tests work only because `PipelineTestHarness.seedRunbooks()` manually bypasses the ingestion pipeline—these are not true end-to-end tests.

## What Changes

- **ServiceFactory**: Add `createCloudStorageAdapter()`, `createRunbookChunker()`, and `createRunbookIngestionService()` methods
- **RunbookSynthesizerApp**: Call runbook ingestion at startup when `runbooks.ingestOnStartup` is true
- **application.yaml**: Add `runbooks.bucket` and `runbooks.ingestOnStartup` configuration
- **True E2E Tests**: Refactor `FullPipelineE2EIT` to use real ingestion from S3 instead of manual seeding
- **New Tests**: Add `RunbookIngestionE2EIT` and `StartupIngestionIT` for comprehensive coverage

## Impact

- **Affected specs**: rag-pipeline, integration-tests
- **Affected code**:
  - `ServiceFactory.java` (new methods)
  - `RunbookSynthesizerApp.java` (startup logic)
  - `application.yaml` (configuration)
  - `FullPipelineE2EIT.java` (refactor to true E2E)
  - New test files: `RunbookIngestionE2EIT.java`, `StartupIngestionIT.java`
