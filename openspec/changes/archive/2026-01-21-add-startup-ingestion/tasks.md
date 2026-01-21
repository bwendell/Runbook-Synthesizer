# Tasks: Add Startup Runbook Ingestion

## 1. Add Configuration for Runbook Ingestion (TDD)

- [x] **1.1 RED: Write failing unit test for runbook configuration**
  
  **Acceptance Criteria:**
  - Create `RunbookConfigTest.java` in `src/test/java/com/oracle/runbook/config/`
  - Test `shouldLoadDefaultBucketName_WhenNotConfigured()` asserts `runbook-synthesizer-runbooks`
  - Test `shouldLoadDefaultIngestOnStartup_WhenNotConfigured()` asserts `true`
  - Test `shouldLoadCustomBucketName_WhenConfigured()` asserts custom value
  - Test `shouldLoadIngestOnStartup_False_WhenConfigured()` asserts `false`
  - Tests fail with `NoSuchElementException` or incorrect values (method not implemented)
  
  **Test Pattern:**
  ```java
  @Test
  @DisplayName("Should load default bucket name when not configured")
  void shouldLoadDefaultBucketName_WhenNotConfigured() {
    Config config = Config.builder()
        .sources(ConfigSources.create(Map.of()))
        .build();
    
    RunbookConfig runbookConfig = new RunbookConfig(config);
    
    assertThat(runbookConfig.bucket()).isEqualTo("runbook-synthesizer-runbooks");
  }
  ```
  
  **Verification:**
  ```powershell
  mvn test -Dtest=RunbookConfigTest
  # Expected: Tests FAIL because RunbookConfig class does not exist
  ```

- [x] **1.2 GREEN: Create RunbookConfig class and update application.yaml**
  
  **Acceptance Criteria:**
  - Create `RunbookConfig.java` record in `src/main/java/com/oracle/runbook/config/`
  - Record has fields: `bucket()`, `ingestOnStartup()`
  - Constructor reads from Helidon Config with proper defaults
  - Add `runbooks.bucket` and `runbooks.ingestOnStartup` to `application.yaml`
  - All `RunbookConfigTest` tests pass
  
  **Implementation Pattern:**
  ```java
  public record RunbookConfig(String bucket, boolean ingestOnStartup) {
    public RunbookConfig(Config config) {
      this(
        config.get("runbooks.bucket").asString()
            .orElse("runbook-synthesizer-runbooks"),
        config.get("runbooks.ingestOnStartup").asBoolean().orElse(true)
      );
    }
  }
  ```
  
  **Verification:**
  ```powershell
  mvn test -Dtest=RunbookConfigTest
  # Expected: All tests PASS
  ```

- [x] **1.3 REFACTOR: Clean up and document**
  
  **Acceptance Criteria:**
  - Javadoc added to `RunbookConfig`
  - Configuration comments in `application.yaml`
  - Tests remain green after refactoring
  
  **Verification:**
  ```powershell
  mvn test -Dtest=RunbookConfigTest
  # Expected: All tests PASS
  ```

---

## 2. Extend ServiceFactory with Ingestion Support (TDD)

### 2.A. Create CloudStorageAdapter Factory Method

- [x] **2.1 RED: Write failing test for `createCloudStorageAdapter()` return type**
  
  **Acceptance Criteria:**
  - Add test to `ServiceFactoryTest.java` in `HappyPathTests` nested class
  - Test `shouldCreateCloudStorageAdapter_WhenConfigured()` asserts non-null
  - Test `shouldCreateAwsS3StorageAdapter_WhenProviderIsAws()` asserts correct type
  - Use AssertJ fluent assertions per `testing-patterns-java` skill
  - Tests FAIL with compilation error (method does not exist)
  
  **Test Pattern:**
  ```java
  @Test
  @DisplayName("Should create CloudStorageAdapter when configured")
  void shouldCreateCloudStorageAdapter_WhenConfigured() {
    Config config = createValidConfig();
    ServiceFactory factory = new ServiceFactory(config);
    
    CloudStorageAdapter adapter = factory.createCloudStorageAdapter();
    
    assertThat(adapter).isNotNull();
  }
  
  @Test
  @DisplayName("Should create AwsS3StorageAdapter when provider is AWS")
  void shouldCreateAwsS3StorageAdapter_WhenProviderIsAws() {
    Config config = createValidConfig();
    ServiceFactory factory = new ServiceFactory(config);
    
    CloudStorageAdapter adapter = factory.createCloudStorageAdapter();
    
    assertThat(adapter).isInstanceOf(AwsS3StorageAdapter.class);
  }
  ```
  
  **Verification:**
  ```powershell
  mvn test -Dtest=ServiceFactoryTest
  # Expected: Compilation error - method createCloudStorageAdapter() does not exist
  ```

