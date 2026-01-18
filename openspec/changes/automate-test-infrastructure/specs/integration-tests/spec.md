# integration-tests Spec Delta

## ADDED Requirements
### Requirement: Automated Infrastructure Provisioning

The system SHALL support automated provisioning of real infrastructure for integration tests without manual setup.

#### Scenario: Oracle 23ai Testcontainers initialization
- **GIVEN** the `integration-test` Maven profile is active
- **AND** Docker is available in the environment
- **WHEN** integration tests start
- **THEN** an Oracle 23ai Free container is automatically pulled and started
- **AND** JDBC connection properties are dynamically injected into the application context
- **AND** vector store schema is initialized before tests execute

#### Scenario: OCI Auth integration in tests
- **GIVEN** CI environment variables or a local OCI config
- **WHEN** integration tests execute OCI adapter calls
- **THEN** the system automatically authenticates using the available method
- **AND** tests proceed without requiring developer interaction
