# integration-tests Spec Delta

## ADDED Requirements

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
