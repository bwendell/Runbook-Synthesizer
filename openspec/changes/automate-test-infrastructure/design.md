# Design: Automate Test Infrastructure

## OCI Authentication Automation
The system must automatically resolve OCI credentials in different environments:

1. **Local Developer**: Use `~/.oci/config` (default profile) or environment variables if file is missing.
2. **Standard CI (e.g. GitHub Actions)**: Inject API key details via environment variables (OCI_USER, OCI_KEY, etc.).
3. **OCI-Native CI (DevOps/Build Service)**: Use Instance Principals.
4. **OKE/Runtime**: Use Resource Principals.

**Implementation Detail**: `OciAuthProviderFactory` will use a "Chain of Responsibility" or prioritized detection logic to return the most appropriate `AuthenticationDetailsProvider`.

## Oracle 23ai Provisioning
We will use Testcontainers to spin up an Oracle 23ai Free instance for vector store tests.

- **Image**: `gvenzl/oracle-free:23-slim`
- **JDBC**: Use `JdbcDatabaseContainer` with standard Testcontainers lifecycle.
- **Schema Management**: Use SQL scripts or Liquibase to create the vector-enabled tables during container startup.
- **Cleanup**: Containers will be torn down automatically after test execution.

## Configuration Matrix
| Mode | OCI Auth | Oracle DB | Use Case |
|------|----------|-----------|----------|
| **Mock** | WireMock | InMemory | Fast local dev, unit tests |
| **Hybrid** | Real (API Key) | Testcontainers | Deep integration testing |
| **Full** | Real (Principal) | ADB-S (Cloud) | E2E, Pre-prod validation |
