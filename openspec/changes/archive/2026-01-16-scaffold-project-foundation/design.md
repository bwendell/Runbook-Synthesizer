# Design: Runbook-Synthesizer Project Foundation

## Context

This is a greenfield Java 25 project using Helidon SE as the web framework, targeting deployment on OCI. The scaffolding must support:

- TDD workflow with JUnit 5
- OCI SDK integrations (future phases)
- LangChain4j for RAG pipeline (future phases)
- Native GraalVM compilation (optional, future)

**Stakeholders**: SRE teams, platform engineers, on-call responders

## Goals

- Establish a working Helidon SE skeleton that starts and responds to `/health`
- Configure Maven with all necessary dependency management (BOMs)
- Create correct package structure per DESIGN.md architecture
- Enable TDD workflow from day one
- Pass `mvn verify` with zero warnings

## Non-Goals

- Implement any business logic (domain models, adapters, etc.)
- Configure OCI authentication (deferred to Phase 4)
- Add Docker/container configuration (deferred to deployment phase)
- Configure CI/CD pipelines

## Decisions

### Decision 1: Use Helidon SE 4.x with Virtual Threads

**Rationale**: Helidon SE 4.x requires Java 21+ and leverages virtual threads for optimal performance. Using Java 25 provides access to the latest language features and LTS stability. Oracle's native microframework with first-class OCI support.

**Alternatives considered**:
- Spring Boot: Heavier, not Oracle-native
- Micronaut: Good alternative but less OCI integration
- Quarkus: Red Hat ecosystem, less OCI alignment

### Decision 2: Maven with BOM-based Dependency Management

**Rationale**: Maven is the standard for OCI SDK projects. Using BOMs (Helidon, OCI SDK) ensures version consistency.

**Structure**:
```xml
<dependencyManagement>
  <dependencies>
    <dependency>io.helidon:helidon-bom</dependency>
    <dependency>com.oracle.oci.sdk:oci-java-sdk-bom</dependency>
  </dependencies>
</dependencyManagement>
```

### Decision 3: Package Structure Aligned with Hexagonal Architecture

**Rationale**: DESIGN.md specifies domain/ports/adapters pattern.

```
com.oracle.runbook/
├── domain/       # Pure domain models (no dependencies)
├── ingestion/    # Alert source adapters
├── enrichment/   # Context enrichment ports + adapters
├── rag/          # RAG pipeline (embeddings, retrieval, generation)
├── api/          # REST resources (Helidon handlers)
├── output/       # Webhook destinations
└── config/       # Application configuration
```

### Decision 4: JUnit 5 Platform with Helidon Test Extensions

**Rationale**: Standard testing, with Helidon's WebClient test utilities for API testing.

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| Helidon SE 4.x breaking changes | Pin specific version in BOM |
| OCI SDK version conflicts | Use OCI SDK BOM for consistency |
| Package bloat in scaffolding | Create only necessary packages initially |

## Open Questions

1. Should we include GraalVM native-image configuration in Phase 1?
   - **Recommendation**: No, defer to later phase
   
2. Which Helidon SE version specifically?
   - **Recommendation**: Use latest stable 4.x from Maven Central
