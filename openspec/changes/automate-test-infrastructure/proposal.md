# Proposal: Automate Test Infrastructure

## Why
Currently, integration tests rely on in-memory mocks (SQLite, TestLlmProvider) or require a manual OCI config file setup. To ensure production readiness and support seamless CI/CD, we need to automate the provisioning and authentication of real infrastructure (OCI Services and Oracle 23ai Database) for integration and E2E tests.

## What Changes
- **OCI Auth Automation**: Extend `OciAuthProviderFactory` to support multiple auth methods (API Keys, Instance Principals, Resource Principals) based on environment context.
- **Oracle 23ai Automation**: Integrate Testcontainers for Oracle 23ai Free to provide a real vector database for local and CI testing.
- **CI Secret Management Docs**: Define how OCI credentials should be injected into CI pipelines.
- **Test Profiles**: Add Maven profiles to toggle between "Mock", "Local Container", and "Cloud Integration" test modes.

## Impact
- **Affected Specs**: `integration-tests`, `oci-adapters`, `rag-pipeline`
- **Affected Code**: `pom.xml`, `OciAuthProviderFactory.java`, new base classes for container tests.
- **Environment**: Docker required for local container tests.

## Dependencies
- Docker/Testcontainers
- OCI SDK for Java
- Oracle JDBC Driver (ojdbc11 for 23ai support)
