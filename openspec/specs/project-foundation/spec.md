# project-foundation Specification

## Purpose
TBD - created by archiving change scaffold-project-foundation. Update Purpose after archive.
## Requirements
### Requirement: Maven Project Configuration

The system SHALL use Maven as the build tool with Java 25 compiler settings and dependency management via Helidon SE and OCI SDK BOMs.

#### Scenario: Build compiles successfully
- **WHEN** `mvn compile` is executed
- **THEN** the build completes with SUCCESS status

#### Scenario: Tests execute with JUnit 5
- **WHEN** `mvn test` is executed
- **THEN** JUnit 5 tests are discovered and executed

---

### Requirement: Helidon SE Web Server

The system SHALL expose a Helidon SE web server that starts on the configured port and accepts HTTP requests.

#### Scenario: Server starts successfully
- **WHEN** the application is started
- **THEN** the server listens on port 8080 (or configured port)

#### Scenario: Server accepts HTTP requests
- **WHEN** an HTTP GET request is sent to the server
- **THEN** the server responds with an appropriate HTTP response

---

### Requirement: Health Endpoint

The system SHALL expose a `/health` endpoint that returns server health status.

#### Scenario: Health check returns 200 OK
- **WHEN** a GET request is sent to `/health`
- **THEN** the response status is 200 OK
- **AND** the response includes health status information

---

### Requirement: Graceful Shutdown

The system SHALL handle shutdown signals gracefully, completing in-flight requests before termination.

#### Scenario: Clean shutdown on SIGINT
- **WHEN** SIGINT (Ctrl+C) is received
- **THEN** the server logs shutdown initiation
- **AND** the server terminates within 10 seconds

---

### Requirement: Package Structure

The system SHALL organize code into hexagonal architecture packages: domain, ingestion, enrichment, rag, api, output, and config.

#### Scenario: All packages exist
- **WHEN** the project structure is examined
- **THEN** all specified packages exist under `com.oracle.runbook`

---

### Requirement: Application Configuration

The system SHALL load configuration from `application.yaml` using Helidon Config.

#### Scenario: Server port configurable
- **WHEN** `server.port` is set to `9090` in application.yaml
- **THEN** the server starts on port 9090

---

### Requirement: Sample Runbook Templates

The project SHALL include sample runbook templates in the `examples/runbooks/` directory following the DESIGN.md format specification.

#### Scenario: Sample runbooks exist
- **WHEN** the examples directory is examined
- **THEN** runbooks exist for memory, cpu, and gpu troubleshooting

#### Scenario: Runbooks have valid frontmatter
- **WHEN** a runbook file is parsed
- **THEN** it contains valid YAML frontmatter with title, tags, and applicable_shapes

