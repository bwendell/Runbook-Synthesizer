## ADDED Requirements

### Requirement: AWS CDK Infrastructure Provisioning

The system SHALL provide AWS CDK (TypeScript) infrastructure-as-code for provisioning real AWS e2e test resources. The CDK stack SHALL be idempotent and support manual cleanup.

#### Scenario: CDK stack deploys S3 bucket
- **GIVEN** the CDK project in `infra/` directory
- **AND** valid AWS credentials configured
- **WHEN** `npm run cdk:deploy` is executed
- **THEN** an S3 bucket named `runbook-synthesizer-e2e-{accountId}` is created
- **AND** the bucket is tagged with `ManagedBy=runbook-synthesizer-e2e`

#### Scenario: CDK stack deploys CloudWatch Log Group
- **GIVEN** the CDK project in `infra/` directory
- **AND** valid AWS credentials configured
- **WHEN** `npm run cdk:deploy` is executed
- **THEN** a CloudWatch Log Group named `/runbook-synthesizer/e2e` is created
- **AND** the log group has 1-day retention

#### Scenario: CDK stack is idempotent
- **GIVEN** the CDK stack is already deployed
- **WHEN** `npm run cdk:deploy` is executed again
- **THEN** no duplicate resources are created
- **AND** the deploy succeeds with no changes (or updates only changed resources)

#### Scenario: CDK outputs resource identifiers
- **GIVEN** the CDK stack is deployed
- **WHEN** deployment completes
- **THEN** CloudFormation outputs include `BucketName` and `LogGroupName`
- **AND** these values can be retrieved via `aws cloudformation describe-stacks`

---

### Requirement: Extensible CDK Construct Architecture

The system SHALL use an extensible construct pattern for AWS service provisioning, enabling easy addition of new services in the future.

#### Scenario: S3Construct is standalone reusable
- **GIVEN** the `S3Construct` class in `lib/constructs/`
- **WHEN** added to any CDK stack
- **THEN** it provisions an S3 bucket with configurable name prefix
- **AND** it exports a `bucket` property for further configuration

#### Scenario: Adding new AWS service
- **GIVEN** a new AWS service requirement (e.g., DynamoDB)
- **WHEN** a developer creates `DynamoDbConstruct` in `lib/constructs/`
- **THEN** they can add it to `E2eTestStack` with one line
- **AND** existing constructs remain unchanged

---

### Requirement: Real AWS E2E Test Base Class

The system SHALL provide a `RealAwsTestBase` class for Java e2e tests that validates AWS credentials and configures AWS clients for real AWS service access.

#### Scenario: Tests disabled by default
- **GIVEN** `AWS_E2E_ENABLED` environment variable is not set
- **WHEN** test runner processes `*IT.java` files in `aws/e2e/` package
- **THEN** all real AWS tests are skipped

#### Scenario: Tests enabled via environment variable
- **GIVEN** `AWS_E2E_ENABLED=true` environment variable is set
- **AND** valid AWS credentials are available
- **WHEN** test runner processes `*IT.java` files in `aws/e2e/` package
- **THEN** real AWS tests execute against provisioned resources

#### Scenario: Credential validation fails fast
- **GIVEN** `AWS_E2E_ENABLED=true` is set
- **AND** no valid AWS credentials are available
- **WHEN** tests start
- **THEN** a clear error message indicates missing credentials
- **AND** tests fail immediately rather than timing out

---

### Requirement: Real AWS S3 E2E Tests

The system SHALL provide e2e tests validating S3 storage operations against a real AWS S3 bucket.

#### Scenario: Upload and retrieve runbook from real S3
- **GIVEN** the CDK stack is deployed
- **AND** `AWS_E2E_ENABLED=true`
- **WHEN** a runbook is uploaded to the e2e bucket via `AwsS3StorageAdapter`
- **THEN** the runbook content can be retrieved via the same adapter
- **AND** the content matches exactly

#### Scenario: List runbooks from real S3
- **GIVEN** the CDK stack is deployed
- **AND** sample runbooks uploaded to the e2e bucket
- **WHEN** `listRunbooks()` is called on `AwsS3StorageAdapter`
- **THEN** all uploaded runbook files are listed

---

### Requirement: Real AWS CloudWatch Logs E2E Tests

The system SHALL provide e2e tests validating CloudWatch Logs operations against a real AWS log group.

#### Scenario: Write and read logs from real CloudWatch
- **GIVEN** the CDK stack is deployed
- **AND** `AWS_E2E_ENABLED=true`
- **WHEN** log events are written via `AwsCloudWatchLogsAdapter`
- **THEN** the events can be read back via the same adapter
- **AND** timestamps and messages match

---

### Requirement: Real AWS E2E Maven Profile

The system SHALL provide a Maven profile for running real AWS e2e tests isolated from other test types.

#### Scenario: Profile runs only real AWS tests
- **GIVEN** the `e2e-aws-real` Maven profile
- **WHEN** `mvnw verify -Pe2e-aws-real` is executed
- **THEN** only tests in `**/aws/e2e/*IT.java` are executed
- **AND** LocalStack and unit tests are not executed

#### Scenario: Profile sets required environment
- **GIVEN** the `e2e-aws-real` Maven profile
- **WHEN** tests execute under this profile
- **THEN** `AWS_E2E_ENABLED=true` is set automatically
