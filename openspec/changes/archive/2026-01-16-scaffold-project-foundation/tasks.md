# Tasks: Scaffold Project Foundation

**Goal:** Create a working Helidon SE project skeleton with Maven, Java 25, and correct directory structure.

**Architecture:** Helidon SE 4.x microframework with hexagonal package structure. All dependencies managed via BOMs (Helidon, OCI SDK). TDD-first with JUnit 5.

**Tech Stack:** Java 25, Helidon SE 4.x, Maven 3.9+, JUnit 5, Mockito

---

## Task 1: Create Maven pom.xml with Dependency Management [M]

**Files:**
- Create: `pom.xml`

**Step 1: Write failing build test**

```bash
# Verify pom.xml doesn't exist yet
mvn --version
# Expected: Maven works, no project yet
```

**Step 2: Create pom.xml skeleton**

Create `pom.xml` with:
- `groupId`: `com.oracle.runbook`
- `artifactId`: `runbook-synthesizer`  
- `version`: `1.0.0-SNAPSHOT`
- `packaging`: `jar`
- Java 25 compiler properties
- UTF-8 encoding

**Hints:**
- Use `<maven.compiler.source>25</maven.compiler.source>`
- Use `<maven.compiler.release>25</maven.compiler.release>` for cross-compilation safety

**Step 3: Add Helidon BOM to dependencyManagement**

Import Helidon BOM as `<scope>import</scope>` dependency.

**Hints:**
- Check Maven Central for latest `io.helidon:helidon-bom` version (4.x series)
- Use `<type>pom</type>` with `<scope>import</scope>`

**Step 4: Add OCI SDK BOM to dependencyManagement**

**Hints:**
- Artifact: `com.oracle.oci.sdk:oci-java-sdk-bom`
- Same import pattern as Helidon

**Step 5: Add core dependencies**

Add these dependencies (versions managed by BOMs):
- `io.helidon.webserver:helidon-webserver`
- `io.helidon.webserver:helidon-webserver-http2`
- `io.helidon.config:helidon-config-yaml`
- `io.helidon.health:helidon-health`
- `io.helidon.health:helidon-health-checks`

**Step 6: Add test dependencies**

- JUnit 5 (`junit-jupiter`) with `<scope>test</scope>`
- Mockito (`mockito-core`, `mockito-junit-jupiter`)
- Helidon test utilities: `io.helidon.webserver.testing.junit5:helidon-webserver-testing-junit5`

**Step 7: Add Maven plugins**

Configure in `<build><plugins>`:
- `maven-compiler-plugin` (3.11+)
- `maven-surefire-plugin` (3.1+)
- `exec-maven-plugin` for running app
- `jib-maven-plugin` (optional, for container builds)

**Step 8: Verify build**

```bash
mvn validate
# Expected: BUILD SUCCESS
```

**Step 9: Commit**

```bash
git add pom.xml
git commit -m "feat: add Maven pom.xml with Helidon SE and OCI SDK BOMs"
```

---

## Task 2: Create Directory Structure [S]

**Files:**
- Create: `src/main/java/com/oracle/runbook/` (and subpackages)
- Create: `src/main/resources/`
- Create: `src/test/java/com/oracle/runbook/`
- Create: `src/test/resources/sample-runbooks/`
- Create: `examples/runbooks/memory/`
- Create: `examples/runbooks/cpu/`
- Create: `examples/runbooks/gpu/`

**Step 1: Create source directories**

Create the full package structure per DESIGN.md:
```
src/main/java/com/oracle/runbook/
├── domain/
├── ingestion/
├── enrichment/
├── rag/
├── api/
├── output/
└── config/
```

**Hints:**
- Use `mkdir -p` or PowerShell `New-Item -ItemType Directory -Force`
- Create `.gitkeep` files to preserve empty directories in git

**Step 2: Create test directory mirror**

Mirror the main structure under `src/test/java/com/oracle/runbook/`

**Step 3: Create resources directories**

- `src/main/resources/META-INF/`
- `src/test/resources/sample-runbooks/`

**Step 4: Create examples directory**

Per DESIGN.md project structure:
```
examples/runbooks/
├── memory/
├── cpu/
└── gpu/
```

**Step 5: Verify structure**

```bash
find src examples -type d | head -30
# Or PowerShell: Get-ChildItem -Path src,examples -Recurse -Directory
```

**Step 6: Commit**

```bash
git add src/ examples/ .gitkeep
git commit -m "feat: create directory structure per DESIGN.md"
```

---

## Task 3: Create Application Configuration [S]

**Files:**
- Create: `src/main/resources/application.yaml`
- Create: `src/main/resources/META-INF/services/` (placeholder)

**Step 1: Create application.yaml**

Create Helidon config file with:
- Server port: `8080`
- Server host: `0.0.0.0`
- Server features: health enabled

**Hints:**
- Helidon SE 4.x uses `server.port` and `server.host` paths
- Reference Helidon SE 4.x documentation for exact config keys

