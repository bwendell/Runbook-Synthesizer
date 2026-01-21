# Design: Add Startup Runbook Ingestion

## Context

The application's RAG pipeline requires runbook chunks in the vector store to provide relevant context to the LLM. Currently, `ServiceFactory` creates an empty `InMemoryVectorStoreRepository`, but no code ingests runbooks from cloud storage at startup.

**Stakeholders:**
- Operators running the application in production
- Developers testing the pipeline locally
- CI/CD systems running automated tests

**Constraints:**
- Must work with LocalStack for testing and real AWS in production
- Must not block application startup if ingestion fails
- Must support both in-memory (local) and cloud-based (AWS/OCI) vector stores

## Goals / Non-Goals

**Goals:**
- Ingest runbooks from S3/OCI Object Storage into vector store at startup
- Provide configuration to enable/disable startup ingestion
- Create true E2E tests that test the actual ingestion pipeline
- Refactor existing tests to avoid manual vector store seeding

**Non-Goals:**
- Real-time runbook sync (watching for S3 changes) - future enhancement
- Multi-bucket ingestion - single bucket for MVP
- Incremental ingestion - full re-index each startup for simplicity

## Decisions

### Decision 1: Ingestion at Application Startup

**What:** Call `RunbookIngestionService.ingestAll(bucket)` during `RunbookSynthesizerApp.main()` when in non-stub mode.

**Why:** 
- Simple, synchronous approach ensures runbooks are available before first alert
- No need for background workers or event-driven architecture for MVP
- Aligns with similar patterns in LangChain4j applications

**Alternatives considered:**
- Lazy ingestion on first alert: Adds latency to first request, complicates error handling
- Background worker: Over-engineered for MVP, adds concurrency complexity
- Event-driven (S3 trigger): Requires additional infrastructure, future enhancement

### Decision 2: Graceful Degradation on Ingestion Failure

**What:** If ingestion fails, log a warning and continue startup. Application will function with empty vector store.

**Why:**
- Prevents ingestion failures from blocking entire application
- Allows debugging/testing even if S3 is unavailable
- Operator can address issue and restart when ready

### Decision 3: True E2E Tests Using LocalStack

**What:** Refactor `FullPipelineE2EIT` to upload runbooks to LocalStack S3 and ingest via `RunbookIngestionService`.

**Why:**
- Current tests manually seed vector store, bypassing the actual ingestion code path
- True E2E tests catch integration bugs (S3 client config, chunking, embedding)
- Provides confidence in production behavior

## Component Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    RunbookSynthesizerApp                        │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                         Startup                            │  │
│  │  if (!stubMode && ingestOnStartup) {                       │  │
│  │    RunbookIngestionService.ingestAll(bucket)               │  │
│  │  }                                                         │  │
│  └───────────────────────────────────────────────────────────┘  │
└────────────────────────────────────┬────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                       ServiceFactory                            │
│  ┌─────────────────┐  ┌─────────────────┐  ┌────────────────┐   │
│  │createCloudStorageAdapter│  │createRunbookChunker│  │createEmbeddingService│   │
│  └─────────────────┘  └─────────────────┘  └────────────────┘   │
│  ┌─────────────────┐  ┌─────────────────┐                       │
│  │createVectorStoreRepository│  │createRunbookIngestionService│                       │
│  └─────────────────┘  └─────────────────┘                       │
└─────────────────────────────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                   RunbookIngestionService                       │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  ingestAll(bucket)                                        │   │
│  │    1. CloudStorageAdapter.listRunbooks(bucket)            │   │
│  │    2. For each runbook:                                   │   │
│  │       a. CloudStorageAdapter.getRunbookContent(...)       │   │
│  │       b. RunbookChunker.chunk(content)                    │   │
│  │       c. EmbeddingService.embedBatch(chunks)              │   │
│  │       d. VectorStoreRepository.storeBatch(chunks)         │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| Startup latency increases with large runbook corpus | Log timing, consider async ingestion for v2 |
| Ingestion failure blocks app in strict environments | Graceful degradation with warning log |
| S3 rate limiting during test parallelization | Use LocalStack with generous limits |

## Open Questions

- Should we expose an API endpoint to trigger re-ingestion? (Defer to v1.1)
- Should ingestion be async with a readiness check? (Keep sync for MVP simplicity)
