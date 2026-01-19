# Tasks: Enable AWS Full-Stack Functionality

## Task 1: Add AWS SDK Dependencies

**Description:** Add required AWS SDK artifacts to pom.xml for Bedrock runtime.

### Subtasks
- [x] 1.1 Add `software.amazon.awssdk:bedrockruntime` dependency
- [x] 1.2 Verify Maven compiles successfully

### Verification Steps
```bash
cd c:\Users\bwend\repos\ops-scribe
mvn compile -q
```
**Expected:** Build succeeds with no errors.

### Acceptance Criteria
- [x] `pom.xml` contains `bedrockruntime` dependency
- [x] `mvn compile` passes

---

## Task 2: Implement AwsSnsAlertSourceAdapter

**Description:** Create adapter to parse CloudWatch Alarm JSON delivered via SNS webhook.

### Subtasks
- [x] 2.1 Create `AwsSnsAlertSourceAdapter.java` in `infrastructure/cloud/aws/`
- [x] 2.2 Implement `sourceType()` returning `"aws-cloudwatch-sns"`
- [x] 2.3 Implement `canHandle()` to detect SNS/CloudWatch signature
- [x] 2.4 Implement `parseAlert()` with severity mapping:
  - `ALARM` → `CRITICAL`
  - `INSUFFICIENT_DATA` → `WARNING`
  - `OK` → Return null (skip)
- [x] 2.5 Create unit tests `AwsSnsAlertSourceAdapterTest.java`

### Verification Steps
```bash
cd c:\Users\bwend\repos\ops-scribe
mvn test -Dtest=AwsSnsAlertSourceAdapterTest -q
```
**Expected:** All unit tests pass.

### Acceptance Criteria
- [x] `AwsSnsAlertSourceAdapter` implements `AlertSourceAdapter` interface
- [x] Parses SNS message envelope correctly (extracts `Message` field)
- [x] Parses CloudWatch Alarm JSON within Message
- [x] Maps `AlarmName` → `Alert.title`
- [x] Maps `NewStateReason` → `Alert.message`
- [x] Maps `NewStateValue` → `Alert.severity` (using approved mapping)
- [x] Returns null for `OK` state
- [x] Unit tests cover: valid ALARM, valid INSUFFICIENT_DATA, OK state skip, invalid JSON handling

### Sample Test Payloads
See `src/test/resources/fixtures/alerts/cloudwatch-alarm-sns.json`

---

## Task 3: Implement LLM Providers (Ollama + Bedrock)

**Description:** Create pluggable LLM providers with Ollama as MVP default and AWS Bedrock for production.

### Subtask 3A: OllamaLlmProvider (MVP Default)


- [x] 3A.1 Create `OllamaLlmProvider.java` in `infrastructure/llm/`
- [x] 3A.2 Implement `providerId()` returning `"ollama"`
- [x] 3A.3 Implement `generateText()` using Ollama REST API:
  - Endpoint: `POST /api/generate`
  - Model: `llama3.2:3b` (configurable)
- [x] 3A.4 Implement `generateEmbedding()` using Ollama REST API:
  - Endpoint: `POST /api/embeddings`
  - Model: `nomic-embed-text`
- [x] 3A.5 Implement `generateEmbeddings()` for batch embeddings
- [x] 3A.6 Create `OllamaConfig.java` record for configuration
- [x] 3A.7 Create unit tests `OllamaLlmProviderTest.java`

**Verification (Ollama):**
```bash
# Ensure Ollama is running locally
ollama serve &
ollama pull llama3.2:3b
ollama pull nomic-embed-text

cd c:\Users\bwend\repos\ops-scribe
mvn test -Dtest=OllamaLlmProviderTest -q
```

**Acceptance Criteria (Ollama):**
- [x] `OllamaLlmProvider` implements `LlmProvider` interface
- [x] Uses async HTTP client for non-blocking calls
- [x] Connects to configurable Ollama base URL
- [x] Unit tests pass with WireMock or similar

---

### Subtask 3B: AwsBedrockLlmProvider (Production)

- [x] 3B.1 Create `AwsBedrockLlmProvider.java` in `infrastructure/cloud/aws/`
- [x] 3B.2 Implement `providerId()` returning `"aws-bedrock"`
- [x] 3B.3 Implement `generateText()` using Claude 3 Haiku:
  - Model ID: `anthropic.claude-3-haiku-20240307-v1:0`
  - Use `BedrockRuntimeAsyncClient.invokeModel()`
- [x] 3B.4 Implement `generateEmbedding()` using Cohere Embed v3:
  - Model ID: `cohere.embed-english-v3`
  - Use `BedrockRuntimeAsyncClient.invokeModel()`
- [x] 3B.5 Implement `generateEmbeddings()` for batch embeddings
- [x] 3B.6 Create `AwsBedrockConfig.java` record for configuration
- [x] 3B.7 Create unit tests `AwsBedrockLlmProviderTest.java` (mocked SDK)

**Verification (Bedrock):**
```bash
cd c:\Users\bwend\repos\ops-scribe
mvn test -Dtest=AwsBedrockLlmProviderTest -q
```

**Acceptance Criteria (Bedrock):**
- [x] `AwsBedrockLlmProvider` implements `LlmProvider` interface
- [x] Uses `BedrockRuntimeAsyncClient` for async operations
- [x] Correctly formats Claude 3 request body (Messages API format)
- [x] Correctly formats Cohere Embed request body
- [x] Parses response JSON and extracts embeddings/text
- [x] Unit tests cover: text generation, single embedding, batch embeddings, error handling