**Step 2: Add placeholder config sections**

Add commented placeholder sections for:
- `llm:` configuration
- `output.webhooks:` configuration  
- `oci:` configuration

**Hints:**
- Use YAML comments (`#`) to indicate future configuration
- Match the config structure from DESIGN.md

**Step 3: Verify YAML syntax**

```bash
# Use yamllint or similar, or just let Maven validate
cat src/main/resources/application.yaml
```

**Step 4: Commit**

```bash
git add src/main/resources/
git commit -m "feat: add application.yaml with server configuration"
```

---

## Task 4: Create Main Application Class [M]

**Files:**
- Create: `src/main/java/com/oracle/runbook/RunbookSynthesizerApp.java`
- Create: `src/test/java/com/oracle/runbook/RunbookSynthesizerAppTest.java`

**Step 1: Write failing test**

Create `RunbookSynthesizerAppTest.java` with a test that:
- Verifies the app class exists
- Verifies it has a `main` method

**Hints:**
- Use reflection to verify main method signature
- Test: `RunbookSynthesizerApp.class.getMethod("main", String[].class)`

**Step 2: Run test to verify it fails**

```bash
mvn test -Dtest=RunbookSynthesizerAppTest -q
# Expected: FAIL - class not found
```

**Step 3: Create minimal RunbookSynthesizerApp**

Create class with:
- Package: `com.oracle.runbook`
- Main method that creates Helidon WebServer
- Starts server and waits for termination

**Hints:**
- Use `WebServer.builder()` pattern from Helidon SE 4.x
- Register routing in builder
- Call `.build().start()` and keep reference

**Step 4: Run test to verify it passes**

```bash
mvn test -Dtest=RunbookSynthesizerAppTest -q
# Expected: PASS
```

**Step 5: Commit**

```bash
git add src/main/java/com/oracle/runbook/RunbookSynthesizerApp.java
git add src/test/java/com/oracle/runbook/RunbookSynthesizerAppTest.java
git commit -m "feat: add RunbookSynthesizerApp entry point"
```

---

## Task 5: Add Health Endpoint [M]

**Files:**
- Modify: `src/main/java/com/oracle/runbook/RunbookSynthesizerApp.java`
- Create: `src/test/java/com/oracle/runbook/HealthEndpointTest.java`

**Step 1: Write failing test**

Create integration test that:
- Starts the Helidon server
- Makes GET request to `/health`
- Expects 200 OK response

**Hints:**
- Use `@ServerTest` annotation from Helidon testing
- Inject `WebClient` for requests
- Assert response status is 200

**Step 2: Run test to verify it fails**

```bash
mvn test -Dtest=HealthEndpointTest -q
# Expected: FAIL - no health endpoint configured
```

**Step 3: Add health feature to server**

Modify `RunbookSynthesizerApp` to:
- Register Helidon health feature
- Use `HealthObserver` or equivalent from Helidon 4.x
- Wire health checks to the server

**Hints:**
- Helidon SE 4.x uses observer pattern for features
- Look for `HealthFeature` or `HealthObserver` classes
- Register on the `WebServer.Builder`

**Step 4: Run test to verify it passes**

```bash
mvn test -Dtest=HealthEndpointTest -q
# Expected: PASS
```

**Step 5: Commit**

```bash
git add src/main/java/com/oracle/runbook/RunbookSynthesizerApp.java
git add src/test/java/com/oracle/runbook/HealthEndpointTest.java
git commit -m "feat: add /health endpoint"
```

---

## Task 6: Add Graceful Shutdown [S]

**Files:**
- Modify: `src/main/java/com/oracle/runbook/RunbookSynthesizerApp.java`

**Step 1: Verify current shutdown behavior**

Start server and verify it responds to SIGINT/SIGTERM.

**Step 2: Add shutdown hook or lifecycle handling**

Ensure the server:
- Logs when starting
- Logs when shutting down
- Cleans up resources gracefully

**Hints:**
- Helidon WebServer has lifecycle callbacks
- Use `Runtime.getRuntime().addShutdownHook()` if needed
- Log using `java.util.logging` or SLF4J

**Step 3: Verify shutdown**

```bash
# Start server in background, then kill
mvn exec:java -Dexec.mainClass="com.oracle.runbook.RunbookSynthesizerApp" &
# Wait for startup message, then Ctrl+C
# Expected: Clean shutdown log message
```

**Step 4: Commit**

```bash
git add src/main/java/com/oracle/runbook/RunbookSynthesizerApp.java
git commit -m "feat: add graceful shutdown handling"
```

---

## Task 7: Create Package Info Files [S]

