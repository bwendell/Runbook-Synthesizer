# Implement Ports/Interfaces

## Summary

Define the ports layer (interfaces) that establish contracts between the domain and external systems. This follows Hexagonal Architecture principles where the domain layer depends only on abstractions (ports), and concrete implementations (adapters) will be added in Phase 4+.

## Motivation

The Runbook-Synthesizer requires a pluggable architecture to:
1. Support multiple observability sources (OCI Monitoring, Prometheus, OCI Logging, Loki)
2. Allow swappable LLM providers (OCI GenAI, OpenAI, Ollama)
3. Enable configurable output channels (webhooks for Slack, PagerDuty, etc.)
4. Decouple the RAG pipeline from specific storage implementations

This phase creates the interface contracts that adapters will implement.

## Scope

### In Scope
- `MetricsSourceAdapter` interface for fetching metrics
- `LogSourceAdapter` interface for fetching logs
- `LlmProvider` interface for text generation and embeddings
- `WebhookDestination` interface for output channels
- `AlertSourceAdapter` interface for alert ingestion
- `RunbookRetriever` interface for RAG retrieval
- `ChecklistGenerator` interface for checklist synthesis
- `VectorStoreRepository` interface for vector operations
- `EmbeddingService` interface for embedding generation
- Supporting records/types for interface method signatures

### Out of Scope
- Concrete adapter implementations (Phase 4+)
- Configuration loading
- Dependency injection wiring

## Design Approach

All interfaces will:
1. Use `CompletableFuture<T>` for async operations (non-blocking Helidon SE philosophy)
2. Return domain model types (from Phase 2)
3. Include a `String sourceType()` or similar identifier method where applicable
4. Be placed in their respective package directories per DESIGN.md structure

## Affected Specifications

- **NEW**: `ports-interfaces` - Requirements for all pluggable component contracts
