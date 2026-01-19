# Tasks: Add Cloud Provider Abstraction

## 1. Foundation: Add New Interfaces

### 1.1 Create Cloud Infrastructure Package Structure
- [x] Create `com.oracle.runbook.infrastructure.cloud` package
- [x] Create `com.oracle.runbook.infrastructure.cloud.oci` subpackage
- [x] Create `com.oracle.runbook.infrastructure.cloud.aws` subpackage
- [x] Add `package-info.java` with documentation for each package

**Acceptance Criteria:**
- Package structure exists and follows design.md specification
- Javadoc explains the purpose of each package

**Verification:**
- Run `mvn compile` successfully
- Package structure visible in IDE

---

### 1.2 Create CloudConfig Interface
- [x] Create `CloudConfig` interface with `provider()` and `region()` methods
- [x] Add Javadoc explaining the interface purpose

**Acceptance Criteria:**
- Interface defined in `infrastructure/cloud/CloudConfig.java`
- Methods return non-null strings

**Verification:**
- Run `mvn compile` successfully
- Unit test verifies interface contract

---

### 1.3 Create CloudStorageAdapter Interface
- [x] Create `CloudStorageAdapter` interface
- [x] Define `providerType()`, `listRunbooks()`, `getRunbookContent()` methods
- [x] All methods return `CompletableFuture` for async support
- [x] Add comprehensive Javadoc

**Acceptance Criteria:**
- Interface defined in `infrastructure/cloud/CloudStorageAdapter.java`
- Methods follow existing `MetricsSourceAdapter` patterns
- Returns `CompletableFuture` for Helidon SE compatibility

**Verification:**
- Run `mvn compile` successfully
- Interface has no compilation errors

---

### 1.4 Create ComputeMetadataAdapter Interface
- [x] Create `ComputeMetadataAdapter` interface
- [x] Define `providerType()` and `getInstance()` methods
- [x] Return type is `CompletableFuture<Optional<ResourceMetadata>>`
- [x] Add comprehensive Javadoc

**Acceptance Criteria:**
- Interface defined in `infrastructure/cloud/ComputeMetadataAdapter.java`
- Follows existing adapter interface patterns
- Uses domain `ResourceMetadata` for return type

**Verification:**
- Run `mvn compile` successfully
- Interface has no compilation errors

---

## 2. Refactor OCI Implementations

### 2.1 Refactor OciConfig to Implement CloudConfig
- [x] Modify `OciConfig` to implement `CloudConfig` interface
- [x] Add `provider()` method returning `"oci"`
- [x] Ensure backward compatibility with existing usage

**Acceptance Criteria:**
- `OciConfig` implements `CloudConfig`
- All existing tests continue to pass
- No breaking changes to existing API

**Verification:**
- Run `mvn test -Dtest=OciConfigTest` passes
- Run `mvn test` - all tests pass

---

### 2.2 Refactor OciObjectStorageClient to OciObjectStorageAdapter
- [x] Rename `OciObjectStorageClient` to `OciObjectStorageAdapter`
- [x] Move to `infrastructure.cloud.oci` package
- [x] Implement `CloudStorageAdapter` interface
- [x] Adapt method signatures to match interface
- [x] Add `providerType()` returning `"oci"`
- [x] Update all references throughout codebase

**Acceptance Criteria:**
- Class renamed and implements `CloudStorageAdapter`
- All existing functionality preserved
- All callers updated to use new class name

**Verification:**
- Run `mvn compile` successfully
- Run `mvn test` - all tests pass
- Grep for old class name returns no results

---

### 2.3 Refactor OciComputeClient to OciComputeMetadataAdapter
- [x] Rename `OciComputeClient` to `OciComputeMetadataAdapter`
- [x] Move to `infrastructure.cloud.oci` package
- [x] Implement `ComputeMetadataAdapter` interface
- [x] Add `providerType()` returning `"oci"`
- [x] Update all references throughout codebase

**Acceptance Criteria:**
- Class renamed and implements `ComputeMetadataAdapter`
- All existing functionality preserved
- All callers updated to use new class name

**Verification:**
- Run `mvn compile` successfully
- Run `mvn test` - all tests pass
- Grep for old class name returns no results

---

### 2.4 Move OCI Config Classes to New Package
- [x] Move `OciConfig` to `infrastructure.cloud.oci` package
- [x] Move `OciAuthProviderFactory` to `infrastructure.cloud.oci` package
- [x] Update all import statements throughout codebase
- [x] Ensure no circular dependencies

**Acceptance Criteria:**
- Classes relocated to new package
- All imports updated
- No compilation errors

**Verification:**
- Run `mvn compile` successfully
- Run `mvn test` - all tests pass

---

## 3. Add AWS Dependencies

