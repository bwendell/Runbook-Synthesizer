# Tasks: Standardize Testing Patterns

## 1. Fix High-Priority Anti-Pattern Tests

### 1.1 Refactor WebhookDispatcherTest to Use Outcome-Based Assertions
- [x] **Task**: Replace Mockito `verify()` calls with assertions on actual outcomes
- **Files**: `src/test/java/com/oracle/runbook/output/WebhookDispatcherTest.java`
- **Acceptance Criteria**:
  - All 10 `verify()` calls are removed or replaced with outcome assertions
  - Tests assert on `List<WebhookResult>` return values instead of mock interactions
  - Test behavior remains the same (same conditions tested)
- **Verification Steps**:
  ```bash
  mvn test -Dtest=WebhookDispatcherTest
  # All tests pass
  # Verify no verify() calls remain: rg "verify\(" src/test/java/com/oracle/runbook/output/WebhookDispatcherTest.java
  # Should return empty
  ```

### 1.2 Refactor OciLoggingAdapterTest to Use Behavioral Tests
- [x] **Task**: Remove reflection-based tests and replace with actual behavior tests using mocks
- **Files**: `src/test/java/com/oracle/runbook/enrichment/OciLoggingAdapterTest.java`
- **Acceptance Criteria**:
  - Remove `testSourceTypeContract()` reflection test
  - Remove `testFetchLogsContract()` reflection test
  - Add actual behavioral test: `sourceType_ReturnsOciLogging()`
  - Keep null-check constructor tests (valid behavioral tests)
- **Verification Steps**:
  ```bash
  mvn test -Dtest=OciLoggingAdapterTest
  # All tests pass
  # Verify no reflection: rg "getMethod|getDeclaredMethod" src/test/java/com/oracle/runbook/enrichment/OciLoggingAdapterTest.java
  # Should return empty
  ```

### 1.3 Fix OciMonitoringAdapterTest Reflection Tests (if present)
- [x] **Task**: Check for similar reflection patterns and fix if present
- **Files**: `src/test/java/com/oracle/runbook/enrichment/OciMonitoringAdapterTest.java`
- **Acceptance Criteria**:
  - No reflection-based method signature tests remain
  - All tests verify actual behavior
- **Verification Steps**:
  ```bash
  mvn test -Dtest=OciMonitoringAdapterTest
  ```

---

## 2. Migrate Unit Tests to AssertJ

### 2.1 Migrate Domain Model Tests to AssertJ
- [ ] **Task**: Convert all domain tests from JUnit to AssertJ assertions
- **Files** (13 files):
  - `domain/AlertTest.java`
  - `domain/AlertSeverityTest.java`
  - `domain/ChecklistStepTest.java`
  - `domain/DynamicChecklistTest.java`
  - `domain/EnrichedContextTest.java`
  - `domain/GenerationConfigTest.java`
  - `domain/LogEntryTest.java`
  - `domain/MetricSnapshotTest.java`
  - `domain/ResourceMetadataTest.java`
  - `domain/RetrievedChunkTest.java`
  - `domain/RunbookChunkTest.java`
  - `domain/StepPriorityTest.java`
- **Acceptance Criteria**:
  - Import `org.assertj.core.api.Assertions.assertThat` instead of JUnit
  - Replace `assertEquals()` with `assertThat().isEqualTo()`
  - Replace `assertTrue()` with `assertThat().isTrue()`
  - Replace `assertThrows()` with `assertThatThrownBy()`
- **Verification Steps**:
  ```bash
  mvn test -Dtest="com.oracle.runbook.domain.*Test"
  # All tests pass
  # Verify JUnit assertions removed:
  rg "import static org.junit.jupiter.api.Assertions" src/test/java/com/oracle/runbook/domain/
  # Should return empty
  ```

### 2.2 Migrate API Tests to AssertJ
- [ ] **Task**: Convert API resource tests to AssertJ
- **Files** (6 files):
  - `api/AlertResourceTest.java`
  - `api/HealthResourceTest.java`
  - `api/RunbookResourceTest.java`
  - `api/WebhookResourceTest.java`
  - `api/dto/*Test.java` (5 files)
- **Acceptance Criteria**:
  - All assertions use AssertJ
  - Fluent assertion chains used for complex checks
- **Verification Steps**:
  ```bash
  mvn test -Dtest="com.oracle.runbook.api.*Test,com.oracle.runbook.api.dto.*Test"
  ```

### 2.3 Migrate Output Tests to AssertJ
- [ ] **Task**: Convert webhook/output tests to AssertJ
- **Files** (11 files in `output/` package)
- **Acceptance Criteria**:
  - All assertions use AssertJ
  - Keep WireMock `verify()` calls (acceptable for HTTP verification)
