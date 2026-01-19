# integration-tests Specification

## Purpose
TBD - created by archiving change implement-integration-tests. Update Purpose after archive.
## Requirements
### Requirement: Integration Test Infrastructure

The system SHALL provide integration test infrastructure using Helidon Test framework with WireMock for external service mocking. All tests SHALL use AssertJ fluent assertions for readable and maintainable test code.

#### Scenario: Test base class setup
- **GIVEN** a test class extending `IntegrationTestBase`
- **WHEN** the test executes
- **THEN** Helidon web server starts with correct configuration
- **AND** WireMock server is available for HTTP mocking

#### Scenario: Test fixture loading
- **GIVEN** JSON fixtures in `src/test/resources/fixtures/`
- **WHEN** `TestFixtures.loadAs(path, Class)` is called
- **THEN** the fixture is deserialized to the specified type

#### Scenario: AssertJ fluent assertions
- **GIVEN** any test class in the test suite
- **WHEN** assertions are made
- **THEN** AssertJ `assertThat()` is used instead of JUnit `assertEquals()`/`assertTrue()`
- **AND** exception testing uses `assertThatThrownBy()` for fluent chaining

---

### Requirement: Alert Ingestion Integration Tests

The system SHALL provide integration tests validating the alert ingestion flow from REST endpoint to enrichment trigger.

#### Scenario: Valid OCI alarm ingestion
- **GIVEN** a valid OCI Monitoring Alarm JSON payload
- **WHEN** POST to `/api/v1/alerts`
- **THEN** HTTP 200 response with `DynamicChecklist` body
- **AND** alert is normalized to canonical format
- **AND** context enrichment is triggered

#### Scenario: Invalid payload rejection
- **GIVEN** an invalid alert payload (missing required fields)
- **WHEN** POST to `/api/v1/alerts`
- **THEN** HTTP 400 response with validation error details

#### Scenario: OCI alarm normalization
- **GIVEN** OCI-specific alarm format with `body.alarmName`, `body.severity`
- **WHEN** processed by `OciMonitoringAlarmAdapter`
- **THEN** canonical `Alert` has `sourceService` = "oci-monitoring"
- **AND** dimensions extracted from OCI payload

---

### Requirement: Context Enrichment Integration Tests

The system SHALL provide integration tests validating context enrichment from OCI services.

#### Scenario: Metrics fetching via OCI Monitoring
- **GIVEN** an alert with `resourceId` dimension
- **AND** WireMock stub for OCI Monitoring API
- **WHEN** enrichment service processes the alert
- **THEN** `EnrichedContext.recentMetrics()` contains metrics from mock

#### Scenario: Logs fetching via OCI Logging
- **GIVEN** an alert with `resourceId` dimension
- **AND** WireMock stub for OCI Logging API
- **WHEN** enrichment service processes the alert
- **THEN** `EnrichedContext.recentLogs()` contains logs from mock

#### Scenario: Resource metadata resolution
- **GIVEN** an alert referencing an OCI Compute instance
- **AND** WireMock stub for OCI Compute API
- **WHEN** enrichment service processes the alert
- **THEN** `EnrichedContext.resource()` contains shape, availability domain, tags

---

### Requirement: RAG Pipeline Integration Tests

The system SHALL provide integration tests validating the RAG retrieval and embedding pipeline.

#### Scenario: Vector store operations
- **GIVEN** a `RunbookChunk` with embedding vector
- **WHEN** stored in vector store
- **AND** queried with similar embedding
- **THEN** chunk returned with similarity score above threshold

#### Scenario: Top-K retrieval
- **GIVEN** 10 runbook chunks in vector store
- **AND** a query similar to 3 specific chunks
- **WHEN** retrieval with `topK=3`
- **THEN** the 3 most relevant chunks returned in ranked order

#### Scenario: Embedding generation
- **GIVEN** text input for embedding
- **AND** WireMock stub for OCI GenAI embeddings API
- **WHEN** embedding service called
- **THEN** float[] of correct dimensions returned (1024 for Cohere)

---

### Requirement: Checklist Generation Integration Tests

The system SHALL provide integration tests validating checklist generation from enriched context and retrieved chunks.

#### Scenario: Full generation pipeline
- **GIVEN** enriched context with memory alert and VM shape
- **AND** vector store seeded with memory runbook chunks
- **AND** WireMock stub for LLM generation
- **WHEN** generator produces checklist
- **THEN** checklist contains steps from memory runbook
- **AND** steps reference current metric values

#### Scenario: Shape-based filtering
- **GIVEN** context with non-GPU shape (VM.Standard)
- **AND** runbook chunks with GPU-only steps
- **WHEN** checklist generated
- **THEN** GPU-specific steps excluded from output

---

### Requirement: Webhook Dispatcher Integration Tests

