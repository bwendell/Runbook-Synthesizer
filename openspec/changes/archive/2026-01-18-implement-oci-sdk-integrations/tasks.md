# OCI SDK Integrations Tasks

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement OCI SDK adapters for Monitoring, Logging, Compute, and Object Storage APIs.

**Architecture:** Adapter pattern wrapping OCI SDK clients, translating OCI types to domain models, with async CompletableFuture returns.

**Tech Stack:** Java 25, OCI Java SDK 3.77.2, Helidon SE 4.3.3, JUnit 5, Mockito

---

## Pre-requisites

- [x] Phase 3 (`implement-ports-interfaces`) must be complete
- [x] Interfaces available: `MetricsSourceAdapter`, `LogSourceAdapter`

---

## Task 1: Add OCI SDK Dependencies [S]

**Files:**
- Modify: `pom.xml`

**Step 1: Add OCI SDK dependencies**

Add the following dependencies to `pom.xml` within the `<dependencies>` section:
- `oci-java-sdk-monitoring` - for OCI Monitoring API
- `oci-java-sdk-loggingsearch` - for OCI Logging search API
- `oci-java-sdk-objectstorage` - for Object Storage operations
- `oci-java-sdk-core` - for Compute instance metadata

**Hint:** These are managed by the OCI SDK BOM already configured, so no `<version>` needed.

**Step 2: Verify build compiles**

Run: `wsl mvn compile -q`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add pom.xml
git commit -m "chore: add OCI SDK dependencies for monitoring, logging, object storage, compute"
```

---

## Task 2: Create OCI Configuration Model [S]

**Files:**
- Create: `src/main/java/com/oracle/runbook/config/OciConfig.java`
- Test: `src/test/java/com/oracle/runbook/config/OciConfigTest.java`

**Step 1: Write the failing test**

Test that `OciConfig` record holds configuration fields:
- `compartmentId` (required, non-null)
- `region` (optional)
- `configFilePath` (optional, defaults to `~/.oci/config`)
- `profile` (optional, defaults to `DEFAULT`)

**Hint:** Test compact constructor validation for null compartmentId.

**Step 2: Run test to verify it fails**

Run: `wsl mvn test -Dtest=OciConfigTest -q`
Expected: FAIL with "cannot find symbol: class OciConfig"

**Step 3: Write minimal implementation**

Create `OciConfig` as a Java record with:
- Compact constructor that validates `compartmentId` is not null
- Default values applied in static factory or using Optional

**Step 4: Run test to verify it passes**

Run: `wsl mvn test -Dtest=OciConfigTest -q`
Expected: BUILD SUCCESS

**Step 5: Commit**

```bash
git add src/main/java/com/oracle/runbook/config/OciConfig.java src/test/java/com/oracle/runbook/config/OciConfigTest.java
git commit -m "feat(config): add OciConfig record for OCI authentication settings"
```

---

## Task 3: Create OCI Authentication Provider Factory [M]

**Files:**
- Create: `src/main/java/com/oracle/runbook/config/OciAuthProviderFactory.java`
- Test: `src/test/java/com/oracle/runbook/config/OciAuthProviderFactoryTest.java`

**Step 1: Write the failing test**

Test that `OciAuthProviderFactory.create(OciConfig)`:
- Returns an `AuthenticationDetailsProvider` (OCI SDK interface)
- Uses `ConfigFileAuthenticationDetailsProvider` when config file exists
- Throws meaningful exception when authentication fails

**Hint:** Mock the file system check or use a test config file in `src/test/resources/`.

**Step 2: Run test to verify it fails**

Run: `wsl mvn test -Dtest=OciAuthProviderFactoryTest -q`
Expected: FAIL with "cannot find symbol"

**Step 3: Write minimal implementation**

Create factory that:
1. Checks if running on OCI (instance principals)
2. Falls back to config file authentication
3. Uses environment variables as final fallback
4. Returns `BasicAuthenticationDetailsProvider` or throws

**Hint:** Use `com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider`.

**Step 4: Run test to verify it passes**

Run: `wsl mvn test -Dtest=OciAuthProviderFactoryTest -q`
Expected: BUILD SUCCESS

**Step 5: Commit**

```bash
git add src/main/java/com/oracle/runbook/config/OciAuthProviderFactory.java src/test/java/com/oracle/runbook/config/OciAuthProviderFactoryTest.java
git commit -m "feat(config): add OciAuthProviderFactory for OCI authentication chain"
```

---

## Task 4: Implement OciMonitoringAdapter [L]

**Files:**
- Create: `src/main/java/com/oracle/runbook/enrichment/OciMonitoringAdapter.java`
- Test: `src/test/java/com/oracle/runbook/enrichment/OciMonitoringAdapterTest.java`

**Step 1: Write failing test for sourceType()**

Test that `sourceType()` returns `"oci-monitoring"`.

**Step 2: Run test, verify failure**

Run: `wsl mvn test -Dtest=OciMonitoringAdapterTest#testSourceType -q`
Expected: FAIL

