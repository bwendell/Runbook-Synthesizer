# Tasks: Integration Tests Implementation

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement comprehensive integration tests validating end-to-end component interactions in Runbook-Synthesizer.

**Architecture:** Helidon Test infrastructure with WireMock for external service mocking, in-memory vector store for RAG tests, and categorized test suites for CI/CD integration.

**Tech Stack:** Helidon Test, WireMock, AssertJ, Awaitility, JSON-B

---

## 1. Test Infrastructure Setup

### Task 1.1: Add Integration Test Dependencies [S]

**Files:**
- Modify: `pom.xml`

**Step 1: Write the failing test**
- Create a minimal integration test class that imports Helidon test annotations
- Hint: Use `@HelidonTest` annotation and `WebTarget` injection

**Step 2: Run test to verify it fails**
- Run: `wsl bash -c 'cd /mnt/c/Users/bwend/repos/ops-scribe && ./mvnw test -Dtest=*IT -q'`
- Expected: FAIL - dependencies not found

**Step 3: Add dependencies**
- Add `helidon-testing-junit5` for Helidon test support
- Add `wiremock-jre8-standalone` for HTTP mocking
- Add `awaitility` for async assertion support
- Add `assertj-core` for fluent assertions
- Hint: Check Helidon docs for correct version alignment

**Step 4: Run test to verify it compiles**
- Run: `wsl bash -c 'cd /mnt/c/Users/bwend/repos/ops-scribe && ./mvnw compile test-compile -q'`
- Expected: BUILD SUCCESS

**Step 5: Commit**
```bash
git add pom.xml
git commit -m "build: add integration test dependencies"
```

---

### Task 1.2: Create IntegrationTestBase [S]

**Files:**
- Create: `src/test/java/com/oracle/runbook/integration/IntegrationTestBase.java`

**Step 1: Write the test**
- Create base class with `@HelidonTest` annotation
- Inject `WebTarget` for HTTP requests
- Add `@BeforeAll` / `@AfterAll` for WireMock server lifecycle
- Hint: Use `WireMockServer` with dynamic port allocation

**Step 2: Verify compilation**
- Run: `wsl bash -c 'cd /mnt/c/Users/bwend/repos/ops-scribe && ./mvnw test-compile -q'`
- Expected: BUILD SUCCESS

**Step 3: Commit**
```bash
git add src/test/java/com/oracle/runbook/integration/
git commit -m "test: add IntegrationTestBase with WireMock setup"
```

---

### Task 1.3: Create TestFixtures Utility [S]

**Files:**
- Create: `src/test/java/com/oracle/runbook/integration/TestFixtures.java`

**Step 1: Write the failing test**
- Test that `TestFixtures.loadJson("alerts/high-memory-alert.json")` returns valid JSON
- Hint: Use `getClass().getResourceAsStream()` pattern

**Step 2: Run test to verify it fails**
- Run: `wsl bash -c 'cd /mnt/c/Users/bwend/repos/ops-scribe && ./mvnw test -Dtest=TestFixturesTest -q'`
- Expected: FAIL - class not found

**Step 3: Implement fixture loading**
- Load JSON from `src/test/resources/fixtures/`
- Parse with JSON-B to specified type
- Hint: Add `loadAs(path, Class<T>)` generic method

**Step 4: Run test to verify it passes**
- Expected: PASS

**Step 5: Commit**
```bash
git add src/test/
git commit -m "test: add TestFixtures utility for loading test data"
```

---

### Task 1.4: Create Sample Test Fixtures [S]

**Files:**
- Create: `src/test/resources/fixtures/alerts/oci-monitoring-alarm.json`
- Create: `src/test/resources/fixtures/alerts/high-memory-alert.json`
- Create: `src/test/resources/fixtures/contexts/enriched-vm-host.json`

**Step 1: Write OCI alarm fixture**
- Use OCI Monitoring Alarm JSON structure
- Hint: Reference OCI Events Service payload format

**Step 2: Write canonical alert fixture**
- Match `Alert` record structure from domain models
- Include dimensions: `compartmentId`, `resourceId`

**Step 3: Write enriched context fixture**
- Include `Alert`, `ResourceMetadata`, sample metrics, sample logs

