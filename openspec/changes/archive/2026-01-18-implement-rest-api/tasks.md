# REST API Endpoints Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement the REST API layer with Helidon SE 4.x handlers for alert ingestion, runbook sync, webhook management, and health checks.

**Architecture:** Functional HTTP routing with HttpService implementations. Each endpoint gets a dedicated resource class with request/response DTOs. Service dependencies are injected via constructor, using stubs initially.

**Tech Stack:** Java 25, Helidon SE 4.x, helidon-webserver-http, jakarta.json for JSON handling

---

## Task 1: Create HealthResource handler [S]

**Files:**
- Create: `src/main/java/com/oracle/runbook/api/HealthResource.java`
- Test: `src/test/java/com/oracle/runbook/api/HealthResourceTest.java`
- Modify: `src/main/java/com/oracle/runbook/RunbookSynthesizerApp.java` (routing integration)

**Context:**
The health endpoint is the simplest API to implement. It provides `/api/v1/health` for Kubernetes probes and monitoring.

**Step 1: Write the failing test**
- Create test class with `@Test` method `testHealthEndpoint_ReturnsUpStatus`
- Use Helidon WebClient test utilities to call GET `/api/v1/health`
- Assert response status 200 and JSON body contains `"status": "UP"`

**Step 2: Run test to verify it fails**
```shell
wsl ./mvnw test -Dtest=HealthResourceTest -q
```
Expected: FAIL (class not found or 404)

**Step 3: Write minimal implementation**
- Create `HealthResource implements HttpService`
- Override `routing(HttpRules rules)` to register GET `/` handler
- Handler returns JSON `{"status": "UP", "timestamp": <current-time>}`
- Mount at `/api/v1/health` in RunbookSynthesizerApp

**Step 4: Run test to verify it passes**
```shell
wsl ./mvnw test -Dtest=HealthResourceTest -q
```
Expected: PASS

**Step 5: Commit**
```shell
git add src/main/java/com/oracle/runbook/api/HealthResource.java src/test/java/com/oracle/runbook/api/HealthResourceTest.java
git commit -m "feat(api): add HealthResource with /api/v1/health endpoint"
```

---

## Task 2: Create API error response model [S]

**Files:**
- Create: `src/main/java/com/oracle/runbook/api/dto/ErrorResponse.java`
- Test: `src/test/java/com/oracle/runbook/api/dto/ErrorResponseTest.java`

**Context:**
Standardize error responses across all endpoints with correlation ID, error code, and field-level validation details.

**Step 1: Write the failing test**
- Test record instantiation with required fields
- Test JSON serialization produces expected format
- Test validation error list handling

**Step 2: Run test to verify it fails**
```shell
wsl ./mvnw test -Dtest=ErrorResponseTest -q
```
Expected: FAIL (class not found)

**Step 3: Write minimal implementation**
- Create `ErrorResponse` record with fields: `correlationId`, `errorCode`, `message`, `timestamp`, `details` (Map for field errors)
- Add compact constructor for validation
- Use java.time.Instant for timestamp

**Step 4: Run test to verify it passes**
```shell
wsl ./mvnw test -Dtest=ErrorResponseTest -q
```
Expected: PASS

**Step 5: Commit**
```shell
git add src/main/java/com/oracle/runbook/api/dto/ErrorResponse.java src/test/java/com/oracle/runbook/api/dto/ErrorResponseTest.java
git commit -m "feat(api): add ErrorResponse DTO for standardized error handling"
```

---

## Task 3: Create AlertRequest DTO [S]

**Files:**
- Create: `src/main/java/com/oracle/runbook/api/dto/AlertRequest.java`
- Test: `src/test/java/com/oracle/runbook/api/dto/AlertRequestTest.java`

**Context:**
The request body for POST `/api/v1/alerts` containing alert details to process.

**Step 1: Write the failing test**
- Test record instantiation with valid fields
- Test JSON deserialization from request body format
- Test null validation for required fields (title, severity)

**Step 2: Run test to verify it fails**
```shell
wsl ./mvnw test -Dtest=AlertRequestTest -q
```
Expected: FAIL (class not found)

**Step 3: Write minimal implementation**
- Create `AlertRequest` record matching DESIGN.md AlertRequest schema
- Fields: `title`, `message`, `severity` (String), `sourceService`, `dimensions` (Map), `labels` (Map), `rawPayload`
- Add validation in compact constructor

**Step 4: Run test to verify it passes**
```shell
wsl ./mvnw test -Dtest=AlertRequestTest -q
```
Expected: PASS

**Step 5: Commit**
```shell
git add src/main/java/com/oracle/runbook/api/dto/AlertRequest.java src/test/java/com/oracle/runbook/api/dto/AlertRequestTest.java
git commit -m "feat(api): add AlertRequest DTO for alert ingestion"
```