- [x] **2.2 GREEN: Implement `createCloudStorageAdapter()` in ServiceFactory**
  
  **Acceptance Criteria:**
  - Add `private CloudStorageAdapter cachedStorageAdapter;` field
  - Add public `createCloudStorageAdapter()` method
  - Method reads `cloud.provider` config (default: `aws`)
  - AWS provider creates `AwsS3StorageAdapter` with S3AsyncClient
  - Method caches result for reuse
  - Tests now pass
  
  **Implementation Pattern:**
  ```java
  public CloudStorageAdapter createCloudStorageAdapter() {
    if (cachedStorageAdapter != null) {
      return cachedStorageAdapter;
    }
    
    String provider = cloudAdapterFactory.getProviderType();
    if ("aws".equals(provider)) {
      S3AsyncClient s3Client = S3AsyncClient.builder()
          .region(Region.of(getAwsRegion()))
          .build();
      cachedStorageAdapter = new AwsS3StorageAdapter(s3Client);
    } else {
      throw new IllegalStateException("Unsupported cloud provider: " + provider);
    }
    
    LOGGER.info("Created CloudStorageAdapter: " + provider);
    return cachedStorageAdapter;
  }
  ```
  
  **Verification:**
  ```powershell
  mvn test -Dtest=ServiceFactoryTest
  # Expected: All tests PASS
  ```

- [x] **2.3 RED: Write failing test for adapter caching behavior**
  
  **Acceptance Criteria:**
  - Test `shouldCacheCloudStorageAdapter_OnSubsequentCalls()` asserts same instance
  - Test uses `assertThat(adapter1).isSameAs(adapter2)` for identity check
  - Test FAILS because caching is not yet verified
  
  **Verification:**
  ```powershell
  mvn test -Dtest=ServiceFactoryTest
  # Expected: Test PASSES if caching implemented correctly in 2.2
  ```

### 2.B. Create RunbookChunker Factory Method

- [x] **2.4 RED: Write failing test for `createRunbookChunker()`**
  
  **Acceptance Criteria:**
  - Add test `shouldCreateRunbookChunker_WhenCalled()` to `ServiceFactoryTest.java`
  - Test asserts `createRunbookChunker()` returns non-null `RunbookChunker`
  - Test FAILS with compilation error (method does not exist)
  
  **Test Pattern:**
  ```java
  @Test
  @DisplayName("Should create RunbookChunker when called")
  void shouldCreateRunbookChunker_WhenCalled() {
    Config config = createValidConfig();
    ServiceFactory factory = new ServiceFactory(config);
    
    RunbookChunker chunker = factory.createRunbookChunker();
    
    assertThat(chunker).isNotNull();
  }
  ```
  
  **Verification:**
  ```powershell
  mvn test -Dtest=ServiceFactoryTest
  # Expected: Compilation error - method createRunbookChunker() does not exist
  ```

- [x] **2.5 GREEN: Implement `createRunbookChunker()` in ServiceFactory**
  
  **Acceptance Criteria:**
  - Add public `createRunbookChunker()` method
  - Method creates and returns new `RunbookChunker` instance
  - No caching needed (chunker is stateless)
  - Tests pass
  
  **Verification:**
  ```powershell
  mvn test -Dtest=ServiceFactoryTest
  # Expected: All tests PASS
  ```

### 2.C. Create RunbookIngestionService Factory Method

- [x] **2.6 RED: Write failing tests for `createRunbookIngestionService()`**
  
  **Acceptance Criteria:**
  - Add test `shouldCreateRunbookIngestionService_WhenCalled()` to `ServiceFactoryTest.java`
  - Test asserts non-null `RunbookIngestionService` returned
  - Add test `shouldCacheRunbookIngestionService_OnSubsequentCalls()`
  - Tests FAIL with compilation error (method does not exist)
  
  **Test Pattern:**
  ```java
  @Test
  @DisplayName("Should create RunbookIngestionService when called")
  void shouldCreateRunbookIngestionService_WhenCalled() {
    Config config = createValidConfig();
    ServiceFactory factory = new ServiceFactory(config);
    
    RunbookIngestionService service = factory.createRunbookIngestionService();
    
    assertThat(service).isNotNull();
  }
  
  @Test
  @DisplayName("Should cache RunbookIngestionService on subsequent calls")
  void shouldCacheRunbookIngestionService_OnSubsequentCalls() {
    Config config = createValidConfig();
    ServiceFactory factory = new ServiceFactory(config);
    
    RunbookIngestionService service1 = factory.createRunbookIngestionService();
    RunbookIngestionService service2 = factory.createRunbookIngestionService();
    
    assertThat(service1).isSameAs(service2);
  }
  ```
  
  **Verification:**
  ```powershell
  mvn test -Dtest=ServiceFactoryTest
  # Expected: Compilation error - method createRunbookIngestionService() does not exist
  ```

