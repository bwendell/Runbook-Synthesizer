## ADDED Requirements

### Requirement: Vector Store Provider Selection

The `CloudAdapterFactory` SHALL provide a `getVectorStoreClass()` method that returns the appropriate `VectorStoreRepository` implementation based on configuration.

#### Scenario: Local provider selection
- **GIVEN** configuration `vectorStore.provider: local`
- **WHEN** `CloudAdapterFactory.getVectorStoreClass()` is called
- **THEN** return `InMemoryVectorStoreRepository.class`

#### Scenario: OCI provider selection
- **GIVEN** configuration `vectorStore.provider: oci`
- **WHEN** `CloudAdapterFactory.getVectorStoreClass()` is called
- **THEN** return `OracleVectorStoreRepository.class`

#### Scenario: AWS provider selection
- **GIVEN** configuration `vectorStore.provider: aws`
- **WHEN** `CloudAdapterFactory.getVectorStoreClass()` is called
- **THEN** return `AwsOpenSearchVectorStoreRepository.class`

---

### Requirement: In-Memory Vector Store

The system SHALL provide an `InMemoryVectorStoreRepository` for local development and testing that implements the `VectorStoreRepository` interface.

#### Scenario: Store and search chunks
- **GIVEN** a `RunbookChunk` with embedding vector `[0.1, 0.2, 0.3]`
- **WHEN** stored via `store()` and searched with similar query vector
- **THEN** the chunk is returned with appropriate similarity score

#### Scenario: Delete chunks by path
- **GIVEN** chunks stored with `runbookPath: "memory/high-memory.md"`
- **WHEN** `delete("memory/high-memory.md")` is called
- **THEN** all chunks with that path are removed

---

### Requirement: Runbook Chunker

The system SHALL provide a `RunbookChunker` that splits markdown runbooks into semantic chunks preserving procedure boundaries.

#### Scenario: Extract YAML frontmatter
- **GIVEN** a markdown file with YAML frontmatter containing `title`, `tags`, `applicable_shapes`
- **WHEN** chunked
- **THEN** all chunks include the frontmatter metadata

#### Scenario: Split by headers
- **GIVEN** a markdown file with H2 and H3 sections
- **WHEN** chunked
- **THEN** each section becomes a separate chunk with section title preserved

#### Scenario: Respect chunk size limits
- **GIVEN** a section with content exceeding max chunk size (2000 chars)
- **WHEN** chunked
- **THEN** the section is split into multiple chunks respecting boundaries

---

### Requirement: Runbook Ingestion Service

The system SHALL provide a `RunbookIngestionService` that fetches runbooks from cloud storage, chunks them, generates embeddings, and stores in the vector store.

#### Scenario: Ingest single runbook
- **GIVEN** a runbook path `runbooks/memory/high-memory.md` in S3
- **WHEN** `ingest("runbooks/memory/high-memory.md")` is called
- **THEN** the runbook is fetched, chunked, embedded, and stored in vector store

#### Scenario: Ingest all runbooks
- **GIVEN** multiple `.md` files in the S3 bucket under `runbooks/` prefix
- **WHEN** `ingestAll()` is called
- **THEN** all runbooks are processed and stored in vector store
