# Tasks: Add API-Level Integration Tests

## 1. Test Infrastructure Setup

- [ ] 1.1 Create `api/` package under `integration/` for API-level tests
  - **Verification**: Directory exists at `src/test/java/com/oracle/runbook/integration/api/`
  - **Acceptance**: Package compiles without errors

---

## 2. AlertApiIT Implementation (S)

- [ ] 2.1 Create `AlertApiIT.java` with test class extending `IntegrationTestBase`
  - **Verification**: `mvn test-compile` succeeds; class is annotated with `@ServerTest`
  - **Acceptance**: Test class compiles and Helidon injects `WebServer`

- [ ] 2.2 Implement test: POST `/api/v1/alerts` with valid JSON → 200 + ChecklistResponse
  - **Verification**: Run `mvn verify -Pit -Dtest=AlertApiIT#postValidAlert*`
  - **Acceptance**: HTTP 200 returned; response contains `alertId`, `steps`, `generatedAt`

- [ ] 2.3 Implement test: POST `/api/v1/alerts` with invalid severity → 400 + ErrorResponse
  - **Verification**: Run `mvn verify -Pit -Dtest=AlertApiIT#postInvalidSeverity*`
  - **Acceptance**: HTTP 400 returned; `errorCode` equals `VALIDATION_ERROR`

- [ ] 2.4 Implement test: POST `/api/v1/alerts` with missing required fields → 400 + validation error
  - **Verification**: Run `mvn verify -Pit -Dtest=AlertApiIT#postMissingFields*`
  - **Acceptance**: HTTP 400 returned; message indicates missing `title` or `severity`

- [ ] 2.5 Implement test: Verify JSON response structure matches DTOs
  - **Verification**: Run all `AlertApiIT` tests; assertions use JsonPath or JSON-B deserialization
  - **Acceptance**: All fields from `ChecklistResponse` and `ErrorResponse` are validated

---

## 3. HealthApiIT Implementation (S)

- [ ] 3.1 Create `HealthApiIT.java` with test class extending `IntegrationTestBase`
  - **Verification**: `mvn test-compile` succeeds; class is annotated with `@ServerTest`
  - **Acceptance**: Test class compiles and Helidon injects `WebServer`

- [ ] 3.2 Implement test: GET `/api/v1/health` → 200 + `{"status": "UP", "timestamp": ...}`
  - **Verification**: Run `mvn verify -Pit -Dtest=HealthApiIT#healthCheckReturnsUp*`
  - **Acceptance**: HTTP 200 returned; `status` equals `UP`; `timestamp` is valid ISO-8601

---

## 4. Final Verification

- [ ] 4.1 Run `mvn verify -Pit` to confirm all API integration tests pass
  - **Verification**: Full integration test suite green
  - **Acceptance**: Zero test failures; build succeeds

- [ ] 4.2 Verify tests use Helidon's HTTP client to hit real endpoints (no mocked handlers)
  - **Verification**: Code review confirms no `@SetUpServer` stubs bypass real `AlertResource`/`HealthResource`
  - **Acceptance**: Tests exercise actual routing defined in application

---

## Overall Acceptance Criteria
- All tests pass with `mvn verify -Pit`
- Tests exercise the real Helidon server (not mocked routing)
- JSON structure assertions verify DTO contract compliance
- Each test method is self-documenting with descriptive names