---

## Task 4: Create ChecklistResponse DTO [S]

**Files:**
- Create: `src/main/java/com/oracle/runbook/api/dto/ChecklistResponse.java`
- Test: `src/test/java/com/oracle/runbook/api/dto/ChecklistResponseTest.java`

**Context:**
The response body for POST `/api/v1/alerts` containing the generated checklist.

**Step 1: Write the failing test**
- Test record instantiation from domain `DynamicChecklist`
- Test JSON serialization produces expected format
- Test factory method `fromDomain(DynamicChecklist)`

**Step 2: Run test to verify it fails**
```shell
wsl ./mvnw test -Dtest=ChecklistResponseTest -q
```
Expected: FAIL (class not found)

**Step 3: Write minimal implementation**
- Create `ChecklistResponse` record with fields matching `DynamicChecklist`
- Add `static ChecklistResponse fromDomain(DynamicChecklist)` factory method
- Include nested `ChecklistStepResponse` for step serialization

**Step 4: Run test to verify it passes**
```shell
wsl ./mvnw test -Dtest=ChecklistResponseTest -q
```
Expected: PASS

**Step 5: Commit**
```shell
git add src/main/java/com/oracle/runbook/api/dto/ChecklistResponse.java src/test/java/com/oracle/runbook/api/dto/ChecklistResponseTest.java
git commit -m "feat(api): add ChecklistResponse DTO for checklist output"
```

---

## Task 5: Create AlertResource handler [M]

**Files:**
- Create: `src/main/java/com/oracle/runbook/api/AlertResource.java`
- Test: `src/test/java/com/oracle/runbook/api/AlertResourceTest.java`
- Modify: `src/main/java/com/oracle/runbook/RunbookSynthesizerApp.java`

**Context:**
The core endpoint that receives alerts and returns generated checklists. Initially uses a stub service.

**Step 1: Write the failing tests**
- `testPostAlert_ValidRequest_ReturnsChecklist`: POST valid JSON, expect 200 with checklist
- `testPostAlert_MissingTitle_ReturnsBadRequest`: POST without title, expect 400
- `testPostAlert_InvalidSeverity_ReturnsBadRequest`: POST with unknown severity, expect 400

**Step 2: Run tests to verify they fail**
```shell
wsl ./mvnw test -Dtest=AlertResourceTest -q
```
Expected: FAIL (class not found or 404)

**Step 3: Write minimal implementation**
- Create `AlertResource implements HttpService`
- Inject stub/mock service for checklist generation
- POST handler: parse JSON → validate → call service → return ChecklistResponse
- Error handling: catch validation exceptions, return ErrorResponse
- Mount at `/api/v1/alerts` in RunbookSynthesizerApp

**Step 4: Run tests to verify they pass**
```shell
wsl ./mvnw test -Dtest=AlertResourceTest -q
```
Expected: PASS

**Step 5: Commit**
```shell
git add src/main/java/com/oracle/runbook/api/AlertResource.java src/test/java/com/oracle/runbook/api/AlertResourceTest.java
git commit -m "feat(api): add AlertResource with POST /api/v1/alerts endpoint"
```

---

## Task 6: Create WebhookConfig DTO [S]

**Files:**
- Create: `src/main/java/com/oracle/runbook/api/dto/WebhookConfig.java`
- Test: `src/test/java/com/oracle/runbook/api/dto/WebhookConfigTest.java`

**Context:**
Request/response DTO for webhook registration and listing.

**Step 1: Write the failing test**
- Test record instantiation with required fields (name, type, url)
- Test optional fields (enabled, filter severities, headers)
- Test JSON serialization/deserialization

**Step 2: Run test to verify it fails**
```shell
wsl ./mvnw test -Dtest=WebhookConfigTest -q
```
Expected: FAIL (class not found)

**Step 3: Write minimal implementation**
- Create `WebhookConfig` record with fields: `name`, `type`, `url`, `enabled`, `filterSeverities` (List), `headers` (Map)
- Add validation in compact constructor (name and url required)

**Step 4: Run test to verify it passes**
```shell
wsl ./mvnw test -Dtest=WebhookConfigTest -q
```
Expected: PASS

**Step 5: Commit**
```shell
git add src/main/java/com/oracle/runbook/api/dto/WebhookConfig.java src/test/java/com/oracle/runbook/api/dto/WebhookConfigTest.java
git commit -m "feat(api): add WebhookConfig DTO for webhook management"
```

---

## Task 7: Create WebhookResource handler [M]

**Files:**
- Create: `src/main/java/com/oracle/runbook/api/WebhookResource.java`
- Test: `src/test/java/com/oracle/runbook/api/WebhookResourceTest.java`
- Modify: `src/main/java/com/oracle/runbook/RunbookSynthesizerApp.java`

