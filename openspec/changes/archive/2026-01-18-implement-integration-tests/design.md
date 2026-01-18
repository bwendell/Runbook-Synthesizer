# Design: Integration Tests

## Context

Runbook-Synthesizer requires comprehensive integration tests to validate component interactions before production deployment. The system has multiple integration points:
- REST API → Alert processing pipeline
- Alert pipeline → OCI SDK clients (Monitoring, Logging, Object Storage)
- RAG pipeline → Vector store (Oracle 23ai)
- RAG pipeline → LLM provider (OCI GenAI)
- Checklist generation → Webhook dispatcher

Testing these flows requires careful mocking strategies and test data management.

## Goals / Non-Goals

**Goals:**
- Validate end-to-end data flow from alert ingestion to webhook output
- Catch integration bugs that unit tests miss (serialization, HTTP handling, error propagation)
- Provide regression safety for refactoring
- Document expected behavior through executable specifications
- Enable CI/CD pipeline quality gates

**Non-Goals:**
- Replace unit tests (they remain for fast feedback)
- Test actual OCI services (use mocks, not cloud resources)
- Performance/load testing (separate concern)
- UI testing (no UI in v1.0)

## Technical Decisions

### Test Framework Stack

| Component | Technology | Rationale |
|-----------|------------|-----------|
| HTTP Testing | Helidon Test (WebClient) | Native Helidon integration |
| API Mocking | WireMock | Industry standard for HTTP service mocking |
| Containers | Testcontainers (optional) | For Oracle 23ai integration if needed |
| Assertions | AssertJ | Fluent, readable assertions |
| JSON Processing | JSON-B (Helidon native) | Consistent with production code |

### Mocking Strategy

```
┌─────────────────────────────────────────────────────────┐
│                   Integration Test                       │
├─────────────────────────────────────────────────────────┤
│  Real: Helidon Server, Routes, Handlers, Domain Logic   │
│  Real: JSON serialization, HTTP handling, Validation    │
├─────────────────────────────────────────────────────────┤
│  Mocked: OCI SDK clients (Monitoring, Logging, GenAI)   │
│  Mocked: External webhooks (Slack, PagerDuty)           │
│  In-Memory: Vector store for RAG retrieval tests        │
└─────────────────────────────────────────────────────────┘
```

### Test Data Management

**Fixture Location:** `src/test/resources/`
```
src/test/resources/
├── fixtures/
│   ├── alerts/
│   │   ├── oci-monitoring-alarm.json    # OCI alarm payload
│   │   ├── high-memory-alert.json       # Canonical alert
│   │   └── critical-cpu-alert.json      # Critical severity
│   ├── contexts/
│   │   └── enriched-gpu-host.json       # Sample enriched context
│   └── checklists/
│       └── expected-memory-checklist.json
├── sample-runbooks/
│   ├── memory/high-memory.md
│   ├── cpu/high-cpu.md
│   └── gpu/gpu-diagnostics.md
└── wiremock/
    ├── mappings/
    │   ├── oci-monitoring.json
    │   ├── oci-logging.json
    │   └── oci-genai.json
    └── __files/
        ├── metrics-response.json
        └── genai-response.json
```

### Test Categories

| Category | Scope | Speed | CI Stage |
|----------|-------|-------|----------|
| `@IntegrationTest` | Component interaction | Medium | PR checks |
| `@EndToEnd` | Full alert → webhook flow | Slow | Merge to main |
| `@ContractTest` | External API contracts | Fast | PR checks |

## Package Structure

```
src/test/java/com/oracle/runbook/integration/
├── IntegrationTestBase.java       # Shared setup, Helidon server lifecycle
├── TestFixtures.java              # Fixture loading utilities
├── alert/
│   ├── AlertIngestionIT.java      # POST /api/v1/alerts flow
│   └── AlertNormalizationIT.java  # Payload → canonical Alert
├── enrichment/
│   ├── ContextEnrichmentIT.java   # Alert → EnrichedContext
│   └── OciClientMockIT.java       # OCI SDK mock verification
├── rag/
│   ├── EmbeddingServiceIT.java    # Text → vector embedding
│   ├── VectorStoreIT.java         # Store + retrieve chunks
│   └── RetrievalIT.java           # Query → relevant chunks
├── generation/
│   └── ChecklistGenerationIT.java # Context + chunks → checklist
├── webhook/
│   ├── WebhookDispatcherIT.java   # Checklist → webhook calls
│   └── SlackFormattingIT.java     # Slack Block Kit output
└── e2e/
    ├── AlertToChecklistIT.java    # Full happy path
    └── ErrorPropagationIT.java    # Failure mode testing
```

## Risks / Trade-offs

| Risk | Impact | Mitigation |
|------|--------|------------|
| Flaky tests from async operations | CI failures | Use `awaitility` for async assertions |
| Slow test suite | Developer friction | Parallelize, categorize by speed |
| Mock drift from real APIs | False confidence | Contract tests, periodic validation |
| Test data maintenance | Stale fixtures | Generate from schemas where possible |

## Open Questions

1. Should we use Testcontainers for Oracle 23ai vector store, or is an in-memory implementation sufficient for integration tests?
2. How do we handle LLM response variability in generation tests? (Suggestion: Use deterministic mock responses rather than testing actual LLM output)
