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

> **CRITICAL FOR AI AGENTS:** This task requires incremental verification. DO NOT proceed to the next subtask until you have verified the current one works. Test discovery issues are common and fail silently.

### Prerequisites
- Docker Desktop must be running (verify with `docker info`)
- AWS credentials must be configured (`aws sts get-caller-identity` should succeed)
- CDK infrastructure must be deployed (Task 1-2 complete, `npm run cdk:deploy` succeeded)

### Known Issues & Mitigations

| Issue | Root Cause | Mitigation |
|-------|------------|------------|
| JUnit 6.x tests show 0 runs | JUnit 6.x milestone releases have Failsafe compatibility issues | Use JUnit 5.11.x (stable) |
| `@EnabledIfEnvironmentVariable` not triggered | Failsafe `<environmentVariables>` unreliable on Windows | Use `@EnabledIfSystemProperty` with `<systemPropertyVariables>` |
| Test classes found but 0 methods discovered | `@BeforeAll` throwing before test discovery OR abstract class annotation inheritance issues | Verify with minimal standalone test first |
| Tests silently skipped | Conditional annotation not satisfied | Check system property is set: look for `aws.e2e.enabled=true` in Failsafe debug output |

### Directory Structure

```
integration/aws/
├── cloud/                              # Tests against real AWS cloud services
│   ├── CloudAwsTestBase.java           # Base class with credential validation
│   ├── AwsS3StorageCloudIT.java        # S3 cloud tests
│   └── AwsCloudWatchLogsCloudIT.java   # CloudWatch Logs cloud tests
└── local/                              # Tests using LocalStack (emulated)
    ├── AwsS3StorageLocalIT.java
    ├── AwsCloudWatchLogsLocalIT.java
    ├── AwsCloudWatchMetricsLocalIT.java
    └── AwsRagPipelineLocalIT.java
```

### Acceptance Criteria
- Minimal diagnostic test (no inheritance) runs and passes
- `CloudAwsTestBase` validates AWS credentials in `@BeforeAll`
- Tests are skipped when `aws.cloud.enabled` system property is NOT set
- Tests pass when run with `-Pe2e-aws-cloud` profile against provisioned AWS resources
- All tests use `@EnabledIfSystemProperty(named = "aws.cloud.enabled", matches = "true")`
- Docker unavailability fails fast with clear error message

---

### Task 3.1: Verify JUnit + Failsafe Discovery Works

**Purpose:** Establish that JUnit 5 test discovery works with Failsafe before adding any complexity.

**Tasks:**
- [X] 3.1.1 Downgrade JUnit from 6.x to 5.11.4 in `pom.xml` (`<junit.version>5.11.4</junit.version>`)
- [X] 3.1.2 Create minimal test `src/test/java/com/oracle/runbook/integration/aws/cloud/MinimalDiagnosticIT.java`:
  ```java
  package com.oracle.runbook.integration.aws.cloud;
  
  import static org.assertj.core.api.Assertions.assertThat;
  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Test;
  
  @DisplayName("Minimal Diagnostic IT")
  class MinimalDiagnosticIT {
    @Test
    @DisplayName("JUnit 5 discovery works")
    void junitDiscoveryWorks() {
      System.out.println("[MinimalDiagnosticIT] Test is running!");
      assertThat(true).isTrue();
    }
  }
  ```

**Verification:**
```powershell
.\mvnw.cmd clean verify -Pe2e-aws-cloud 2>&1 | Tee-Object -FilePath "verify_3_1.log"
# SUCCESS CRITERIA: Output contains "Tests run: 1" for MinimalDiagnosticIT
# FAILURE: If "Tests run: 0" → JUnit/Failsafe configuration issue, DO NOT PROCEED
```

**Troubleshooting (if 0 tests run):**
```powershell
# Check JUnit version is 5.11.x (NOT 6.x)
.\mvnw.cmd help:effective-pom | Select-String "junit.version"

# Check Failsafe includes pattern matches
.\mvnw.cmd verify -Pe2e-aws-cloud -X 2>&1 | Select-String "includes"

# Verify .class file exists
Get-ChildItem target/test-classes/com/oracle/runbook/integration/aws/cloud/*.class
```