The system SHALL provide integration tests validating webhook dispatch to multiple destinations.

#### Scenario: Multi-channel dispatch
- **GIVEN** 2 webhook destinations configured (Slack, PagerDuty)
- **AND** WireMock stubs for webhook endpoints
- **WHEN** checklist with CRITICAL severity dispatched
- **THEN** both webhooks receive POST requests

#### Scenario: Severity filtering
- **GIVEN** webhook configured with `severities: [CRITICAL]`
- **WHEN** checklist with WARNING severity dispatched
- **THEN** webhook NOT called

#### Scenario: Slack Block Kit formatting
- **GIVEN** Slack webhook destination
- **WHEN** checklist dispatched
- **THEN** request body contains Slack Block Kit structure
- **AND** includes section blocks with checklist steps

---

### Requirement: End-to-End Flow Tests

The system SHALL provide end-to-end tests validating complete alert-to-checklist flow.

#### Scenario: Happy path flow
- **GIVEN** valid OCI alarm payload
- **AND** all OCI service mocks configured
- **AND** webhook destinations configured
- **WHEN** POST to `/api/v1/alerts`
- **THEN** `DynamicChecklist` response contains populated steps
- **AND** response includes source runbooks and LLM provider info
- **AND** configured webhooks receive dispatch

#### Scenario: OCI service failure handling
- **GIVEN** OCI Monitoring mock returns 503 Service Unavailable
- **WHEN** POST valid alert
- **THEN** system degrades gracefully
- **AND** error logged with context

#### Scenario: Timeout handling
- **GIVEN** OCI GenAI mock with 30-second delay
- **WHEN** alert processed
- **THEN** request times out within configured bounds
- **AND** timeout error returned, not hang

---

### Requirement: Test Configuration and CI Integration

The system SHALL provide Maven configuration for running integration tests in CI/CD pipelines. Tests SHALL follow behavior-driven testing patterns without testing implementation details.

#### Scenario: Test categorization
- **GIVEN** `*Test.java` files for unit tests
- **AND** `*IT.java` files for integration tests
- **WHEN** `mvn verify` executed
- **THEN** unit tests run via Surefire plugin
- **AND** integration tests run via Failsafe plugin

#### Scenario: Integration test execution
- **GIVEN** Failsafe plugin configured
- **WHEN** `mvn failsafe:integration-test` executed
- **THEN** only `*IT.java` tests run
- **AND** WireMock server lifecycle managed correctly

#### Scenario: No reflection-based testing
- **GIVEN** any test file in the test suite
- **WHEN** testing component behavior
- **THEN** tests invoke methods directly and verify outputs
- **AND** tests do NOT use reflection to verify method signatures or return types

#### Scenario: Outcome-based mock assertions
- **GIVEN** a test using Mockito mocks for dependencies
- **WHEN** verifying correct behavior
- **THEN** tests assert on actual return values and outcomes
- **AND** tests prefer outcome assertions over `verify()` calls on mock methods

---

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

### Requirement: Automated Infrastructure Provisioning

The system SHALL support automated provisioning of real infrastructure for integration tests without manual setup.

#### Scenario: Oracle 23ai Testcontainers initialization
- **GIVEN** the `integration-test` Maven profile is active
- **AND** Docker is available in the environment
- **WHEN** integration tests start
- **THEN** an Oracle 23ai Free container is automatically pulled and started
- **AND** JDBC connection properties are dynamically injected into the application context
- **AND** vector store schema is initialized before tests execute

#### Scenario: OCI Auth integration in tests
- **GIVEN** CI environment variables or a local OCI config
- **WHEN** integration tests execute OCI adapter calls
- **THEN** the system automatically authenticates using the available method
- **AND** tests proceed without requiring developer interaction

### Requirement: Test Fixture Standards

The system SHALL provide standardized test fixtures for reusable test data. Tests SHOULD load test data from fixtures rather than constructing inline data.

#### Scenario: Test fixture structure
- **GIVEN** the `src/test/resources/fixtures/` directory
- **WHEN** test data is needed for alerts, contexts, checklists, or webhooks
- **THEN** JSON fixture files exist under appropriate subdirectories
- **AND** fixtures are loadable via `TestFixtures.loadAs()` or `TestFixtures.loadJson()`

#### Scenario: Alert fixtures available
- **GIVEN** a test requiring `Alert` domain objects
- **WHEN** loading test data
- **THEN** `fixtures/alerts/*.json` contains valid canonical alert payloads
- **AND** fixtures include both OCI-specific and generic alert formats

#### Scenario: Context fixtures available
- **GIVEN** a test requiring `EnrichedContext` domain objects
- **WHEN** loading test data
- **THEN** `fixtures/contexts/*.json` contains complete context payloads
- **AND** fixtures include resource metadata, metrics, and logs samples