**Context:**
Endpoints for listing and registering webhook destinations.

**Step 1: Write the failing tests**
- `testGetWebhooks_ReturnsConfiguredWebhooks`: GET returns list (can be empty initially)
- `testPostWebhook_ValidConfig_ReturnsCreated`: POST valid config, expect 201
- `testPostWebhook_MissingName_ReturnsBadRequest`: POST without name, expect 400

**Step 2: Run tests to verify they fail**
```shell
wsl ./mvnw test -Dtest=WebhookResourceTest -q
```
Expected: FAIL (class not found or 404)

**Step 3: Write minimal implementation**
- Create `WebhookResource implements HttpService`
- Use in-memory list for webhook storage (stub implementation)
- GET: return current list as JSON array
- POST: validate config → add to list → return 201 with config
- Mount at `/api/v1/webhooks` in RunbookSynthesizerApp

**Step 4: Run tests to verify they pass**
```shell
wsl ./mvnw test -Dtest=WebhookResourceTest -q
```
Expected: PASS

**Step 5: Commit**
```shell
git add src/main/java/com/oracle/runbook/api/WebhookResource.java src/test/java/com/oracle/runbook/api/WebhookResourceTest.java
git commit -m "feat(api): add WebhookResource with GET/POST /api/v1/webhooks endpoints"
```

---

## Task 8: Create SyncRequest and SyncResponse DTOs [S]

**Files:**
- Create: `src/main/java/com/oracle/runbook/api/dto/SyncRequest.java`
- Create: `src/main/java/com/oracle/runbook/api/dto/SyncResponse.java`
- Test: `src/test/java/com/oracle/runbook/api/dto/SyncRequestTest.java`
- Test: `src/test/java/com/oracle/runbook/api/dto/SyncResponseTest.java`

**Context:**
DTOs for the runbook sync endpoint. Request is optional (can trigger full sync), response indicates status.

**Step 1: Write the failing tests**
- Test SyncRequest with optional bucket/prefix filter fields
- Test SyncResponse with status, documentsProcessed, errors fields

**Step 2: Run tests to verify they fail**
```shell
wsl ./mvnw test -Dtest=SyncRequestTest,SyncResponseTest -q
```
Expected: FAIL (classes not found)

**Step 3: Write minimal implementation**
- `SyncRequest` record: `bucketName` (optional), `prefix` (optional), `forceRefresh` (boolean)
- `SyncResponse` record: `status` (enum: STARTED, COMPLETED, FAILED), `documentsProcessed`, `errors` (List), `requestId`

**Step 4: Run tests to verify they pass**
```shell
wsl ./mvnw test -Dtest=SyncRequestTest,SyncResponseTest -q
```
Expected: PASS

**Step 5: Commit**
```shell
git add src/main/java/com/oracle/runbook/api/dto/SyncRequest.java src/main/java/com/oracle/runbook/api/dto/SyncResponse.java
git add src/test/java/com/oracle/runbook/api/dto/SyncRequestTest.java src/test/java/com/oracle/runbook/api/dto/SyncResponseTest.java
git commit -m "feat(api): add SyncRequest and SyncResponse DTOs for runbook sync"
```

---

## Task 9: Create RunbookResource handler [M]

**Files:**
- Create: `src/main/java/com/oracle/runbook/api/RunbookResource.java`
- Test: `src/test/java/com/oracle/runbook/api/RunbookResourceTest.java`
- Modify: `src/main/java/com/oracle/runbook/RunbookSynthesizerApp.java`

**Context:**
Endpoint for triggering runbook re-indexing from OCI Object Storage.

**Step 1: Write the failing tests**
- `testPostSync_NoBody_StartsFullSync`: POST with empty body starts full sync, returns 202 Accepted
- `testPostSync_WithBucket_StartsFilteredSync`: POST with bucket filter, returns 202
- `testPostSync_ReturnsRequestId`: Response includes unique request ID for tracking

**Step 2: Run tests to verify they fail**
```shell
wsl ./mvnw test -Dtest=RunbookResourceTest -q
```
Expected: FAIL (class not found or 404)

**Step 3: Write minimal implementation**
- Create `RunbookResource implements HttpService`
- POST handler: parse optional SyncRequest → start async sync (stub) → return 202 with SyncResponse
- Use UUID for request ID
- Mount at `/api/v1/runbooks` in RunbookSynthesizerApp

**Step 4: Run tests to verify they pass**
```shell
wsl ./mvnw test -Dtest=RunbookResourceTest -q
```
Expected: PASS

**Step 5: Commit**
```shell
git add src/main/java/com/oracle/runbook/api/RunbookResource.java src/test/java/com/oracle/runbook/api/RunbookResourceTest.java
git commit -m "feat(api): add RunbookResource with POST /api/v1/runbooks/sync endpoint"
```