**Step 3: Implement sourceType() method**

Create class implementing `MetricsSourceAdapter` with `sourceType()` returning the string literal.

**Step 4: Run test, verify pass**

Run: `wsl mvn test -Dtest=OciMonitoringAdapterTest#testSourceType -q`
Expected: PASS

**Step 5: Write failing test for fetchMetrics()**

Test that `fetchMetrics(resourceId, lookback)`:
- Calls the OCI Monitoring SDK `SummarizeMetricsData` API
- Converts `MetricData` results to `List<MetricSnapshot>`
- Uses mock `MonitoringClient` to verify correct query construction

**Hint:** Verify the MQL query includes the resourceId and time range.

**Step 6: Run test, verify failure**

Run: `wsl mvn test -Dtest=OciMonitoringAdapterTest#testFetchMetrics* -q`
Expected: FAIL

**Step 7: Implement fetchMetrics()**

Implement using:
- `MonitoringClient.summarizeMetricsData()` 
- Build `SummarizeMetricsDataDetails` with:
  - `namespace`: `oci_computeagent`
  - `query`: MQL filtering by resourceId
  - `startTime` and `endTime` based on lookback Duration
- Map `MetricData` → `MetricSnapshot` domain model

**Hint:** Return `CompletableFuture.supplyAsync()` for async behavior.

**Step 8: Run test, verify pass**

Run: `wsl mvn test -Dtest=OciMonitoringAdapterTest -q`
Expected: BUILD SUCCESS

**Step 9: Commit**

```bash
git add src/main/java/com/oracle/runbook/enrichment/OciMonitoringAdapter.java src/test/java/com/oracle/runbook/enrichment/OciMonitoringAdapterTest.java
git commit -m "feat(enrichment): implement OciMonitoringAdapter for OCI Monitoring API"
```

---

## Task 5: Implement OciLoggingAdapter [L]

**Files:**
- Create: `src/main/java/com/oracle/runbook/enrichment/OciLoggingAdapter.java`
- Test: `src/test/java/com/oracle/runbook/enrichment/OciLoggingAdapterTest.java`

**Step 1: Write failing test for sourceType()**

Test that `sourceType()` returns `"oci-logging"`.

**Step 2: Run test, verify failure**

Run: `wsl mvn test -Dtest=OciLoggingAdapterTest#testSourceType -q`
Expected: FAIL

**Step 3: Implement sourceType()**

**Step 4: Run test, verify pass**

**Step 5: Write failing test for fetchLogs()**

Test that `fetchLogs(resourceId, lookback, query)`:
- Calls `LogSearchClient.searchLogs()` API
- Builds correct search query combining resourceId filter with user query
- Converts `SearchResult` items to `List<LogEntry>`

**Hint:** OCI Logging uses a query language similar to SQL. Test query construction.

**Step 6: Run test, verify failure**

Run: `wsl mvn test -Dtest=OciLoggingAdapterTest#testFetchLogs* -q`
Expected: FAIL

**Step 7: Implement fetchLogs()**

Implement using:
- `LogSearchClient.searchLogs()`
- Build `SearchLogsDetails` with:
  - `searchQuery`: Combine resourceId and user query
  - `timeStart` and `timeEnd` from lookback
  - `isReturnFieldInfo`: false
- Map results to `LogEntry` domain model

**Hint:** Handle pagination if result set is large (use `limit` parameter).

**Step 8: Run test, verify pass**

Run: `wsl mvn test -Dtest=OciLoggingAdapterTest -q`
Expected: BUILD SUCCESS

**Step 9: Commit**

```bash
git add src/main/java/com/oracle/runbook/enrichment/OciLoggingAdapter.java src/test/java/com/oracle/runbook/enrichment/OciLoggingAdapterTest.java
git commit -m "feat(enrichment): implement OciLoggingAdapter for OCI Logging search API"
```

---

## Task 6: Implement OciComputeClient [M]

**Files:**
- Create: `src/main/java/com/oracle/runbook/enrichment/OciComputeClient.java`
- Test: `src/test/java/com/oracle/runbook/enrichment/OciComputeClientTest.java`

**Step 1: Write failing test for getInstance()**

Test that `getInstance(instanceOcid)`:
- Calls `ComputeClient.getInstance()` API
- Converts `Instance` response to `ResourceMetadata` domain model
- Handles instance not found gracefully (returns Optional.empty)

**Step 2: Run test, verify failure**

Run: `wsl mvn test -Dtest=OciComputeClientTest#testGetInstance* -q`
Expected: FAIL

**Step 3: Implement getInstance()**

Implement using:
- `ComputeClient.getInstance(GetInstanceRequest)`
- Map `Instance` fields to `ResourceMetadata`:
  - `id` → `ocid`
  - `displayName` → `displayName`
  - `compartmentId` → `compartmentId`
  - `shape` → `shape`
  - `availabilityDomain` → `availabilityDomain`
  - `freeformTags` → `freeformTags`
  - `definedTags` → flatten to `definedTags`

