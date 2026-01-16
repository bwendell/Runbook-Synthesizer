# Change: Implement Domain Models

## Why

Phase 2 of the Runbook-Synthesizer implementation requires creating the core domain models that represent alerts, context enrichment, RAG components, and generated checklists. These immutable record classes form the foundation for all subsequent layers (ports, adapters, API).

## What Changes

- **NEW** `Alert` record with `AlertSeverity` enum - canonical alert model for normalized ingestion
- **NEW** `ResourceMetadata` record - OCI compute instance metadata container
- **NEW** `MetricSnapshot` record - point-in-time metric value representation
- **NEW** `LogEntry` record - log event representation from OCI Logging/Loki
- **NEW** `EnrichedContext` record - aggregated alert + resource + metrics + logs context
- **NEW** `RunbookChunk` record - semantically chunked runbook section with embeddings
- **NEW** `RetrievedChunk` record - retrieval result with similarity scores
- **NEW** `DynamicChecklist` record with `ChecklistStep` and `StepPriority` enum - generated output
- **NEW** `GenerationConfig` record - LLM generation parameters

## Impact

- **Affected specs**: `domain-models` (new capability)
- **Affected code**:
  - `src/main/java/com/oracle/runbook/domain/` - all new files
  - `src/test/java/com/oracle/runbook/domain/` - unit tests for each model
- **Dependencies**: None (domain models are self-contained)
- **Downstream consumers**: All ports and adapters in Phase 3+
