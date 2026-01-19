# Design: Cloud Provider Abstraction Layer

## Context

The Runbook-Synthesizer application currently uses 5 OCI-specific components:

| Component | Purpose | OCI SDK Usage |
|-----------|---------|---------------|
| `OciObjectStorageClient` | Read runbook markdown files | `ObjectStorageClient` |
| `OciComputeClient` | Fetch instance metadata | `ComputeClient` |
| `OciMonitoringAdapter` | Fetch metrics | `MonitoringClient` |
| `OciLoggingAdapter` | Search logs | `LoggingManagementClient` |
| `OciAuthProviderFactory` | Create auth providers | Multiple auth providers |

### Existing Abstractions

The codebase already follows Hexagonal Architecture with pluggable interfaces:
- `MetricsSourceAdapter` - OCI Monitoring or Prometheus
- `LogSourceAdapter` - OCI Logging or Grafana Loki
- `LlmProvider` - OCI GenAI, OpenAI, or Ollama
- `VectorStoreRepository` - Oracle 23ai (no alternative planned)

### Gap Analysis

Missing abstractions to enable multi-cloud:
1. **Cloud Storage** - `OciObjectStorageClient` is OCI-specific
2. **Compute Metadata** - `OciComputeClient` is OCI-specific
3. **Cloud Configuration** - `OciConfig` is OCI-specific
4. **Cloud Authentication** - `OciAuthProviderFactory` is OCI-specific

## Goals / Non-Goals

### Goals
- Enable first-class support for both AWS and OCI as production-ready deployment targets
- Allow seamless toggling between providers via configuration
- Follow existing Hexagonal Architecture patterns
- Maintain backward compatibility with current OCI deployments
- Support future multi-cloud deployments (Azure, GCP)

### Non-Goals
- Abstract the vector store (Oracle 23ai only for v1.x)
- Add Azure or GCP support in this change
- Change the existing `LlmProvider` abstraction
- Modify webhook output destinations

## Decisions

### 1. Provider Selection via Configuration

**Decision:** Use `cloud.provider` configuration property to select active provider.

**Rationale:**
- Simple, explicit configuration
- Easy to understand and debug
- Follows 12-factor app principles
- Consistent with existing `llm.provider` pattern in the codebase

**Configuration Structure:**
```yaml
cloud:
  provider: aws  # or "oci"
  
  aws:
    region: us-east-1
    storage:
      bucket: runbook-synthesizer-runbooks
    # Uses AWS default credential chain
    
  oci:
    region: us-ashburn-1
    compartmentId: ${OCI_COMPARTMENT_ID}
    storage:
      namespace: ${OCI_NAMESPACE}
      bucket: runbook-synthesizer-runbooks
```

### 2. Interface Design Pattern

**Decision:** Create new adapter interfaces following existing patterns.

**Rationale:**
- Consistent with existing `MetricsSourceAdapter` and `LogSourceAdapter`
- Follows Hexagonal Architecture (ports and adapters)
- Enables easy addition of new providers

**New Interfaces:**
```java
public interface CloudStorageAdapter {
    String providerType();
    CompletableFuture<List<String>> listRunbooks(String containerName);
    CompletableFuture<Optional<String>> getRunbookContent(
        String containerName, String objectPath);
}

public interface ComputeMetadataAdapter {
    String providerType();
    CompletableFuture<Optional<ResourceMetadata>> getInstance(String instanceId);
}

public interface CloudConfig {
    String provider();
    String region();
}

public interface CloudAuthFactory<T> {
    T createAuthProvider(CloudConfig config);
}
```

### 3. AWS SDK Version

**Decision:** Use AWS SDK v2 (software.amazon.awssdk).

**Rationale:**
- Modern, non-blocking API with CompletableFuture support
- Better performance with virtual threads (Helidon SE 4.x)
- Active development and support
- Consistent async API matches existing Helidon patterns

**Dependencies:**
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>bom</artifactId>
            <version>2.29.x</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>s3</artifactId>
    </dependency>
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>ec2</artifactId>
    </dependency>
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>cloudwatch</artifactId>
    </dependency>
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>cloudwatchlogs</artifactId>
    </dependency>