#### Scenario: Checklist fixtures available
- **GIVEN** a test requiring `DynamicChecklist` domain objects
- **WHEN** loading test data
- **THEN** `fixtures/checklists/*.json` contains sample checklist payloads
- **AND** fixtures include steps with all priority levels

### Requirement: In-Memory Cloud Adapter Stubs

The system SHALL provide in-memory stub implementations of cloud adapters for testing without cloud services.

#### Scenario: InMemoryCloudStorageAdapter lists seeded runbooks
- **GIVEN** an `InMemoryCloudStorageAdapter` with seeded content
- **AND** runbooks `memory-leak.md` and `cpu-spike.md` seeded in bucket "test-bucket"
- **WHEN** `listRunbooks("test-bucket")` is called
- **THEN** the result contains exactly `["memory-leak.md", "cpu-spike.md"]`

#### Scenario: InMemoryCloudStorageAdapter returns content for existing object
- **GIVEN** an `InMemoryCloudStorageAdapter` with seeded content
- **AND** content "# Memory Leak Runbook" seeded for "runbooks/memory-leak.md"
- **WHEN** `getRunbookContent("test-bucket", "runbooks/memory-leak.md")` is called
- **THEN** returns `Optional.of("# Memory Leak Runbook")`

#### Scenario: InMemoryCloudStorageAdapter returns empty for missing object
- **GIVEN** an `InMemoryCloudStorageAdapter` with no seeded content
- **WHEN** `getRunbookContent("test-bucket", "nonexistent.md")` is called
- **THEN** returns `Optional.empty()`

#### Scenario: StubMetricsSourceAdapter returns configured metrics
- **GIVEN** a `StubMetricsSourceAdapter` with configured metrics list
- **WHEN** `fetchMetrics(resourceId, lookback)` is called
- **THEN** returns the configured `List<MetricSnapshot>`

#### Scenario: StubLogSourceAdapter returns configured logs
- **GIVEN** a `StubLogSourceAdapter` with configured logs list
- **WHEN** `fetchLogs(resourceId, lookback, query)` is called
- **THEN** returns the configured `List<LogEntry>`

---

### Requirement: LocalStack EC2 Service Support

The system SHALL include EC2 service in LocalStack container for AWS metadata integration tests.

#### Scenario: EC2 client creation from LocalStack
- **GIVEN** a test class extending `LocalStackContainerBase`
- **WHEN** `createEc2Client()` is called
- **THEN** an `Ec2AsyncClient` is returned configured to connect to LocalStack

#### Scenario: EC2 DescribeInstances works against LocalStack
- **GIVEN** an EC2 client created via `createEc2Client()`
- **WHEN** `describeInstances()` is called
- **THEN** the call completes without connection errors

---

### Requirement: AWS RAG Pipeline Integration Test

The system SHALL provide an integration test validating the full RAG pipeline using AWS services via LocalStack.

#### Scenario: Alert to checklist flow with AWS S3 runbooks
- **GIVEN** runbook markdown files stored in LocalStack S3
- **AND** a valid alert with AWS resource dimensions
- **AND** a stubbed LLM provider
- **WHEN** the alert is processed through `RagPipelineService`
- **THEN** a `DynamicChecklist` is returned
- **AND** checklist steps reference content from S3-stored runbooks

---

### Requirement: Cloud Provider Configuration Switching Test

The system SHALL validate that cloud provider configuration correctly switches between OCI and AWS adapters.

#### Scenario: OCI provider configuration returns OCI adapters
- **GIVEN** application configuration with `cloud.provider=oci`
- **WHEN** `CloudAdapterFactory.createStorageAdapter()` is called
- **THEN** the returned adapter is of type `OciObjectStorageAdapter`

#### Scenario: AWS provider configuration returns AWS adapters
- **GIVEN** application configuration with `cloud.provider=aws`
- **WHEN** `CloudAdapterFactory.createStorageAdapter()` is called
- **THEN** the returned adapter is of type `AwsS3StorageAdapter`

#### Scenario: Compute metadata adapter follows provider configuration
- **GIVEN** application configuration with `cloud.provider=aws`
- **WHEN** `CloudAdapterFactory.createComputeMetadataAdapter()` is called
- **THEN** the returned adapter is of type `AwsEc2MetadataAdapter`

### Requirement: AWS CDK Infrastructure Provisioning

The system SHALL provide AWS CDK (TypeScript) infrastructure-as-code for provisioning real AWS e2e test resources. The CDK stack SHALL be idempotent and support manual cleanup.

#### Scenario: CDK stack deploys S3 bucket
- **GIVEN** the CDK project in `infra/` directory
- **AND** valid AWS credentials configured
- **WHEN** `npm run cdk:deploy` is executed
- **THEN** an S3 bucket named `runbook-synthesizer-e2e-{accountId}` is created
- **AND** the bucket is tagged with `ManagedBy=runbook-synthesizer-e2e`

