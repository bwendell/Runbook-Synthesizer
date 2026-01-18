# Change: Add Testcontainers E2E Testing Infrastructure

## Why

The current integration test suite uses in-memory implementations (InMemoryVectorStore, TestLlmProvider, TestEmbeddingService) and WireMock for HTTP mocking. While this provides fast, isolated tests, it doesn't validate real infrastructure interactions. Testcontainers enables true E2E testing with actual Oracle 23ai vector stores, real LLM inference (via Ollama), and genuine multi-component integration.

This change assumes:
- Option A (HTTP API-level tests) is complete
- Option B (AlertResource wired to RAG pipeline) is complete

## What Changes

- **[NEW]** Testcontainers dependency with Oracle Database and Ollama containers
- **[NEW]** Container-based base class `ContainerTestBase` for E2E tests
- **[NEW]** Real Oracle 23ai vector store integration tests
- **[NEW]** Real Ollama LLM integration tests (embeddings + generation)
- **[NEW]** Full-stack E2E tests hitting HTTP API with real backend services
- **[MODIFIED]** Test profiles for Docker vs non-Docker environments

## Impact

- **Affected specs**: `integration-tests`
- **Affected code**:
  - `pom.xml` - Testcontainers dependencies
  - `src/test/java/com/oracle/runbook/integration/containers/` - new package
  - Docker requirement for running full E2E tests

## Dependencies

- Phase A: API-level HTTP integration tests (complete)
- Phase B: AlertResource wired to RAG pipeline (complete)
- Docker installed and accessible from test environment

## Testing Philosophy

Container-based tests follow the same TDD principles but trade speed for realism:
- Use real Oracle 23ai for vector similarity search
- Use real Ollama for embedding generation and text completion
- Test actual JDBC connections, not in-memory mocks
- Validate real network behavior between containers
