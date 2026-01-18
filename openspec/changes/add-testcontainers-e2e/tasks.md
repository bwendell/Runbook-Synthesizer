# Tasks: Testcontainers E2E Testing Infrastructure

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add Docker-based E2E testing infrastructure using Testcontainers with real Oracle 23ai and Ollama containers.

**Prerequisites:** Options A (HTTP API tests) and B (AlertResource → RAG wiring) are complete.

**Tech Stack:** Testcontainers, Oracle Database 23ai, Ollama, JUnit 5 `@Tag`

---

## 1. Infrastructure Setup

### Task 1.1: Add Testcontainers Dependencies [S]

**Files:**
- Modify: `pom.xml`

**Step 1: Add dependencies**
```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.20.4</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>1.20.4</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>oracle-free</artifactId>
    <version>1.20.4</version>
    <scope>test</scope>
</dependency>
```

**Step 2: Add Ollama container (generic container)**
- Testcontainers doesn't have a native Ollama module; use `GenericContainer`

**Step 3: Verify compilation**
- Run: `.\mvnw.cmd test-compile --batch-mode`
- Expected: BUILD SUCCESS

**Step 4: Commit**
```bash
git add pom.xml
git commit -m "build: add Testcontainers dependencies for E2E testing"
```

---

### Task 1.2: Add Container Test Profile [S]

**Files:**
- Modify: `pom.xml`

**Step 1: Add Maven profile for container tests**
```xml
<profile>
    <id>e2e-containers</id>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-failsafe-plugin</artifactId>
                <configuration>
                    <groups>container</groups>
                </configuration>
            </plugin>
        </plugins>
    </build>
</profile>
```

**Step 2: Configure default to exclude container tests**
- Update Failsafe plugin default config to exclude `container` tag

**Step 3: Verify profiles**
- Run: `.\mvnw.cmd help:active-profiles -Pe2e-containers`
- Expected: Profile listed

**Step 4: Commit**
```bash
git add pom.xml
git commit -m "build: add Maven profile for container E2E tests"
```

---

### Task 1.3: Create ContainerTestBase [M]

**Files:**
- Create: `src/test/java/com/oracle/runbook/integration/containers/ContainerTestBase.java`

**Step 1: Write base class**
```java
@Testcontainers
@Tag("container")
public abstract class ContainerTestBase {
    
    // Start containers in parallel using static declaration
    @Container
    protected static final OracleContainer oracle = new OracleContainer("gvenzl/oracle-free:23-slim")
        .withNetwork(Network.newNetwork())
        .withNetworkAliases("oracle")
        .withReuse(true); // Optimize local runs
    
    @Container
    protected static final GenericContainer<?> ollama = new GenericContainer<>("ollama/ollama:latest")
        .withNetwork(oracle.getNetwork())
        .withNetworkAliases("ollama")
        .withExposedPorts(11434)
        .waitingFor(Wait.forHttp("/api/tags").forPort(11434));
    
    @BeforeAll
    static void setup() {
        // Parallel startup is handled by @Testcontainers for static fields
    }
}
```

**Step 2: Add Docker availability check**
- Add `@EnabledIf("isDockerAvailable")` condition using `Testcontainers.createDockerClientStrategy()`

**Step 3: Verify**
- Run with Docker: Containers should start in parallel (check logs)

**Step 4: Commit**
```bash
git add src/test/java/com/oracle/runbook/integration/containers/
git commit -m "test: add ContainerTestBase with parallel Oracle and Ollama containers"
```

---

### Task 1.4: Validate Centralized Oracle Schema [S]

> **Dependency**: Requires `automate-test-infrastructure` Task 2.3 (centralized schema) to be complete.

**Files:**
- Reference: `src/test/resources/schema/oracle-vector-schema.sql` (created by `automate-test-infrastructure`)

**Step 1: Validate schema compatibility**
- Verify the centralized schema includes all columns required by E2E tests:
  - `id`, `runbook_path`, `section_title`, `content`, `embedding VECTOR(768, FLOAT32)`, `tags`, `applicable_shapes`, `created_at`
- Verify vector index exists with COSINE distance metric

