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
- `nomic-embed-text` - 768-dimension embeddings, fast, open-source
- `llama3.2:1b` - smallest Llama variant, fast inference for tests
- Ollama provides OpenAI-compatible API

**Alternatives considered:**
- Mock LLM responses - current approach, doesn't test real inference
- GPU containers - requires NVIDIA Docker runtime, not portable

### Decision 3: Test Categorization

Use Maven profiles and JUnit 5 tags:
- `@Tag("container")` for Testcontainers-based tests
- Profile `e2e-containers` to run container tests
- Default `verify` goal excludes container tests

**Rationale:**
- Fast feedback for standard `./mvnw verify`
- Opt-in for slower container tests
- CI/CD can run both in parallel stages

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
| Slow startup (~60s total) | Use `@Testcontainers` with reusable containers |
| Flaky if Docker unavailable | Skip with `@EnabledIf("dockerAvailable")` |
| Large image downloads first run | Document in README, cache in CI |
| Oracle licensing | Use `gvenzl/oracle-free` (free for dev/test) |

## Open Questions

1. **Ollama model selection**: Should we use a smaller model than llama3.2:1b for even faster tests? (Trade-off: less realistic responses)

2. **Container startup parallelization**: Should Oracle and Ollama start in parallel, or is sequential safer?

3. **Test data fixtures**: Should container tests use the same `TestFixtures` or separate realistic data?