- **Verification Steps**:
  ```bash
  mvn test -Dtest="com.oracle.runbook.output.*Test"
  ```

### 2.4 Migrate RAG Tests to AssertJ
- [ ] **Task**: Convert RAG pipeline tests to AssertJ
- **Files** (10 files in `rag/` package)
- **Acceptance Criteria**:
  - All assertions use AssertJ
- **Verification Steps**:
  ```bash
  mvn test -Dtest="com.oracle.runbook.rag.*Test"
  ```

### 2.5 Migrate Enrichment Tests to AssertJ
- [ ] **Task**: Convert enrichment adapter tests to AssertJ
- **Files** (6 files in `enrichment/` package)
- **Acceptance Criteria**:
  - All assertions use AssertJ
- **Verification Steps**:
  ```bash
  mvn test -Dtest="com.oracle.runbook.enrichment.*Test"
  ```

### 2.6 Migrate Config Tests to AssertJ
- [ ] **Task**: Convert configuration tests to AssertJ
- **Files** (3 files in `config/` package)
- **Acceptance Criteria**:
  - All assertions use AssertJ
- **Verification Steps**:
  ```bash
  mvn test -Dtest="com.oracle.runbook.config.*Test"
  ```

---

## 3. Expand Test Fixtures

### 3.1 Add Additional Fixture Files
- [ ] **Task**: Create new fixture files for common test scenarios
- **Files to Create**:
  - `fixtures/contexts/enriched-context-memory.json`
  - `fixtures/checklists/sample-checklist.json`
  - `fixtures/webhooks/slack-payload.json`
  - `fixtures/webhooks/pagerduty-payload.json`
- **Acceptance Criteria**:
  - Each fixture file is valid JSON
  - Fixtures can be loaded via `TestFixtures.loadAs()`
- **Verification Steps**:
  ```bash
  mvn test -Dtest=TestFixturesTest
  ```

### 3.2 Update Tests to Use New Fixtures
- [ ] **Task**: Refactor tests that inline test data to use fixtures
- **Priority Files**:
  - `ChecklistGeneratorTest.java` - replace `createTestContext()` helper
  - `WebhookDispatcherTest.java` - replace `createTestChecklist()` helper
- **Acceptance Criteria**:
  - Helper methods that create test data are removed
  - Tests load data via `TestFixtures.loadAs()`
- **Verification Steps**:
  ```bash
  mvn test -Dtest="ChecklistGeneratorTest,WebhookDispatcherTest"
  ```

---

## 4. Minor Cleanups

### 4.1 Fix TestFixturesTest Assertion Wrappers
- [ ] **Task**: Remove unnecessary `Objects.requireNonNull()` wrappers around AssertJ assertions
- **Files**: `src/test/java/com/oracle/runbook/integration/TestFixturesTest.java`
- **Acceptance Criteria**:
  - Line 18, 19, 32 no longer use `Objects.requireNonNull()`
  - Assertions are direct AssertJ calls
- **Verification Steps**:
  ```bash
  mvn test -Dtest=TestFixturesTest
  # Verify: rg "Objects.requireNonNull" src/test/java/com/oracle/runbook/integration/TestFixturesTest.java
  # Should return empty
  ```

---

## 5. Validation

### 5.1 Run Full Test Suite
- [ ] **Task**: Verify all tests pass after changes
- **Acceptance Criteria**:
  - `mvn test` completes with 0 failures
  - `mvn verify -Pit` completes with 0 failures (integration tests)
- **Verification Steps**:
  ```bash
  mvn clean verify -Pit
  ```

### 5.2 Verify Pattern Compliance
- [ ] **Task**: Confirm no remaining violations
- **Acceptance Criteria**:
  - No files import JUnit assertions: `rg "import static org.junit.jupiter.api.Assertions" src/test/java/com/oracle/runbook/`
  - No reflection-based tests: `rg "getMethod|getDeclaredMethod" src/test/java/com/oracle/runbook/`
- **Verification Steps**:
  ```bash
  rg "import static org.junit.jupiter.api.Assertions" src/test/java/com/oracle/runbook/
  # Expected: No results
  
  rg "getMethod|getDeclaredMethod" src/test/java/com/oracle/runbook/
  # Expected: No results
  ```

---

## Dependencies & Notes

- **Parallelizable**: Tasks 2.1-2.6 can run in parallel
- **Sequential**: Task 1.x must complete before Task 5.x validation
- **Risk mitigation**: Each task includes individual verification steps to catch issues early
- **Estimated effort**: ~2-3 hours for complete migration