- [x] **2.7 GREEN: Implement `createRunbookIngestionService()` in ServiceFactory**
  
  **Acceptance Criteria:**
  - Add `private RunbookIngestionService cachedIngestionService;` field
  - Add public `createRunbookIngestionService()` method
  - Method wires: `createCloudStorageAdapter()`, `createRunbookChunker()`, `createEmbeddingService()`, `createVectorStoreRepository()`
  - Method caches result for reuse
  - All tests pass
  
  **Implementation Pattern:**
  ```java
  public RunbookIngestionService createRunbookIngestionService() {
    if (cachedIngestionService != null) {
      return cachedIngestionService;
    }
    
    cachedIngestionService = new RunbookIngestionService(
        createCloudStorageAdapter(),
        createRunbookChunker(),
        createEmbeddingService(),
        createVectorStoreRepository()
    );
    LOGGER.info("Created RunbookIngestionService");
    return cachedIngestionService;
  }
  ```
  
  **Verification:**
  ```powershell
  mvn test -Dtest=ServiceFactoryTest
  # Expected: All tests PASS
  ```

- [x] **2.8 REFACTOR: Add createRunbookConfig() and integrate**
  
  **Acceptance Criteria:**
  - Add `createRunbookConfig()` method to ServiceFactory
  - Returns cached `RunbookConfig` instance
  - Tests remain green after refactoring
  - Run full test suite to check for regressions
  
  **Verification:**
  ```powershell
  mvn test -Dtest=ServiceFactoryTest
  mvn test
  # Expected: All tests PASS
  ```

---

## 3. Add Startup Ingestion to Application (TDD)

- [x] **3.1 RED: Write failing integration test for startup ingestion**
  
  **Acceptance Criteria:**
  - `StartupIngestionIT` test exists in `integration/` package
  - Test uploads runbooks to LocalStack S3
  - Test creates `ServiceFactory` with ingestion config enabled
  - Test calls `createRunbookIngestionService().ingestAll(bucket)`
  - Test verifies chunks exist in vector store after ingestion
  - Test FAILS because application doesn't call ingestion at startup
  
  **Verification:**
  - Run `mvn failsafe:integration-test -Dit.test=StartupIngestionIT` and verify failure

- [x] **3.2 GREEN: Implement startup ingestion in RunbookSynthesizerApp**
  
  **Acceptance Criteria:**
  - When `stubMode=false` and `runbooks.ingestOnStartup=true`, app calls ingestion
  - Ingestion logs bucket name and chunk count
  - Graceful error handling if ingestion fails (app continues with warning)
  - Integration test passes
  
  **Verification:**
  - Run `mvn failsafe:integration-test -Dit.test=StartupIngestionIT`

---

## 4. Create True E2E Ingestion Tests (TDD)

- [x] **4.1 RED: Write `RunbookIngestionE2EIT` for S3-to-vector-store flow**
  
  **Acceptance Criteria:**
  - Test extends `LocalStackContainerBase`
  - Test uploads markdown runbooks to S3 bucket
  - Test invokes `RunbookIngestionService.ingestAll(bucket)`
  - Test verifies correct number of chunks stored
  - Test verifies chunks are searchable via `VectorStoreRepository.search()`
  - Test FAILS initially
  
  **Verification:**
  - Run `mvn failsafe:integration-test -Dit.test=RunbookIngestionE2EIT`

- [x] **4.2 GREEN: Verify ingestion pipeline works end-to-end**
  
  **Acceptance Criteria:**
  - Ingestion successfully fetches from S3
  - Chunker parses markdown into chunks
  - Embedding service generates embeddings
  - Vector store stores and indexes chunks
  - Search returns semantically similar chunks
  - All tests pass
  
  **Verification:**
  - Run `mvn failsafe:integration-test -Dit.test=RunbookIngestionE2EIT`

---

## 5. Refactor FullPipelineE2EIT to Use True Ingestion (TDD)

- [x] **5.1 Refactor PipelineTestHarness to support S3 ingestion**
  
  **Acceptance Criteria:**
  - Add `ingestRunbooksFromS3(bucket)` method to `PipelineTestHarness`
  - Method calls `RunbookIngestionService.ingestAll()` instead of manual seeding
  - Builder supports LocalStack container configuration
  
  **Verification:**
  - Existing tests still compile

- [x] **5.2 Update FullPipelineE2EIT to use real ingestion**
  
  **Acceptance Criteria:**
  - `@BeforeEach` uploads runbooks to LocalStack S3 (not manual seeding)
  - `harness.ingestRunbooksFromS3(bucket)` replaces `harness.seedRunbooks(...)`
  - All existing test assertions still pass
  - Tests are now TRUE end-to-end tests
  
  **Verification:**
  - Run `mvn failsafe:integration-test -Dit.test=FullPipelineE2EIT`
  - All 11 tests pass

---

## 6. Final Validation

- [x] **6.1 Run full test suite**
  
  **Acceptance Criteria:**
  - `mvn test` passes (unit tests)
  - `mvn failsafe:integration-test` passes (integration tests)
  - No regressions in existing functionality
  
  **Verification:**
  - Run full build: `mvn verify`

- [x] **6.2 Manual verification with demo script**
  
  **Acceptance Criteria:**
  - Start app with `mvn exec:java`
  - Look for startup logs: `Ingesting runbooks from S3 bucket: ...`
  - Run `demo-e2e-pipeline.ps1`
  - Response includes relevant runbook sections (not "No specific runbook sections found")
  
  **Verification:**
  - Terminal output shows runbook sections in LLM response
