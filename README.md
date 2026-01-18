# Runbook-Synthesizer

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](.)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-25-orange.svg)](.)

An open-source Java tool that transforms static runbooks into intelligent, context-aware troubleshooting guides by leveraging RAG (Retrieval Augmented Generation) with real-time infrastructure state.

## Features

- **Dynamic SOP Generation**: Generate troubleshooting checklists tailored to specific alerts and hosts
- **Context Enrichment**: Enrich runbook content with real-time metrics, logs, and host metadata
- **RAG Pipeline**: Retrieve only relevant procedures from your knowledge base
- **Multi-Channel Output**: Deliver checklists via Slack, PagerDuty, or custom webhooks
- **OCI Native**: Built for Oracle Cloud Infrastructure with first-class support

## Quick Start

### Prerequisites

- Java 25+
- Maven 3.9+
- OCI Account (for full functionality)

### Build and Run

```bash
# Clone the repository
git clone https://github.com/oracle/runbook-synthesizer.git
cd runbook-synthesizer

# Build the project
mvn clean package

# Run the application
mvn exec:java

# Or run the JAR directly
java --enable-preview -jar target/runbook-synthesizer-1.0.0-SNAPSHOT.jar
```

### Verify

```bash
# Check health endpoint
curl http://localhost:8080/health
# Expected: {"status":"UP"}
```

### Testing

```powershell
# Run unit tests only
.\mvnw.cmd test --batch-mode

# Run unit + integration tests (no Docker required)
.\mvnw.cmd verify --batch-mode

# Run container E2E tests (requires Docker)
.\mvnw.cmd verify -Dtest.use.containers=true --batch-mode
```

See [E2E Testing Guidelines](docs/E2E_TESTING_GUIDELINES.md) for detailed testing documentation.

## Tech Stack

| Component | Technology |
|-----------|------------|
| Framework | Helidon SE 4.x |
| Language | Java 25 |
| Build | Maven |
| Vector Store | Oracle Database 23ai |
| LLM | Pluggable (OCI GenAI, OpenAI, Ollama) |

## Documentation

- [Architecture](docs/ARCHITECTURE.md) - System design and component overview
- [Design](docs/DESIGN.md) - Detailed technical design document
- [E2E Testing Guidelines](docs/E2E_TESTING_GUIDELINES.md) - Container-based E2E testing with Testcontainers
- [Contributing](CONTRIBUTING.md) - How to contribute

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
