## ADDED Requirements

### Requirement: File Output Adapter

The system SHALL provide a `FileOutputAdapter` that writes generated checklists to the local filesystem.

#### Scenario: Write checklist to file
- **GIVEN** a `DynamicChecklist` with `alertId: "alert-123"`
- **WHEN** dispatched to `FileOutputAdapter`
- **THEN** a JSON file is created at `{output.file.path}/checklist-alert-123-{timestamp}.json`

#### Scenario: Create output directory
- **GIVEN** configured output path does not exist
- **WHEN** first checklist is dispatched
- **THEN** the directory is created automatically

#### Scenario: Handle write failure
- **GIVEN** the output path is not writable
- **WHEN** checklist is dispatched
- **THEN** return `WebhookResult.failure()` with error message