**Step 4: Commit**
```bash
git add src/test/resources/fixtures/
git commit -m "test: add sample alert and context fixtures"
```

---

## 2. Alert Ingestion Integration Tests

### Task 2.1: AlertIngestionIT - Happy Path [M]

**Files:**
- Create: `src/test/java/com/oracle/runbook/integration/alert/AlertIngestionIT.java`

**Step 1: Write the failing test**
- POST valid OCI alarm JSON to `/api/v1/alerts`
- Assert HTTP 200 response
- Assert response contains `DynamicChecklist` structure
- Hint: Extend `IntegrationTestBase`, use `WebTarget.request().post()`

**Step 2: Run test to verify it fails**
- Run: `wsl bash -c 'cd /mnt/c/Users/bwend/repos/ops-scribe && ./mvnw test -Dtest=AlertIngestionIT -q'`
- Expected: FAIL - route not connected or mocks not configured

**Step 3: Configure WireMock stubs**
- Stub OCI Monitoring `/metrics` endpoint to return sample metrics
- Stub OCI Logging `/search` endpoint to return sample logs  
- Stub OCI GenAI `/generate` endpoint to return mock checklist text
- Hint: Use `stubFor(get(urlPathEqualTo(...)).willReturn(...))`

**Step 4: Run test to verify it passes**
- Expected: PASS with full flow executed

**Step 5: Commit**
```bash
git add src/test/java/com/oracle/runbook/integration/alert/
git commit -m "test: add AlertIngestionIT happy path"
```

---

### Task 2.2: AlertIngestionIT - Validation Errors [S]

**Files:**
- Modify: `src/test/java/com/oracle/runbook/integration/alert/AlertIngestionIT.java`

**Step 1: Write the failing test**
- POST invalid JSON (missing required fields) to `/api/v1/alerts`
- Assert HTTP 400 response
- Assert error message indicates validation failure
- Hint: Test with empty object `{}` and missing `title`

**Step 2: Run test to verify behavior**
- Expected: Should fail if validation not implemented, pass if it is

**Step 3: Implement validation (if needed)**
- Add request validation in alert handler
- Return proper HTTP 400 with error details

**Step 4: Commit**
```bash
git add src/
git commit -m "test: add AlertIngestionIT validation error cases"
```

---

### Task 2.3: AlertNormalizationIT [M]

**Files:**
- Create: `src/test/java/com/oracle/runbook/integration/alert/AlertNormalizationIT.java`

**Step 1: Write the failing test**
- POST OCI-specific alarm format
- Assert normalized `Alert` has correct `sourceService` = "oci-monitoring"
- Assert dimensions extracted from OCI payload
- Hint: OCI alarms have `body.alarmName`, `body.severity`, `body.dimensions`

**Step 2: Run test**
- Expected: FAIL if adapter not implemented

**Step 3: Wire adapter (if needed)**
- Ensure `OciMonitoringAlarmAdapter` transforms payloads correctly

**Step 4: Commit**
```bash
git add src/
git commit -m "test: add AlertNormalizationIT for OCI payloads"
```

---

## 3. Context Enrichment Integration Tests

### Task 3.1: ContextEnrichmentIT - Metrics Fetching [M]

**Files:**
- Create: `src/test/java/com/oracle/runbook/integration/enrichment/ContextEnrichmentIT.java`

**Step 1: Write the failing test**
- Given: Alert with `resourceId` dimension
- When: Enrichment service processes alert
- Then: `EnrichedContext.recentMetrics()` contains metrics from mock
- Hint: Stub OCI Monitoring to return CPU/Memory metrics

**Step 2: Run test to verify it fails**
- Expected: FAIL - metrics not fetched or not included

**Step 3: Configure mocks and verify**
- Stub `/monitoring/...` with sample `MetricSnapshot` data
- Verify enrichment service correctly populates context

**Step 4: Commit**
```bash
git add src/test/java/com/oracle/runbook/integration/enrichment/
git commit -m "test: add ContextEnrichmentIT metrics fetching"
```

---

### Task 3.2: ContextEnrichmentIT - Logs Fetching [M]

**Files:**
- Modify: `src/test/java/com/oracle/runbook/integration/enrichment/ContextEnrichmentIT.java`

