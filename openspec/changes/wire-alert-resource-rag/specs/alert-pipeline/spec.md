## ADDED Requirements

### Requirement: AlertResource integrates with RAG Pipeline
The AlertResource endpoint SHALL invoke the RagPipelineService to generate real troubleshooting checklists when not in stub mode.

#### Scenario: Real mode generates checklist via RAG pipeline
- **WHEN** an alert is posted to `/api/v1/alerts` with `stubMode=false`
- **AND** RagPipelineService is available
- **THEN** the system SHALL convert the request to an Alert domain object
- **AND** invoke `RagPipelineService.processAlert()` with the alert
- **AND** return the generated DynamicChecklist as a JSON response

#### Scenario: Stub mode returns hardcoded checklist
- **WHEN** an alert is posted to `/api/v1/alerts` with `stubMode=true`
- **THEN** the system SHALL return a stub checklist response
- **AND** NOT invoke the RagPipelineService

### Requirement: AlertResource dispatches to webhooks
The AlertResource endpoint SHALL dispatch generated checklists to configured webhook destinations after returning the HTTP response.

#### Scenario: Webhook dispatch on successful checklist generation
- **WHEN** an alert is posted in real mode
- **AND** checklist generation succeeds
- **THEN** the system SHALL dispatch the checklist to all matching webhook destinations
- **AND** the HTTP response SHALL NOT wait for webhook delivery

#### Scenario: Webhook dispatch failure does not affect HTTP response
- **WHEN** an alert is posted in real mode
- **AND** webhook dispatch fails
- **THEN** the HTTP response SHALL still return successfully with the checklist
- **AND** the failure SHALL be logged

### Requirement: AlertResource supports dependency injection
The AlertResource class SHALL accept dependencies via constructor injection to enable testing and flexibility.

#### Scenario: Constructor injection for dependencies
- **WHEN** AlertResource is instantiated with RagPipelineService, WebhookDispatcher, and stubMode flag
- **THEN** the resource SHALL use the injected dependencies for processing

#### Scenario: No-arg constructor defaults to stub mode
- **WHEN** AlertResource is instantiated with no arguments
- **THEN** the resource SHALL operate in stub mode
- **AND** NOT require RagPipelineService or WebhookDispatcher