---

### Task 3.2: Create CloudAwsTestBase with Credential Validation

**Purpose:** Base class that validates AWS credentials before any tests run.

**Tasks:**
- [X] 3.2.1 Create `src/test/java/com/oracle/runbook/integration/aws/cloud/CloudAwsTestBase.java`
- [X] 3.2.2 Add `@EnabledIfSystemProperty(named = "aws.cloud.enabled", matches = "true")` annotation
- [X] 3.2.3 Implement `@BeforeAll static void validateAwsCredentials()` using STS GetCallerIdentity
- [X] 3.2.4 Add fail-fast check for Docker availability (for future containerized tests)
- [X] 3.2.5 Provide helper methods: `createS3Client()`, `createCloudWatchLogsClient()`, `getBucketName()`

**Key Implementation Details:**
```java
@EnabledIfSystemProperty(named = "aws.cloud.enabled", matches = "true")
public abstract class CloudAwsTestBase {
  
  @BeforeAll
  static void validateAwsCredentials() {
    // MUST use try-catch and assumeThat() for graceful skip
    try (StsClient stsClient = StsClient.builder()...build()) {
      var identity = stsClient.getCallerIdentity();
      assertThat(identity.account()).isNotNull();
      System.out.printf("[CloudAwsTestBase] Authenticated: %s%n", identity.arn());
    } catch (Exception e) {
      assumeThat(false).as("AWS credentials not available: " + e.getMessage()).isTrue();
    }
  }
}
```

**Verification:**
```powershell
# Create a simple test extending CloudAwsTestBase
# Add to MinimalDiagnosticIT or create CloudAwsBaseTestIT

.\mvnw.cmd clean verify -Pe2e-aws-cloud 2>&1 | Tee-Object -FilePath "verify_3_2.log"
# SUCCESS CRITERIA: 
#   - Output contains "[CloudAwsTestBase] Authenticated: arn:aws:..."
#   - Test count > 0
# FAILURE:
#   - If test is skipped → Check aws.cloud.enabled system property is set
#   - If 0 tests → Annotation inheritance issue, add annotation to concrete class too
```

**Troubleshooting:**
```powershell
# Verify system property reaches forked JVM
.\mvnw.cmd verify -Pe2e-aws-cloud -X 2>&1 | Select-String "aws.cloud.enabled"
# Should show: [DEBUG] Setting system property [aws.cloud.enabled]=[true]

# If tests still not discovered, check XML report for property
Get-Content target/failsafe-reports/*.xml | Select-String "aws.cloud.enabled"
```

---

### Task 3.3: Create AwsS3StorageCloudIT

**Purpose:** Integration tests for S3 operations against real AWS cloud.

**Tasks:**
- [X] 3.3.1 Create `src/test/java/com/oracle/runbook/integration/aws/cloud/AwsS3StorageCloudIT.java`
- [X] 3.3.2 Add `@EnabledIfSystemProperty` annotation (MUST be on concrete class, not just inherited)
- [X] 3.3.3 Implement `@BeforeAll` to upload test files to S3 bucket
- [X] 3.3.4 Test `listRunbooks()` returns expected markdown files
- [X] 3.3.5 Test `getRunbookContent()` returns file content
- [X] 3.3.6 Test `getRunbookContent()` returns empty for non-existent key

**Verification:**
```powershell
.\mvnw.cmd clean verify -Pe2e-aws-cloud 2>&1 | Tee-Object -FilePath "verify_3_3.log"
# SUCCESS CRITERIA:
#   - "Running AWS S3 Cloud E2E Tests"
#   - "Tests run: 3" (or more) for AwsS3StorageCloudIT
#   - "[AwsS3StorageCloudIT] Uploaded test file:" messages visible
```

---

### Task 3.4: Create AwsCloudWatchLogsCloudIT

**Purpose:** Integration tests for CloudWatch Logs operations against real AWS cloud.