---

## Task 4: Enhance CloudAdapterFactory

**Description:** Add factory methods for metrics, logs, alerts, and LLM adapters.

### Subtasks
- [x] 4.1 Add `getMetricsAdapterClass()` method
- [x] 4.2 Add `getLogsAdapterClass()` method
- [x] 4.3 Add `getAlertSourceAdapterClass()` method
- [x] 4.4 Add `getLlmProviderClass()` method
- [x] 4.5 Update `CloudAdapterFactoryTest.java` with new test cases

### Verification Steps
```bash
cd c:\Users\bwend\repos\ops-scribe
mvn test -Dtest=CloudAdapterFactoryTest -q
```
**Expected:** All unit tests pass including new factory methods.

### Acceptance Criteria
- [x] `getMetricsAdapterClass()` returns `AwsCloudWatchMetricsAdapter.class` for AWS
- [x] `getLogsAdapterClass()` returns `AwsCloudWatchLogsAdapter.class` for AWS
- [x] `getAlertSourceAdapterClass()` returns `AwsSnsAlertSourceAdapter.class` for AWS
- [x] `getLlmProviderClass()` returns `AwsBedrockLlmProvider.class` for AWS
- [x] All methods throw `IllegalStateException` for unsupported providers
- [x] Unit tests verify both AWS and OCI provider selection

---

## Task 5: Update Configuration

**Description:** Set AWS as default cloud provider and add complete AWS configuration.

### Subtasks
- [x] 5.1 Update `application.yaml`:
  - Change `cloud.provider` default to `aws`
  - Uncomment and complete AWS section
  - Add `llm.aws-bedrock` configuration
  - Add `llm.ollama` configuration
- [x] 5.2 Create `AwsBedrockConfig.java` if not created in Task 3
- [x] 5.3 Update `AwsConfig.java` if additional fields needed (no changes needed)
- [x] 5.4 Add configuration tests

### Verification Steps
```bash
cd c:\Users\bwend\repos\ops-scribe
mvn test -Dtest=*ConfigTest -q
```
**Expected:** All config tests pass.

### Acceptance Criteria
- [x] `cloud.provider` defaults to `aws`
- [x] AWS configuration section is complete and uncommented
- [x] Environment variable placeholders use sensible defaults
- [x] LLM configuration specifies Bedrock model IDs
- [x] Application starts successfully with AWS configuration

---

## Task 6: E2E Test Integration

**Description:** Add E2E tests for new components using existing CDK infrastructure.

> **MVP Scope Reduction:** Bedrock E2E tests deferred per user decision to use local Ollama LLM for MVP. The `AwsSnsAlertSourceAdapter` is a pure JSON parser (no AWS API calls) - existing unit tests in `AwsSnsAlertSourceAdapterTest.java` (32 tests) provide complete coverage.

### Subtasks
- [x] 6.1 ~~Extend CDK stack with SNS topic and Bedrock IAM~~ - **DEFERRED** (not needed for MVP)
- [x] 6.2 ~~Create `AwsSnsAlertCloudIT.java`~~ - **NOT NEEDED** (unit tests already cover parsing end-to-end)
- [x] 6.3 ~~Create `AwsBedrockLlmCloudIT.java`~~ - **DEFERRED** (MVP uses local Ollama)
- [x] 6.4 Update documentation with MVP scope decision

### Verification Steps
```bash
# Existing unit tests verify all parsing logic
cd c:\Users\bwend\repos\ops-scribe
mvn test -Dtest=AwsSnsAlertSourceAdapterTest -q
```
**Expected:** All 32 unit tests pass.

### Acceptance Criteria (MVP)
- [x] `AwsSnsAlertSourceAdapter` tested via comprehensive unit tests (32 tests)
- [x] Bedrock E2E deferred to post-MVP release
- [x] MVP uses local Ollama LLM (configured in Task 5)


---

## Task 7: Documentation

**Description:** Update documentation to reflect AWS-first approach.

### Subtasks
- [x] 7.1 Update `docs/ARCHITECTURE.md` with AWS default note
- [x] 7.2 Update `docs/DESIGN.md` with AWS-first approach and LLM abstraction
- [x] 7.3 Update `README.md` with AWS quickstart
- [x] 7.4 Update `openspec/project.md` to reflect AWS as primary

### Verification Steps
Manual review of documentation accuracy.

### Acceptance Criteria
- [x] Documentation reflects AWS as default provider
- [x] `docs/DESIGN.md` documents LLM provider abstraction (Ollama vs Bedrock)
- [x] Setup instructions work for AWS
- [x] OCI is documented as alternative

---

## Dependency Graph

```mermaid
flowchart TD
    T1[Task 1: SDK Dependencies] --> T2[Task 2: Alert Adapter]
    T1 --> T3[Task 3: Bedrock Provider]
    T2 --> T4[Task 4: Factory Wiring]
    T3 --> T4
    T4 --> T5[Task 5: Configuration]
    T5 --> T6[Task 6: E2E Tests]
    T6 --> T7[Task 7: Documentation]
```

## Parallelization

- **Parallel:** Tasks 2 and 3 can be implemented in parallel after Task 1
- **Sequential:** Task 4 depends on Tasks 2 and 3 completion
- **Sequential:** Tasks 5, 6, 7 are sequential
