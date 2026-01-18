## ADDED Requirements

### Requirement: Alert API Integration Testing
The system SHALL provide HTTP-level integration tests for the `/api/v1/alerts` endpoint that verify the complete request/response cycle.

#### Scenario: Valid alert ingestion returns checklist
- **WHEN** a POST request is sent to `/api/v1/alerts` with valid JSON containing title, severity, message
- **THEN** the server responds with HTTP 200
- **AND** the response body contains a valid `ChecklistResponse` JSON with `alertId`, `summary`, `steps`, `sourceRunbooks`, `generatedAt`, and `llmProviderUsed`

#### Scenario: Invalid severity returns error
- **WHEN** a POST request is sent to `/api/v1/alerts` with severity value not in `CRITICAL|WARNING|INFO`
- **THEN** the server responds with HTTP 400
- **AND** the response body contains an `ErrorResponse` JSON with `errorCode` set to `VALIDATION_ERROR`

#### Scenario: Missing required fields returns error
- **WHEN** a POST request is sent to `/api/v1/alerts` missing required `title` or `severity` fields
- **THEN** the server responds with HTTP 400
- **AND** the response body contains an `ErrorResponse` JSON with descriptive message

---

### Requirement: Health API Integration Testing
The system SHALL provide HTTP-level integration tests for the `/api/v1/health` endpoint that verify service availability.

#### Scenario: Health check returns UP status
- **WHEN** a GET request is sent to `/api/v1/health`
- **THEN** the server responds with HTTP 200
- **AND** the response body contains JSON with `status` equal to `UP` and a valid ISO-8601 `timestamp`
