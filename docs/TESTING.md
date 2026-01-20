# End-to-End (E2E) Testing Guide

This guide describes how to run end-to-end tests for the Runbook Synthesizer, covering both local validation (using LocalStack) and production validation (using real AWS services).

## Testing Tiers

| Tier            | Scope            | Infrastructure      | Speed        | Command                                       |
| :-------------- | :--------------- | :------------------ | :----------- | :-------------------------------------------- |
| **Unit**        | Class level      | Mocks only          | Fast (<10s)  | `mvn test`                                    |
| **Integration** | Component wiring | WireMock/H2         | Medium (~1m) | `mvn verify`                                  |
| **E2E (Local)** | Full Pipeline    | LocalStack + Ollama | Slow (~5m)   | `mvn verify -Dtest=LocalStackE2EPipelineIT`   |
| **E2E (Cloud)** | Full Pipeline    | Real AWS Services   | Slow (~5m)   | `mvn verify -Pe2e-aws-cloud`                  |

## Local E2E Tests (Recommended)

Local E2E tests validate the entire pipeline using Docker containers to simulate cloud services. This allows for offline testing ensuring functional correctness without incurring cloud costs.

### Prerequisites

1. **Docker Desktop** installed and running
2. **Maven 3.9+**
3. At least **8GB RAM** available for Docker (for Oracle DB + Ollama + LocalStack)

### Services Simulated

- **AWS S3** (via LocalStack)
- **AWS CloudWatch Logs** (via LocalStack)
- **AWS CloudWatch Metrics** (via LocalStack)
- **LLM Inference** (via Ollama container)
- **Vector Store** (via In-Memory implementation)

### Running the Test

```powershell
# Run the specific LocalStack E2E test
mvn verify -Dtest=LocalStackE2EPipelineIT
```

**What happens:**

1. Starts LocalStack container (S3, CloudWatch)
2. Starts Ollama container (pulls models if needed)
3. Seeds sample runbooks into S3 and Vector Store
4. Triggers a high-memory alert simulation
5. Verifies a checklist is generated and written to `output/`

---

## Real AWS E2E Tests

These tests run against your actual AWS account. **Warning: This may incur costs.**

### Prerequisites (Cloud)

1. **AWS Credentials** configured locally (profile or env vars)
2. **CDK Stack Deployed**: The test expects resources to exist.

### Deployment (Resources)

Before running real cloud tests, ensure the infrastructure is provisioned:

```powershell
cd infra
npm ci
npm run cdk:deploy
```

This creates:

- S3 Bucket (`runbook-synthesizer-runbooks-${ACCOUNT_ID}-${REGION}`)
- CloudWatch Log Group (`/runbook-synthesizer/e2e-tests`)
- SNS Topic (for alerts)

### Running the Test (Cloud)

Activate the `e2e-aws-cloud` profile to run these tests:

```powershell
mvn verify -Pe2e-aws-cloud -Dtest=AwsCloudE2EPipelineIT
```

### Cleanup

The test automatically cleans up S3 objects and log streams it creates, but does not delete the Bucket or Log Group itself.

---

## Demo Mode

You can run the full application locally in "real mode" (connected to LocalStack or AWS) and trigger alerts manually to demonstrate the flow.

### 1. Prerequisites (Demo)

- **Docker Desktop** installed and running
- **JDK 25**

### 2. Start the Demo Environment

We provide a utility to spin up the required Docker containers (LocalStack + Ollama) using the same infrastructure as our E2E tests.

Run the following command *in a separate terminal* to start the environment. It will keep running until you press **CTRL+C** or terminate the process.

```powershell
mvn org.apache.maven.plugins:maven-failsafe-plugin:3.5.4:integration-test "-Dit.test=DemoEnvironmentIT" "-Pe2e-containers"
```

Wait until you see **"--- ENVIRONMENT READY ---"** and **">>> ENVIRONMENT RUNNING - PRESS CTRL+C TO SHUT DOWN <<<"**.

> [!NOTE]
> The first run may take several minutes as it automatically pulls the required LLM models (`llama3.2:1b` and `nomic-embed-text`) into the Ollama container.

The output will print the configuration values you need.

### 3. Configure Application

Update `src/main/resources/application.yaml` with the values printed by the runner. It will look something like this:

```yaml
app:
  stub-mode: false

cloud:
  provider: aws
  aws:
    endpoint: http://localhost:4566      # Copy from runner output
    region: us-east-1
    credentials:
      access-key: test                   # Copy from runner output
      secret-key: test                   # Copy from runner output
    
vectorStore:
  provider: local
  
llm:
  provider: ollama
  ollama:
    url: http://localhost:32768          # Copy from runner output
```

### 4. Start the Application

```powershell
mvn exec:java
```

### 5. Run the Demo Script

In a new terminal, use the provided PowerShell script to trigger alerts:

```powershell
# Trigger a High Memory Alert
./scripts/demo-e2e-pipeline.ps1 -AlertType memory -Severity warning

# Trigger a Critical CPU Alert
./scripts/demo-e2e-pipeline.ps1 -AlertType cpu -Severity critical
```

**Output:**

The script will display the alert ID. The application logs will show the RAG process. finally, a JSON checklist file will appear in the `output/` directory.

### 6. Verify Output

Check the generated file in the output folder:

```powershell
Get-Content output/checklist-*.json | jq
```