---

## Task 10: Update RunbookSynthesizerApp with API routing [M]

**Files:**
- Modify: `src/main/java/com/oracle/runbook/RunbookSynthesizerApp.java`
- Test: `src/test/java/com/oracle/runbook/api/ApiRoutingIntegrationTest.java`

**Context:**
Wire all resource handlers into the main application routing.

**Step 1: Write the failing test**
- Integration test that boots the server
- Verify all endpoints respond:
  - GET /api/v1/health → 200
  - POST /api/v1/alerts → 200 (with valid body) or 400 (invalid)
  - GET /api/v1/webhooks → 200 (empty list)
  - POST /api/v1/runbooks/sync → 202

**Step 2: Run test to verify it fails**
```shell
wsl ./mvnw test -Dtest=ApiRoutingIntegrationTest -q
```
Expected: FAIL (routes not configured)

**Step 3: Write minimal implementation**
- Update `configureRouting` to register all HttpService instances:
  ```java
  routing.register("/api/v1/health", new HealthResource())
         .register("/api/v1/alerts", new AlertResource(...))
         .register("/api/v1/webhooks", new WebhookResource())
         .register("/api/v1/runbooks", new RunbookResource(...));
  ```
- Create service stubs/factories as needed for dependency injection

**Step 4: Run test to verify it passes**
```shell
wsl ./mvnw test -Dtest=ApiRoutingIntegrationTest -q
```
Expected: PASS

**Step 5: Commit**
```shell
git add src/main/java/com/oracle/runbook/RunbookSynthesizerApp.java src/test/java/com/oracle/runbook/api/ApiRoutingIntegrationTest.java
git commit -m "feat(api): wire all API routes in RunbookSynthesizerApp"
```

---

## Task 11: Add JSON media support configuration [S]

**Files:**
- Modify: `pom.xml` (if helidon-media-jsonp not already present)
- Modify: `src/main/resources/application.yaml`
- Test: verify JSON content-type handling works

**Context:**
Ensure proper JSON serialization/deserialization is configured for all endpoints.

**Step 1: Verify dependencies**
- Check pom.xml includes `helidon-webserver-media-jsonp` or equivalent
- If missing, add the dependency

**Step 2: Configure media support**
- Ensure application.yaml has media configuration if needed
- Helidon 4.x auto-discovers JSON media support

**Step 3: Run all API tests**
```shell
wsl ./mvnw test -Dtest="*ResourceTest,*IntegrationTest" -q
```
Expected: All PASS with proper JSON handling

**Step 4: Commit (if changes made)**
```shell
git add pom.xml src/main/resources/application.yaml
git commit -m "feat(api): configure JSON media support"
```

---

## Task 12: Add OpenAPI specification [L]

**Files:**
- Create: `src/main/resources/META-INF/openapi.yaml`
- Create: `docs/API.md` (human-readable API documentation)

**Context:**
Document the API using OpenAPI 3.0 specification for client code generation and documentation.

**Step 1: Create OpenAPI spec**
- Define all endpoints from DESIGN.md section 4
- Include request/response schemas matching DTOs
- Add example values for each endpoint

**Step 2: Create human-readable docs**
- Create `docs/API.md` with curl examples
- Document authentication requirements (OCI IAM - coming in Phase 4)
- Document error codes and responses

**Step 3: Validate spec**
- Verify OpenAPI spec is valid YAML
- Test that examples match implementation

**Step 4: Commit**
```shell
git add src/main/resources/META-INF/openapi.yaml docs/API.md
git commit -m "docs(api): add OpenAPI specification and API documentation"
```

---

## Summary

| Task | Complexity | Description |
|------|------------|-------------|
| 1 | [S] | HealthResource handler |
| 2 | [S] | ErrorResponse DTO |
| 3 | [S] | AlertRequest DTO |
| 4 | [S] | ChecklistResponse DTO |
| 5 | [M] | AlertResource handler |
| 6 | [S] | WebhookConfig DTO |
| 7 | [M] | WebhookResource handler |
| 8 | [S] | SyncRequest/SyncResponse DTOs |
| 9 | [M] | RunbookResource handler |
| 10 | [M] | API routing integration |
| 11 | [S] | JSON media configuration |
| 12 | [L] | OpenAPI specification |

**Total Tasks:** 12
**Estimated Time:** 60-90 minutes following TDD cycle

**Dependency Order:**
1. Tasks 1-4 (independent DTOs and HealthResource)
2. Task 5 (AlertResource needs DTOs from 2-4)
3. Task 6-7 (WebhookResource)
4. Task 8-9 (RunbookResource)
5. Task 10 (wires everything together)
6. Tasks 11-12 (configuration and documentation)
