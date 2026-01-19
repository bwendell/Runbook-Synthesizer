# cloud-abstraction Specification Delta

## ADDED Requirements

### Requirement: CloudConfig Interface

The system SHALL provide a `CloudConfig` interface that defines the common contract for cloud provider configuration.

#### Scenario: Interface provides provider identifier
- **GIVEN** a CloudConfig implementation
- **WHEN** `provider()` is called
- **THEN** it returns a non-null string identifying the cloud provider (e.g., `"oci"`, `"aws"`)

#### Scenario: Interface provides region
- **GIVEN** a CloudConfig implementation
- **WHEN** `region()` is called
- **THEN** it returns a non-null string identifying the cloud region

---

### Requirement: CloudStorageAdapter Interface

The system SHALL provide a `CloudStorageAdapter` interface that defines the contract for cloud object storage operations.

#### Scenario: Interface provides provider identifier
- **GIVEN** a CloudStorageAdapter implementation
- **WHEN** `providerType()` is called
- **THEN** it returns a non-null string identifying the storage provider (e.g., `"oci"`, `"aws"`)

#### Scenario: Interface lists runbooks asynchronously
- **GIVEN** a CloudStorageAdapter implementation
- **WHEN** `listRunbooks(containerName)` is called
- **THEN** it returns a `CompletableFuture<List<String>>` containing object names ending with `.md`

#### Scenario: Interface retrieves runbook content asynchronously
- **GIVEN** a CloudStorageAdapter implementation
- **WHEN** `getRunbookContent(containerName, objectPath)` is called
- **THEN** it returns a `CompletableFuture<Optional<String>>` containing the file content if exists, or empty if not

---

### Requirement: ComputeMetadataAdapter Interface

The system SHALL provide a `ComputeMetadataAdapter` interface that defines the contract for retrieving compute instance metadata.

#### Scenario: Interface provides provider identifier
- **GIVEN** a ComputeMetadataAdapter implementation
- **WHEN** `providerType()` is called
- **THEN** it returns a non-null string identifying the compute provider (e.g., `"oci"`, `"aws"`)

#### Scenario: Interface retrieves instance metadata asynchronously
- **GIVEN** a ComputeMetadataAdapter implementation
- **WHEN** `getInstance(instanceId)` is called
- **THEN** it returns a `CompletableFuture<Optional<ResourceMetadata>>` containing instance details if found, or empty if not

---

### Requirement: AwsConfig Record

The system SHALL provide an `AwsConfig` record implementing `CloudConfig` for AWS connection settings.

#### Scenario: Create config with required fields
- **GIVEN** a region and bucket name
- **WHEN** creating an `AwsConfig` with these values
- **THEN** the config is valid
- **AND** `provider()` returns `"aws"`

#### Scenario: Reject null region
- **WHEN** attempting to create `AwsConfig` with null region
- **THEN** the constructor throws `NullPointerException`

#### Scenario: Create config from environment
- **GIVEN** environment variables `AWS_REGION` and `AWS_BUCKET` are set
- **WHEN** `AwsConfig.fromEnvironment()` is called
- **THEN** the config is created with values from environment variables

---

### Requirement: AwsS3StorageAdapter

The system SHALL provide an `AwsS3StorageAdapter` class implementing `CloudStorageAdapter` for AWS S3 operations.

#### Scenario: List runbook files in S3 bucket
- **GIVEN** an S3 bucket containing markdown files
- **WHEN** `listRunbooks(bucketName)` is called
- **THEN** the adapter returns a list of object keys
- **AND** only includes files ending with `.md` extension

#### Scenario: Read runbook content from S3
- **GIVEN** a valid object key to a markdown file
- **WHEN** `getRunbookContent(bucketName, objectKey)` is called
- **THEN** the adapter returns the file content as a UTF-8 String

#### Scenario: Handle missing S3 object gracefully
- **GIVEN** an object key that does not exist in the bucket
- **WHEN** `getRunbookContent()` is called
- **THEN** the adapter returns `Optional.empty()` (not throw exception)

