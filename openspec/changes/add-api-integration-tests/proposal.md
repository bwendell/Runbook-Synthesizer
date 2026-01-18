# Change: Add API-Level Integration Tests

## Why
The existing integration test suite validates component-level flows (E2E alert-to-checklist, webhook dispatch), but lacks HTTP-level tests that exercise the real Helidon server endpoints. API integration tests will catch routing bugs, JSON serialization issues, and HTTP status code correctness that unit tests cannot.

## What Changes
- Add **AlertApiIT** to test the complete HTTP request/response cycle for `/api/v1/alerts`
- Add **HealthApiIT** to test the health check endpoint at `/api/v1/health`
- Extend the test infrastructure to support HTTP client testing against the Helidon test server

## Impact
- Affected specs: `api-testing` (NEW capability)
- Affected code:
  - `src/test/java/com/oracle/runbook/integration/api/AlertApiIT.java` (NEW)
  - `src/test/java/com/oracle/runbook/integration/api/HealthApiIT.java` (NEW)