### 3.1 Add AWS SDK Dependencies to pom.xml
- [x] Add AWS BOM for version management
- [x] Add `software.amazon.awssdk:s3` dependency
- [x] Add `software.amazon.awssdk:ec2` dependency
- [x] Add `software.amazon.awssdk:cloudwatch` dependency
- [x] Add `software.amazon.awssdk:cloudwatchlogs` dependency
- [x] Add scope for optional AWS compilation

**Acceptance Criteria:**
- AWS SDK v2 dependencies added
- Version managed via BOM
- Dependencies resolve successfully

**Verification:**
- Run `mvn dependency:tree` shows AWS SDK dependencies
- Run `mvn compile` successfully

---

## 4. Implement AWS Adapters

### 4.1 Create AwsConfig Record
- [x] Create `AwsConfig` record implementing `CloudConfig`
- [x] Include `region`, `bucket`, optional `accessKeyId`, `secretAccessKey` fields
- [x] Add factory method `fromEnvironment()` using AWS default credential chain
- [x] Add validation for required fields

**Acceptance Criteria:**
- Record defined in `infrastructure.cloud.aws.AwsConfig`
- Implements `CloudConfig` interface
- `provider()` returns `"aws"`

**Verification:**
- Run `mvn compile` successfully
- Unit test `AwsConfigTest` passes

---

### 4.2 Create AwsS3StorageAdapter
- [x] Create `AwsS3StorageAdapter` implementing `CloudStorageAdapter`
- [x] Implement `listRunbooks()` using S3 listObjects
- [x] Implement `getRunbookContent()` using S3 getObject
- [x] Handle missing objects by returning `Optional.empty()`
- [x] Use async S3 client for CompletableFuture support

**Acceptance Criteria:**
- Class implements `CloudStorageAdapter`
- `providerType()` returns `"aws"`
- Handles S3 errors gracefully

**Verification:**
- Run unit tests with mocked S3 client
- Run integration test with LocalStack container

---

### 4.3 Create AwsEc2MetadataAdapter
- [x] Create `AwsEc2MetadataAdapter` implementing `ComputeMetadataAdapter`
- [x] Implement `getInstance()` using EC2 describeInstances
- [x] Map EC2 instance data to `ResourceMetadata` domain model
- [x] Handle missing instances by returning `Optional.empty()`

**Acceptance Criteria:**
- Class implements `ComputeMetadataAdapter`
- `providerType()` returns `"aws"`
- Maps EC2 tags to `ResourceMetadata` tags

**Verification:**
- Run unit tests with mocked EC2 client
- Run integration test with LocalStack container

---

### 4.4 Create AwsCloudWatchMetricsAdapter
- [x] Create `AwsCloudWatchMetricsAdapter` implementing `MetricsSourceAdapter`
- [x] Implement `fetchMetrics()` using CloudWatch getMetricData
- [x] Map CloudWatch metrics to `MetricSnapshot` domain model
- [x] Support configurable namespace and dimension mapping

**Acceptance Criteria:**
- Class implements existing `MetricsSourceAdapter` interface
- `sourceType()` returns `"aws-cloudwatch"`
- Returns empty list when no metrics found

**Verification:**
- Run unit tests with mocked CloudWatch client
- Run integration test with LocalStack container

---

### 4.5 Create AwsCloudWatchLogsAdapter
- [x] Create `AwsCloudWatchLogsAdapter` implementing `LogSourceAdapter`
- [x] Implement `fetchLogs()` using CloudWatch Logs filterLogEvents
- [x] Map CloudWatch log events to `LogEntry` domain model
- [x] Support CloudWatch Insights query syntax

**Acceptance Criteria:**
- Class implements existing `LogSourceAdapter` interface
- `sourceType()` returns `"aws-cloudwatch-logs"`
- Returns empty list when no logs found

**Verification:**
- Run unit tests with mocked CloudWatch Logs client
- Run integration test with LocalStack container

---

## 5. Add Configuration Support

### 5.1 Update application.yaml Schema
- [x] Add `cloud.provider` configuration property
- [x] Add `cloud.aws` section with S3 bucket, region settings
- [x] Document configuration in code comments
- [x] Add example values in commented section

**Acceptance Criteria:**
- Configuration schema supports both AWS and OCI
- Default provider is `oci` for backward compatibility
- All fields are well-documented

**Verification:**
- Application starts with new configuration
- Helidon reads configuration correctly

---

### 5.2 Create CloudAdapterFactory
- [x] Create factory class to instantiate correct adapters based on config
- [x] Read `cloud.provider` from configuration
- [x] Create OCI adapters when provider is `"oci"`
- [x] Create AWS adapters when provider is `"aws"`
- [x] Throw clear exception for unknown providers

**Acceptance Criteria:**
- Factory correctly instantiates adapters based on configuration
- Error messages are clear and actionable
- Factory is injectable via Helidon

