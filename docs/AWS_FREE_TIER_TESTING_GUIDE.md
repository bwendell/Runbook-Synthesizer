# AWS Free Tier Testing Guide

This guide walks you through manually testing the Runbook-Synthesizer application end-to-end on AWS while minimizing costs. The approach uses the **AWS Free Tier** exclusively—no paid services required.

> [!TIP]
> **Cheapest Strategy**: Run the application **locally** while connecting to AWS services (S3, CloudWatch). This avoids EC2 instance costs completely and uses only free-tier-eligible services.

## Cost Summary

| Service | Free Tier Allowance | Estimated Usage | Cost |
|---------|---------------------|-----------------|------|
| **S3** | 5GB storage, 20K GET, 2K PUT | < 100 objects | **$0** |
| **CloudWatch Logs** | 5GB ingestion/month | < 10MB | **$0** |
| **CloudWatch Metrics** | 10 custom metrics | 0 custom metrics | **$0** |
| **IAM** | Always free | ∞ | **$0** |
| **Total** | — | — | **$0** |

---

## Prerequisites

- AWS Account (Free Tier eligible)
- AWS CLI v2 installed
- Java 25+ installed locally
- Maven 3.9+ installed locally
- Git (to clone the repo)

---

## Step 1: Install and Configure AWS CLI

### 1.1 Install AWS CLI

```powershell
# Download and run the installer (Windows)
msiexec.exe /i https://awscli.amazonaws.com/AWSCLIV2.msi

# Verify installation
aws --version
# Expected: aws-cli/2.x.x ...
```

### 1.2 Create an IAM User

1. Go to **IAM Console**: https://console.aws.amazon.com/iam/
2. Click **Users** → **Create user**
3. User name: `runbook-synthesizer-dev`
4. Click **Next**
5. Select **Attach policies directly**
6. Search and attach these policies:
   - `AmazonS3FullAccess`
   - `CloudWatchLogsFullAccess`
   - `CloudWatchReadOnlyAccess`
7. Click **Next** → **Create user**

### 1.3 Create Access Keys