**Tasks:**
- [X] 3.4.1 Create `src/test/java/com/oracle/runbook/integration/aws/cloud/AwsCloudWatchLogsCloudIT.java`
- [X] 3.4.2 Add `@EnabledIfSystemProperty` annotation
- [X] 3.4.3 Implement `@BeforeAll` to create log stream and write test events
- [X] 3.4.4 Test `fetchLogs()` returns written log entries
- [X] 3.4.5 Test filter patterns work correctly

**Verification:**
```powershell
.\mvnw.cmd clean verify -Pe2e-aws-cloud 2>&1 | Tee-Object -FilePath "verify_3_4.log"
# SUCCESS CRITERIA:
#   - "Running AWS CloudWatch Logs Cloud E2E Tests"
#   - "Tests run: 2" (or more) for AwsCloudWatchLogsCloudIT
```

---

### Task 3.5: Final Integration Verification

**Purpose:** Verify all E2E tests work together with proper isolation.

**Verification:**
```powershell
# Full E2E test run
.\mvnw.cmd clean verify -Pe2e-aws-cloud 2>&1 | Tee-Object -FilePath "verify_3_5.log"

# SUCCESS CRITERIA (all must pass):
# 1. MinimalDiagnosticIT runs (Tests run: 1)
# 2. AwsS3StorageCloudIT runs (Tests run: 3+)
# 3. AwsCloudWatchLogsCloudIT runs (Tests run: 2+)
# 4. BUILD SUCCESS
# 5. No Docker-related errors

# Verify tests are skipped when property not set
.\mvnw.cmd clean verify 2>&1 | Select-String "CloudIT"
# Should show: "Tests run: 0" for all CloudIT tests (skipped)
```

---

### Debugging Checklist (If Tests Don't Run)

Use this systematic checklist if tests show "Tests run: 0":

```powershell
# 1. Is Docker running?
docker info
# Expected: Docker version info. If error → Start Docker Desktop

# 2. Is JUnit 5.11.x (not 6.x)?
.\mvnw.cmd help:effective-pom | Select-String "junit-jupiter"
# Expected: 5.11.4. If 6.x → Change <junit.version> in pom.xml

# 3. Is the system property being set?
.\mvnw.cmd verify -Pe2e-aws-cloud -X 2>&1 | Select-String "aws.cloud.enabled"
# Expected: "[DEBUG] Setting system property [aws.cloud.enabled]=[true]"

# 4. Does the property reach the forked JVM?
Get-Content target/failsafe-reports/*.xml | Select-String "aws.cloud.enabled"
# Expected: <property name="aws.cloud.enabled" value="true"/>

# 5. Are test classes being matched?
.\mvnw.cmd verify -Pe2e-aws-cloud 2>&1 | Select-String "Running"
# Expected: "Running AWS S3 Cloud E2E Tests" etc.

# 6. Are there any errors in Failsafe reports?
Get-Content target/failsafe-reports/*.txt
# Check for exceptions or error messages
```

---

## 4. Maven Profile Integration

> **CRITICAL FOR AI AGENTS:** Task 3 MUST be completed first. The Maven profile provides the infrastructure to run the tests created in Task 3. Verify Task 3.1 (MinimalDiagnosticIT) passes before proceeding here.

### Prerequisites

- Task 3.1 complete (MinimalDiagnosticIT exists and test discovery verified)
- Existing pom.xml has Failsafe plugin configured in `<build><plugins>` section

### Known Issues & Mitigations

| Issue | Root Cause | Mitigation |
|-------|------------|------------|
| Environment variables not reaching tests | Failsafe `<environmentVariables>` unreliable on Windows | Use `<systemPropertyVariables>` instead |
| Unit tests (Surefire) running and failing | Profile only configures Failsafe, not Surefire | Add Surefire `<skipTests>true</skipTests>` to profile |
| Tests from other profiles running | Failsafe running twice (default + profile) | Ensure profile's Failsafe has explicit `<includes>` |
| `-DskipTests` skips ALL tests | This flag affects both Surefire AND Failsafe | Never use `-DskipTests` with e2e profile |