**Step 1: Write the failing test**
- Given: Alert with `resourceId`
- When: Enrichment service processes alert
- Then: `EnrichedContext.recentLogs()` contains logs from mock
- Hint: Filter logs by time window (e.g., last 15 minutes)

**Step 2: Configure OCI Logging mock**
- Return sample `LogEntry` objects with timestamps and messages

**Step 3: Verify**
- Run test and confirm logs included in context

**Step 4: Commit**
```bash
git add src/
git commit -m "test: add ContextEnrichmentIT logs fetching"
```

---

### Task 3.3: ContextEnrichmentIT - Resource Metadata [S]

**Files:**
- Modify: `src/test/java/com/oracle/runbook/integration/enrichment/ContextEnrichmentIT.java`

**Step 1: Write the failing test**
- Assert `EnrichedContext.resource()` contains correct `shape`, `availabilityDomain`, `tags`
- Hint: Mock OCI Compute API `getInstance()` response

**Step 2: Configure OCI Compute mock**
- Return instance details with shape = "VM.Standard.E4.Flex"

**Step 3: Verify**
- Assert metadata correctly populated

**Step 4: Commit**
```bash
git add src/
git commit -m "test: add ContextEnrichmentIT resource metadata"
```

---

## 4. RAG Pipeline Integration Tests

### Task 4.1: VectorStoreIT - Store and Retrieve [M]

**Files:**
- Create: `src/test/java/com/oracle/runbook/integration/rag/VectorStoreIT.java`

**Step 1: Write the failing test**
- Store sample `RunbookChunk` with embedding
- Query with similar embedding vector
- Assert chunk returned with similarity score > threshold
- Hint: Use in-memory vector store implementation for tests

**Step 2: Run test**
- Expected: FAIL if vector store not implemented

**Step 3: Implement or configure in-memory store**
- Create test-specific `InMemoryVectorStore` if needed

**Step 4: Commit**
```bash
git add src/
git commit -m "test: add VectorStoreIT store and retrieve"
```

---

### Task 4.2: RetrievalIT - Top-K Retrieval [M]

**Files:**
- Create: `src/test/java/com/oracle/runbook/integration/rag/RetrievalIT.java`

**Step 1: Write the failing test**
- Seed vector store with 10 runbook chunks
- Query with context similar to 3 specific chunks
- Assert top-3 results match expected chunks
- Hint: Use fixed embeddings for deterministic testing

**Step 2: Run test**
- Expected: FAIL or PASS depending on retriever implementation

**Step 3: Verify ranking logic**
- Ensure retriever respects `topK` parameter

**Step 4: Commit**
```bash
git add src/
git commit -m "test: add RetrievalIT top-K retrieval"
```

---

### Task 4.3: EmbeddingServiceIT - Mock LLM Embeddings [S]

**Files:**
- Create: `src/test/java/com/oracle/runbook/integration/rag/EmbeddingServiceIT.java`

**Step 1: Write the failing test**
- Call embedding service with sample text
- Assert returns float[] of expected dimension (e.g., 1024 for Cohere)
- Hint: Mock OCI GenAI embeddings endpoint

**Step 2: Configure WireMock**
- Stub `/generativeai/.../embedText` to return fixed embedding vector

**Step 3: Verify**
- Assert embedding dimensions match model specification

**Step 4: Commit**
```bash
git add src/
git commit -m "test: add EmbeddingServiceIT with mocked LLM"
```

---

## 5. Checklist Generation Integration Tests

### Task 5.1: ChecklistGenerationIT - Full Pipeline [L]

**Files:**
- Create: `src/test/java/com/oracle/runbook/integration/generation/ChecklistGenerationIT.java`

**Step 1: Write the failing test**
- Given: Enriched context with memory alert, VM shape
- Given: Vector store seeded with memory runbook chunks
- When: Generator produces checklist
- Then: Checklist contains steps from memory runbook
- Then: Steps reference current metric values
- Hint: Mock LLM to return deterministic checklist JSON

**Step 2: Configure mocks**
- Seed vector store with sample chunks
- Mock LLM generation to return structured checklist

**Step 3: Verify**
- Assert `DynamicChecklist.steps()` contains expected instructions

**Step 4: Commit**
```bash
git add src/test/java/com/oracle/runbook/integration/generation/
git commit -m "test: add ChecklistGenerationIT full pipeline"
```

