# E2E Testing Guidelines

This document describes end-to-end (E2E) testing infrastructure using Testcontainers with real Oracle 23ai and Ollama containers.

## Overview

The Runbook Synthesizer uses a tiered testing approach:

| Test Type | Speed | Infrastructure | Purpose |
|-----------|-------|----------------|---------|
| Unit Tests | Fast (~ms) | None | Component isolation |
| Integration Tests | Medium (~s) | WireMock | API boundaries |
| Container E2E Tests | Slow (~min) | Docker | Real infrastructure validation |

## Prerequisites

### Docker Installation

Container tests require Docker to be installed and running:

**Windows:**
- Install [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- Ensure WSL 2 backend is enabled for better performance
- Start Docker Desktop before running tests

**Linux:**
```bash
# Install Docker
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER

# Verify installation
docker run hello-world
```

### Disk Space Requirements

First-run image downloads require significant disk space:

| Container | Image Size | Notes |
|-----------|-----------|-------|
| Oracle 23ai | ~1.5 GB | `gvenzl/oracle-free:23-slim` |
| Ollama | ~800 MB | Base image |
| nomic-embed-text | ~300 MB | Embedding model |
| llama3.2:1b | ~1.2 GB | Generation model |

**Total: ~4 GB** for all container tests

### First Run Time

Initial test runs will download container images and Ollama models:
- Oracle container: ~2-3 minutes
- Ollama models: ~5-10 minutes (depending on network speed)

Subsequent runs are much faster due to Docker layer caching and model persistence.

## Running Tests

### Standard Tests (No Docker Required)

Run unit tests and WireMock-based integration tests:

```powershell
.\mvnw.cmd verify --batch-mode
```

### Container E2E Tests (Requires Docker)

Run container-based E2E tests:

```powershell
# Run all integration tests including containers
.\mvnw.cmd verify -Dtest.use.containers=true --batch-mode

# Run only container tests
.\mvnw.cmd failsafe:integration-test -Dtest.use.containers=true --batch-mode

# Run specific container test class
.\mvnw.cmd verify -Dtest.use.containers=true -Dit.test=OracleVectorStoreContainerIT
```

### CI Integration Profile

For CI environments with real OCI services (no containers):

```powershell
.\mvnw.cmd verify -Pci-integration --batch-mode
```

## Test Categories

### Oracle Container Tests

Located in: `src/test/java/com/oracle/runbook/integration/rag/`

| Test Class | Purpose |
|------------|---------|
| `OracleVectorStoreContainerIT` | Vector storage and similarity search |

These tests use `OracleContainerBase` which manages:
- Container lifecycle with `@BeforeAll`/`@AfterAll`
- Shared network for multi-container communication
- JDBC connection helpers

### Ollama Container Tests

Located in: `src/test/java/com/oracle/runbook/integration/containers/`

| Test Class | Purpose |
|------------|---------|
| `OllamaEmbeddingContainerIT` | Embedding generation, dimension verification |
| `OllamaGenerationContainerIT` | Text generation, prompt compliance |

These tests use `OllamaContainerSupport` for container creation and extend `OracleContainerBase` for shared networking.

### Full-Stack E2E Tests

Located in: `src/test/java/com/oracle/runbook/integration/e2e/`

| Test Class | Purpose |
|------------|---------|
| `AlertToChecklistIT` | Complete alert → checklist flow |
| `ErrorPropagationIT` | Error handling across components |

## Test Data Management

### Scenario Seeders

Use `RunbookSeeder` for pre-built runbook scenarios:

```java
RunbookSeeder seeder = new RunbookSeeder(vectorStore, embeddingGenerator);
seeder.seedMemoryRunbook();  // Memory troubleshooting chunks
seeder.seedCpuRunbook();     // CPU troubleshooting chunks
seeder.seedAllRunbooks();    // All available scenarios
```

### Data Factories

Use `E2EDataFactory` for test data creation:

```java
// Alerts
Alert alert = E2EDataFactory.createMemoryAlert();
Alert cpuAlert = E2EDataFactory.createCpuAlert();

// Context
EnrichedContext context = E2EDataFactory.createEnrichedContext(alert);

// Embeddings
float[] embedding = E2EDataFactory.createRandomEmbedding(768);
float[] categoryEmbedding = E2EDataFactory.createCategoryEmbedding(768, 0);
```

## Best Practices

### 1. Use Container Reuse for Local Development

Enable Testcontainers reuse for faster local iterations:

```properties
# ~/.testcontainers.properties
testcontainers.reuse.enable=true
```

### 2. Tag Container Tests

All container tests should use the `@Tag("container")` annotation:

```java
@Tag("container")
@EnabledIfSystemProperty(named = "test.use.containers", matches = "true")
class MyContainerIT extends OracleContainerBase {
    // ...
}
```

### 3. Handle Model Download Gracefully

Ollama models take time to download on first run. Tests use long timeouts:

```java
.readTimeout(Duration.ofMinutes(5))  // Model download can take time
```

### 4. Clean Up Test Data

Use `@BeforeEach` to clean database state between tests:

```java
@BeforeEach
void cleanUp() throws Exception {
    try (Connection conn = getConnection()) {
        conn.createStatement().execute("DELETE FROM my_table");
        conn.commit();
    }
}
```

## Troubleshooting

### Docker Not Running

**Symptom:** `Docker environment is not available`

**Solution:** Start Docker Desktop (Windows) or Docker daemon (Linux)

### Container Startup Timeout

**Symptom:** `Timed out waiting for container`

**Solution:** 
1. Check Docker has enough resources (CPU, memory)
2. Increase startup timeout in test configuration
3. Pull images manually: `docker pull gvenzl/oracle-free:23-slim`

### Oracle Container Fails to Start

**Symptom:** Container exits immediately

**Solutions:**
1. Ensure sufficient memory (Oracle needs ~2GB)
2. Check for port conflicts on 1521
3. Verify Docker can access network
4. Try with increased memory: `docker run -m 4g gvenzl/oracle-free:23-slim`

### Ollama Model Not Found

**Symptom:** `model not found` error

**Solution:** Ensure model is pulled in `@BeforeAll`:

```java
@BeforeAll
static void startContainer() {
    ollama.start();
    pullModel("nomic-embed-text");  // Pull before using
}
```

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Test JVM                                 │
│  ┌──────────────────┐  ┌──────────────────┐                 │
│  │ OracleContainerBase│  │ OllamaContainerSupport│           │
│  └────────┬─────────┘  └────────┬─────────┘                 │
└───────────┼──────────────────────┼───────────────────────────┘
            │                      │
   ┌────────▼────────┐    ┌───────▼────────┐
   │ Oracle 23ai     │    │ Ollama         │
   │ (Vector Store)  │    │ (LLM Inference)│
   └─────────────────┘    └────────────────┘
```

Both containers share a Testcontainers network for container-to-container communication.
