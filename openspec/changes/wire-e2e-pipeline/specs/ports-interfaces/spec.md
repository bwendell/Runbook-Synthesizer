## ADDED Requirements

### Requirement: Default Context Enrichment Service

The system SHALL provide a `DefaultContextEnrichmentService` that implements the `ContextEnrichmentService` interface by orchestrating calls to metadata, metrics, and log adapters.

#### Scenario: Enrich alert with full context
- **GIVEN** an `Alert` with resource dimensions
- **WHEN** `enrich()` is called
- **THEN** return `EnrichedContext` containing:
  - Resource metadata from `ComputeMetadataAdapter`
  - Recent metrics from `MetricsSourceAdapter`
  - Recent logs from `LogSourceAdapter`

#### Scenario: Parallel execution
- **GIVEN** all three adapters are available
- **WHEN** `enrich()` is called
- **THEN** metadata, metrics, and logs are fetched in parallel

#### Scenario: Handle partial failure
- **GIVEN** the metrics adapter fails with an exception
- **WHEN** `enrich()` is called
- **THEN** return `EnrichedContext` with available data (metadata and logs) and empty metrics list