**Hint:** `definedTags` in OCI SDK is `Map<String, Map<String, Object>>` - flatten to `Map<String, String>`.

**Step 4: Run test, verify pass**

Run: `wsl mvn test -Dtest=OciComputeClientTest -q`
Expected: BUILD SUCCESS

**Step 5: Commit**

```bash
git add src/main/java/com/oracle/runbook/enrichment/OciComputeClient.java src/test/java/com/oracle/runbook/enrichment/OciComputeClientTest.java
git commit -m "feat(enrichment): implement OciComputeClient for instance metadata retrieval"
```

---

## Task 7: Implement OciObjectStorageClient [M]

**Files:**
- Create: `src/main/java/com/oracle/runbook/rag/OciObjectStorageClient.java`
- Test: `src/test/java/com/oracle/runbook/rag/OciObjectStorageClientTest.java`

**Step 1: Write failing test for listRunbooks()**

Test that `listRunbooks(namespace, bucketName)`:
- Calls `ObjectStorageClient.listObjects()` API
- Returns list of object names (markdown files)
- Filters to `.md` files only

**Step 2: Run test, verify failure**

Run: `wsl mvn test -Dtest=OciObjectStorageClientTest#testListRunbooks* -q`
Expected: FAIL

**Step 3: Implement listRunbooks()**

Implement using:
- `ObjectStorageClient.listObjects(ListObjectsRequest)`
- Filter by prefix if provided
- Return stream of object names ending in `.md`

**Step 4: Run test, verify pass**

**Step 5: Write failing test for getRunbookContent()**

Test that `getRunbookContent(namespace, bucketName, objectName)`:
- Calls `ObjectStorageClient.getObject()` API
- Returns the markdown content as String
- Handles object not found with Optional.empty

**Step 6: Run test, verify failure**

**Step 7: Implement getRunbookContent()**

Implement using:
- `ObjectStorageClient.getObject(GetObjectRequest)`
- Read `InputStream` from response
- Convert to String using UTF-8

**Hint:** Use `try-with-resources` for proper stream handling.

**Step 8: Run test, verify pass**

Run: `wsl mvn test -Dtest=OciObjectStorageClientTest -q`
Expected: BUILD SUCCESS

**Step 9: Commit**

```bash
git add src/main/java/com/oracle/runbook/rag/OciObjectStorageClient.java src/test/java/com/oracle/runbook/rag/OciObjectStorageClientTest.java
git commit -m "feat(rag): implement OciObjectStorageClient for runbook bucket operations"
```

---

## Task 8: Create Integration Test Fixtures [S]

**Files:**
- Create: `src/test/resources/oci/mock-monitoring-response.json`
- Create: `src/test/resources/oci/mock-logging-response.json`
- Create: `src/test/resources/oci/mock-compute-response.json`

**Step 1: Create realistic mock response files**

Create JSON files matching OCI API response structures for:
- Monitoring: `SummarizeMetricsDataResponse` with sample metric data points
- Logging: `SearchLogsResponse` with sample log entries
- Compute: `GetInstanceResponse` with sample instance metadata

**Hint:** Reference OCI SDK documentation for response schemas.

**Step 2: Verify resources are accessible in tests**

Run: `wsl mvn test -Dtest=OciMonitoringAdapterTest -q`
Expected: BUILD SUCCESS (tests use the mock data)

**Step 3: Commit**

```bash
git add src/test/resources/oci/
git commit -m "test: add OCI SDK mock response fixtures for adapter tests"
```

---

## Task 9: Add Package Documentation [S]

**Files:**
- Modify: `src/main/java/com/oracle/runbook/enrichment/package-info.java`
- Modify: `src/main/java/com/oracle/runbook/config/package-info.java`

**Step 1: Update package-info.java files**

Add Javadoc explaining:
- The enrichment package now contains OCI SDK adapters
- The config package contains OCI authentication configuration

**Step 2: Run Javadoc generation (optional)**

Run: `wsl mvn javadoc:javadoc -q`
Expected: No errors

**Step 3: Commit**

```bash
git add src/main/java/com/oracle/runbook/enrichment/package-info.java src/main/java/com/oracle/runbook/config/package-info.java
git commit -m "docs: update package documentation for OCI SDK adapters"
```

---

## Verification Checklist

After completing all tasks:

- [x] All unit tests pass: `wsl mvn test -q`
- [x] Build succeeds: `wsl mvn package -DskipTests -q`
- [x] No compiler warnings: `wsl mvn compile -Xlint:all`
- [x] OCI SDK dependencies resolve correctly
- [x] Each adapter implements its corresponding interface
- [x] All async methods return CompletableFuture
- [x] Mock responses cover happy path and error cases
