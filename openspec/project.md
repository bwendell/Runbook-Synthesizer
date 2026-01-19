# Project Context

## Purpose

**Runbook-Synthesizer** is an open-source Java tool that transforms static runbooks into intelligent, context-aware troubleshooting guides by leveraging RAG (Retrieval Augmented Generation) with real-time infrastructure state.

> **Note**: AWS is the default cloud provider. OCI is supported as an alternative.

### Problem Solved
- **Context Blindness**: Generic runbook steps don't account for specific host type, configuration, or current state
- **Staleness**: Infrastructure evolves faster than documentation
- **Information Overload**: Operators must mentally filter irrelevant steps during incidents

### Solution
- Dynamically generate troubleshooting checklists tailored to specific alerts and hosts
- Enrich runbook content with real-time system context (metrics, logs, host metadata)
- Use RAG to retrieve only relevant procedures from knowledge base

## Tech Stack

### Core Framework
- **Language**: Java 25
- **Framework**: Helidon SE (Oracle's native microframework, first-class OCI support, GraalVM ready)
- **Build**: Maven
- **RAG Framework**: LangChain4j

### Cloud Services

| Provider | Service | Usage |
|----------|---------|-------|
| **AWS** (default) | S3 | Runbook storage |
| | CloudWatch | Metrics and logs |
| | SNS | CloudWatch Alarm delivery |
| | Bedrock | LLM inference (Claude 3 Haiku, Cohere Embed v3) |
| **OCI** | Object Storage | Runbook storage |
| | Monitoring | Metrics |
| | Logging | Logs |
| | Generative AI | LLM inference (Cohere Command, Cohere Embed v3) |

### LLM Stack

| Environment | Provider | Text Model | Embedding Model |
|-------------|----------|------------|------------------|
| MVP / Local Dev | Ollama | llama3.2:3b | nomic-embed-text |
| Production (AWS) | AWS Bedrock | Claude 3 Haiku | Cohere Embed v3 |
| Production (OCI) | OCI GenAI | Cohere Command | Cohere Embed v3 |

## Project Conventions

### Code Style
- Standard Java conventions
- Record classes for immutable data models (`Alert`, `EnrichedContext`, `DynamicChecklist`)
- Interface-first design for pluggable components (`LlmProvider`, `MetricsSourceAdapter`, `WebhookDestination`)

### Architecture Patterns

**Layered Architecture:**
1. **Ingestion Layer**: Receives alerts from OCI Monitoring Alarms, normalizes to canonical format
2. **Enrichment Layer**: Gathers real-time infrastructure state from multiple observability sources
3. **RAG Pipeline**: Document ingestion → Vector search → Re-ranking → Checklist generation
4. **Output Layer**: REST API core + configurable webhook dispatcher

**Key Interfaces:**
- `LlmProvider`: Pluggable LLM backends (Ollama for MVP, AWS Bedrock for production, OCI GenAI)
- `AlertSourceAdapter`: Alert ingestion (AWS SNS/CloudWatch, OCI Monitoring Alarms)
- `MetricsSourceAdapter` / `LogSourceAdapter`: Observability source adapters
- `WebhookDestination`: Output channel implementations

### Testing Strategy
- Unit tests for domain models and business logic
- Integration tests with sample runbooks in `src/test/resources/sample-runbooks/`
- Example runbooks in `examples/runbooks/` for memory, CPU, and GPU scenarios

### Git Workflow
- Apache 2.0 license
- Conventional commits
- Feature branches with PR review

## Domain Context

### Core Domain Models
- **Alert**: Canonical alert model with severity, dimensions, labels, and raw payload
- **EnrichedContext**: Alert + resource metadata + recent metrics + recent logs
- **RunbookChunk**: Semantically chunked runbook section with embeddings
- **DynamicChecklist**: Generated troubleshooting checklist with prioritized steps

### Runbook Format
Markdown files with YAML frontmatter containing:
- `title`, `tags`, `applicable_shapes`
- `severity_triggers`
- Structured steps with commands and conditionals (e.g., "GPU Hosts Only")

## Important Constraints

- **Authentication**: OCI IAM exclusively for v1.0
- **Human-in-the-Loop**: None - checklists auto-sent to webhooks
- **Generation Latency**: Target <5s P95 API response time
- **Runbook Relevance**: Target >80% user satisfaction on generated steps

## External Dependencies

### Cloud Services
| Provider | Service | Usage |
|----------|---------|-------|
| **AWS** (default) | CloudWatch Alarms via SNS | Primary alert source |
| | S3 | Runbook storage with event-triggered indexing |
| | Bedrock | LLM inference and embeddings (Claude 3 Haiku, Cohere Embed v3) |
| **OCI** | Monitoring Alarms | Alert source via Events Service |
| | Object Storage | Runbook storage with automatic event-triggered indexing |
| | Generative AI | LLM inference and embeddings |
| Any | Oracle Database 23ai | Vector store for RAG retrieval |

### Observability Sources
| Source | Type | Use Case |
|--------|------|----------|
| OCI Monitoring | Metrics | OCI-native compute/DB metrics |
| OCI Logging | Logs | OCI service and custom logs |
| Prometheus | Metrics | Existing on-prem/K8s metrics |
| Grafana Loki | Logs | Existing log aggregation |

### Output Destinations
- Slack (Block Kit formatting)
- PagerDuty (Events API v2)
- Generic HTTP webhooks

## Roadmap

### v1.0 (MVP)
- CloudWatch Alarms / OCI Monitoring Alarms ingestion
- Context enrichment (CloudWatch / OCI Monitoring, Logging, Compute metadata)
- RAG pipeline with Oracle 23ai Vector Search
- Pluggable LLM interface (Ollama default for MVP, AWS Bedrock / OCI GenAI for production)
- REST API core + configurable webhook output framework
- S3 / OCI Object Storage integration for runbooks

### v1.1
- CLI tool for local testing/debugging
- Slack/PagerDuty webhook integrations
- Prometheus/Loki adapter implementations
- GPU host enricher

### v2.0
- Azure Monitor alert sources
- Learning from resolution feedback
