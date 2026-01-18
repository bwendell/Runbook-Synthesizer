# rag-pipeline Spec Delta

## MODIFIED Requirements
### Requirement: OracleVectorStoreRepository uses Oracle 23ai

The system SHALL provide an Oracle 23ai implementation of VectorStoreRepository, ensuring compatibility with both cloud-hosted (ADB-S) and containerized (Testcontainers) Oracle 23ai instances.

#### Scenario: Initialize repository with dynamic JDBC URL
- **GIVEN** a dynamically provided JDBC URL and credentials (from Testcontainers or Cloud)
- **WHEN** OracleVectorStoreRepository is initialized
- **THEN** it successfully connects to the instance
- **AND** performs vector operations as expected
