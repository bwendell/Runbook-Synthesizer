# Tasks: Add API-Level Integration Tests

## 1. Test Infrastructure Setup
- [ ] 1.1 Create `api/` package under `integration/` for API-level tests

## 2. AlertApiIT Implementation (S)
- [ ] 2.1 Create `AlertApiIT.java` with test class extending `IntegrationTestBase`
- [ ] 2.2 Implement test: POST `/api/v1/alerts` with valid JSON → 200 + ChecklistResponse
- [ ] 2.3 Implement test: POST `/api/v1/alerts` with invalid severity → 400 + ErrorResponse
- [ ] 2.4 Implement test: POST `/api/v1/alerts` with missing required fields → 400 + validation error
- [ ] 2.5 Implement test: Verify JSON response structure matches DTOs (`ChecklistResponse`, `ErrorResponse`)

## 3. HealthApiIT Implementation (S)
- [ ] 3.1 Create `HealthApiIT.java` with test class extending `IntegrationTestBase`
- [ ] 3.2 Implement test: GET `/api/v1/health` → 200 + `{"status": "UP", "timestamp": ...}`

## 4. Verification
- [ ] 4.1 Run `mvn verify -Pit` to confirm all API integration tests pass
- [ ] 4.2 Verify tests use Helidon's HTTP client to hit real endpoints (no mocked handlers)

## Acceptance Criteria
- All tests pass with `mvn verify -Pit`
- Tests exercise the real Helidon server (not mocked routing)
- JSON structure assertions verify DTO contract compliance
