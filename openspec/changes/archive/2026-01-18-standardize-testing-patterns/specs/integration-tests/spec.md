## MODIFIED Requirements

### Requirement: Integration Test Infrastructure

The system SHALL provide integration test infrastructure using Helidon Test framework with WireMock for external service mocking. All tests SHALL use AssertJ fluent assertions for readable and maintainable test code.

#### Scenario: Test base class setup
- **GIVEN** a test class extending `IntegrationTestBase`
- **WHEN** the test executes
- **THEN** Helidon web server starts with correct configuration
- **AND** WireMock server is available for HTTP mocking

#### Scenario: Test fixture loading
- **GIVEN** JSON fixtures in `src/test/resources/fixtures/`
- **WHEN** `TestFixtures.loadAs(path, Class)` is called
- **THEN** the fixture is deserialized to the specified type

#### Scenario: AssertJ fluent assertions
- **GIVEN** any test class in the test suite
- **WHEN** assertions are made
- **THEN** AssertJ `assertThat()` is used instead of JUnit `assertEquals()`/`assertTrue()`
- **AND** exception testing uses `assertThatThrownBy()` for fluent chaining

---

### Requirement: Test Configuration and CI Integration

The system SHALL provide Maven configuration for running integration tests in CI/CD pipelines. Tests SHALL follow behavior-driven testing patterns without testing implementation details.

#### Scenario: Test categorization
- **GIVEN** `*Test.java` files for unit tests
- **AND** `*IT.java` files for integration tests
- **WHEN** `mvn verify` executed
- **THEN** unit tests run via Surefire plugin
- **AND** integration tests run via Failsafe plugin

#### Scenario: Integration test execution
- **GIVEN** Failsafe plugin configured
- **WHEN** `mvn failsafe:integration-test` executed
- **THEN** only `*IT.java` tests run
- **AND** WireMock server lifecycle managed correctly

#### Scenario: No reflection-based testing
- **GIVEN** any test file in the test suite
- **WHEN** testing component behavior
- **THEN** tests invoke methods directly and verify outputs
- **AND** tests do NOT use reflection to verify method signatures or return types

#### Scenario: Outcome-based mock assertions
- **GIVEN** a test using Mockito mocks for dependencies
- **WHEN** verifying correct behavior
- **THEN** tests assert on actual return values and outcomes
- **AND** tests prefer outcome assertions over `verify()` calls on mock methods

---

## ADDED Requirements

### Requirement: Test Fixture Standards

The system SHALL provide standardized test fixtures for reusable test data. Tests SHOULD load test data from fixtures rather than constructing inline data.

#### Scenario: Test fixture structure
- **GIVEN** the `src/test/resources/fixtures/` directory
- **WHEN** test data is needed for alerts, contexts, checklists, or webhooks
- **THEN** JSON fixture files exist under appropriate subdirectories
- **AND** fixtures are loadable via `TestFixtures.loadAs()` or `TestFixtures.loadJson()`

#### Scenario: Alert fixtures available
- **GIVEN** a test requiring `Alert` domain objects
- **WHEN** loading test data
- **THEN** `fixtures/alerts/*.json` contains valid canonical alert payloads
- **AND** fixtures include both OCI-specific and generic alert formats

#### Scenario: Context fixtures available
- **GIVEN** a test requiring `EnrichedContext` domain objects
- **WHEN** loading test data
- **THEN** `fixtures/contexts/*.json` contains complete context payloads
- **AND** fixtures include resource metadata, metrics, and logs samples

#### Scenario: Checklist fixtures available
- **GIVEN** a test requiring `DynamicChecklist` domain objects
- **WHEN** loading test data
- **THEN** `fixtures/checklists/*.json` contains sample checklist payloads
- **AND** fixtures include steps with all priority levels
