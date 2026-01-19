# Change: Add Cloud Provider Abstraction Layer

## Why

The application is currently tightly coupled to Oracle Cloud Infrastructure (OCI) services:
- Object Storage for runbook storage
- Compute API for instance metadata
- Monitoring API for metrics
- Logging API for log retrieval
- OCI-specific authentication and configuration

This coupling limits deployment options to OCI-only environments. Multi-cloud support is on the v2.0 roadmap, and abstracting cloud-specific components now enables:
1. **First-class AWS support** - Full production-ready AWS deployment capability
2. **Multi-cloud deployment** - AWS, OCI, Azure, or hybrid deployments
3. **Vendor flexibility** - Avoid lock-in to a single cloud provider

## What Changes

### New Abstractions (interfaces)

- **`CloudStorageAdapter`** - Abstract cloud object storage operations (list/read runbooks)
- **`ComputeMetadataAdapter`** - Abstract compute instance metadata retrieval
- **`CloudAuthFactory`** - Abstract cloud authentication provider creation
- **`CloudConfig`** - Abstract base interface for cloud provider configuration

### New Implementations

- **`AwsS3StorageAdapter`** - AWS S3 implementation of `CloudStorageAdapter`
- **`AwsEc2MetadataAdapter`** - AWS EC2 implementation of `ComputeMetadataAdapter`
- **`AwsCloudWatchMetricsAdapter`** - AWS CloudWatch implementation of `MetricsSourceAdapter`
- **`AwsCloudWatchLogsAdapter`** - AWS CloudWatch Logs implementation of `LogSourceAdapter`
- **`AwsConfig`** - AWS-specific configuration record
- **`AwsAuthFactory`** - AWS credential provider factory

### Refactored Components

- **`OciObjectStorageClient`** → `OciObjectStorageAdapter` (implement `CloudStorageAdapter`)
- **`OciComputeClient`** → `OciComputeMetadataAdapter` (implement `ComputeMetadataAdapter`)
- **`OciConfig`** → Implement `CloudConfig` interface
- **`OciAuthProviderFactory`** → Implement `CloudAuthFactory` interface

### Configuration Changes

- Add `cloud.provider` toggle (`aws` | `oci`)
- Add AWS-specific configuration section in `application.yaml`
- Add provider selection logic in application startup

### New Dependencies

- AWS SDK v2 for S3, EC2, CloudWatch, CloudWatch Logs

## Impact

### Affected Specs
- **`ports-interfaces`** - Add new interfaces (`CloudStorageAdapter`, `ComputeMetadataAdapter`, `CloudAuthFactory`, `CloudConfig`)
- **`oci-adapters`** - Modify existing OCI implementations to implement new interfaces
- **NEW `cloud-abstraction`** - New spec for multi-cloud architecture

### Affected Code
- `com.oracle.runbook.config` - Add `AwsConfig`, modify `OciConfig`, add provider factory
- `com.oracle.runbook.enrichment` - Add AWS adapters, refactor OCI clients
- `com.oracle.runbook.rag` - Refactor `OciObjectStorageClient`
- `pom.xml` - Add AWS SDK dependencies
- `application.yaml` - Add cloud provider configuration

### Affected Tests
- New unit tests for AWS adapter implementations
- New integration tests with LocalStack for AWS services
- Existing OCI tests continue to work

## Non-Goals

- **Vector Store abstraction** - Oracle 23ai Vector Store remains the only option for v1.x
- **LLM provider abstraction** - Already exists via `LlmProvider` interface
- **Full AWS feature parity** - MVP focuses on core operations; advanced AWS features can be added incrementally
- **Azure support** - Not in scope for this change; can be added later using same pattern
