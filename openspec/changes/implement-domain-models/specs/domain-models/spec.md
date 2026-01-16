# Domain Models Capability

## ADDED Requirements

### Requirement: Canonical Alert Model

The system SHALL provide an immutable `Alert` record that normalizes alert data from multiple sources into a canonical format with id, title, message, severity, sourceService, dimensions, labels, timestamp, and rawPayload.

#### Scenario: Alert construction from OCI Monitoring Alarm
- **GIVEN** an OCI Monitoring Alarm payload
- **WHEN** an Alert is constructed with required fields
- **THEN** the Alert record contains all normalized data with immutable collections

#### Scenario: Alert validation rejects null required fields
- **GIVEN** a null value for `id` or `title`
- **WHEN** Alert construction is attempted
- **THEN** a NullPointerException is thrown

---

### Requirement: Alert Severity Enumeration

The system SHALL provide an `AlertSeverity` enum with values CRITICAL, WARNING, and INFO for categorizing alert urgency.

#### Scenario: Parse severity from string
- **GIVEN** a case-insensitive severity string like "critical" or "CRITICAL"
- **WHEN** AlertSeverity.fromString() is called
- **THEN** the matching enum value is returned

#### Scenario: Invalid severity throws exception
- **GIVEN** an unknown severity string like "unknown"
- **WHEN** AlertSeverity.fromString() is called
- **THEN** IllegalArgumentException is thrown

---

### Requirement: Resource Metadata Model

The system SHALL provide a `ResourceMetadata` record containing OCI compute instance details: ocid, displayName, compartmentId, shape, availabilityDomain, freeformTags, and definedTags.

#### Scenario: ResourceMetadata captures OCI instance details
- **GIVEN** OCI Compute API response data
- **WHEN** ResourceMetadata is constructed
- **THEN** all instance metadata fields are captured with immutable tag maps

---

### Requirement: Metric Snapshot Model

The system SHALL provide a `MetricSnapshot` record representing a point-in-time metric value with metricName, namespace, value, unit, and timestamp.

#### Scenario: MetricSnapshot captures OCI Monitoring data
- **GIVEN** metric data from OCI Monitoring API
- **WHEN** MetricSnapshot is constructed
- **THEN** the metric value and metadata are captured

---

### Requirement: Log Entry Model

The system SHALL provide a `LogEntry` record representing a log event with id, timestamp, level, message, and metadata.

#### Scenario: LogEntry captures OCI Logging data
- **GIVEN** log data from OCI Logging or Loki
- **WHEN** LogEntry is constructed
- **THEN** the log content and context metadata are captured

---

### Requirement: Enriched Context Model

The system SHALL provide an `EnrichedContext` record that aggregates an Alert with ResourceMetadata, recent MetricSnapshots, recent LogEntries, and custom properties for complete troubleshooting context.

#### Scenario: EnrichedContext aggregates all context sources
- **GIVEN** an Alert, resource metadata, metrics, and logs
- **WHEN** EnrichedContext is constructed
- **THEN** all data sources are composed into a single immutable context object

---

### Requirement: Runbook Chunk Model

The system SHALL provide a `RunbookChunk` record representing a semantically chunked runbook section with id, runbookPath, sectionTitle, content, tags, applicableShapes, and embedding vector.

#### Scenario: RunbookChunk stores vector embedding
- **GIVEN** a chunked section of runbook markdown with embedding
- **WHEN** RunbookChunk is constructed
- **THEN** the embedding array is defensively copied for immutability

---

### Requirement: Retrieved Chunk Model

The system SHALL provide a `RetrievedChunk` record that wraps a RunbookChunk with retrieval scoring: similarityScore, metadataBoost, and finalScore.

#### Scenario: RetrievedChunk wraps chunk with scores
- **GIVEN** a RunbookChunk and similarity scores from vector search
- **WHEN** RetrievedChunk is constructed
- **THEN** the chunk and all scores are captured

---

### Requirement: Step Priority Enumeration

The system SHALL provide a `StepPriority` enum with values HIGH, MEDIUM, and LOW for prioritizing checklist steps.

#### Scenario: StepPriority supports ordering
- **GIVEN** multiple checklist steps with different priorities
- **WHEN** priorities are compared
- **THEN** HIGH priority steps can be identified as most urgent

---

### Requirement: Checklist Step Model

The system SHALL provide a `ChecklistStep` record representing a single troubleshooting step with order, instruction, rationale, currentValue, expectedValue, priority, and commands.

#### Scenario: ChecklistStep contains actionable instructions
- **GIVEN** generated step data from LLM
- **WHEN** ChecklistStep is constructed
- **THEN** the step includes executable commands and context-aware values

---

### Requirement: Dynamic Checklist Model

The system SHALL provide a `DynamicChecklist` record representing a complete generated troubleshooting guide with alertId, summary, steps, sourceRunbooks, generatedAt timestamp, and llmProviderUsed.

#### Scenario: DynamicChecklist aggregates steps
- **GIVEN** LLM-generated steps and source runbook references
- **WHEN** DynamicChecklist is constructed
- **THEN** all steps and metadata are captured in order

---

### Requirement: Generation Config Model

The system SHALL provide a `GenerationConfig` record for LLM generation parameters: temperature, maxTokens, and optional modelOverride.

#### Scenario: GenerationConfig validates parameters
- **GIVEN** temperature value outside 0.0-1.0 range or maxTokens <= 0
- **WHEN** GenerationConfig is constructed
- **THEN** IllegalArgumentException is thrown

#### Scenario: GenerationConfig allows optional model override
- **GIVEN** an Optional.empty() for modelOverride
- **WHEN** GenerationConfig is constructed
- **THEN** the default model configured in application.yaml will be used
