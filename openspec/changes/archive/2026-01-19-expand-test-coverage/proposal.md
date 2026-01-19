# Change: Expand Test Coverage for Maximum Local Testing Without Cloud Services

## Why

Analysis of the current test suite reveals **78 source files covered by 98 test files** with strong infrastructure including LocalStack for AWS services. However, several critical gaps exist that prevent achieving maximum test coverage without requiring AWS or OCI cloud services:

1. **`PromptTemplates`** - Zero unit tests for LLM prompt template structure; changes could break generation without detection
2. **`OciObjectStorageClient`** - Only constructor null-check tested (22 LOC); no behavior tests for `listRunbooks()` or `getRunbookContent()`
3. **`RagPipelineService`** - Only happy-path tested; no error propagation, null input, or exception handling tests
4. **`ScoredChunk`** - Record constructor validation not directly tested despite extensive use in test data
5. **Missing In-Memory OCI Adapters** - No test implementations for cloud-free OCI testing

The goal is to enable **95%+ test coverage without any cloud provider credentials**.

## What Changes

### Unit Test Additions
- **NEW** `PromptTemplatesTest.java` - Validate template structure, placeholder formatting, and consistency
- **MODIFIED** `OciObjectStorageClientTest.java` - Add mock-based tests for `listRunbooks()`, `getRunbookContent()`, 404 handling, and error propagation
- **MODIFIED** `RagPipelineServiceTest.java` - Add null input validation, enrichment service failure, retriever exceptions, and generator failure scenarios
- **NEW** `ScoredChunkTest.java` - Constructor validation for null chunk rejection

### Infrastructure Additions
- **NEW** `InMemoryCloudStorageAdapter.java` - In-memory implementation of `CloudStorageAdapter` for OCI-free testing
- **NEW** `StubMetricsSourceAdapter.java` - Stub implementation for metrics testing without OCI
- **NEW** `StubLogSourceAdapter.java` - Stub implementation for log testing without OCI

### Integration Test Enhancements
- **MODIFIED** `LocalStackContainerBase` - Add EC2 service for metadata integration tests
- **NEW** `AwsRagPipelineIT.java` - AWS-backed RAG pipeline integration test using LocalStack
- **NEW** `CloudProviderSwitchingIT.java` - Verify OCI↔AWS configuration switching

## Impact
- **Affected specs**: `integration-tests`, `rag-pipeline`
- **Affected code**: 
  - `src/test/java/com/oracle/runbook/rag/`
  - `src/test/java/com/oracle/runbook/integration/`
  - `src/main/java/com/oracle/runbook/infrastructure/cloud/` (new test implementations)
- **No breaking changes**: All additions are test-only or provide optional test implementations

## Target Audience: AI Agents

This proposal is designed for AI coding agents to implement autonomously. Key guidance:

1. **Follow TDD principles** per `test-driven-development` skill - write failing tests first
2. **Use AssertJ** for all assertions per `testing-patterns-java` skill
3. **Use `TestFixtures.loadAs()`** for test data per existing patterns
4. **Use mocks only for external dependencies** - prefer real implementations where possible
5. **Each task includes verification command** - run tests after each change

## Verification Strategy

All changes can be verified without cloud services:

```bash
# Unit tests only (no containers)
./mvnw test

# Include LocalStack-based integration tests  
./mvnw verify -Pe2e-containers

# Generate coverage report
./mvnw test jacoco:report
```
