## 1. CDK Infrastructure Setup

**Acceptance Criteria:**
- `infra/` directory contains valid CDK project with all required config files
- `npm install` succeeds without errors
- `npm run cdk:synth` produces valid CloudFormation template

**Tasks:**
- [X] 1.1 Create `infra/` directory with `package.json` and `cdk.json`
- [X] 1.2 Create `tsconfig.json` for TypeScript compilation
- [X] 1.3 Create CDK app entry point (`bin/e2e-infra.ts`)
- [X] 1.4 Create main stack (`lib/e2e-test-stack.ts`)

**Verification:**
```powershell
cd infra
npm install
npm run cdk:synth
# Verify: CloudFormation JSON output, no errors
```

---

## 2. CDK Constructs (Extensible)

**Acceptance Criteria:**
- Each construct is self-contained and independently testable
- Constructs expose resource references via public properties
- CloudFormation outputs include bucket name and log group ARN
- All resources tagged with `ManagedBy=runbook-synthesizer-e2e`

**Tasks:**
- [X] 2.1 Create `S3Construct` for runbook bucket provisioning
- [X] 2.2 Create `CloudWatchLogsConstruct` for log group provisioning
- [X] 2.3 Create `CloudWatchMetricsConstruct` (placeholder for future alarms/dashboards)
- [X] 2.4 Add CDK outputs for resource identifiers (bucket name, log group ARN)

**Verification:**
```powershell
npm run cdk:synth
# Verify synthesized template includes:
# - AWS::S3::Bucket with correct tagging
# - AWS::Logs::LogGroup with 1-day retention
# - Outputs section with BucketName and LogGroupName
```

---

## 3. Java E2E Test Integration

**Acceptance Criteria:**
- `RealAwsTestBase` validates AWS credentials before test execution
- Tests are skipped when `AWS_E2E_ENABLED` is not set
- Tests pass when run against provisioned AWS resources
- All tests use `@EnabledIfEnvironmentVariable` annotation

**Tasks:**
- [ ] 3.1 Create `RealAwsTestBase` base class with AWS credential validation
- [ ] 3.2 Create `RealAwsS3StorageIT` testing S3 operations against real AWS
- [ ] 3.3 Create `RealAwsCloudWatchLogsIT` testing CloudWatch Logs operations
- [ ] 3.4 Ensure tests are disabled by default (require `AWS_E2E_ENABLED=true`)

**Verification:**
```powershell
# Without env var - tests should be skipped
.\mvnw.cmd test -Dtest="**/aws/e2e/*IT"
# Verify: 0 tests run (all skipped)

# With env var and credentials - tests should pass
$env:AWS_E2E_ENABLED = "true"
.\mvnw.cmd verify -Pe2e-aws-real
# Verify: All tests pass, no connection errors
```

---

## 4. Maven Profile Integration

**Acceptance Criteria:**
- `e2e-aws-real` profile only includes `**/aws/e2e/*IT.java` tests
- Profile sets `AWS_E2E_ENABLED=true` automatically
- Running profile without AWS credentials fails with clear error message

**Tasks:**
- [ ] 4.1 Add `e2e-aws-real` profile to `pom.xml`
- [ ] 4.2 Configure Failsafe plugin to only include `**/aws/e2e/*IT.java`
- [ ] 4.3 Set `AWS_E2E_ENABLED=true` environment variable in profile

**Verification:**
```powershell
# Check profile runs only real AWS tests
.\mvnw.cmd verify -Pe2e-aws-real -DskipTests=false 2>&1 | Select-String "Running"
# Verify: Only **/aws/e2e/*IT tests appear

# Check other profiles don't run real AWS tests
.\mvnw.cmd verify -Pe2e-containers 2>&1 | Select-String "RealAws"
# Verify: No RealAws tests appear
```

---

## 5. Documentation

**Acceptance Criteria:**
- `AWS_FREE_TIER_TESTING_GUIDE.md` has new "Automated E2E Testing" section
- Documentation includes CDK prerequisites, deploy/destroy commands
- All environment variables documented with defaults

**Tasks:**
- [X] 5.1 Update `AWS_FREE_TIER_TESTING_GUIDE.md` with CDK automation section
- [X] 5.2 Document CDK deploy/destroy commands
- [X] 5.3 Document environment variable requirements

**Verification:**
- [X] Manual review: Guide contains complete CDK workflow
- [X] Manual review: Commands are copy-pasteable and work

---

## 6. Verification

**Acceptance Criteria:**
- Full workflow works end-to-end: CDK deploy → run tests → manual CDK destroy
- Idempotency verified: second deploy succeeds with no changes
- Cleanup verified: `cdk destroy` removes all resources

**Tasks:**
- [ ] 6.1 Run `openspec validate add-aws-e2e-automation --strict`
- [ ] 6.2 Verify CDK synth produces valid CloudFormation template
- [ ] 6.3 Deploy stack to test AWS account and run e2e tests
- [ ] 6.4 Verify manual `cdk destroy` cleanly removes all resources

**Verification:**
```powershell
# Full integration test
cd infra
npm run cdk:deploy
# Verify: Stack deployed, outputs visible

cd ..
$env:AWS_E2E_ENABLED = "true"
.\mvnw.cmd verify -Pe2e-aws-real
# Verify: All tests pass

# Idempotency check
cd infra
npm run cdk:deploy
# Verify: "No changes" or minimal updates

# Cleanup check (manual, when ready)
npm run cdk:destroy
# Verify: All resources removed from AWS console
```
