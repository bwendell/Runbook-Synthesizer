# ports-interfaces Specification Delta

## ADDED Requirements

### Requirement: CloudStorageAdapter Interface

The system SHALL define a `CloudStorageAdapter` interface that allows pluggable cloud storage implementations (OCI Object Storage, AWS S3).

#### Scenario: Interface provides provider identifier
- **GIVEN** a CloudStorageAdapter implementation
- **WHEN** `providerType()` is called
- **THEN** it returns a non-null string identifier (e.g., `"oci"`, `"aws"`)

#### Scenario: Interface lists runbooks asynchronously
- **GIVEN** a CloudStorageAdapter implementation
- **WHEN** `listRunbooks(containerName)` is called
- **THEN** it returns a `CompletableFuture<List<String>>` containing object paths

#### Scenario: Interface fetches runbook content asynchronously
- **GIVEN** a CloudStorageAdapter implementation
- **WHEN** `getRunbookContent(containerName, objectPath)` is called
- **THEN** it returns a `CompletableFuture<Optional<String>>` containing file content if exists

---

### Requirement: ComputeMetadataAdapter Interface

The system SHALL define a `ComputeMetadataAdapter` interface that allows pluggable compute metadata implementations (OCI Compute, AWS EC2).

#### Scenario: Interface provides provider identifier
- **GIVEN** a ComputeMetadataAdapter implementation
- **WHEN** `providerType()` is called
- **THEN** it returns a non-null string identifier (e.g., `"oci"`, `"aws"`)

#### Scenario: Interface fetches instance metadata asynchronously
- **GIVEN** a ComputeMetadataAdapter implementation
- **WHEN** `getInstance(instanceId)` is called
- **THEN** it returns a `CompletableFuture<Optional<ResourceMetadata>>`

---

### Requirement: CloudConfig Interface

The system SHALL define a `CloudConfig` interface that allows pluggable cloud configuration implementations.

#### Scenario: Interface provides cloud provider identifier
- **GIVEN** a CloudConfig implementation
- **WHEN** `provider()` is called
- **THEN** it returns a non-null string identifier (e.g., `"oci"`, `"aws"`)

#### Scenario: Interface provides region
- **GIVEN** a CloudConfig implementation
- **WHEN** `region()` is called
- **THEN** it returns a non-null string identifying the cloud region
