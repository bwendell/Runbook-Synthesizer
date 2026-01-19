# Design: Expand Test Coverage for Maximum Local Testing

## Context

The Runbook Synthesizer supports both OCI and AWS cloud providers. While AWS services can be tested locally using LocalStack (Testcontainers), OCI-specific code currently has limited test coverage because OCI lacks an equivalent local mock service.

**Current State:**
- 98 test files covering 78 source files
- LocalStack infrastructure for AWS (S3, CloudWatch, CloudWatch Logs) ✅
- Oracle 23ai Testcontainers for vector store ✅
- Ollama Testcontainers for LLM ✅
- OCI SDK tests rely on mocks with minimal coverage

**Stakeholders:**
- AI coding agents implementing this proposal
- Human reviewers validating test quality
- CI/CD pipelines running automated tests

## Goals / Non-Goals

### Goals
1. **Maximize unit test coverage** without cloud dependencies
2. **Create in-memory adapters** for cloud-free integration testing
3. **Document testing patterns** for AI agents to follow
4. **Enable 95%+ coverage** achievable in local development

### Non-Goals
- Adding new production features
- Changing OCI SDK usage patterns
- Creating mock OCI services (no LocalStack equivalent exists)
- Performance testing

## Decisions

### Decision 1: Use Mock-Based Unit Tests for OCI Clients
**What:** Add Mockito-based unit tests for `OciObjectStorageClient`  
**Why:** OCI SDK clients cannot be tested against LocalStack; mock-based tests provide fast feedback on behavior  
**Alternatives considered:**
- WireMock for OCI API simulation → Too complex, requires OCI API knowledge
- Integration tests only → No fast local feedback loop

### Decision 2: Create In-Memory Adapter Implementations
**What:** Add `InMemoryCloudStorageAdapter`, `StubMetricsSourceAdapter`, `StubLogSourceAdapter`  
**Why:** Enables integration tests without any cloud services; follows hexagonal architecture  
**Trade-off:** Stub behavior may drift from real implementation → mitigated by maintaining OCI SDK mock tests

### Decision 3: Add Prompt Template Validation Tests
**What:** Create unit tests for `PromptTemplates` verifying placeholder structure  
**Why:** LLM prompt changes are high-risk; template structure should be validated  
**Alternatives considered:**
- Integration tests with Ollama → Slow, doesn't catch structural issues
- No tests → Unacceptable risk for core functionality

### Decision 4: Test Error Paths in RagPipelineService
**What:** Add tests for null inputs, service failures, and exception propagation  
**Why:** Current tests only cover happy path; production errors would go undetected  
**Pattern:** Use `assertThatThrownBy()` for exception testing per `testing-patterns-java`

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| Mock behavior drifts from real SDK | Maintain small set of real-SDK tests; review mock expectations during SDK upgrades |
| In-memory adapters diverge from real adapters | Use same interface; add contract tests for interface compliance |
| Test maintenance overhead | Use `TestFixtures` for shared test data; keep tests focused and minimal |
| Over-mocking hides integration issues | Maintain LocalStack tests for AWS; use real Testcontainers where possible |

## Component Architecture

```
┌────────────────────────────────────────────────────────────────┐
│                        Test Pyramid                              │
├────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │     E2E Tests (AlertToChecklistIT, ErrorPropagationIT)   │   │
│  │     - Real Helidon server                                │   │
│  │     - WireMock/LocalStack/Testcontainers                 │   │
│  └─────────────────────────────────────────────────────────┘   │
│                           ▲                                      │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │    Integration Tests (*IT.java)                          │   │
│  │    - LocalStack for AWS (S3, CloudWatch)                 │   │
│  │    - In-memory adapters for OCI                          │   │
│  │    - WireMock for external webhooks                      │   │
│  └─────────────────────────────────────────────────────────┘   │
│                           ▲                                      │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │    Unit Tests (*Test.java)                               │   │
│  │    - Mocks for SDK clients                               │   │
│  │    - TestFixtures for domain data                        │   │
│  │    - Real implementations where possible                 │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                  │
└────────────────────────────────────────────────────────────────┘
```

## In-Memory Adapter Design

```java
// InMemoryCloudStorageAdapter implements CloudStorageAdapter
// - Uses ConcurrentHashMap<String, Map<String, String>> for buckets
// - Key = bucket name, Value = Map<objectKey, content>
// - Implements listRunbooks(), getRunbookContent() with filtering

// StubMetricsSourceAdapter implements MetricsSourceAdapter
// - Returns configurable List<MetricSnapshot>
// - Supports setMetrics() for test setup

// StubLogSourceAdapter implements LogSourceAdapter
// - Returns configurable List<LogEntry>
// - Supports setLogs() for test setup
```

## File Organization

```
src/test/java/com/oracle/runbook/
├── rag/
│   ├── PromptTemplatesTest.java    [NEW]
│   ├── ScoredChunkTest.java        [NEW]
│   ├── OciObjectStorageClientTest.java [MODIFIED]
│   └── RagPipelineServiceTest.java [MODIFIED]
├── integration/
│   ├── LocalStackContainerBase.java [MODIFIED - add EC2]
│   ├── AwsRagPipelineIT.java       [NEW]
│   ├── CloudProviderSwitchingIT.java [NEW]
│   └── stubs/
│       ├── InMemoryCloudStorageAdapter.java [NEW]
│       ├── StubMetricsSourceAdapter.java    [NEW]
│       └── StubLogSourceAdapter.java        [NEW]
```

## Open Questions

1. **Q:** Should in-memory adapters live in `src/test/java` or `src/main/java`?
   **A:** `src/test/java` - they are test infrastructure, not production code.

2. **Q:** Should we add JaCoCo coverage reporting now?
   **A:** Deferred to follow-up change - focus on test content first.

3. **Q:** Should EC2 be added to LocalStack given limited API support?
   **A:** Yes, for DescribeInstances only - matches our adapter usage.
