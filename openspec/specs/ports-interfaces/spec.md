# ports-interfaces Specification

## Purpose
TBD - created by archiving change implement-ports-interfaces. Update Purpose after archive.
## Requirements
### Requirement: MetricsSourceAdapter Interface

The system SHALL define a `MetricsSourceAdapter` interface that allows pluggable metrics source implementations (OCI Monitoring, Prometheus).

#### Scenario: Interface provides source identifier
**Given** a MetricsSourceAdapter implementation
**When** `sourceType()` is called
**Then** it returns a non-null string identifier (e.g., "oci-monitoring", "prometheus")

#### Scenario: Interface fetches metrics asynchronously
**Given** a MetricsSourceAdapter implementation
**When** `fetchMetrics(resourceId, lookback)` is called
**Then** it returns a `CompletableFuture<List<MetricSnapshot>>`

---

### Requirement: LogSourceAdapter Interface

The system SHALL define a `LogSourceAdapter` interface that allows pluggable log source implementations (OCI Logging, Grafana Loki).

#### Scenario: Interface provides source identifier
**Given** a LogSourceAdapter implementation
**When** `sourceType()` is called
**Then** it returns a non-null string identifier (e.g., "oci-logging", "loki")

#### Scenario: Interface fetches logs asynchronously
**Given** a LogSourceAdapter implementation
**When** `fetchLogs(resourceId, lookback, query)` is called
**Then** it returns a `CompletableFuture<List<LogEntry>>`

---

### Requirement: LlmProvider Interface

The system SHALL define a pluggable `LlmProvider` interface supporting text generation and embedding creation.

#### Scenario: Interface provides provider identifier
**Given** an LlmProvider implementation
**When** `providerId()` is called
**Then** it returns a non-null string identifier (e.g., "oci-genai", "openai", "ollama")

#### Scenario: Interface generates text asynchronously
**Given** an LlmProvider implementation
**When** `generateText(prompt, config)` is called
**Then** it returns a `CompletableFuture<String>` containing the generated text

#### Scenario: Interface generates single embedding
**Given** an LlmProvider implementation
**When** `generateEmbedding(text)` is called
**Then** it returns a `CompletableFuture<float[]>` containing the embedding vector

#### Scenario: Interface generates batch embeddings
**Given** an LlmProvider implementation
**When** `generateEmbeddings(texts)` is called with a list of texts
**Then** it returns a `CompletableFuture<List<float[]>>` with one embedding per input text

---

### Requirement: WebhookDestination Interface

The system SHALL define a `WebhookDestination` interface for configurable output channels.

#### Scenario: Interface provides webhook metadata
**Given** a WebhookDestination implementation
**When** `name()` and `type()` are called
**Then** they return non-null strings identifying the destination

#### Scenario: Interface sends checklist asynchronously
**Given** a WebhookDestination implementation
**When** `send(checklist)` is called with a DynamicChecklist
**Then** it returns a `CompletableFuture<WebhookResult>` with delivery status

#### Scenario: Interface filters by checklist content
**Given** a WebhookDestination implementation
**When** `shouldSend(checklist)` is called
**Then** it returns a boolean indicating whether this checklist should be sent to this destination

---

### Requirement: AlertSourceAdapter Interface

The system SHALL define an `AlertSourceAdapter` interface for normalizing alerts from different sources.

#### Scenario: Interface provides source identifier
**Given** an AlertSourceAdapter implementation
**When** `sourceType()` is called
**Then** it returns a non-null string identifier (e.g., "oci-monitoring", "oci-events")

#### Scenario: Interface parses raw payload to Alert
**Given** an AlertSourceAdapter implementation and a valid JSON payload
**When** `parseAlert(rawPayload)` is called
**Then** it returns an Alert domain object with normalized fields

#### Scenario: Interface detects handleable payloads
**Given** an AlertSourceAdapter implementation
**When** `canHandle(rawPayload)` is called
**Then** it returns true if this adapter can parse the payload, false otherwise

---

### Requirement: RunbookRetriever Interface

The system SHALL define a `RunbookRetriever` interface for RAG-based runbook chunk retrieval.

#### Scenario: Interface retrieves relevant chunks
**Given** a RunbookRetriever implementation
**When** `retrieve(context, topK)` is called with an EnrichedContext
**Then** it returns a List of RetrievedChunk objects ranked by relevance

---

### Requirement: ChecklistGenerator Interface

The system SHALL define a `ChecklistGenerator` interface for synthesizing troubleshooting checklists.

#### Scenario: Interface generates checklist from context and chunks
**Given** a ChecklistGenerator implementation
**When** `generate(context, relevantChunks)` is called
**Then** it returns a DynamicChecklist with steps tailored to the alert context

---

### Requirement: VectorStoreRepository Interface

The system SHALL define a `VectorStoreRepository` interface for vector storage operations.

#### Scenario: Interface stores single chunk
**Given** a VectorStoreRepository implementation
**When** `store(chunk)` is called with a RunbookChunk
**Then** the chunk is persisted with its embedding

#### Scenario: Interface stores batch of chunks
**Given** a VectorStoreRepository implementation
**When** `storeBatch(chunks)` is called with a list of RunbookChunks
**Then** all chunks are persisted efficiently

#### Scenario: Interface searches by embedding
**Given** a VectorStoreRepository implementation with stored chunks
**When** `search(queryEmbedding, topK)` is called
**Then** it returns the topK most similar RunbookChunks

#### Scenario: Interface deletes chunks by runbook path
**Given** a VectorStoreRepository implementation with stored chunks
**When** `delete(runbookPath)` is called
**Then** all chunks with that runbook path are removed

---

### Requirement: EmbeddingService Interface

The system SHALL define an `EmbeddingService` interface as a facade for embedding generation.

#### Scenario: Interface embeds single text
**Given** an EmbeddingService implementation
**When** `embed(text)` is called
**Then** it returns a `CompletableFuture<float[]>` containing the embedding

#### Scenario: Interface embeds batch of texts
**Given** an EmbeddingService implementation
**When** `embedBatch(texts)` is called
**Then** it returns a `CompletableFuture<List<float[]>>` with one embedding per text

---

### Requirement: ContextEnrichmentService Interface

The system SHALL define a `ContextEnrichmentService` interface for alert context enrichment.

#### Scenario: Interface enriches alert with context
**Given** a ContextEnrichmentService implementation
**When** `enrich(alert)` is called with an Alert
**Then** it returns a `CompletableFuture<EnrichedContext>` containing metrics, logs, and resource metadata