**Files:**
- Create: `src/main/java/com/oracle/runbook/package-info.java`
- Create: `src/main/java/com/oracle/runbook/domain/package-info.java`
- Create: `src/main/java/com/oracle/runbook/ingestion/package-info.java`
- Create: `src/main/java/com/oracle/runbook/enrichment/package-info.java`
- Create: `src/main/java/com/oracle/runbook/rag/package-info.java`
- Create: `src/main/java/com/oracle/runbook/api/package-info.java`
- Create: `src/main/java/com/oracle/runbook/output/package-info.java`
- Create: `src/main/java/com/oracle/runbook/config/package-info.java`

**Step 1: Create package-info.java files**

Each file should contain:
- Package declaration
- Javadoc describing the package's purpose

**Hints:**
- Root package: "Runbook-Synthesizer dynamic SOP generation"
- domain: "Pure domain models - Alert, EnrichedContext, DynamicChecklist"
- ingestion: "Alert source adapters and normalization"
- enrichment: "Context enrichment service and observability adapters"
- rag: "RAG pipeline - embeddings, retrieval, generation"
- api: "REST API resources"
- output: "Webhook destinations"
- config: "Application configuration"

**Step 2: Verify compilation**

```bash
mvn compile -q
# Expected: BUILD SUCCESS
```

**Step 3: Commit**

```bash
git add src/main/java/com/oracle/runbook/**/package-info.java
git commit -m "docs: add package-info.java documentation"
```

---

## Task 8: Create Sample Runbook Templates [S]

**Files:**
- Create: `examples/runbooks/memory/high-memory.md`
- Create: `examples/runbooks/cpu/high-cpu.md`
- Create: `examples/runbooks/gpu/gpu-errors.md`

**Step 1: Create high-memory.md**

Use the format from DESIGN.md:
- YAML frontmatter with title, tags, applicable_shapes
- Prerequisites section
- Numbered troubleshooting steps with bash commands

**Hints:**
- Reference the Runbook Format Specification in DESIGN.md (lines 701-743)
- Include `applicable_shapes: ["VM.*", "BM.*"]`

**Step 2: Create high-cpu.md**

Similar structure for CPU troubleshooting.

**Step 3: Create gpu-errors.md**

Include GPU-specific commands like `nvidia-smi`.

**Hints:**
- Use `applicable_shapes: ["GPU*", "BM.GPU*"]`

**Step 4: Verify markdown syntax**

```bash
cat examples/runbooks/memory/high-memory.md
# Verify YAML frontmatter and markdown structure
```

**Step 5: Commit**

```bash
git add examples/
git commit -m "docs: add sample runbook templates"
```

---

## Task 9: Create Project Documentation [S]

**Files:**
- Update: `README.md`
- Create: `docs/ARCHITECTURE.md`
- Create: `CONTRIBUTING.md`
- Create: `LICENSE` (Apache 2.0)

**Step 1: Create/update README.md**

Include:
- Project name and description
- Quick start instructions
- Tech stack summary
- Link to ARCHITECTURE.md

**Hints:**
- Keep it concise but informative
- Add badges (build status, license) placeholders

**Step 2: Create ARCHITECTURE.md**

High-level architecture overview with:
- Component diagram (mermaid)
- Package structure explanation
- Key design decisions summary

**Hints:**
- Reference DESIGN.md for diagrams
- Link to DESIGN.md for full details

**Step 3: Create CONTRIBUTING.md**

Standard contribution guidelines:
- How to build
- How to test
- Code style
- PR process

**Step 4: Create LICENSE**

Apache 2.0 license text (as specified in DESIGN.md).

**Step 5: Commit**

```bash
git add README.md docs/ARCHITECTURE.md CONTRIBUTING.md LICENSE
git commit -m "docs: add project documentation"
```

---

## Task 10: Final Verification [S]

**Files:** None (verification only)

**Step 1: Full build verification**

```bash
mvn clean verify
# Expected: BUILD SUCCESS with all tests passing
```

**Step 2: Application startup test**

```bash
mvn exec:java -Dexec.mainClass="com.oracle.runbook.RunbookSynthesizerApp" &
sleep 5
curl http://localhost:8080/health
# Expected: 200 OK with health status
kill %1
```

**Step 3: Verify directory structure**

```bash
find . -name "*.java" | wc -l
# Expected: At least 10 files (main + tests + package-info)
```

**Step 4: Final commit and tag**

```bash
git log --oneline | head -10
# Verify all commits are present
git tag -a v0.1.0-scaffold -m "Phase 1: Project scaffolding complete"
```

---

## Verification Plan

### Automated Tests

| Test | Command | Expected |
|------|---------|----------|
| Build compiles | `mvn compile` | BUILD SUCCESS |
| Tests pass | `mvn test` | All tests PASS |
| Full lifecycle | `mvn verify` | BUILD SUCCESS |
| Health endpoint | Start app, `curl /health` | 200 OK |

### Manual Verification

1. Start the application with `mvn exec:java`
2. Verify health endpoint responds at `http://localhost:8080/health`
3. Send SIGINT (Ctrl+C) and verify graceful shutdown log
4. Review generated Javadocs for package-info files