**Step 2: Configure container to use centralized init script**
- Use `oracle.withInitScript("schema/oracle-vector-schema.sql")` (NOT `containers/oracle-init.sql`)

**Step 3: Commit**

```bash
git commit -m "test: configure E2E containers to use centralized Oracle schema"
```

---

### Task 1.5: Implement Scenario Seeder and Factories [M]

**Files:**

- Create: `src/test/java/com/oracle/runbook/integration/containers/RunbookSeeder.java`
- Create: `src/test/java/com/oracle/runbook/integration/containers/E2EDataFactory.java`

**Step 1: Implement RunbookSeeder**

- Method `seedRunbook(String path)`: Loads fixture and inserts into real Oracle container
- Hint: Use JDBC `PreparedStatement` with vector storage

**Step 2: Implement E2EDataFactory**

- Method `createTestAlert()`: Returns `Alert` record with sensible defaults
- Follow `testing-patterns` by supporting property overrides

**Step 3: Verify**

- Run a simple unit test for the seeder (against a mock DB if needed, or wait for Task 2.1)

**Step 4: Commit**

```bash
git add src/test/java/com/oracle/runbook/integration/containers/
git commit -m "test: implement Scenario Seeder and Data Factories for E2E tests"
```

---

## 2. Oracle Vector Store Integration

### Task 2.1: Create OracleVectorStoreContainerIT [M]

**Files:**
- Create: `src/test/java/com/oracle/runbook/integration/containers/OracleVectorStoreContainerIT.java`

**Step 1: Write failing test**
```java
@Test
void storeAndRetrieve_WithRealVectorSimilarity_ReturnsTopMatches() {
    // Given: Store 10 chunks with real embeddings
    // When: Query with similar embedding
    // Then: Top-K results have cosine similarity > 0.8
}
```

**Step 2: Run to verify failure**
- Expected: FAIL - implementation not connected

**Step 3: Implement Oracle-backed VectorStoreRepository**
- Create `OracleVectorStore` implementing `VectorStoreRepository`
- Use JDBC with Oracle vector operators

**Step 4: Run to verify pass**

**Step 5: Commit**
```bash
git add src/
git commit -m "test: add OracleVectorStoreContainerIT with real vector search"
```

---

### Task 2.2: Oracle Vector Search Quality Test [M]

**Files:**
- Modify: `src/test/java/com/oracle/runbook/integration/containers/OracleVectorStoreContainerIT.java`

**Step 1: Write semantic similarity test**
```java
@Test
void search_WithSemanticallySimilarQuery_ReturnsSameTopicChunks() {
    // Given: Chunks about "memory", "CPU", "network"
    // When: Query with "RAM usage troubleshooting"
    // Then: Memory-related chunks ranked highest
}
```

**Step 2: Use real Ollama embeddings**
- Embed query and chunks using Ollama API

**Step 3: Verify semantic ranking**

**Step 4: Commit**
```bash
git add src/
git commit -m "test: add semantic similarity test for Oracle vector search"
```

---

## 3. Ollama LLM Integration

### Task 3.1: Create OllamaEmbeddingContainerIT [M]

**Files:**
- Create: `src/test/java/com/oracle/runbook/integration/containers/OllamaEmbeddingContainerIT.java`

**Step 1: Write failing test**
```java
@Test
void embed_WithNomicModel_Returns768Dimensions() {
    // Given: Text input
    // When: Call Ollama embedding API
    // Then: Returns float[768]
}
```

**Step 2: Implement OllamaEmbeddingService**
- Call `/api/embeddings` endpoint on Ollama container
- Parse response into float array

**Step 3: Verify dimensions match model spec

**Step 4: Commit**
```bash
git add src/
git commit -m "test: add OllamaEmbeddingContainerIT with real embeddings"
```

---

### Task 3.2: Create OllamaGenerationContainerIT [M]

**Files:**
- Create: `src/test/java/com/oracle/runbook/integration/containers/OllamaGenerationContainerIT.java`

**Step 1: Write failing test**
```java
@Test
void generateText_WithChecklistPrompt_ReturnsStructuredSteps() {
    // Given: Prompt with context and runbook chunks
    // When: Call Ollama generation
    // Then: Response contains Step 1, Step 2, etc.
}
```

