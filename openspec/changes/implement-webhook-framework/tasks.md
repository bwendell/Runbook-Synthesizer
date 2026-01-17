# Webhook Output Framework Tasks

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement configurable multi-channel webhook output framework for delivering generated checklists.

**Architecture:** Port/adapter pattern with async dispatch. Core `WebhookDestination` interface defines contract, `WebhookDispatcher` handles routing/retry logic, concrete adapters implement channel-specific formatting.

**Tech Stack:** Java 25 records, Helidon SE WebClient, CompletableFuture for async.

---

## Task 1: WebhookResult Record [S]

**Files:**
- Create: `src/main/java/com/oracle/runbook/output/WebhookResult.java`
- Test: `src/test/java/com/oracle/runbook/output/WebhookResultTest.java`

**Acceptance Criteria:**
- Immutable record capturing delivery outcome
- Fields: `destinationName`, `success`, `statusCode`, `errorMessage` (Optional), `sentAt`
- Factory methods: `success(name, statusCode)`, `failure(name, errorMessage)`

**Hints:**
- Use `Optional<String>` for nullable error message
- Add `isSuccess()` convenience method that checks `success` field
- Consider including response time in milliseconds for observability

**Test Command:**
```bash
wsl bash -c "cd /mnt/c/Users/bwend/repos/ops-scribe && ./mvnw test -Dtest=WebhookResultTest -q"
```

**Commit:** `feat(output): add WebhookResult record`

---

## Task 2: WebhookFilter Record [S]

**Files:**
- Create: `src/main/java/com/oracle/runbook/output/WebhookFilter.java`
- Test: `src/test/java/com/oracle/runbook/output/WebhookFilterTest.java`

**Acceptance Criteria:**
- Immutable record for filtering which checklists to send
- Fields: `severities` (Set<AlertSeverity>), `requiredLabels` (Map<String, String>)
- Method: `matches(DynamicChecklist)` → boolean
- Empty filter matches everything (permissive default)

**Hints:**
- Import `AlertSeverity` from domain package
- Use `DynamicChecklist.alertId()` to look up original alert severity
- Consider adding static factory `allowAll()` for no-filter case

**Dependencies:** Requires `AlertSeverity` from domain models

**Test Command:**
```bash
wsl bash -c "cd /mnt/c/Users/bwend/repos/ops-scribe && ./mvnw test -Dtest=WebhookFilterTest -q"
```

**Commit:** `feat(output): add WebhookFilter record`

---

## Task 3: WebhookConfig Record [S]

**Files:**
- Create: `src/main/java/com/oracle/runbook/output/WebhookConfig.java`
- Test: `src/test/java/com/oracle/runbook/output/WebhookConfigTest.java`

**Acceptance Criteria:**
- Immutable record for webhook destination configuration
- Fields: `name`, `type`, `url`, `enabled`, `filter` (WebhookFilter), `headers` (Map)
- Validation: name and url are required, type must be non-empty
- Builder pattern for ergonomic construction

**Hints:**
- Use inner `Builder` class with fluent API
- Validate URL format in builder's `build()` method
- Default `enabled` to true, default `headers` to empty map

**Test Command:**
```bash
wsl bash -c "cd /mnt/c/Users/bwend/repos/ops-scribe && ./mvnw test -Dtest=WebhookConfigTest -q"
```

**Commit:** `feat(output): add WebhookConfig record with builder`

---

## Task 4: WebhookDestination Interface [S]

**Files:**
- Create: `src/main/java/com/oracle/runbook/output/WebhookDestination.java`
- Test: `src/test/java/com/oracle/runbook/output/WebhookDestinationTest.java`

**Acceptance Criteria:**
- Interface defining the output port contract
- Methods:
  - `String name()` - unique identifier
  - `String type()` - destination type ("slack", "pagerduty", "generic")
  - `CompletableFuture<WebhookResult> send(DynamicChecklist checklist)`
  - `boolean shouldSend(DynamicChecklist checklist)` - filter logic
  - `WebhookConfig config()` - access underlying configuration

**Hints:**
- Default `shouldSend` can delegate to `config().filter().matches()`
- Consider adding `default` method implementation for `shouldSend`
- Document expected exception handling in Javadoc

**Test Command:**
```bash
wsl bash -c "cd /mnt/c/Users/bwend/repos/ops-scribe && ./mvnw test -Dtest=WebhookDestinationTest -q"
```

**Commit:** `feat(output): add WebhookDestination port interface`

---

## Task 5: GenericWebhookDestination Adapter [M]