---

### Task 5.2: ChecklistGenerationIT - Shape Filtering [M]

**Files:**
- Modify: `src/test/java/com/oracle/runbook/integration/generation/ChecklistGenerationIT.java`

**Step 1: Write the failing test**
- Given: Context with non-GPU shape (VM.Standard)
- Given: Runbook chunks include GPU-only steps
- Then: Generated checklist excludes GPU steps
- Hint: Check `applicableShapes` metadata in chunk filtering

**Step 2: Verify filtering logic**
- Assert no `nvidia-smi` commands for non-GPU hosts

**Step 3: Commit**
```bash
git add src/
git commit -m "test: add ChecklistGenerationIT shape filtering"
```

---

## 6. Webhook Dispatcher Integration Tests

### Task 6.1: WebhookDispatcherIT - Multi-Channel [M]

**Files:**
- Create: `src/test/java/com/oracle/runbook/integration/webhook/WebhookDispatcherIT.java`

**Step 1: Write the failing test**
- Configure 2 webhook destinations (Slack, PagerDuty)
- Dispatch checklist with CRITICAL severity
- Assert both webhooks receive requests
- Hint: Use WireMock to capture webhook calls

**Step 2: Configure test webhooks**
- Stub Slack webhook URL
- Stub PagerDuty events API

**Step 3: Verify**
- Use `WireMock.verify(postRequestedFor(...))` to confirm calls

**Step 4: Commit**
```bash
git add src/test/java/com/oracle/runbook/integration/webhook/
git commit -m "test: add WebhookDispatcherIT multi-channel"
```

---

### Task 6.2: WebhookDispatcherIT - Severity Filtering [S]

**Files:**
- Modify: `src/test/java/com/oracle/runbook/integration/webhook/WebhookDispatcherIT.java`

**Step 1: Write the failing test**
- Configure webhook with filter `severities: [CRITICAL]`
- Dispatch checklist with WARNING severity
- Assert webhook NOT called
- Hint: Check `WebhookDestination.shouldSend()` logic

**Step 2: Verify**
- Use `WireMock.verify(0, postRequestedFor(...))` for no-call assertion

**Step 3: Commit**
```bash
git add src/
git commit -m "test: add WebhookDispatcherIT severity filtering"
```

---

### Task 6.3: SlackFormattingIT - Block Kit Output [M]

**Files:**
- Create: `src/test/java/com/oracle/runbook/integration/webhook/SlackFormattingIT.java`

**Step 1: Write the failing test**
- Generate checklist and send to Slack destination
- Capture request body from WireMock
- Assert JSON contains Slack Block Kit structure
- Hint: Look for `"type": "section"`, `"type": "divider"` blocks

**Step 2: Verify formatting**
- Parse captured body and validate structure

**Step 3: Commit**
```bash
git add src/
git commit -m "test: add SlackFormattingIT Block Kit validation"
```

---

## 7. End-to-End Flow Tests

### Task 7.1: AlertToChecklistIT - Happy Path [L]

**Files:**
- Create: `src/test/java/com/oracle/runbook/integration/e2e/AlertToChecklistIT.java`

**Step 1: Write the failing test**
- Full flow: POST OCI alarm → receive `DynamicChecklist` response
- Assert checklist has populated steps, source runbooks, LLM provider info
- Assert webhooks dispatched (verify WireMock)
- Hint: This ties together all previous integration tests

**Step 2: Configure all mocks**
- OCI Monitoring, Logging, Compute, GenAI
- Webhook destinations

**Step 3: Verify**
- Run complete flow and validate all assertions

**Step 4: Commit**
```bash
git add src/test/java/com/oracle/runbook/integration/e2e/
git commit -m "test: add AlertToChecklistIT end-to-end happy path"
```

---

### Task 7.2: ErrorPropagationIT - OCI Failure Handling [M]

**Files:**
- Create: `src/test/java/com/oracle/runbook/integration/e2e/ErrorPropagationIT.java`

**Step 1: Write the failing test**
- Configure OCI Monitoring mock to return 503 Service Unavailable
- POST valid alert
- Assert graceful degradation (partial context or error response)
- Hint: System should not crash, should log error

**Step 2: Test various failure modes**
- OCI Logging failure
- GenAI failure
- Webhook failure

