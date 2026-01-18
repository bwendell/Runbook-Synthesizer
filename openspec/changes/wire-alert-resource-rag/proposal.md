# Change: Wire AlertResource to RAG Pipeline

## Why
The `AlertResource` endpoint currently returns stub checklist data via `generateStubChecklist()`, blocking true end-to-end testing and production readiness. To enable the full alert-to-checklist flow, `AlertResource` must invoke the actual `RagPipelineService` and dispatch results via `WebhookDispatcher`.

## What Changes
- **Inject dependencies** into `AlertResource`: `RagPipelineService` and `WebhookDispatcher`
- **Replace stub logic** with actual pipeline invocation
- **Add configuration** for stub/real mode switching (to preserve test isolation)
- **Update application wiring** in `RunbookSynthesizerApp` to construct and inject dependencies
- **Update existing tests** to accommodate new constructor signature

## Impact
- Affected specs: `project-foundation` (alert processing capability)
- Affected code:
  - `AlertResource.java` - Modify constructor, add mode config, replace stub
  - `RunbookSynthesizerApp.java` - Wire dependencies
  - `AlertResourceTest.java` - Update for new constructor
  - `ApiRoutingIntegrationTest.java` - Update for new constructor
  - `AlertIngestionIT.java` - Update for new constructor
  - `AlertNormalizationIT.java` - Update for new constructor