**Step 2: Implement OllamaLlmProvider**
- Call `/api/generate` endpoint
- Parse streaming response

**Step 3: Verify structured output

**Step 4: Commit**
```bash
git add src/
git commit -m "test: add OllamaGenerationContainerIT with real LLM inference"
```

---

## 4. Full-Stack E2E Tests

### Task 4.1: Create FullStackAlertIT [L]

**Files:**
- Create: `src/test/java/com/oracle/runbook/integration/containers/FullStackAlertIT.java`

**Step 1: Write failing test**
```java
@Test
void postAlert_WithRealComponents_ReturnsGeneratedChecklist() {
    // Given: Oracle seeded with runbook chunks
    // Given: Ollama with models loaded
    // When: POST /api/v1/alerts with memory alert
    // Then: Response contains LLM-generated steps
    // Then: Steps reference seeded runbook content
}
```

**Step 2: Configure all real components**
- Real Helidon server
- Real Oracle vector store
- Real Ollama LLM

**Step 3: Verify end-to-end flow

**Step 4: Commit**
```bash
git add src/
git commit -m "test: add FullStackAlertIT with all real components"
```

---

### Task 4.2: Create FullStackWebhookIT [M]

**Files:**
- Create: `src/test/java/com/oracle/runbook/integration/containers/FullStackWebhookIT.java`

**Step 1: Write failing test**
```java
@Test
void postAlert_WithWebhookConfigured_DispatchesToWireMock() {
    // Given: Full stack with real LLM
    // Given: WireMock webhook endpoint
    // When: POST alert
    // Then: WireMock receives LLM-generated checklist
}
```

**Step 2: Combine containers with WireMock**
- Containers for Oracle + Ollama
- WireMock for webhook capture (not containerized)

**Step 3: Verify webhook payload

**Step 4: Commit**
```bash
git add src/
git commit -m "test: add FullStackWebhookIT combining containers and WireMock"
```

---

## 5. Documentation and CI

### Task 5.1: Update Testing Documentation [S]

**Files:**
- Create: `docs/E2E_TESTING_GUIDELINES.md`
- Modify: `README.md`

**Step 1: Document container test requirements**
- Docker installation
- Required disk space for images
- First-run image download time

**Step 2: Document test commands**
```bash
# Run unit + WireMock integration tests (fast)
.\mvnw.cmd verify

# Run container E2E tests (requires Docker)
.\mvnw.cmd verify -Pe2e-containers
```

**Step 3: Commit**
```bash
git add docs/ README.md
git commit -m "docs: add E2E testing guidelines with Testcontainers"
```

---

### Task 5.2: Add CI Container Test Stage [S]

**Files:**
- Modify: `.github/workflows/ci.yml` (if exists) or document manual CI setup

**Step 1: Add container test job**
```yaml
container-tests:
  runs-on: ubuntu-latest
  services:
    docker:
      image: docker:dind
  steps:
    - uses: actions/checkout@v4
    - name: Run container tests
      run: ./mvnw verify -Pe2e-containers --batch-mode
```

**Step 2: Configure parallel execution with unit tests

**Step 3: Commit**
```bash
git add .github/
git commit -m "ci: add container E2E test stage"
```

---

## Summary

| Task | Complexity | Focus Area |
| :--- | :--- | :--- |
| 1.1-1.5 | S-S-M-S-M | Infrastructure |
| 2.1-2.2 | M-M | Oracle Vector Store |
| 3.1-3.2 | M-M | Ollama LLM |
| 4.1-4.2 | L-M | Full Stack |
| 5.1-5.2 | S-S | Documentation |

**Total: 13 tasks** (5 Small, 6 Medium, 2 Large)

---

## Test Commands Reference

```powershell
# Standard tests (fast, no Docker)
.\mvnw.cmd verify --batch-mode

# Container tests only (requires Docker)
.\mvnw.cmd failsafe:integration-test -Pe2e-containers --batch-mode

# All tests including containers
.\mvnw.cmd verify -Pe2e-containers --batch-mode

# Specific container test
.\mvnw.cmd verify -Pe2e-containers -Dit.test=OracleVectorStoreContainerIT
```