**Step 3: Verify**
- Assert appropriate HTTP status and error messages

**Step 4: Commit**
```bash
git add src/
git commit -m "test: add ErrorPropagationIT failure handling"
```

---

### Task 7.3: ErrorPropagationIT - Timeout Handling [S]

**Files:**
- Modify: `src/test/java/com/oracle/runbook/integration/e2e/ErrorPropagationIT.java`

**Step 1: Write the failing test**
- Configure WireMock with `withFixedDelay(30000)` on GenAI endpoint
- Assert request times out gracefully within acceptable bounds
- Hint: Use `Awaitility` with timeout shorter than delay

**Step 2: Verify**
- Assert timeout error returned, not hang

**Step 3: Commit**
```bash
git add src/
git commit -m "test: add ErrorPropagationIT timeout handling"
```

---

## 8. Test Configuration and CI Integration

### Task 8.1: Configure Maven Surefire for Integration Tests [S]

**Files:**
- Modify: `pom.xml`

**Step 1: Configure test categorization**
- Unit tests: `*Test.java` (default)
- Integration tests: `*IT.java` (failsafe plugin)
- Hint: Add `maven-failsafe-plugin` configuration

**Step 2: Add test profiles**
- Profile `unit-tests`: Runs only unit tests (fast)
- Profile `integration-tests`: Runs IT tests with WireMock

**Step 3: Verify**
- Run: `wsl bash -c 'cd /mnt/c/Users/bwend/repos/ops-scribe && ./mvnw verify -q'`
- Expected: Both unit and integration tests run

**Step 4: Commit**
```bash
git add pom.xml
git commit -m "build: configure Maven Failsafe for integration tests"
```

---

### Task 8.2: Add Test Logging Configuration [S]

**Files:**
- Create: `src/test/resources/logging.properties`

**Step 1: Configure test logging**
- Set appropriate log levels for tests (FINE for application, WARNING for frameworks)
- Hint: Use java.util.logging format

**Step 2: Verify**
- Run tests and confirm log output is useful but not excessive

**Step 3: Commit**
```bash
git add src/test/resources/logging.properties
git commit -m "test: add logging configuration for integration tests"
```

---

### Task 8.3: Create WireMock Mappings Directory Structure [S]

**Files:**
- Create: `src/test/resources/wiremock/mappings/.gitkeep`
- Create: `src/test/resources/wiremock/__files/.gitkeep`

**Step 1: Create directory structure**
- WireMock expects `mappings/` for stub definitions
- WireMock expects `__files/` for response bodies

**Step 2: Add documentation**
- Brief README in wiremock folder explaining structure

**Step 3: Commit**
```bash
git add src/test/resources/wiremock/
git commit -m "test: scaffold WireMock directory structure"
```

---

## Summary

| Task | Complexity | Focus Area |
|------|------------|------------|
| 1.1-1.4 | S-S-S-S | Infrastructure |
| 2.1-2.3 | M-S-M | Alert Ingestion |
| 3.1-3.3 | M-M-S | Context Enrichment |
| 4.1-4.3 | M-M-S | RAG Pipeline |
| 5.1-5.2 | L-M | Checklist Generation |
| 6.1-6.3 | M-S-M | Webhook Dispatcher |
| 7.1-7.3 | L-M-S | End-to-End |
| 8.1-8.3 | S-S-S | CI Configuration |

**Total: 22 tasks** (8 Small, 10 Medium, 4 Large)

---

## Test Commands Reference

```bash
# Run all tests
wsl bash -c 'cd /mnt/c/Users/bwend/repos/ops-scribe && ./mvnw verify -q'

# Run only unit tests
wsl bash -c 'cd /mnt/c/Users/bwend/repos/ops-scribe && ./mvnw test -q'

# Run only integration tests
wsl bash -c 'cd /mnt/c/Users/bwend/repos/ops-scribe && ./mvnw failsafe:integration-test -q'

# Run specific integration test
wsl bash -c 'cd /mnt/c/Users/bwend/repos/ops-scribe && ./mvnw verify -Dit.test=AlertIngestionIT -q'

# Run with verbose output
wsl bash -c 'cd /mnt/c/Users/bwend/repos/ops-scribe && ./mvnw verify -Dsurefire.useFile=false'
```
