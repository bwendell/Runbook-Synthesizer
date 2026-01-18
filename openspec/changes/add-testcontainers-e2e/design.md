# Design: Testcontainers E2E Testing Infrastructure

## Context

Current integration tests use:
- `InMemoryVectorStore` - HashMap-based, no real vector similarity
- `TestLlmProvider` / `TestEmbeddingService` - Fixed responses, no real inference
- `WireMock` - HTTP mocking for OCI APIs

This provides fast, deterministic tests but doesn't validate:
- Actual Oracle 23ai vector search behavior
- Real embedding dimension compatibility
- LLM prompt → response quality
- Container networking and connection pooling

## Goals / Non-Goals

**Goals:**
- Enable optional E2E tests using real infrastructure via Docker
- Test Oracle 23ai vector similarity search with real embeddings
- Test Ollama LLM integration for embedding + generation
- Maintain fast test feedback with proper test categorization
- Support both local Docker and CI/CD container environments

**Non-Goals:**
- Replace existing WireMock-based tests (keep both)
- Test against real OCI services (too slow, requires credentials)
- 100% code coverage with containers (use for critical paths only)

## Decisions

### Decision 1: Oracle Database Container

Use `gvenzl/oracle-free:23-slim` for Oracle 23ai vector support.

**Rationale:**
- Official-ish community image with vector search capability
- Slim variant for faster startup (~30s vs ~2min)
- Drop-in replacement for production Oracle 23ai

**Alternatives considered:**
- `container-registry.oracle.com/database/express` - requires registry login, slower
- PostgreSQL with pgvector - different SQL dialect, not representative

### Decision 2: Ollama Container

Use `ollama/ollama:latest` with `nomic-embed-text` and `llama3.2:1b` models.

**Rationale:**
- `llama3.2:1b` is fine for realistic E2E verification of checklist formatting and flow.
- Ollama provides OpenAI-compatible API.

**Alternatives considered:**
- Smaller models (e.g., TinyLlama) - too low quality for E2E.

### Decision 3: Test Data Management (Scenario Seeder)


Adopt the **Scenario Seeder** pattern using domain-focused factories.

**Rationale:**
- **testing-patterns**: Emphasizes factory functions (`getMockX`) with sensible defaults.
- **software-architecture**: Recommends avoiding generic `utils` and keeping data close to the domain.
- E2E tests will use a `RunbookSeeder` to populate Oracle 23ai per-test, ensuring test isolation.

**Alternatives considered:**
- Shared static fixtures - prone to state leakage and brittle tests.


## Container Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Test JVM                                 │
│  ┌──────────────────┐  ┌──────────────────┐                 │
│  │ ContainerTestBase│  │ OracleVectorStore│                 │
│  │ (Manages containers)│  Container│                         │
│  └────────┬─────────┘  └────────┬─────────┘                 │
│           │                      │                           │
│           ▼                      ▼                           │
│  ┌────────────────────────────────────────────┐             │
│  │           Testcontainers Network            │             │
│  └────────┬─────────────────────┬─────────────┘             │
│           │                      │                           │
└───────────┼──────────────────────┼───────────────────────────┘
            │                      │
   ┌────────▼────────┐    ┌───────▼────────┐
   │ Oracle 23ai     │    │ Ollama         │
   │ (gvenzl/oracle- │    │ (ollama/ollama)│
   │ free:23-slim)   │    │ nomic-embed +  │
   │                 │    │ llama3.2:1b    │
   └─────────────────┘    └────────────────┘
```

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| Slow startup (~60s total) | Use parallel startup with `@Testcontainers` |
| Flaky if Docker unavailable | Skip with `@EnabledIf("dockerAvailable")` |
| Large image downloads first run | Document in README, cache in CI |
| Oracle licensing | Use `gvenzl/oracle-free` (free for dev/test) |


## Implementation Details

### Parallel Startup

Oracle and Ollama containers will be started in parallel to minimize total setup time. Testcontainers executes static initializers in parallel when possible.

### Data Factory Pattern

Use `AlertFactory.createWarningAlert()` and `RunbookSeeder.seedMemoryRunbook()` to keep test setup readable and domain-aligned.
