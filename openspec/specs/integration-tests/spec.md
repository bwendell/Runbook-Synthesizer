# integration-tests Specification

## Purpose
TBD - created by archiving change implement-integration-tests. Update Purpose after archive.
## Requirements
### Requirement: Integration Test Infrastructure

The system SHALL provide integration test infrastructure using Helidon Test framework with WireMock for external service mocking.

#### Scenario: Test base class setup
- **GIVEN** a test class extending `IntegrationTestBase`
- **WHEN** the test executes
- **THEN** Helidon web server starts with correct configuration
- **AND** WireMock server is available for HTTP mocking

#### Scenario: Test fixture loading
- **GIVEN** JSON fixtures in `src/test/resources/fixtures/`
- **WHEN** `TestFixtures.loadAs(path, Class)` is called
- **THEN** the fixture is deserialized to the specified type

---

### Requirement: Alert Ingestion Integration Tests

The system SHALL provide integration tests validating the alert ingestion flow from REST endpoint to enrichment trigger.

#### Scenario: Valid OCI alarm ingestion
- **GIVEN** a valid OCI Monitoring Alarm JSON payload
- **WHEN** POST to `/api/v1/alerts`
- **THEN** HTTP 200 response with `DynamicChecklist` body
- **AND** alert is normalized to canonical format
- **AND** context enrichment is triggered

#### Scenario: Invalid payload rejection
- **GIVEN** an invalid alert payload (missing required fields)
- **WHEN** POST to `/api/v1/alerts`
- **THEN** HTTP 400 response with validation error details

#### Scenario: OCI alarm normalization
- **GIVEN** OCI-specific alarm format with `body.alarmName`, `body.severity`
- **WHEN** processed by `OciMonitoringAlarmAdapter`
- **THEN** canonical `Alert` has `sourceService` = "oci-monitoring"
- **AND** dimensions extracted from OCI payload

---

### Requirement: Context Enrichment Integration Tests

The system SHALL provide integration tests validating context enrichment from OCI services.

#### Scenario: Metrics fetching via OCI Monitoring
- **GIVEN** an alert with `resourceId` dimension
- **AND** WireMock stub for OCI Monitoring API
- **WHEN** enrichment service processes the alert
- **THEN** `EnrichedContext.recentMetrics()` contains metrics from mock

#### Scenario: Logs fetching via OCI Logging
- **GIVEN** an alert with `resourceId` dimension
- **AND** WireMock stub for OCI Logging API
- **WHEN** enrichment service processes the alert
- **THEN** `EnrichedContext.recentLogs()` contains logs from mock

#### Scenario: Resource metadata resolution
- **GIVEN** an alert referencing an OCI Compute instance
- **AND** WireMock stub for OCI Compute API
- **WHEN** enrichment service processes the alert
- **THEN** `EnrichedContext.resource()` contains shape, availability domain, tags

---

### Requirement: RAG Pipeline Integration Tests

The system SHALL provide integration tests validating the RAG retrieval and embedding pipeline.

#### Scenario: Vector store operations
- **GIVEN** a `RunbookChunk` with embedding vector
- **WHEN** stored in vector store
- **AND** queried with similar embedding
- **THEN** chunk returned with similarity score above threshold

#### Scenario: Top-K retrieval
- **GIVEN** 10 runbook chunks in vector store
- **AND** a query similar to 3 specific chunks
- **WHEN** retrieval with `topK=3`
- **THEN** the 3 most relevant chunks returned in ranked order

#### Scenario: Embedding generation
- **GIVEN** text input for embedding
- **AND** WireMock stub for OCI GenAI embeddings API
- **WHEN** embedding service called
- **THEN** float[] of correct dimensions returned (1024 for Cohere)

---

### Requirement: Checklist Generation Integration Tests

The system SHALL provide integration tests validating checklist generation from enriched context and retrieved chunks.

#### Scenario: Full generation pipeline
- **GIVEN** enriched context with memory alert and VM shape
- **AND** vector store seeded with memory runbook chunks
- **AND** WireMock stub for LLM generation
- **WHEN** generator produces checklist
- **THEN** checklist contains steps from memory runbook
- **AND** steps reference current metric values

#### Scenario: Shape-based filtering
- **GIVEN** context with non-GPU shape (VM.Standard)
- **AND** runbook chunks with GPU-only steps
- **WHEN** checklist generated
- **THEN** GPU-specific steps excluded from output

---

### Requirement: Webhook Dispatcher Integration Tests

The system SHALL provide integration tests validating webhook dispatch to multiple destinations.

#### Scenario: Multi-channel dispatch
- **GIVEN** 2 webhook destinations configured (Slack, PagerDuty)
- **AND** WireMock stubs for webhook endpoints
- **WHEN** checklist with CRITICAL severity dispatched
- **THEN** both webhooks receive POST requests

#### Scenario: Severity filtering
- **GIVEN** webhook configured with `severities: [CRITICAL]`
- **WHEN** checklist with WARNING severity dispatched
- **THEN** webhook NOT called

#### Scenario: Slack Block Kit formatting
- **GIVEN** Slack webhook destination
- **WHEN** checklist dispatched
- **THEN** request body contains Slack Block Kit structure
- **AND** includes section blocks with checklist steps

---

### Requirement: End-to-End Flow Tests

The system SHALL provide end-to-end tests validating complete alert-to-checklist flow.

#### Scenario: Happy path flow
- **GIVEN** valid OCI alarm payload
- **AND** all OCI service mocks configured
- **AND** webhook destinations configured
- **WHEN** POST to `/api/v1/alerts`
- **THEN** `DynamicChecklist` response contains populated steps
- **AND** response includes source runbooks and LLM provider info
- **AND** configured webhooks receive dispatch

#### Scenario: OCI service failure handling
- **GIVEN** OCI Monitoring mock returns 503 Service Unavailable
- **WHEN** POST valid alert
- **THEN** system degrades gracefully
- **AND** error logged with context

#### Scenario: Timeout handling
- **GIVEN** OCI GenAI mock with 30-second delay
- **WHEN** alert processed
- **THEN** request times out within configured bounds
- **AND** timeout error returned, not hang

---

### Requirement: Test Configuration and CI Integration

The system SHALL provide Maven configuration for running integration tests in CI/CD pipelines.

#### Scenario: Test categorization
- **GIVEN** `*Test.java` files for unit tests
- **AND** `*IT.java` files for integration tests
- **WHEN** `mvn verify` executed
- **THEN** unit tests run via Surefire plugin
- **AND** integration tests run via Failsafe plugin

#### Scenario: Integration test execution
- **GIVEN** Failsafe plugin configured
- **WHEN** `mvn failsafe:integration-test` executed
- **THEN** only `*IT.java` tests run
- **AND** WireMock server lifecycle managed correctly

