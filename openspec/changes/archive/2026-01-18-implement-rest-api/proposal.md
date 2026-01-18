# Implement REST API Endpoints

## Summary

Implement the REST API layer (Phase 6) for Runbook-Synthesizer using Helidon SE 4.x. This phase creates the HTTP endpoints defined in DESIGN.md that expose the application's functionality to external clients.

## Motivation

The REST API is the primary interface for:
1. Ingesting alerts and triggering checklist generation (`POST /api/v1/alerts`)
2. Managing runbook re-indexing from Object Storage (`POST /api/v1/runbooks/sync`)
3. Managing webhook destinations (`GET/POST /api/v1/webhooks`)
4. Health monitoring (`GET /api/v1/health`)

This phase transforms the application from a scaffold into a fully functional HTTP service.

## Scope

### In Scope
- `AlertResource` handler for alert ingestion and checklist generation
- `RunbookResource` handler for runbook sync operations
- `WebhookResource` handler for webhook CRUD operations
- `HealthResource` handler for health/readiness checks
- JSON serialization/deserialization with request/response DTOs
- Input validation and error handling
- Integration with routing in `RunbookSynthesizerApp`
- Comprehensive unit tests for each resource

### Out of Scope
- Authentication/authorization (OCI IAM integration is Phase 4)
- Full end-to-end integration tests (Phase 8)
- Actual service implementations (uses mocks/stubs in handlers)
- Rate limiting and advanced security features

## Design Approach

### Architecture Pattern
Use Helidon SE 4.x functional routing with `HttpService` implementations:
- Each resource is a separate class implementing `HttpService`
- DTOs (Data Transfer Objects) for request/response serialization
- Service stubs that return mock data (real implementations wired in later phases)
- Consistent error response format across all endpoints

### API Structure per DESIGN.md
```
/api/v1/alerts      - POST (ingest alert, return checklist)
/api/v1/runbooks/sync - POST (trigger re-indexing)
/api/v1/webhooks    - GET (list), POST (register)
/api/v1/health      - GET (health status)
```

### Error Handling
- Validation errors: 400 Bad Request with field-level details
- Not found: 404 with resource context
- Server errors: 500 with correlation ID

## Dependencies

- **Phase 2 (Domain Models)**: `Alert`, `DynamicChecklist`, `ChecklistStep`, `AlertSeverity` - ✅ Complete
- **Phase 3 (Ports/Interfaces)**: Service interfaces that handlers will call - Partially complete
- **Helidon SE 4.x**: HTTP routing, JSON media support

## Affected Specifications

- **NEW**: `rest-api` - Requirements for all REST endpoints
