# Tasks: Wire AlertResource to RAG Pipeline

## Task Summary

| Task | Description | Complexity | Dependencies |
|------|-------------|------------|--------------|
| 1 | Add constructor with dependency injection | S | None |
| 2 | Implement mode switching logic | S | Task 1 |
| 3 | Update RunbookSynthesizerApp wiring | M | Tasks 1-2 |
| 4 | Update existing unit tests | S | Task 1 |
| 5 | Update integration tests | S | Task 1 |
| 6 | Add new integration test for real mode | M | Tasks 1-3 |

---

## Task 1: Add AlertResource constructor with dependency injection [S]

**Files**:
- Modify: `src/main/java/com/oracle/runbook/api/AlertResource.java`

**Acceptance Criteria**:
- [ ] Add constructor accepting `RagPipelineService`, `WebhookDispatcher`, `boolean stubMode`
- [ ] Store dependencies as final fields
- [ ] Add backward-compatible no-arg constructor defaulting to stub mode

**Hints**:
```java
private final RagPipelineService ragPipeline;
private final WebhookDispatcher webhookDispatcher;
private final boolean stubMode;

public AlertResource(RagPipelineService ragPipeline, 
                     WebhookDispatcher webhookDispatcher,
                     boolean stubMode) { ... }

public AlertResource() {
  this(null, null, true);  // Backward compatible stub mode
}
```

**Verification**:
```powershell
wsl ./mvnw test -Dtest=AlertResourceTest -q
```

---

## Task 2: Implement mode switching logic in handlePost [S]

**Files**:
- Modify: `src/main/java/com/oracle/runbook/api/AlertResource.java`

**Acceptance Criteria**:
- [ ] Check `stubMode` flag in `handlePost()`
- [ ] If stub mode: use existing `generateStubChecklist()` 
- [ ] If real mode: convert `AlertRequest` to `Alert`, call `ragPipeline.processAlert()`, convert result to `ChecklistResponse`
- [ ] In real mode: dispatch to webhooks after returning response
- [ ] Handle exceptions from pipeline with 500 status

**Hints**:
```java
if (stubMode) {
  var checklistResponse = generateStubChecklist(alertRequest);
  res.send(toJson(checklistResponse));
} else {
  Alert alert = convertToAlert(alertRequest);
  DynamicChecklist checklist = ragPipeline.processAlert(alert, 5).join();
  var checklistResponse = convertToResponse(checklist);
  res.send(toJson(checklistResponse));
  webhookDispatcher.dispatch(checklist);
}
```

**Verification**:
```powershell
wsl ./mvnw test -Dtest=AlertResourceTest -q
```

---

## Task 3: Update RunbookSynthesizerApp wiring [M]

**Files**:
- Modify: `src/main/java/com/oracle/runbook/RunbookSynthesizerApp.java`

**Acceptance Criteria**:
- [ ] Create stub implementations of pipeline dependencies in main app
- [ ] Instantiate `RagPipelineService` with dependencies
- [ ] Instantiate `WebhookDispatcher` with configured destinations
- [ ] Pass dependencies to `AlertResource` constructor  
- [ ] Add configuration reading for stub vs real mode

**Hints**:
```java
// For now, use stub mode - real implementations will be wired in future
boolean stubMode = config.get("app.stub-mode").asBoolean().orElse(true);

if (stubMode) {
  routing.register("/api/v1/alerts", new AlertResource());
} else {
  // Wire real dependencies
  var ragPipeline = createRagPipeline(config);
  var dispatcher = createWebhookDispatcher(config);
  routing.register("/api/v1/alerts", 
      new AlertResource(ragPipeline, dispatcher, false));
}
```

**Verification**:
```powershell
wsl ./mvnw compile -q
wsl ./mvnw test -Dtest=AlertResourceTest,ApiRoutingIntegrationTest -q
```

---

## Task 4: Update existing unit tests [S]

**Files**:
- Modify: `src/test/java/com/oracle/runbook/api/AlertResourceTest.java`

**Acceptance Criteria**:
- [ ] Tests continue to pass with no-arg constructor (stub mode)
- [ ] Optionally add test verifying stub mode behavior explicitly

**Verification**:
```powershell
wsl ./mvnw test -Dtest=AlertResourceTest -q
```

---

## Task 5: Update integration tests [S]

**Files**:
- Modify: `src/test/java/com/oracle/runbook/integration/alert/AlertIngestionIT.java`
- Modify: `src/test/java/com/oracle/runbook/integration/alert/AlertNormalizationIT.java`
- Modify: `src/test/java/com/oracle/runbook/api/ApiRoutingIntegrationTest.java`

**Acceptance Criteria**:
- [ ] Integration tests continue to pass with no-arg constructor
- [ ] No functional changes needed if backward-compat constructor works

**Verification**:
```powershell
wsl ./mvnw verify -Dtest.skip=false -DskipITs=false -q
```

---

## Task 6: Add integration test for real mode [M]

**Files**:
- Modify: `src/test/java/com/oracle/runbook/integration/e2e/AlertToChecklistIT.java`

**Acceptance Criteria**:
- [ ] Add test method that wires `AlertResource` with real `RagPipelineService`
- [ ] Test verifies full flow: HTTP POST → RAG Pipeline → Checklist Response → Webhook Dispatch
- [ ] Use test doubles for LLM/embedding (already exists in test file)
- [ ] Use WireMock for webhook verification (already set up)

**Hints**:
- Leverage existing `TestLlmProvider`, `TestEmbeddingService`, `InMemoryVectorStore` from the test file
- Create `AlertResource` with injected dependencies and `stubMode=false`

**Verification**:
```powershell
wsl ./mvnw verify -Dit.test=AlertToChecklistIT -DskipITs=false -q
```

---

## Dependency Graph

```
Task 1 (constructor) 
    ↓
Task 2 (mode logic) ←─ Task 1
    ↓
Task 3 (app wiring) ←─ Tasks 1, 2
    ↓
Tasks 4, 5 (update tests) ←─ Task 1 (can be parallelized)
    ↓
Task 6 (new E2E test) ←─ Tasks 1, 2, 3
```

## Commit Strategy

After each task passes verification:
```powershell
git add -A
git commit -m "feat(api): <task description>"
```

Final commit message:
```
feat(api): wire AlertResource to RAG Pipeline

- Add constructor injection for RagPipelineService and WebhookDispatcher
- Implement stub/real mode switching via configuration
- Update app wiring to construct dependencies
- Maintain backward compatibility for existing tests
```
