# webhook-output Specification

## Purpose
TBD - created by archiving change implement-webhook-framework. Update Purpose after archive.
## Requirements
### Requirement: Webhook Destination Interface

The system SHALL provide a pluggable `WebhookDestination` interface for delivering generated checklists to external systems.

#### Scenario: Interface defines required methods
- **WHEN** a class implements `WebhookDestination`
- **THEN** it MUST implement `name()`, `type()`, `send()`, `shouldSend()`, and `config()`

#### Scenario: Send returns async result
- **WHEN** `send(DynamicChecklist)` is called
- **THEN** a `CompletableFuture<WebhookResult>` is returned containing delivery outcome

---

### Requirement: Webhook Configuration

The system SHALL support configuration-driven webhook destinations via `application.yaml`.

#### Scenario: Webhook config loaded from YAML
- **WHEN** `output.webhooks` list is defined in application.yaml
- **THEN** each entry is parsed into a `WebhookConfig` object

#### Scenario: Environment variable substitution
- **WHEN** a webhook URL contains `${ENV_VAR}` syntax
- **THEN** the value is substituted at runtime from environment variables

#### Scenario: Disabled webhooks are skipped
- **WHEN** a webhook has `enabled: false`
- **THEN** it is not initialized at application startup

---

### Requirement: Webhook Filtering

The system SHALL support filtering checklists by severity and labels before delivery.

#### Scenario: Severity filter matches
- **WHEN** a webhook filter specifies `severities: [CRITICAL]`
- **AND** a checklist originates from a CRITICAL alert
- **THEN** `shouldSend()` returns true

#### Scenario: Severity filter rejects
- **WHEN** a webhook filter specifies `severities: [CRITICAL]`
- **AND** a checklist originates from a WARNING alert
- **THEN** `shouldSend()` returns false

#### Scenario: Empty filter matches all
- **WHEN** a webhook has no filter configured
- **THEN** `shouldSend()` returns true for all checklists

---

### Requirement: Generic Webhook Destination

The system SHALL provide a `GenericWebhookDestination` adapter for sending JSON payloads to arbitrary HTTP endpoints.

#### Scenario: JSON payload sent via HTTP POST
- **WHEN** `send()` is called on a GenericWebhookDestination
- **THEN** the checklist is serialized to JSON
- **AND** an HTTP POST request is sent to the configured URL

#### Scenario: Custom headers included
- **WHEN** the webhook config includes custom headers
- **THEN** those headers appear in the HTTP request

#### Scenario: Success result on 2xx response
- **WHEN** the destination returns a 2xx status code
- **THEN** `WebhookResult.success()` is returned

#### Scenario: Failure result on non-2xx response
- **WHEN** the destination returns a 4xx or 5xx status code
- **THEN** `WebhookResult.failure()` is returned with error details

---

### Requirement: Slack Webhook Destination Stub

The system SHALL include a `SlackWebhookDestination` stub that defers implementation to v1.1.

#### Scenario: Stub throws not implemented
- **WHEN** `send()` is called on SlackWebhookDestination
- **THEN** `UnsupportedOperationException` is thrown
- **AND** message indicates "Slack integration available in v1.1"

---

### Requirement: PagerDuty Webhook Destination Stub

The system SHALL include a `PagerDutyWebhookDestination` stub that defers implementation to v1.1.

#### Scenario: Stub throws not implemented
- **WHEN** `send()` is called on PagerDutyWebhookDestination
- **THEN** `UnsupportedOperationException` is thrown
- **AND** message indicates "PagerDuty integration available in v1.1"

---

### Requirement: Webhook Dispatcher

The system SHALL orchestrate delivery to multiple webhook destinations in parallel.

#### Scenario: Dispatch to all matching destinations
- **WHEN** `dispatch(DynamicChecklist)` is called
- **THEN** `shouldSend()` is evaluated for each registered destination
- **AND** `send()` is called in parallel for all matching destinations

#### Scenario: Results collected from all destinations
- **WHEN** dispatch completes
- **THEN** a `List<WebhookResult>` is returned containing outcome for each destination

#### Scenario: Non-matching destinations skipped
- **WHEN** a destination's `shouldSend()` returns false
- **THEN** it is not included in the dispatch
- **AND** no result is returned for that destination

---

### Requirement: Webhook Retry

The system SHALL retry failed webhook deliveries with exponential backoff.

#### Scenario: Retry on 5xx error
- **WHEN** a webhook returns a 5xx status code
- **THEN** the dispatcher retries up to the configured `retryCount`
- **AND** waits exponentially longer between attempts

#### Scenario: No retry on 4xx error
- **WHEN** a webhook returns a 4xx status code
- **THEN** the delivery is marked as failed immediately
- **AND** no retry is attempted

#### Scenario: Retry on connection failure
- **WHEN** a connection timeout or network error occurs
- **THEN** the dispatcher retries up to the configured `retryCount`

---

### Requirement: Webhook Destination Factory

The system SHALL provide a factory for creating `WebhookDestination` instances from configuration.

#### Scenario: Create generic destination
- **WHEN** `create()` is called with type "generic"
- **THEN** a `GenericWebhookDestination` is returned

#### Scenario: Create slack destination
- **WHEN** `create()` is called with type "slack"
- **THEN** a `SlackWebhookDestination` is returned

#### Scenario: Create pagerduty destination
- **WHEN** `create()` is called with type "pagerduty"
- **THEN** a `PagerDutyWebhookDestination` is returned

#### Scenario: Unknown type throws exception
- **WHEN** `create()` is called with an unknown type
- **THEN** `IllegalArgumentException` is thrown