**Verification:**
- Unit test with mocked configuration
- Integration test with full application context

---

## 6. Add Tests

### 6.1 Add Interface Contract Tests
- [x] Add `CloudStorageAdapterTest` verifying interface methods
- [x] Add `ComputeMetadataAdapterTest` verifying interface methods
- [x] Add `CloudConfigTest` verifying interface contract
- [x] Add tests verifying OCI adapters implement new interfaces correctly

**Acceptance Criteria:**
- Each interface has contract tests verifying method signatures
- Tests use `isAssignableFrom` pattern per existing conventions
- @DisplayName annotations describe behavior clearly

**Verification:**
- Run `mvn test -Dtest=Cloud*Test` - all pass

---

### 6.2 Add AwsConfig Unit Tests
- [x] Add `AwsConfigTest` with required field validation tests
- [x] Add tests for `provider()` returning `"aws"`
- [x] Add tests for `region()` returning configured region
- [x] Add tests for `fromEnvironment()` factory method
- [x] Add tests for missing required fields throwing exceptions
- [x] Add tests for optional fields (accessKeyId, secretAccessKey)

**Acceptance Criteria:**
- Tests follow `OciConfigTest` structure with `@Nested` classes
- Environment variable parsing tested with mock providers
- All field validations covered

**Verification:**
- Run `mvn test -Dtest=AwsConfigTest` - all pass

---

### 6.3 Add AWS Adapter Unit Tests
- [x] Add `AwsS3StorageAdapterTest` with mocked S3AsyncClient
  - [x] Test `providerType()` returns `"aws"`
  - [x] Test `listRunbooks()` success scenario
  - [x] Test `listRunbooks()` with empty bucket
  - [x] Test `getRunbookContent()` success scenario
  - [x] Test `getRunbookContent()` with missing object returns `Optional.empty()`
  - [x] Test constructor rejects null client
  - [x] Test constructor rejects null config
- [x] Add `AwsEc2MetadataAdapterTest` with mocked Ec2AsyncClient
  - [x] Test `providerType()` returns `"aws"`
  - [x] Test `getInstance()` success scenario
  - [x] Test `getInstance()` with missing instance returns `Optional.empty()`
  - [x] Test mapping EC2 tags to `ResourceMetadata`
  - [x] Test constructor null checks
- [x] Add `AwsCloudWatchMetricsAdapterTest` with mocked CloudWatchAsyncClient
  - [x] Test `sourceType()` returns `"aws-cloudwatch"`
  - [x] Test `fetchMetrics()` success scenario
  - [x] Test `fetchMetrics()` with no metrics returns empty list
  - [x] Test mapping CloudWatch metrics to `MetricSnapshot`
  - [x] Test constructor null checks
- [x] Add `AwsCloudWatchLogsAdapterTest` with mocked CloudWatchLogsAsyncClient
  - [x] Test `sourceType()` returns `"aws-cloudwatch-logs"`
  - [x] Test `fetchLogs()` success scenario
  - [x] Test `fetchLogs()` with no logs returns empty list
  - [x] Test mapping CloudWatch log events to `LogEntry`
  - [x] Test constructor null checks

**Acceptance Criteria:**
- Each adapter has at least 80% code coverage
- Uses AssertJ fluent assertions per project conventions
- Tests real behavior, not mock behavior (per testing-patterns-java)
- Error scenarios and empty results tested

**Verification:**
- Run `mvn test -Dtest=Aws*Test` - all pass
- Run `mvn jacoco:report` - coverage > 80%

---

### 6.4 Add CloudAdapterFactory Unit Tests
- [x] Add `CloudAdapterFactoryTest`
  - [x] Test factory creates OCI adapters when `cloud.provider=oci`
  - [x] Test factory creates AWS adapters when `cloud.provider=aws`
  - [x] Test factory throws `IllegalArgumentException` for unknown provider
  - [x] Test error message is clear and actionable
  - [x] Test configuration injection works correctly

**Acceptance Criteria:**
- Factory pattern thoroughly tested with configuration mocks
- Error handling follows fail-fast principle
- Tests follow behavior-driven patterns

**Verification:**
- Run `mvn test -Dtest=CloudAdapterFactoryTest` - all pass

---

### 6.5 Add Error Handling and Edge Case Tests
- [x] Add tests for AWS SDK exception handling
  - [x] Test `S3Exception` handling in storage adapter
  - [x] Test `Ec2Exception` handling in metadata adapter
  - [x] Test `CloudWatchException` handling in metrics adapter
  - [x] Test authentication failures (credentials not found)
  - [x] Test network timeout scenarios
- [x] Add tests for malformed response handling
  - [x] Test empty response bodies
  - [x] Test missing required fields in response
  - [x] Test null values in optional fields

