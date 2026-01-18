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

-- Stored procedure for similarity search
CREATE OR REPLACE PROCEDURE search_similar_chunks (
    p_query_embedding IN VECTOR,
    p_limit IN NUMBER DEFAULT 10,
    p_results OUT SYS_REFCURSOR
) AS
BEGIN
    OPEN p_results FOR
        SELECT id, content, source_file, chunk_index, metadata,
               VECTOR_DISTANCE(embedding, p_query_embedding, COSINE) AS similarity
        FROM runbook_chunks
        WHERE embedding IS NOT NULL
        ORDER BY VECTOR_DISTANCE(embedding, p_query_embedding, COSINE)
        FETCH FIRST p_limit ROWS ONLY;
END;
/

-- Grant necessary permissions (for test user)
-- Note: OracleContainer uses 'test' as default username
GRANT ALL PRIVILEGES ON runbook_chunks TO PUBLIC;
GRANT EXECUTE ON search_similar_chunks TO PUBLIC;

COMMIT;
