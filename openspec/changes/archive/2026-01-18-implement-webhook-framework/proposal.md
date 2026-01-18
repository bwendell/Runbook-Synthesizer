# Implement Webhook Output Framework

## Summary

Implement the configurable webhook output framework that enables multi-channel delivery of generated checklists. This includes the core `WebhookDestination` interface, `WebhookDispatcher` for routing, severity-based filtering, and built-in adapters for Slack and PagerDuty (interface stubs in v1.0, full implementations in v1.1).

## Motivation

The Runbook-Synthesizer generates dynamic checklists that must be delivered to operators through their preferred incident management channels. A pluggable webhook framework allows:

- **Flexibility**: Support any HTTP-based destination without code changes
- **Filtering**: Route checklists by severity to appropriate channels (e.g., CRITICAL → PagerDuty, WARNING → Slack)
- **Extensibility**: Easy addition of new destinations (MS Teams, ServiceNow, etc.)

## User-Facing Changes

- `WebhookDestination` interface for implementing custom output channels
- `WebhookDispatcher` orchestrates delivery to multiple configured destinations
- Configuration-driven webhook routing via `application.yaml`
- Built-in `GenericWebhookDestination` for arbitrary HTTP endpoints
- Interface stubs for `SlackWebhookDestination` and `PagerDutyWebhookDestination`

## Dependencies

- **Requires**: `implement-domain-models` (for `DynamicChecklist`, `AlertSeverity`)
- **Requires**: `implement-ports-interfaces` (for port layer patterns)

## Design Approach

### 1. Core Interfaces

```
src/main/java/com/oracle/runbook/output/
├── WebhookDestination.java       # Port interface
├── WebhookConfig.java            # Configuration record
├── WebhookResult.java            # Delivery result record
├── WebhookFilter.java            # Severity/label filtering
└── WebhookDispatcher.java        # Orchestration service
```

### 2. Adapter Implementations

```
src/main/java/com/oracle/runbook/output/adapters/
├── GenericWebhookDestination.java     # HTTP POST with JSON body
├── SlackWebhookDestination.java       # Block Kit formatting (stub)
└── PagerDutyWebhookDestination.java   # Events API v2 (stub)
```

### 3. Configuration Schema

```yaml
output:
  webhooks:
    - name: <identifier>
      type: <slack|pagerduty|generic>
      url: <endpoint>
      enabled: true|false
      filter:
        severities: [CRITICAL, WARNING, INFO]
        labels: { key: value }
      headers:
        Authorization: Bearer ${TOKEN}
```

## Verification Plan

- Unit tests for each component with mock HTTP client
- Integration test using WireMock for HTTP verification
- Test filtering logic with various severity combinations
- Verify async dispatch with multiple destinations

## References

- [DESIGN.md Section 4: Output Layer](file:///c:/Users/bwend/repos/ops-scribe/docs/DESIGN.md#L402-L500)
