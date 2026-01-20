# Runbook-Synthesizer

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](.)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-25-orange.svg)](.)

An open-source Java tool that transforms static runbooks into intelligent, context-aware troubleshooting guides by leveraging RAG (Retrieval Augmented Generation) with real-time infrastructure state.

## Features

- **Dynamic SOP Generation**: Generate troubleshooting checklists tailored to specific alerts and hosts
- **Context Enrichment**: Enrich runbook content with real-time metrics, logs, and host metadata
- **RAG Pipeline**: Retrieve only relevant procedures from your knowledge base
- **Multi-Channel Output**: Deliver checklists via Slack, PagerDuty, or custom webhooks
- **Multi-Cloud**: AWS is the default provider; OCI is supported as an alternative
- **Pluggable LLM**: Use local Ollama (MVP) or AWS Bedrock (Production)

## Quick Start

### Prerequisites

- Java 25+
- Maven 3.9+

### Build and Run

```bash
# Clone the repository
git clone https://github.com/oracle/runbook-synthesizer.git
cd runbook-synthesizer

# Build the project
mvn clean package

# Run the application
mvn exec:java

# Or run the JAR directly
java --enable-preview -jar target/runbook-synthesizer-1.0.0-SNAPSHOT.jar
```

### Verify

```bash
# Check health endpoint
curl http://localhost:8080/health
# Expected: {"status":"UP"}
```



```powershell
# Run unit tests only
.\mvnw.cmd test --batch-mode

# Run unit + integration tests (no Docker required)
.\mvnw.cmd verify --batch-mode

# Run container E2E tests (requires Docker)
.\mvnw.cmd verify -Dtest.use.containers=true --batch-mode
```

### E2E Testing

For full pipeline validation:

```powershell
# Local E2E (Uses LocalStack + Ollama containers)
.\mvnw.cmd verify -Dtest=LocalStackE2EPipelineIT

# Real AWS E2E (Requires AWS Credentials)
.\mvnw.cmd verify -Pe2e-aws-cloud
```

See [E2E Testing Guidelines](docs/E2E_TESTING_GUIDELINES.md) and [Full Testing Guide](docs/TESTING.md) for detailed documentation.

## Tech Stack

| Component    | Technology                                 |
| :----------- | :----------------------------------------- |
| Framework    | Helidon SE 4.x                             |
| Language     | Java 25                                    |
| Build        | Maven                                      |
| Vector Store | Oracle Database 23ai                       |
| LLM          | Pluggable (Ollama, AWS Bedrock, OCI GenAI) |

## Documentation

- [Architecture](docs/ARCHITECTURE.md) - System design and component overview
- [Design](docs/DESIGN.md) - Detailed technical design document
- [E2E Testing Guidelines](docs/E2E_TESTING_GUIDELINES.md) - Container-based E2E testing with Testcontainers
- [Contributing](CONTRIBUTING.md) - How to contribute

## Cloud Provider Configuration

The application supports both **AWS** (default) and **OCI (Oracle Cloud Infrastructure)** as deployment targets. Select your provider using the `cloud.provider` configuration property.

### AWS Setup

**Prerequisites:**
- AWS Account with IAM permissions for S3, EC2, CloudWatch, and CloudWatch Logs
- AWS credentials configured via one of the supported methods

**Configuration:**

```yaml
# application.yaml
cloud:
  provider: aws
  aws:
    region: us-west-2
    storage:
      bucket: your-runbook-bucket-name
```

**Credential Setup:**

The application uses the [AWS Default Credential Provider Chain](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials-chain.html). Configure credentials using one of these methods:

1. **Environment Variables** (recommended for containers):
   ```bash
   export AWS_ACCESS_KEY_ID=your-access-key
   export AWS_SECRET_ACCESS_KEY=your-secret-key
   export AWS_REGION=us-west-2
   ```

2. **AWS Credentials File** (recommended for local development):
   ```ini
   # ~/.aws/credentials
   [default]
   aws_access_key_id = your-access-key
   aws_secret_access_key = your-secret-key
   ```

3. **IAM Instance Role** (recommended for EC2/ECS/EKS):
   - No explicit configuration needed
   - Instance profile credentials are automatically detected

4. **Web Identity Token (IRSA)** for EKS:
   - Configured automatically via EKS Pod Identity or IRSA

### OCI Setup

**Prerequisites:**
- OCI Account with permissions for Object Storage, Compute, Monitoring, and Logging
- OCI CLI configured or API key setup

**Configuration:**

```yaml
# application.yaml
cloud:
  provider: oci
  oci:
    region: us-ashburn-1
    compartmentId: ocid1.compartment.oc1..example
    storage:
      namespace: your-namespace
      bucket: your-runbook-bucket-name
```

**Credential Setup:**

Configure OCI authentication using one of these methods:

1. **OCI Config File** (recommended for local development):
   ```ini
   # ~/.oci/config
   [DEFAULT]
   user=ocid1.user.oc1..example
   fingerprint=xx:xx:xx:xx...
   tenancy=ocid1.tenancy.oc1..example
   region=us-ashburn-1
   key_file=~/.oci/oci_api_key.pem
   ```

2. **Instance Principal** (recommended for OCI Compute):
   - No explicit configuration needed
   - Instance principal is automatically detected when running on OCI

3. **Resource Principal** (for OCI Functions):
   - Configured via function environment

### Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| `SdkClientException: Unable to load credentials` | AWS credentials not configured | Configure credentials using one of the methods above |
| `BmcException: Could not find config file` | OCI config file missing | Create `~/.oci/config` or use instance principal |
| `NoSuchBucketException` | S3 bucket doesn't exist | Create the bucket or verify bucket name |
| `BucketNotFound` | OCI bucket doesn't exist | Create the bucket in OCI Console |
| `AccessDeniedException` | Insufficient IAM permissions | Grant required permissions to IAM user/role |
| `ConnectionTimeoutException` | Network connectivity issue | Verify network access to cloud provider |

**Logging:**

Enable debug logging for cloud adapter troubleshooting:

```yaml
# application.yaml
logging:
  levels:
    com.oracle.runbook.infrastructure.cloud: DEBUG
```

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
