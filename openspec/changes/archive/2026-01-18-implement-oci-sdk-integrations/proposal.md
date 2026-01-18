# Implement OCI SDK Integrations

## Summary

Implement concrete OCI SDK adapter classes that fulfill the ports/interfaces defined in Phase 3. This phase bridges the domain layer to OCI services, enabling real-world context enrichment and runbook storage.

## Why

The Runbook-Synthesizer's core value proposition relies on:
1. **Real-time context enrichment**: Fetching live metrics from OCI Monitoring and logs from OCI Logging
2. **Compute metadata resolution**: Retrieving instance details (shape, tags, AD) for context-aware checklist generation
3. **Runbook storage**: Reading markdown runbooks from OCI Object Storage buckets

Without OCI SDK integrations, the application cannot:
- Enrich alerts with real infrastructure data
- Access runbooks stored in cloud storage
- Provide context-aware troubleshooting checklists

This phase implements the OCI-specific adapters that bring the application to life with actual cloud infrastructure data.

## Scope

### In Scope
- `OciMonitoringAdapter` - implements `MetricsSourceAdapter` for OCI Monitoring API
- `OciLoggingAdapter` - implements `LogSourceAdapter` for OCI Logging API
- `OciObjectStorageClient` - wrapper for runbook bucket operations
- `OciComputeClient` - wrapper for fetching compute instance metadata
- Configuration model for OCI authentication and endpoints
- Unit tests with mocked OCI SDK clients

### Out of Scope
- Prometheus/Loki adapters (v1.1 roadmap)
- GenAI adapter (part of RAG pipeline, Phase 5)
- Integration tests requiring live OCI credentials

## Dependencies

> **Important:**
> This phase depends on Phase 3 (`implement-ports-interfaces`) being complete. The adapter classes implement interfaces defined there.

### Required Interfaces (from Phase 3)
- `MetricsSourceAdapter`
- `LogSourceAdapter`

### Required Domain Models (from Phase 2)
- `MetricSnapshot`
- `LogEntry`
- `ResourceMetadata`

### Required Maven Dependencies
- `oci-java-sdk-monitoring`
- `oci-java-sdk-loggingsearch`
- `oci-java-sdk-objectstorage`
- `oci-java-sdk-core` (for Compute API)
- `oci-java-sdk-common` (authentication)

## Design Approach

### Authentication Strategy
All OCI clients will use the standard OCI SDK authentication provider chain:
1. Instance principal (when running on OCI)
2. Config file (`~/.oci/config`) for local development
3. Environment variables as fallback

### Adapter Pattern
Each adapter:
1. Wraps the OCI SDK client
2. Translates OCI-specific types to domain model types
3. Returns `CompletableFuture<T>` for async compatibility with Helidon SE
4. Includes proper resource cleanup (`try-with-resources` or explicit `close()`)

### Error Handling
- OCI SDK exceptions mapped to domain-specific exceptions
- Retry logic with exponential backoff for transient failures
- Graceful degradation when services unavailable

## Affected Specifications

- **MODIFIED**: `ports-interfaces` - Verify implementations satisfy contracts
- **NEW**: `oci-adapters` - Requirements for OCI SDK adapter implementations