### Acceptance Criteria

- Profile `e2e-aws-cloud` exists in pom.xml `<profiles>` section
- Profile skips Surefire (unit tests) completely
- Profile only includes `**/aws/cloud/*IT.java` tests in Failsafe
- Profile sets `aws.cloud.enabled=true` via `<systemPropertyVariables>` (NOT environment variables)
- Running profile without AWS credentials shows clear error from `CloudAwsTestBase`
- Other profiles (default, `e2e-containers`) do NOT run CloudAws tests

---

### Task 4.1: Add e2e-aws-cloud Profile Shell

**Purpose:** Create the profile structure in pom.xml.

**Tasks:**

- [ ] 4.1.1 Add `<profile>` with `<id>e2e-aws-cloud</id>` to `<profiles>` section
- [ ] 4.1.2 Add comment explaining the profile's purpose

**Implementation:**
```xml
<profiles>
    <!-- Existing profiles here... -->
    
    <!-- E2E AWS Cloud Profile - Runs tests against real AWS cloud services -->
    <profile>
        <id>e2e-aws-cloud</id>
        <build>
            <plugins>
                <!-- Plugins added in subsequent tasks -->
            </plugins>
        </build>
    </profile>
</profiles>
```

**Verification:**
```powershell
.\mvnw.cmd help:active-profiles -Pe2e-aws-cloud
# SUCCESS CRITERIA: Shows "e2e-aws-cloud" as active profile
```

---

### Task 4.2: Configure Surefire to Skip Unit Tests

**Purpose:** Prevent unit tests from running when using the e2e-aws-cloud profile. Unit tests may depend on Docker/LocalStack which we want to avoid.

**Tasks:**

- [X] 4.2.1 Add Surefire plugin configuration inside the profile
- [X] 4.2.2 Set `<skipTests>true</skipTests>` to skip all unit tests

**Implementation:**
```xml
<profile>
    <id>e2e-aws-cloud</id>
    <build>
        <plugins>
            <!-- Skip unit tests - only run Failsafe integration tests -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.5.4</version>
                <configuration>
                    <skipTests>true</skipTests>
                </configuration>
            </plugin>
            <!-- Failsafe config added next -->
        </plugins>
    </build>
</profile>
```

**Verification:**
```powershell
.\mvnw.cmd verify -Pe2e-aws-cloud 2>&1 | Select-String "Tests are skipped"
# SUCCESS CRITERIA: Output contains "[INFO] Tests are skipped." for Surefire phase
```

---

### Task 4.3: Configure Failsafe with System Property

**Purpose:** Configure Failsafe to run only AWS Cloud E2E tests and pass the enabling system property.

> **CRITICAL:** Use `<systemPropertyVariables>` NOT `<environmentVariables>`. Environment variables are unreliable on Windows with Failsafe.

**Tasks:**

- [X] 4.3.1 Add Failsafe plugin configuration inside the profile
- [X] 4.3.2 Set `<includes>` to `**/aws/cloud/*IT.java`
- [X] 4.3.3 Add `<systemPropertyVariables>` with `<aws.cloud.enabled>true</aws.cloud.enabled>`
- [X] 4.3.4 Add JVM args for preview features: `--enable-preview --enable-native-access=ALL-UNNAMED`