#### Scenario: CDK stack deploys CloudWatch Log Group
- **GIVEN** the CDK project in `infra/` directory
- **AND** valid AWS credentials configured
- **WHEN** `npm run cdk:deploy` is executed
- **THEN** a CloudWatch Log Group named `/runbook-synthesizer/e2e` is created
- **AND** the log group has 1-day retention

#### Scenario: CDK stack is idempotent
- **GIVEN** the CDK stack is already deployed
- **WHEN** `npm run cdk:deploy` is executed again
- **THEN** no duplicate resources are created
- **AND** the deploy succeeds with no changes (or updates only changed resources)

#### Scenario: CDK outputs resource identifiers
- **GIVEN** the CDK stack is deployed
- **WHEN** deployment completes
- **THEN** CloudFormation outputs include `BucketName` and `LogGroupName`
- **AND** these values can be retrieved via `aws cloudformation describe-stacks`

---

### Requirement: Extensible CDK Construct Architecture

The system SHALL use an extensible construct pattern for AWS service provisioning, enabling easy addition of new services in the future.

#### Scenario: S3Construct is standalone reusable
- **GIVEN** the `S3Construct` class in `lib/constructs/`
- **WHEN** added to any CDK stack
- **THEN** it provisions an S3 bucket with configurable name prefix
- **AND** it exports a `bucket` property for further configuration

#### Scenario: Adding new AWS service
- **GIVEN** a new AWS service requirement (e.g., DynamoDB)
- **WHEN** a developer creates `DynamoDbConstruct` in `lib/constructs/`
- **THEN** they can add it to `E2eTestStack` with one line
- **AND** existing constructs remain unchanged

---

### Requirement: Real AWS E2E Test Base Class

The system SHALL provide a `RealAwsTestBase` class for Java e2e tests that validates AWS credentials and configures AWS clients for real AWS service access.

#### Scenario: Tests disabled by default
- **GIVEN** `AWS_E2E_ENABLED` environment variable is not set
- **WHEN** test runner processes `*IT.java` files in `aws/e2e/` package
- **THEN** all real AWS tests are skipped

#### Scenario: Tests enabled via environment variable
- **GIVEN** `AWS_E2E_ENABLED=true` environment variable is set
- **AND** valid AWS credentials are available
- **WHEN** test runner processes `*IT.java` files in `aws/e2e/` package
- **THEN** real AWS tests execute against provisioned resources

#### Scenario: Credential validation fails fast
- **GIVEN** `AWS_E2E_ENABLED=true` is set
- **AND** no valid AWS credentials are available
- **WHEN** tests start
- **THEN** a clear error message indicates missing credentials
- **AND** tests fail immediately rather than timing out

---

### Requirement: Real AWS S3 E2E Tests

The system SHALL provide e2e tests validating S3 storage operations against a real AWS S3 bucket.

#### Scenario: Upload and retrieve runbook from real S3
- **GIVEN** the CDK stack is deployed
- **AND** `AWS_E2E_ENABLED=true`
- **WHEN** a runbook is uploaded to the e2e bucket via `AwsS3StorageAdapter`
- **THEN** the runbook content can be retrieved via the same adapter
- **AND** the content matches exactly

#### Scenario: List runbooks from real S3
- **GIVEN** the CDK stack is deployed
- **AND** sample runbooks uploaded to the e2e bucket
- **WHEN** `listRunbooks()` is called on `AwsS3StorageAdapter`
- **THEN** all uploaded runbook files are listed

---

### Requirement: Real AWS CloudWatch Logs E2E Tests

The system SHALL provide e2e tests validating CloudWatch Logs operations against a real AWS log group.

#### Scenario: Write and read logs from real CloudWatch
- **GIVEN** the CDK stack is deployed
- **AND** `AWS_E2E_ENABLED=true`
- **WHEN** log events are written via `AwsCloudWatchLogsAdapter`
- **THEN** the events can be read back via the same adapter
- **AND** timestamps and messages match

---

### Requirement: Real AWS E2E Maven Profile

The system SHALL provide a Maven profile for running real AWS e2e tests isolated from other test types.

#### Scenario: Profile runs only real AWS tests
- **GIVEN** the `e2e-aws-real` Maven profile
- **WHEN** `mvnw verify -Pe2e-aws-real` is executed
- **THEN** only tests in `**/aws/e2e/*IT.java` are executed
- **AND** LocalStack and unit tests are not executed

#### Scenario: Profile sets required environment
- **GIVEN** the `e2e-aws-real` Maven profile
- **WHEN** tests execute under this profile
- **THEN** `AWS_E2E_ENABLED=true` is set automatically