1. Click on the newly created user
2. Go to **Security credentials** tab
3. Click **Create access key**
4. Select **Command Line Interface (CLI)**
5. Check the acknowledgment box
6. Click **Next** → **Create access key**
7. **Save the Access Key ID and Secret Access Key** (you won't see the secret again!)

### 1.4 Configure AWS CLI

```powershell
aws configure
# AWS Access Key ID: <paste-access-key-id>
# AWS Secret Access Key: <paste-secret-access-key>
# Default region name: us-east-1
# Default output format: json
```

Verify configuration:

```powershell
aws sts get-caller-identity
# Should return your account info
```

---

## Step 2: Create S3 Bucket for Runbooks

### 2.1 Create the Bucket

```powershell
# Use a globally unique bucket name
aws s3 mb s3://runbook-synthesizer-<your-account-id>-test --region us-east-1

# Example:
aws s3 mb s3://runbook-synthesizer-123456789012-test --region us-east-1
```

### 2.2 Upload Sample Runbooks

```powershell
# Navigate to project root
cd c:\Users\bwend\repos\ops-scribe

# Upload example runbooks
aws s3 cp examples/runbooks/ s3://runbook-synthesizer-<your-account-id>-test/runbooks/ --recursive

# Verify upload
aws s3 ls s3://runbook-synthesizer-<your-account-id>-test/runbooks/
```

---

## Step 3: Create CloudWatch Log Group

```powershell
aws logs create-log-group --log-group-name /runbook-synthesizer/test --region us-east-1

# Create a log stream
aws logs create-log-stream \
  --log-group-name /runbook-synthesizer/test \
  --log-stream-name application \
  --region us-east-1
```

---

## Step 4: Configure the Application

### 4.1 Create AWS Configuration

Create or update `src/main/resources/application-aws.yaml`:

```yaml
cloud:
  provider: aws
  aws:
    region: us-east-1
    storage:
      bucket: runbook-synthesizer-<your-account-id>-test

# Use stub/mock LLM for testing (avoids LLM costs)
rag:
  llm:
    provider: stub
  embeddings:
    provider: stub

# Application server
server:
  port: 8080
  host: 0.0.0.0
```

### 4.2 Set Environment Variables

```powershell
# Set AWS credentials (if not using aws configure)
$env:AWS_REGION = "us-east-1"
$env:AWS_ACCESS_KEY_ID = "<your-access-key>"
$env:AWS_SECRET_ACCESS_KEY = "<your-secret-key>"

# Activate AWS profile
$env:SPRING_PROFILES_ACTIVE = "aws"
```

---

## Step 5: Build and Run Locally

### 5.1 Build the Application

```powershell
cd c:\Users\bwend\repos\ops-scribe

# Clean build
.\mvnw.cmd clean package -DskipTests
```

### 5.2 Run the Application

```powershell
# Run with AWS configuration
java --enable-preview -jar target\runbook-synthesizer-1.0.0-SNAPSHOT.jar
```

### 5.3 Verify Health

```powershell
# In a new terminal
curl http://localhost:8080/health
# Expected: {"status":"UP"}
```

---

## Step 6: End-to-End Test Scenarios

### 6.1 Test S3 Storage Integration

```powershell
# List runbooks from S3 (via API if available)
curl http://localhost:8080/api/runbooks

# Or test S3 directly
aws s3 ls s3://runbook-synthesizer-<your-account-id>-test/runbooks/
```

### 6.2 Test Alert Webhook Ingestion

```powershell
# Send a test alert webhook
curl -X POST http://localhost:8080/api/alerts `
  -H "Content-Type: application/json" `
  -d '{
    "alertName": "HighCPUUsage",
    "severity": "critical",
    "host": "web-server-01",
    "timestamp": "2026-01-18T20:00:00Z",
    "description": "CPU usage exceeded 95%"
  }'
```

### 6.3 Test CloudWatch Logs Integration

```powershell
# Write a test log event
aws logs put-log-events `
  --log-group-name /runbook-synthesizer/test `
  --log-stream-name application `
  --log-events "timestamp=$(([DateTimeOffset]::Now.ToUnixTimeMilliseconds())),message='Test log event'" `
  --region us-east-1

# Read logs
aws logs get-log-events `
  --log-group-name /runbook-synthesizer/test `
  --log-stream-name application `
  --region us-east-1
```

### 6.4 Test RAG Pipeline (with Stub LLM)

```powershell
# Generate a dynamic checklist (uses stub LLM)
curl -X POST http://localhost:8080/api/checklists/generate `
  -H "Content-Type: application/json" `
  -d '{
    "alertName": "HighCPUUsage",
    "host": "web-server-01"
  }'
```

---

## Step 7: Cleanup (Important!)

> [!CAUTION]
> **Always clean up after testing** to avoid unexpected charges. Even free-tier resources can incur costs if left running beyond limits.

### 7.1 Delete S3 Bucket Contents and Bucket

```powershell
# Delete all objects first
aws s3 rm s3://runbook-synthesizer-<your-account-id>-test --recursive

# Delete the bucket
aws s3 rb s3://runbook-synthesizer-<your-account-id>-test
```

### 7.2 Delete CloudWatch Log Group

```powershell
aws logs delete-log-group --log-group-name /runbook-synthesizer/test --region us-east-1
```

### 7.3 Delete IAM User (Optional)

1. Go to **IAM Console** → **Users**
2. Select `runbook-synthesizer-dev`
3. Delete access keys first
4. Then delete the user

---

## Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| `AccessDenied` on S3 | Missing IAM permissions | Verify `AmazonS3FullAccess` is attached |
| `InvalidBucketName` | Bucket name not globally unique | Add your account ID to bucket name |
| `ExpiredToken` | Session expired | Run `aws configure` again |
| `Connection refused` on localhost | App not running | Check Java process is running |
| `ClassNotFoundException` | Build issue | Run `mvnw.cmd clean package` |

---

## Alternative: EC2 Deployment (If Needed)

If you specifically need to test on EC2, here's the **cheapest approach**:

> [!WARNING]
> EC2 free tier is **750 hours/month**. A `t2.micro` left running 24/7 uses 720 hours. **Monitor carefully!**

### Quick EC2 Setup

1. Launch a `t2.micro` instance (free tier):
   - AMI: Amazon Linux 2023
   - Instance type: `t2.micro`
   - Enable "Auto-assign public IP"
   
2. SSH and install Java:
   ```bash
   sudo yum install java-21-amazon-corretto-devel -y
   ```

3. Upload and run the JAR:
   ```bash
   scp target/runbook-synthesizer-1.0.0-SNAPSHOT.jar ec2-user@<ip>:~
   java --enable-preview -jar runbook-synthesizer-1.0.0-SNAPSHOT.jar
   ```

4. **STOP the instance immediately after testing** (or terminate it)

---

## Cost Monitoring

Set up a billing alarm to avoid surprises:

```powershell
# Create a billing alarm for $1 threshold
aws cloudwatch put-metric-alarm `
  --alarm-name "BillingAlarm-1Dollar" `
  --alarm-description "Alert when billing exceeds $1" `
  --metric-name EstimatedCharges `
  --namespace AWS/Billing `
  --statistic Maximum `
  --period 21600 `
  --threshold 1 `
  --comparison-operator GreaterThanThreshold `
  --dimensions Name=Currency,Value=USD `
  --evaluation-periods 1 `
  --alarm-actions arn:aws:sns:us-east-1:<account-id>:billing-alerts `
  --region us-east-1
```

Or set this up in the **AWS Console** → **Billing** → **Billing Preferences** → **Alert preferences**.

---

## Summary

This guide tested the Runbook-Synthesizer application using:

| Component | Testing Method | Cost |
|-----------|----------------|------|
| **Application Runtime** | Local Java process | $0 |
| **Runbook Storage** | AWS S3 (free tier) | $0 |
| **Log Integration** | CloudWatch Logs (free tier) | $0 |
| **LLM/Embeddings** | Stub providers (no cloud LLM) | $0 |

**Total estimated cost: $0**

For production testing with real LLMs, consider:
- **AWS Bedrock** (pay-per-token)
- **OpenAI API** (pay-per-token)
- **Ollama** (self-hosted, free)
