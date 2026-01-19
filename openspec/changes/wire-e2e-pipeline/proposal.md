# Change: Wire E2E Pipeline for AWS

Enable full end-to-end testing on AWS by implementing the missing components that connect alert ingestion through to file output.

## Why

The codebase has all the AWS adapters implemented (SNS alerts, S3 storage, CloudWatch metrics/logs, EC2 metadata, Bedrock LLM, Ollama LLM) but they are **not wired together** in the production application. The `RunbookSynthesizerApp` runs in "stub mode" returning mock checklists. To validate the complete flow on AWS, we need:

1. A production `ContextEnrichmentService` implementation
2. A local (in-memory) vector store for MVP (deferring Oracle/AWS implementations)
3. A runbook ingestion pipeline (S3 → chunk → embed → store)
4. File output adapter for simple validation
5. Wiring of the `RagPipelineService` to the production app

## What Changes

### New Components
- **`DefaultContextEnrichmentService`** — Orchestrates AWS adapters to enrich alerts
- **`InMemoryVectorStoreRepository`** — Simple local vector store for MVP E2E testing
- **`VectorStoreFactory`** — Factory to switch between local/oracle/aws vector stores
- **`RunbookIngestionService`** — S3 → parse → chunk → embed → store pipeline
- **`FileOutputAdapter`** — Writes `DynamicChecklist` to file for MVP validation

### Existing Component Changes
- **`CloudAdapterFactory`** — Add vector store factory method
- **`RunbookSynthesizerApp`** — Wire real `RagPipelineService` and remove stub mode
- **`application.yaml`** — Add vector store and file output configuration

## Impact

- **Affected specs:** `rag-pipeline`, `context-enrichment`, `output-destinations`
- **Affected code:**
  - `src/main/java/com/oracle/runbook/rag/` — New vector store classes
  - `src/main/java/com/oracle/runbook/enrichment/` — New enrichment implementation
  - `src/main/java/com/oracle/runbook/output/adapters/` — File output adapter
  - `src/main/java/com/oracle/runbook/RunbookSynthesizerApp.java` — Production wiring
  - `src/main/resources/application.yaml` — Configuration updates
