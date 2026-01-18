## ADDED Requirements

### Requirement: Container-Based E2E Test Infrastructure
The system SHALL provide a Testcontainers-based testing infrastructure that enables running E2E tests with real Oracle 23ai and Ollama containers.

#### Scenario: Oracle container starts successfully
- **WHEN** a container test class extends `ContainerTestBase`
- **THEN** an Oracle 23ai container SHALL be started with vector search capability
- **AND** the container SHALL be accessible via JDBC

#### Scenario: Ollama container starts and loads models
- **WHEN** a container test class extends `ContainerTestBase`
- **THEN** an Ollama container SHALL be started with HTTP API on port 11434
- **AND** the `nomic-embed-text` and `llama3.2:1b` models SHALL be available

#### Scenario: Docker not available graceful skip
- **WHEN** Docker is not available on the test machine
- **THEN** container tests SHALL be skipped with a clear message
- **AND** non-container tests SHALL continue to run

---

### Requirement: Real Vector Store Integration Tests
The system SHALL provide integration tests that validate Oracle 23ai vector search functionality using real database operations.

#### Scenario: Vector store and retrieve with real similarity
- **WHEN** runbook chunks are stored with real embeddings
- **AND** a similarity search is performed
- **THEN** results SHALL be ranked by actual cosine similarity computed by Oracle

#### Scenario: Semantic similarity validation
- **WHEN** chunks about different topics are stored
- **AND** a semantically related query is executed
- **THEN** the most semantically relevant chunks SHALL appear in top results

---

### Requirement: Real LLM Integration Tests
The system SHALL provide integration tests that validate Ollama LLM functionality for embeddings and text generation.

#### Scenario: Embedding generation returns correct dimensions
- **WHEN** text is embedded using the Ollama `nomic-embed-text` model
- **THEN** the returned embedding SHALL have 768 dimensions

#### Scenario: Checklist generation returns structured output
- **WHEN** a checklist generation prompt is sent to Ollama
- **THEN** the response SHALL contain numbered steps suitable for a troubleshooting checklist

---

### Requirement: Full-Stack Container E2E Tests
The system SHALL provide full-stack E2E tests that exercise the complete alert-to-checklist flow using real infrastructure.

#### Scenario: Alert to checklist with real components
- **WHEN** an alert is POSTed to `/api/v1/alerts`
- **AND** real Oracle vector store and Ollama LLM are used
- **THEN** the response SHALL contain an LLM-generated checklist with steps derived from stored runbooks

#### Scenario: Full flow with webhook dispatch
- **WHEN** a full-stack alert is processed
- **AND** a webhook destination is configured
- **THEN** the generated checklist SHALL be dispatched to the webhook endpoint

---

### Requirement: Container Test Profile
The system SHALL provide a Maven profile for running container-based tests separately from standard integration tests.

#### Scenario: Default verify excludes container tests
- **WHEN** `./mvnw verify` is run without profiles
- **THEN** container tests (tagged with `container`) SHALL NOT be executed

#### Scenario: Container profile runs container tests
- **WHEN** `./mvnw verify -Pe2e-containers` is run
- **THEN** only tests tagged with `container` SHALL be executed
