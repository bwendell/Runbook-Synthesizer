# Project Context

## Purpose

**Runbook-Synthesizer** is an open-source Java tool that transforms static runbooks into intelligent, context-aware troubleshooting guides by leveraging RAG (Retrieval Augmented Generation) with real-time infrastructure state.

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

### OCI Services
- **Object Storage**: Store runbook markdown files
- **Events Service**: Trigger on new/updated runbooks
- **Functions**: Serverless runbook ingestion
- **Generative AI**: Cohere embeddings + text generation
- **Monitoring**: Fetch metrics for context
- **Logging**: Fetch logs for context
- **Notifications**: Receive alerts, send outputs

### RAG Stack
- **Embeddings**: OCI GenAI (Cohere Embed v3)
- **Vector Store**: Oracle Database 23ai
- **LLM**: OCI GenAI (Cohere Command) - pluggable to OpenAI/Ollama
- **Output**: Multi-channel webhooks (Slack, PagerDuty, custom)

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
- `LlmProvider`: Pluggable LLM backends (OCI GenAI, OpenAI, Ollama)
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

### OCI Services
| Service | Usage |
|---------|-------|
| OCI Monitoring Alarms | Primary alert source via Events Service |
| OCI Object Storage | Runbook storage with automatic event-triggered indexing |
| OCI Generative AI | LLM inference and embeddings |
| Oracle Database 23ai | Vector store for RAG retrieval |

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
- OCI Monitoring Alarms ingestion via Events Service
- Context enrichment (OCI Monitoring, OCI Logging, Compute metadata)
- RAG pipeline with Oracle 23ai Vector Search
- Pluggable LLM interface (OCI GenAI default)
- REST API core + configurable webhook output framework
- OCI Object Storage integration for runbooks

### v1.1
- CLI tool for local testing/debugging
- Slack/PagerDuty webhook integrations
- Prometheus/Loki adapter implementations
- GPU host enricher

### v2.0
- Multi-cloud alert sources (AWS CloudWatch, Azure Monitor)
- OpenAI/Ollama LLM provider implementations
- Learning from resolution feedback