**Acceptance Criteria:**
- All SDK exceptions wrapped in domain exceptions
- No SDK exceptions leak through adapter boundaries
- Graceful degradation for recoverable errors

**Verification:**
- Run `mvn test -Dtest=*AdapterTest` - all pass

---

### 6.6 Add OCI Adapter Refactoring Regression Tests
- [x] Update existing `OciObjectStorageClientTest` → `OciObjectStorageAdapterTest`
- [x] Add test verifying `OciObjectStorageAdapter` implements `CloudStorageAdapter`
- [x] Update existing `OciComputeClientTest` → `OciComputeMetadataAdapterTest`
- [x] Add test verifying `OciComputeMetadataAdapter` implements `ComputeMetadataAdapter`
- [x] Add test verifying `OciConfig` implements `CloudConfig`
- [x] Ensure all existing OCI tests continue to pass

**Acceptance Criteria:**
- No regression in OCI adapter functionality
- All renamed tests follow new naming conventions
- Interface implementation verified

**Verification:**
- Run `mvn test -Dtest=Oci*Test` - all pass
- Run `mvn test` - all existing tests pass

---

### 6.7 Add Integration Tests with LocalStack
- [x] Add LocalStack Testcontainers dependency to pom.xml
- [x] Create `LocalStackContainerBase` test utility
  - [x] Configure for S3, EC2, CloudWatch, CloudWatch Logs services
  - [x] Add helper methods for creating test buckets/resources
  - [x] Add proper cleanup in @AfterAll
- [x] Add `AwsS3StorageAdapterIT`
  - [x] Test listing runbooks from real S3 bucket
  - [x] Test reading runbook content
  - [x] Test handling non-existent objects
- [ ] Add `AwsEc2MetadataAdapterIT` (Skipped - LocalStack has limited EC2 support)
  - [ ] Test fetching instance metadata
  - [ ] Test handling non-existent instances
- [x] Add `AwsCloudWatchMetricsAdapterIT`
  - [x] Test fetching metrics from CloudWatch
  - [x] Test with custom namespaces and dimensions
- [x] Add `AwsCloudWatchLogsAdapterIT`
  - [x] Test fetching logs from CloudWatch Logs
  - [x] Test log group query handling

**Acceptance Criteria:**
- Integration tests run against LocalStack containers
- Tests are skipped when Docker is unavailable (use `@EnabledIf`)
- Tests clean up resources after completion
- Follow existing `OracleContainerBase` pattern

**Verification:**
- Run `mvn verify -Pe2e-containers` - all pass
- LocalStack containers start and stop correctly

---

### 6.8 Add Configuration Integration Tests
- [x] Add `CloudConfigurationIT`
  - [x] Test application starts with AWS configuration
  - [x] Test application starts with OCI configuration (default)
  - [x] Test switching providers via configuration reload
  - [x] Test invalid configuration fails fast with clear error

**Acceptance Criteria:**
- Full application context tested
- Helidon configuration binding verified
- Error messages actionable

**Verification:**
- Run `mvn verify -Dtest=CloudConfigurationIT` - all pass

---

## 7. Documentation

### 7.1 Update ARCHITECTURE.md
- [x] Add section on cloud provider abstraction
- [x] Document supported providers (OCI, AWS)
- [x] Add mermaid diagram showing adapter structure

**Acceptance Criteria:**
- Documentation explains how to configure different providers
- Examples provided for both AWS and OCI

**Verification:**
- Documentation renders correctly in IDE/GitHub
- Examples are accurate and tested

---

### 7.2 Update README.md
- [x] Add cloud provider configuration section
- [x] Document AWS credentials setup
- [x] Document OCI credentials setup (existing)
- [x] Add troubleshooting section for common issues

**Acceptance Criteria:**
- New users can configure either provider
- Prerequisites are clearly stated

**Verification:**
- Follow README instructions on clean machine
- Application starts successfully

---

## Dependencies

```
1.1 ─► 1.2 ─► 1.3 ─► 1.4 ─► 2.1 ─► 2.2 ─► 2.3 ─► 2.4
                              │
                              ▼
                            3.1 ─► 4.1 ─► 4.2 ─► 4.3 ─► 4.4 ─► 4.5
                                                                  │
                                                                  ▼
                            5.1 ─────────────────────────────► 5.2
                                                                  │
                                                                  ▼
                            6.1 ─────────────────────────────► 6.2
                                                                  │
                                                                  ▼
                                                      7.1 ───► 7.2
```

## Parallelizable Work

The following groups can be worked on in parallel after their dependencies are met:

- **Group A (after 2.4):** 4.1, 4.2 (AWS storage)
- **Group B (after 4.2):** 4.3, 4.4, 4.5 (AWS adapters)
- **Group C (after 4.5):** 6.1, 6.2 (tests) can run in parallel with 7.1, 7.2 (docs)