</dependencies>
```

### 4. AWS Authentication Strategy

**Decision:** Use AWS Default Credential Provider Chain.

**Rationale:**
- Standard AWS pattern, well-understood
- Automatically detects environment (IAM roles, env vars, config files)
- No custom credential management needed
- Works in EKS, ECS, EC2, Lambda, and local development

**Chain order:**
1. Environment variables (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`)
2. AWS credentials file (`~/.aws/credentials`)
3. ECS container credentials
4. EC2 instance profile / IAM role
5. Web identity token (EKS IRSA)

### 5. Package Structure

**Decision:** Create `infrastructure/cloud` package with provider subpackages.

**Rationale:**
- Clear separation of cloud-specific code
- Easy to add new providers (Azure, GCP)
- Follows infrastructure layer pattern

**Structure:**
```
com/oracle/runbook/
├── infrastructure/
│   └── cloud/
│       ├── CloudStorageAdapter.java
│       ├── ComputeMetadataAdapter.java
│       ├── CloudConfig.java
│       ├── CloudAuthFactory.java
│       ├── oci/
│       │   ├── OciObjectStorageAdapter.java
│       │   ├── OciComputeMetadataAdapter.java
│       │   ├── OciConfig.java
│       │   └── OciAuthFactory.java
│       └── aws/
│           ├── AwsS3StorageAdapter.java
│           ├── AwsEc2MetadataAdapter.java
│           ├── AwsCloudWatchMetricsAdapter.java
│           ├── AwsCloudWatchLogsAdapter.java
│           ├── AwsConfig.java
│           └── AwsAuthFactory.java
```

### 6. OCI Client Refactoring Strategy

**Decision:** Refactor existing OCI clients to implement new interfaces without breaking changes.

**Approach:**
1. Create new interfaces in `infrastructure/cloud`
2. Rename `OciObjectStorageClient` → `OciObjectStorageAdapter` 
3. Have renamed class implement `CloudStorageAdapter`
4. Rename `OciComputeClient` → `OciComputeMetadataAdapter`
5. Have renamed class implement `ComputeMetadataAdapter`
6. Update injection points to use interfaces

**Rationale:**
- Minimal disruption to existing code
- Maintains backward compatibility
- Allows gradual migration

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| AWS SDK increases JAR size significantly | Use BOM with minimal modules; consider profiles for cloud-specific builds |
| Configuration complexity increases | Document clearly; provide example configs |
| Testing requires both cloud credentials | Use LocalStack for AWS integration tests |
| OCI-specific features may not map to AWS | Document limitations; fail gracefully |

## Testing Strategy

### Unit Tests
- Mock cloud SDK clients
- Test adapter logic in isolation
- Test configuration parsing and validation

### Integration Tests

**AWS (LocalStack):**
```java
@Testcontainers
class AwsS3StorageAdapterIT {
    @Container
    static LocalStackContainer localstack = new LocalStackContainer(...)
        .withServices(LocalStackContainer.Service.S3);
}
```

**OCI:**
- Existing OCI integration tests continue to work
- Use OCI test compartment when credentials available
- Skip when `OCI_COMPARTMENT_ID` not set

### Configuration Tests
- Test provider selection logic
- Test fallback behavior
- Test invalid configuration handling

## Open Questions

1. **Metric namespace mapping:** How do OCI metric namespaces map to CloudWatch namespaces? 
   - *Proposed:* Define mapping table in configuration

2. **Log query translation:** How do OCI Logging queries translate to CloudWatch Insights?
   - *Proposed:* Accept provider-specific query syntax initially

3. **Instance ID format:** OCI uses OCIDs, AWS uses instance IDs - how to handle?
   - *Proposed:* Treat as opaque strings; adapter validates format

## Migration Plan

### Phase 1: Add Abstractions (this change)
1. Add new interfaces
2. Add AWS implementations
3. Refactor OCI implementations
4. Add configuration support
5. Add tests

### Phase 2: Future (not this change)
- Add Azure implementations
- Add GCP implementations
- Add cross-cloud deployment documentation