**Files:**
- Create: `src/main/java/com/oracle/runbook/output/adapters/GenericWebhookDestination.java`
- Test: `src/test/java/com/oracle/runbook/output/adapters/GenericWebhookDestinationTest.java`

**Acceptance Criteria:**
- Implements `WebhookDestination` interface
- Sends JSON-serialized `DynamicChecklist` via HTTP POST
- Includes configured headers in request
- Returns `WebhookResult.success()` on 2xx, `failure()` otherwise
- Handles connection timeouts gracefully

**Hints:**
- Use Helidon `WebClient` for HTTP calls
- Serialize checklist using Jackson `ObjectMapper`
- Set Content-Type to `application/json`
- Configure 10s connection timeout, 30s read timeout
- Wrap exceptions in `WebhookResult.failure()`

**Test Command:**
```bash
wsl bash -c "cd /mnt/c/Users/bwend/repos/ops-scribe && ./mvnw test -Dtest=GenericWebhookDestinationTest -q"
```

**Commit:** `feat(output): add GenericWebhookDestination adapter`

---

## Task 6: SlackWebhookDestination Stub [S]

**Files:**
- Create: `src/main/java/com/oracle/runbook/output/adapters/SlackWebhookDestination.java`
- Test: `src/test/java/com/oracle/runbook/output/adapters/SlackWebhookDestinationTest.java`

**Acceptance Criteria:**
- Implements `WebhookDestination` interface
- `type()` returns "slack"
- `send()` throws `UnsupportedOperationException` with message "Slack integration available in v1.1"
- Document planned Block Kit formatting in Javadoc

**Hints:**
- Stub class structure mirrors `GenericWebhookDestination`
- Add `@since 1.1` Javadoc tag for future implementation
- Include placeholder method `formatAsBlockKit(DynamicChecklist)` returning null

**Test Command:**
```bash
wsl bash -c "cd /mnt/c/Users/bwend/repos/ops-scribe && ./mvnw test -Dtest=SlackWebhookDestinationTest -q"
```

**Commit:** `feat(output): add SlackWebhookDestination stub`

---

## Task 7: PagerDutyWebhookDestination Stub [S]

**Files:**
- Create: `src/main/java/com/oracle/runbook/output/adapters/PagerDutyWebhookDestination.java`
- Test: `src/test/java/com/oracle/runbook/output/adapters/PagerDutyWebhookDestinationTest.java`

**Acceptance Criteria:**
- Implements `WebhookDestination` interface
- `type()` returns "pagerduty"
- Constructor accepts `routingKey` in addition to `WebhookConfig`
- `send()` throws `UnsupportedOperationException` with message "PagerDuty integration available in v1.1"
- Document planned Events API v2 format in Javadoc

**Hints:**
- PagerDuty Events API v2 endpoint: `https://events.pagerduty.com/v2/enqueue`
- Add placeholder for `buildPagerDutyPayload(DynamicChecklist, routingKey)`
- Include `dedup_key` concept in Javadoc for future implementation

**Test Command:**
```bash
wsl bash -c "cd /mnt/c/Users/bwend/repos/ops-scribe && ./mvnw test -Dtest=PagerDutyWebhookDestinationTest -q"
```

**Commit:** `feat(output): add PagerDutyWebhookDestination stub`

---

## Task 8: WebhookDispatcher Service [M]

**Files:**
- Create: `src/main/java/com/oracle/runbook/output/WebhookDispatcher.java`
- Test: `src/test/java/com/oracle/runbook/output/WebhookDispatcherTest.java`

**Acceptance Criteria:**
- Orchestrates delivery to multiple `WebhookDestination` instances
- Method: `CompletableFuture<List<WebhookResult>> dispatch(DynamicChecklist checklist)`
- Only sends to destinations where `shouldSend()` returns true
- Dispatches to all matching destinations in parallel
- Collects and returns all results

**Hints:**
- Use `CompletableFuture.allOf()` to wait for all sends
- Filter destinations using stream before dispatch
- Log skipped destinations at DEBUG level
- Consider adding `dispatchSync()` for testing convenience

**Test Command:**
```bash
wsl bash -c "cd /mnt/c/Users/bwend/repos/ops-scribe && ./mvnw test -Dtest=WebhookDispatcherTest -q"
```

**Commit:** `feat(output): add WebhookDispatcher service`

---

## Task 9: WebhookDestinationFactory [M]

**Files:**
- Create: `src/main/java/com/oracle/runbook/output/WebhookDestinationFactory.java`
- Test: `src/test/java/com/oracle/runbook/output/WebhookDestinationFactoryTest.java`

**Acceptance Criteria:**
- Factory for creating `WebhookDestination` instances from `WebhookConfig`
- Method: `WebhookDestination create(WebhookConfig config)`
- Supports types: "generic", "slack", "pagerduty"
- Throws `IllegalArgumentException` for unknown types