#### Scenario: Report correct provider type
- **GIVEN** an `AwsS3StorageAdapter` instance
- **WHEN** `providerType()` is called
- **THEN** the adapter returns the string `"aws"`

---

### Requirement: AwsEc2MetadataAdapter

The system SHALL provide an `AwsEc2MetadataAdapter` class implementing `ComputeMetadataAdapter` for AWS EC2 instance metadata.

#### Scenario: Get EC2 instance metadata
- **GIVEN** a valid EC2 instance ID
- **WHEN** `getInstance(instanceId)` is called
- **THEN** the adapter returns a `ResourceMetadata` object
- **AND** the object contains instance ID, display name, instance type, availability zone, and tags

#### Scenario: Handle instance not found
- **GIVEN** an instance ID for a non-existent or terminated instance
- **WHEN** `getInstance()` is called
- **THEN** the adapter returns `Optional.empty()` (not throw exception)

#### Scenario: Report correct provider type
- **GIVEN** an `AwsEc2MetadataAdapter` instance
- **WHEN** `providerType()` is called
- **THEN** the adapter returns the string `"aws"`

---

### Requirement: AwsCloudWatchMetricsAdapter

The system SHALL provide an `AwsCloudWatchMetricsAdapter` class implementing `MetricsSourceAdapter` for AWS CloudWatch metrics.

#### Scenario: Fetch CPU metrics for an EC2 instance
- **GIVEN** a valid EC2 instance ID
- **AND** the adapter is configured with valid AWS credentials
- **WHEN** `fetchMetrics(instanceId, Duration.ofMinutes(15))` is called
- **THEN** the adapter queries CloudWatch for the `CPUUtilization` metric
- **AND** returns a list of `MetricSnapshot` objects within the lookback window

#### Scenario: Handle missing metrics gracefully
- **GIVEN** a valid instance ID with no recent metric data
- **WHEN** `fetchMetrics()` is called
- **THEN** the adapter returns an empty list (not null or exception)

#### Scenario: Report correct source type
- **GIVEN** an `AwsCloudWatchMetricsAdapter` instance
- **WHEN** `sourceType()` is called
- **THEN** the adapter returns the string `"aws-cloudwatch"`

---

### Requirement: AwsCloudWatchLogsAdapter

The system SHALL provide an `AwsCloudWatchLogsAdapter` class implementing `LogSourceAdapter` for AWS CloudWatch Logs.

#### Scenario: Search logs with filter pattern
- **GIVEN** a log group name and filter pattern
- **WHEN** `fetchLogs(resourceId, Duration.ofMinutes(30), "error")` is called
- **THEN** the adapter queries CloudWatch Logs
- **AND** returns a list of `LogEntry` objects matching the filter

#### Scenario: Handle no matching logs
- **GIVEN** a valid log group but no matching log entries
- **WHEN** `fetchLogs()` is called
- **THEN** the adapter returns an empty list

#### Scenario: Report correct source type
- **GIVEN** an `AwsCloudWatchLogsAdapter` instance
- **WHEN** `sourceType()` is called
- **THEN** the adapter returns the string `"aws-cloudwatch-logs"`

---

### Requirement: Cloud Provider Selection

The system SHALL support configuration-driven selection of cloud provider.

#### Scenario: Select OCI provider by default
- **GIVEN** no `cloud.provider` configuration is specified
- **WHEN** the application starts
- **THEN** OCI adapters are instantiated

#### Scenario: Select AWS provider via configuration
- **GIVEN** `cloud.provider` is set to `"aws"` in configuration
- **WHEN** the application starts
- **THEN** AWS adapters are instantiated

#### Scenario: Select OCI provider via configuration
- **GIVEN** `cloud.provider` is set to `"oci"` in configuration
- **WHEN** the application starts
- **THEN** OCI adapters are instantiated

#### Scenario: Reject unknown provider
- **GIVEN** `cloud.provider` is set to an unknown value (e.g., `"azure"`)
- **WHEN** the application starts
- **THEN** startup fails with a clear error message
