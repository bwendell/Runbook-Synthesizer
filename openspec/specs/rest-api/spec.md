# rest-api Specification

## Purpose
TBD - created by archiving change implement-rest-api. Update Purpose after archive.
## Requirements
### Requirement: Health Endpoint
The system SHALL provide a health check endpoint at `/api/v1/health` that returns the current operational status.

#### Scenario: Health check returns UP status
**Given** the Runbook-Synthesizer service is running
**When** a GET request is made to `/api/v1/health`
**Then** the response status code MUST be 200
**And** the response body MUST contain `"status": "UP"`
**And** the response body MUST contain a `timestamp` field with ISO-8601 format

#### Scenario: Health check with degraded status
**Given** one or more dependent services are unavailable
**When** a GET request is made to `/api/v1/health`
**Then** the response status code MUST be 200
**And** the response body MUST contain `"status": "DEGRADED"`
**And** the response body SHOULD list affected components

---

### Requirement: Alert Ingestion Endpoint
The system SHALL provide an alert ingestion endpoint at `POST /api/v1/alerts` that accepts alert payloads and returns generated checklists.

#### Scenario: Valid alert generates checklist
**Given** a valid alert request with required fields (title, severity)
**When** a POST request is made to `/api/v1/alerts`
**Then** the response status code MUST be 200
**And** the response body MUST contain a `DynamicChecklist` structure
**And** the response body MUST include `alertId`, `summary`, and `steps` fields

#### Scenario: Missing required fields returns validation error
**Given** an alert request missing the `title` field
**When** a POST request is made to `/api/v1/alerts`
**Then** the response status code MUST be 400
**And** the response body MUST contain `errorCode: "VALIDATION_ERROR"`
**And** the response body MUST include field-level error details

#### Scenario: Invalid severity returns validation error
**Given** an alert request with severity value "UNKNOWN"
**When** a POST request is made to `/api/v1/alerts`
**Then** the response status code MUST be 400
**And** the error details MUST indicate valid severity values

---

### Requirement: Runbook Sync Endpoint
The system SHALL provide a runbook synchronization endpoint at `POST /api/v1/runbooks/sync` that triggers re-indexing of runbooks from OCI Object Storage.

#### Scenario: Trigger full sync returns accepted
**Given** a POST request with empty or no body
**When** the request is made to `/api/v1/runbooks/sync`
**Then** the response status code MUST be 202 Accepted
**And** the response body MUST include a `requestId` for tracking
**And** the response body MUST include `status: "STARTED"`

#### Scenario: Trigger filtered sync with bucket
**Given** a POST request with `bucketName` specified
**When** the request is made to `/api/v1/runbooks/sync`
**Then** the sync operation MUST only process documents from the specified bucket
**And** the response status code MUST be 202 Accepted

---

### Requirement: Webhook Management Endpoints
The system SHALL provide webhook management endpoints at `/api/v1/webhooks` for listing and registering webhook destinations.

#### Scenario: List webhooks returns configured destinations
**Given** webhook destinations are configured
**When** a GET request is made to `/api/v1/webhooks`
**Then** the response status code MUST be 200
**And** the response body MUST be an array of webhook configurations

#### Scenario: Register new webhook returns created
**Given** a valid webhook configuration with name, type, and url
**When** a POST request is made to `/api/v1/webhooks`
**Then** the response status code MUST be 201 Created
**And** the response body MUST echo the created configuration

#### Scenario: Register webhook with duplicate name returns conflict
**Given** a webhook with name "slack-oncall" already exists
**When** a POST request is made with the same name
**Then** the response status code MUST be 409 Conflict
**And** the response body MUST indicate the naming conflict

---

### Requirement: Standardized Error Responses
The system SHALL return consistent error response structures for all error conditions.

#### Scenario: Error response includes correlation ID
**Given** any API request that results in an error
**When** the error response is returned
**Then** the response body MUST include a `correlationId` field
**And** the `correlationId` MUST be a valid UUID

#### Scenario: Error response includes timestamp
**Given** any API request that results in an error
**When** the error response is returned
**Then** the response body MUST include a `timestamp` field
**And** the `timestamp` MUST be in ISO-8601 format

---

### Requirement: JSON Content Type
The system SHALL accept and return JSON content for all API endpoints.

#### Scenario: Request with JSON content type succeeds
**Given** a POST request with `Content-Type: application/json`
**When** the request body is valid JSON
**Then** the request MUST be processed normally

#### Scenario: Response includes JSON content type
**Given** any API request
**When** the response is returned
**Then** the `Content-Type` header MUST be `application/json`

---