**Hints:**
- Use switch expression for type routing
- Extract PagerDuty `routingKey` from config headers
- Log creation at INFO level with destination name and type
- Consider registry pattern for extensibility

**Test Command:**
```bash
wsl bash -c "cd /mnt/c/Users/bwend/repos/ops-scribe && ./mvnw test -Dtest=WebhookDestinationFactoryTest -q"
```

**Commit:** `feat(output): add WebhookDestinationFactory`

---

## Task 10: Webhook Configuration Loading [M]

**Files:**
- Modify: `src/main/java/com/oracle/runbook/config/AppConfig.java`
- Modify: `src/main/resources/application.yaml`
- Test: `src/test/java/com/oracle/runbook/config/WebhookConfigLoaderTest.java`

**Acceptance Criteria:**
- Parse `output.webhooks` list from application.yaml
- Map YAML to `List<WebhookConfig>` objects
- Support environment variable substitution in URLs and headers
- Validate all configs on application startup

**Hints:**
- Use Helidon Config's `Config.mapList()` for list mapping
- Create helper method `toWebhookConfig(Config)` for mapping
- Log loaded webhook count at startup
- Skip disabled webhooks during initialization

**Test Command:**
```bash
wsl bash -c "cd /mnt/c/Users/bwend/repos/ops-scribe && ./mvnw test -Dtest=WebhookConfigLoaderTest -q"
```

**Commit:** `feat(config): add webhook configuration loading`

---

## Task 11: Webhook Output Integration [L]

**Files:**
- Modify: `src/main/java/com/oracle/runbook/RunbookSynthesizerApp.java`
- Create: `src/test/java/com/oracle/runbook/output/WebhookIntegrationTest.java`
- Create: `src/test/resources/wiremock/` (stubs directory)

**Acceptance Criteria:**
- Wire `WebhookDispatcher` into application context
- Initialize destinations from loaded `WebhookConfig` list
- After checklist generation, automatically dispatch to webhooks
- Integration test verifies full flow with WireMock

**Hints:**
- Add WireMock dependency to test scope in pom.xml
- Create stub files for mock webhook endpoints
- Test with 2+ destinations to verify parallel dispatch
- Verify request body contains expected checklist JSON

**Test Command:**
```bash
wsl bash -c "cd /mnt/c/Users/bwend/repos/ops-scribe && ./mvnw test -Dtest=WebhookIntegrationTest -q"
```

**Commit:** `feat(app): integrate webhook dispatch into main flow`

---

## Task 12: Webhook Error Handling & Retry [M]

**Files:**
- Modify: `src/main/java/com/oracle/runbook/output/WebhookDispatcher.java`
- Modify: `src/main/java/com/oracle/runbook/output/WebhookConfig.java`
- Test: `src/test/java/com/oracle/runbook/output/WebhookRetryTest.java`

**Acceptance Criteria:**
- Add `retryCount` and `retryDelayMs` to `WebhookConfig`
- Implement exponential backoff retry in dispatcher
- Only retry on 5xx errors and connection failures
- Log retry attempts with destination name and attempt number

**Hints:**
- Default: 3 retries, 1000ms initial delay
- Use `CompletableFuture.delayedExecutor()` for delays
- Calculate delay: `initialDelay * 2^attemptNumber`
- Don't retry on 4xx client errors

**Test Command:**
```bash
wsl bash -c "cd /mnt/c/Users/bwend/repos/ops-scribe && ./mvnw test -Dtest=WebhookRetryTest -q"
```

**Commit:** `feat(output): add webhook retry with exponential backoff`

---

## Summary

| Task | Complexity | Dependencies |
|------|------------|--------------|
| 1. WebhookResult | [S] | None |
| 2. WebhookFilter | [S] | Domain models |
| 3. WebhookConfig | [S] | None |
| 4. WebhookDestination Interface | [S] | Tasks 1-3 |
| 5. GenericWebhookDestination | [M] | Task 4 |
| 6. SlackWebhookDestination Stub | [S] | Task 4 |
| 7. PagerDutyWebhookDestination Stub | [S] | Task 4 |
| 8. WebhookDispatcher | [M] | Tasks 4-7 |
| 9. WebhookDestinationFactory | [M] | Tasks 5-7 |
| 10. Configuration Loading | [M] | Task 3 |
| 11. Integration | [L] | Tasks 8-10 |
| 12. Retry Logic | [M] | Task 8 |

**Total Tasks:** 12
**Estimated Time:** 4-6 hours (following TDD cycle)
