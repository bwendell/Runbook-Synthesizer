# oci-adapters Specification

## Purpose
TBD - created by archiving change implement-oci-sdk-integrations. Update Purpose after archive.
## Requirements
### Requirement: OCI Monitoring Adapter

The system SHALL provide an `OciMonitoringAdapter` class that implements the `MetricsSourceAdapter` interface for fetching metrics from OCI Monitoring API.

#### Scenario: Fetch CPU metrics for a compute instance

- **GIVEN** a valid OCI compute instance OCID
- **AND** the adapter is configured with valid OCI credentials
- **WHEN** `fetchMetrics(instanceOcid, Duration.ofMinutes(15))` is called
- **THEN** the adapter queries OCI Monitoring for the `CpuUtilization` metric
- **AND** returns a list of `MetricSnapshot` objects within the lookback window

#### Scenario: Handle missing metrics gracefully

- **GIVEN** a valid OCID for an instance with no recent metric data
- **WHEN** `fetchMetrics()` is called
- **THEN** the adapter returns an empty list (not null or exception)

#### Scenario: Report correct source type

- **GIVEN** an `OciMonitoringAdapter` instance
- **WHEN** `sourceType()` is called
- **THEN** the adapter returns the string `"oci-monitoring"`

---

### Requirement: OCI Logging Adapter

The system SHALL provide an `OciLoggingAdapter` class that implements the `LogSourceAdapter` interface for searching logs from OCI Logging API.

#### Scenario: Search logs for a resource with custom query

- **GIVEN** a valid resource OCID
- **AND** a search query `"error OR exception"`
- **WHEN** `fetchLogs(resourceOcid, Duration.ofMinutes(30), "error OR exception")` is called
- **THEN** the adapter queries OCI Logging Search API
- **AND** returns a list of `LogEntry` objects matching the criteria

#### Scenario: Handle no matching logs

- **GIVEN** a valid OCID but no matching log entries
- **WHEN** `fetchLogs()` is called
- **THEN** the adapter returns an empty list

#### Scenario: Report correct source type

- **GIVEN** an `OciLoggingAdapter` instance
- **WHEN** `sourceType()` is called
- **THEN** the adapter returns the string `"oci-logging"`

---

### Requirement: OCI Compute Client

The system SHALL provide an `OciComputeClient` class for retrieving compute instance metadata.

#### Scenario: Get instance metadata

- **GIVEN** a valid compute instance OCID
- **WHEN** `getInstance(instanceOcid)` is called
- **THEN** the client returns a `ResourceMetadata` object
- **AND** the object contains `ocid`, `displayName`, `shape`, `availabilityDomain`, tags

#### Scenario: Handle instance not found

- **GIVEN** an OCID for a non-existent or terminated instance
- **WHEN** `getInstance()` is called
- **THEN** the client returns `Optional.empty()` (not throw exception)

---

### Requirement: OCI Object Storage Client

The system SHALL provide an `OciObjectStorageClient` class for reading runbook files from OCI Object Storage.

#### Scenario: List runbook files in bucket

- **GIVEN** a namespace and bucket name containing markdown files
- **WHEN** `listRunbooks(namespace, bucketName)` is called
- **THEN** the client returns a list of object names
- **AND** only includes files ending with `.md` extension

#### Scenario: Read runbook content

- **GIVEN** a valid object path to a markdown file
- **WHEN** `getRunbookContent(namespace, bucket, objectName)` is called
- **THEN** the client returns the file content as a UTF-8 String

#### Scenario: Handle missing runbook file

- **GIVEN** an object name that does not exist in the bucket
- **WHEN** `getRunbookContent()` is called
- **THEN** the client returns `Optional.empty()`

---

### Requirement: OCI Configuration Model

The system SHALL provide an `OciConfig` record for OCI connection settings.

#### Scenario: Create config with required fields

- **GIVEN** a compartment ID string
- **WHEN** creating an `OciConfig` with only compartmentId
- **THEN** the config is valid
- **AND** uses default values for optional fields

#### Scenario: Reject null compartment ID

- **WHEN** attempting to create `OciConfig` with null compartmentId
- **THEN** the constructor throws `NullPointerException`

---

### Requirement: OCI Authentication Factory

The system SHALL provide an `OciAuthProviderFactory` for creating OCI SDK authentication providers, supporting multiple authentication methods with automatic prioritized detection.

#### Scenario: Create auth provider from environment variables
- **GIVEN** environment variables `OCI_USER_ID`, `OCI_TENANCY_ID`, `OCI_FINGERPRINT`, `OCI_REGION` and `OCI_PRIVATE_KEY_CONTENT` are set
- **WHEN** `create(ociConfig)` is called
- **THEN** the factory returns a `SimpleAuthenticationDetailsProvider` initialized with these values
- **AND** this takes priority over config file if config file is missing

#### Scenario: Create auth provider using Instance Principals
- **GIVEN** the application is running on an OCI compute instance
- **AND** no config file or environment variables are provided
- **WHEN** `create(ociConfig)` is called
- **THEN** the factory returns an `InstancePrincipalsAuthenticationDetailsProvider`

#### Scenario: Create auth provider using Resource Principals
- **GIVEN** environment variable `OCI_RESOURCE_PRINCIPAL_VERSION` is set
- **WHEN** `create(ociConfig)` is called
- **THEN** the factory returns a `ResourcePrincipalsAuthenticationDetailsProvider`