**Implementation:**
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <version>3.5.4</version>
    <configuration>
        <argLine>--enable-preview --enable-native-access=ALL-UNNAMED</argLine>
        <!-- Include only real AWS cloud tests -->
        <includes>
            <include>**/aws/cloud/*IT.java</include>
        </includes>
        <!-- CRITICAL: Use systemPropertyVariables, NOT environmentVariables -->
        <systemPropertyVariables>
            <aws.cloud.enabled>true</aws.cloud.enabled>
        </systemPropertyVariables>
    </configuration>
    <executions>
        <execution>
            <id>integration-test</id>
            <goals>
                <goal>integration-test</goal>
            </goals>
        </execution>
        <execution>
            <id>verify</id>
            <goals>
                <goal>verify</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

**Verification:**
```powershell
# Verify system property is being set
.\mvnw.cmd verify -Pe2e-aws-cloud -X 2>&1 | Select-String "aws.cloud.enabled"
# SUCCESS CRITERIA: Shows "[DEBUG] Setting system property [aws.cloud.enabled]=[true]"

# Verify only aws/cloud tests are included
.\mvnw.cmd verify -Pe2e-aws-cloud 2>&1 | Select-String "Running"
# SUCCESS CRITERIA: Only shows "Running" for classes in aws/cloud package
```

---

### Task 4.4: Verify Profile Isolation

**Purpose:** Ensure the e2e-aws-cloud profile doesn't run other tests, and other profiles don't run CloudAws tests.

**Verification:**
```powershell
# 1. Verify CloudAws tests run ONLY with e2e-aws-cloud profile
.\mvnw.cmd clean verify -Pe2e-aws-cloud 2>&1 | Select-String "Running.*CloudIT"
# SUCCESS CRITERIA: Shows "Running AWS S3 Cloud E2E Tests" and similar

# 2. Verify default profile doesn't run CloudAws tests
.\mvnw.cmd clean verify 2>&1 | Select-String "CloudIT"
# SUCCESS CRITERIA: No matches (CloudAws tests not in default includes)

# 3. Verify e2e-containers profile doesn't run CloudAws tests
.\mvnw.cmd clean verify -Pe2e-containers 2>&1 | Select-String "CloudIT"
# SUCCESS CRITERIA: No matches

# 4. Verify unit tests are skipped in e2e-aws-cloud
.\mvnw.cmd clean verify -Pe2e-aws-cloud 2>&1 | Select-String "surefire"
# SUCCESS CRITERIA: Shows "[INFO] Tests are skipped." for surefire:test
```

---

### Task 4.5: Final Profile Verification

**Purpose:** Full integration test of the profile.

**Verification:**
```powershell
# Full E2E run (requires AWS credentials and CDK deployed)
.\mvnw.cmd clean verify -Pe2e-aws-cloud 2>&1 | Tee-Object -FilePath "verify_4_5.log"

# SUCCESS CRITERIA (all must be true):
# 1. Surefire shows "Tests are skipped"
# 2. Failsafe shows "Running AWS S3 Cloud E2E Tests"
# 3. Failsafe shows "Running AWS CloudWatch Logs Cloud E2E Tests"
# 4. Tests run count > 0
# 5. BUILD SUCCESS
```

---

### Debugging Checklist (If Profile Doesn't Work)

```powershell
# 1. Is profile active?
.\mvnw.cmd help:active-profiles -Pe2e-aws-cloud
# Expected: Shows e2e-aws-cloud as active

# 2. Is Surefire skipping tests?
.\mvnw.cmd verify -Pe2e-aws-cloud 2>&1 | Select-String "surefire:test"
# Expected: "[INFO] Tests are skipped."

# 3. Is system property being set (NOT environment variable)?
.\mvnw.cmd verify -Pe2e-aws-cloud -X 2>&1 | Select-String "systemPropertyVariables"
# Expected: {aws.cloud.enabled=true}

# 4. Is the includes pattern correct?
.\mvnw.cmd verify -Pe2e-aws-cloud -X 2>&1 | Select-String "includes"
# Expected: **/aws/cloud/*IT.java

# 5. Are test classes being found?
.\mvnw.cmd verify -Pe2e-aws-cloud 2>&1 | Select-String "Running"
# Expected: Shows test class names

# 6. Check for conflicting Failsafe configurations
.\mvnw.cmd help:effective-pom | Select-String -Context 5 "failsafe"
# Look for multiple Failsafe plugin entries that might conflict
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
- [X] 6.1 Run `openspec validate add-aws-e2e-automation --strict`
- [X] 6.2 Verify CDK synth produces valid CloudFormation template
- [X] 6.3 Deploy stack to test AWS account and run e2e tests
- [X] 6.4 Verify manual `cdk destroy` cleanly removes all resources

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
