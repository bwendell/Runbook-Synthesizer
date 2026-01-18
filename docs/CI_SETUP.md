# CI/CD Setup Guide

This document describes how to configure CI/CD pipelines to run integration and E2E tests for Runbook-Synthesizer.

## Required Environment Variables

The following environment variables must be set for OCI-based integration tests:

| Variable | Description | Format | Required |
|----------|-------------|--------|----------|
| `OCI_USER_ID` | OCI User OCID | `ocid1.user.oc1..xxxx` | Yes |
| `OCI_TENANCY_ID` | OCI Tenancy OCID | `ocid1.tenancy.oc1..xxxx` | Yes |
| `OCI_FINGERPRINT` | API Key Fingerprint | `aa:bb:cc:dd:...` | Yes |
| `OCI_REGION` | OCI Region | `us-ashburn-1` | Yes |
| `OCI_COMPARTMENT_ID` | Compartment OCID | `ocid1.compartment.oc1..xxxx` | Yes |
| `OCI_PRIVATE_KEY_CONTENT` | PEM Key Content | Base64 or raw | One of these |
| `OCI_PRIVATE_KEY_FILE` | Path to PEM Key | `/path/to/key.pem` | required |

## Setting Up OCI API Keys

1. Generate an API key pair:
   ```bash
   openssl genrsa -out ~/.oci/oci_api_key.pem 2048
   openssl rsa -pubout -in ~/.oci/oci_api_key.pem -out ~/.oci/oci_api_key_public.pem
   chmod 600 ~/.oci/oci_api_key.pem
   ```

2. Upload the public key to OCI Console:
   - Profile → API Keys → Add API Key → Paste Public Key

3. Note the fingerprint shown after upload.

See [OCI API Key Documentation](https://docs.oracle.com/en-us/iaas/Content/API/Concepts/apisigningkey.htm) for details.

## GitHub Actions Example

```yaml
name: Integration Tests
on: [push, pull_request]

jobs:
  integration-tests:
    runs-on: ubuntu-latest
    services:
      docker:
        image: docker:dind
        options: --privileged
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 25
        uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'temurin'
      
      - name: Run Tests with OCI Credentials
        env:
          OCI_USER_ID: ${{ secrets.OCI_USER_ID }}
          OCI_TENANCY_ID: ${{ secrets.OCI_TENANCY_ID }}
          OCI_FINGERPRINT: ${{ secrets.OCI_FINGERPRINT }}
          OCI_REGION: ${{ secrets.OCI_REGION }}
          OCI_COMPARTMENT_ID: ${{ secrets.OCI_COMPARTMENT_ID }}
          OCI_PRIVATE_KEY_CONTENT: ${{ secrets.OCI_PRIVATE_KEY_CONTENT }}
        run: mvn verify -Pci-integration
```

## OCI DevOps Example

For OCI-native CI using Instance Principals:

```yaml
# build_spec.yaml
version: 0.1
component: build
steps:
  - type: Command
    name: "Run Integration Tests"
    command: |
      mvn verify -Pci-integration
```

No environment variables needed - authentication uses Instance Principals automatically.

## Test Profiles

| Profile | Command | Description |
|---------|---------|-------------|
| Default | `mvn test` | Unit tests only (no Docker required) |
| Integration | `mvn verify` | Unit + container-based integration tests |
| CI Integration | `mvn verify -Pci-integration` | Cloud-auth tests (skips container tests) |

## Docker Requirements

For local container tests (Testcontainers):
- Docker Desktop or Docker Engine running
- At least 4GB memory allocated to Docker
- Oracle 23ai container (~2GB disk space)

## Troubleshooting

### OCI Authentication Errors
- Verify fingerprint matches uploaded key
- Check key permissions (600)
- Confirm user has required IAM policies

### Container Startup Failures
- Check Docker is running: `docker info`
- Verify sufficient memory: `docker stats`
- Check for port conflicts
