-- Oracle 23ai Vector Store Schema
-- Canonical schema for all container-based integration tests
-- This script is executed automatically on container startup via withInitScript()

-- Enable vector operations (23ai has native vector support)
-- Note: VECTOR data type requires Oracle Database 23ai

-- Create runbook_chunks table with vector embedding column
-- VECTOR(768, FLOAT32) is compatible with most embedding models
CREATE TABLE runbook_chunks (
    id VARCHAR2(64) PRIMARY KEY,
    content CLOB NOT NULL,
    source_file VARCHAR2(512) NOT NULL,
    chunk_index NUMBER NOT NULL,
    embedding VECTOR(768, FLOAT32),
    metadata CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create vector similarity search index
-- COSINE distance is standard for embedding similarity
CREATE VECTOR INDEX runbook_chunks_embedding_idx
    ON runbook_chunks (embedding)
    ORGANIZATION NEIGHBOR PARTITIONS
    DISTANCE COSINE;

-- Create index on source file for efficient filtering
CREATE INDEX runbook_chunks_source_idx ON runbook_chunks (source_file);

-- Create sequence for generating chunk IDs if needed
CREATE SEQUENCE runbook_chunk_seq START WITH 1 INCREMENT BY 1;

-- Note: Similarity search is handled by LangChain4j's OracleEmbeddingStore
-- which manages its own table structure and search operations.
-- No stored procedure is needed here.

-- Grant necessary permissions (for test user)
-- Note: OracleContainer uses 'test' as default username
GRANT ALL PRIVILEGES ON runbook_chunks TO PUBLIC;

COMMIT;
