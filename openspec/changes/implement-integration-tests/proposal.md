# Change: Implement Integration Tests

## Why

Phase 8 establishes end-to-end validation of the Runbook-Synthesizer system by implementing integration tests that verify component interactions. While unit tests validate individual domain models in isolation, integration tests ensure proper data flow across layers: Alert Ingestion → Context Enrichment → RAG Pipeline → Checklist Generation → Webhook Output.

## What Changes

- **[NEW]** Integration test infrastructure with Helidon testing support and embedded database for Oracle 23ai Vector Search simulation
- **[NEW]** Alert ingestion integration tests (REST API → Alert normalization → enrichment trigger)
- **[NEW]** Context enrichment integration tests (OCI SDK mock → enriched context assembly)
- **[NEW]** RAG pipeline integration tests (embedding → vector store → retrieval → generation)
- **[NEW]** End-to-end flow tests (alert POST → checklist response + webhook dispatch)
- **[NEW]** Webhook dispatcher integration tests (checklist → multi-channel delivery)
- **[NEW]** Test fixtures and sample runbook data for reproducible scenarios

## Impact

- **Affected specs**: `integration-tests` (new capability)
- **Affected code**:
  - `src/test/java/com/oracle/runbook/integration/` - new package
  - `src/test/resources/` - test fixtures, sample runbooks, mock payloads
  - `pom.xml` - test dependencies (Helidon testing, testcontainers, WireMock)

## Dependencies

This phase depends on all prior phases being complete:
- Phase 1: Project scaffolding (Helidon SE, Maven)
- Phase 2: Domain models (Alert, EnrichedContext, DynamicChecklist, etc.)
- Phase 3: Ports/interfaces (LlmProvider, MetricsSourceAdapter, WebhookDestination)
- Phase 4: OCI SDK integrations (Monitoring, Logging, Object Storage)
- Phase 5: RAG pipeline (embeddings, vector store, retrieval)
- Phase 6: REST API endpoints
- Phase 7: Webhook output framework

## Testing Philosophy

Integration tests follow TDD principles but test **component interactions** rather than isolated units:
- Use real HTTP requests against Helidon test server
- Mock external OCI services using WireMock
- Use embedded/in-memory vector store for RAG tests
- Verify actual data transformation across layer boundaries
- Test failure modes and error propagation
