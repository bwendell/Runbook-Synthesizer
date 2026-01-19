# oci-adapters Specification Delta

## MODIFIED Requirements

### Requirement: OCI Object Storage Adapter

The system SHALL provide an `OciObjectStorageAdapter` class that implements the `CloudStorageAdapter` interface for reading runbook files from OCI Object Storage.

#### Scenario: List runbook files in bucket
- **GIVEN** a namespace and bucket name containing markdown files
- **WHEN** `listRunbooks(bucketName)` is called (with namespace from configuration)
- **THEN** the client returns a list of object names
- **AND** only includes files ending with `.md` extension

#### Scenario: Read runbook content
- **GIVEN** a valid object path to a markdown file
- **WHEN** `getRunbookContent(bucketName, objectName)` is called
- **THEN** the client returns the file content as a UTF-8 String wrapped in `Optional`

#### Scenario: Handle missing runbook file
- **GIVEN** an object name that does not exist in the bucket
- **WHEN** `getRunbookContent()` is called
- **THEN** the adapter returns `Optional.empty()`

#### Scenario: Report correct provider type
- **GIVEN** an `OciObjectStorageAdapter` instance
- **WHEN** `providerType()` is called
- **THEN** the adapter returns the string `"oci"`

---

### Requirement: OCI Compute Metadata Adapter

The system SHALL provide an `OciComputeMetadataAdapter` class that implements the `ComputeMetadataAdapter` interface for retrieving compute instance metadata.

#### Scenario: Get instance metadata
- **GIVEN** a valid compute instance OCID
- **WHEN** `getInstance(instanceOcid)` is called
- **THEN** the adapter returns a `ResourceMetadata` object
- **AND** the object contains `ocid`, `displayName`, `shape`, `availabilityDomain`, tags

#### Scenario: Handle instance not found
- **GIVEN** an OCID for a non-existent or terminated instance
- **WHEN** `getInstance()` is called
- **THEN** the adapter returns `Optional.empty()` (not throw exception)

#### Scenario: Report correct provider type
- **GIVEN** an `OciComputeMetadataAdapter` instance
- **WHEN** `providerType()` is called
- **THEN** the adapter returns the string `"oci"`

---

### Requirement: OCI Configuration Model

The system SHALL provide an `OciConfig` record implementing `CloudConfig` for OCI connection settings.

#### Scenario: Create config with required fields
- **GIVEN** a compartment ID string
- **WHEN** creating an `OciConfig` with only compartmentId
- **THEN** the config is valid
- **AND** uses default values for optional fields
- **AND** `provider()` returns `"oci"`

#### Scenario: Reject null compartment ID
- **WHEN** attempting to create `OciConfig` with null compartmentId
- **THEN** the constructor throws `NullPointerException`

#### Scenario: Implements CloudConfig interface
- **GIVEN** an `OciConfig` instance
- **WHEN** `provider()` is called
- **THEN** it returns `"oci"`
- **AND** when `region()` is called it returns the configured OCI region
