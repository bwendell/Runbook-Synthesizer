# oci-adapters Spec Delta

## MODIFIED Requirements
### Requirement: OCI Authentication Factory

The system SHALL provide an `OciAuthProviderFactory` for creating OCI SDK authentication providers, supporting multiple authentication methods with automatic prioritized detection.

#### Scenario: Create auth provider from environment variables
- **GIVEN** environment variables `OCI_USER_ID`, `OCI_TENANCY_ID`, `OCI_FINGERPRINT`, `OCI_REGION` and `OCI_PRIVATE_KEY_CONTENT` are set
- **WHEN** `create(ociConfig)` is called
- **THEN** the factory returns a `SimpleAuthenticationDetailsProvider` initialized with these values
- **AND** this takes priority over config file if config file is missing

#### Scenario: Create auth provider using Instance Principals
- **GIVEN** the application is running on an OCI compute instance
- **AND** no config file or environment variables are provided
- **WHEN** `create(ociConfig)` is called
- **THEN** the factory returns an `InstancePrincipalsAuthenticationDetailsProvider`

#### Scenario: Create auth provider using Resource Principals
- **GIVEN** environment variable `OCI_RESOURCE_PRINCIPAL_VERSION` is set
- **WHEN** `create(ociConfig)` is called
- **THEN** the factory returns a `ResourcePrincipalsAuthenticationDetailsProvider`
