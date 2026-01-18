package com.oracle.runbook.integration.rag;

import static org.assertj.core.api.Assertions.assertThat;

import com.oracle.runbook.domain.RunbookChunk;
import com.oracle.runbook.integration.OracleContainerBase;
import com.oracle.runbook.rag.OracleVectorStoreRepository;
import com.oracle.runbook.rag.ScoredChunk;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.oracle.CreateOption;
import dev.langchain4j.store.embedding.oracle.OracleEmbeddingStore;
import java.sql.Connection;
import java.util.List;
import oracle.jdbc.pool.OracleDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Integration tests for OracleVectorStoreRepository using real Oracle 23ai container.
 *
 * <p>These tests verify vector storage and similarity search operations against Oracle Database
 * 23ai with native VECTOR type support.
 *
 * <p>Requires Docker to be running. Enable with: {@code -Dtest.use.containers=true}
 */
@EnabledIfSystemProperty(named = "test.use.containers", matches = "true")
class OracleVectorStoreContainerIT extends OracleContainerBase {

  private static final String TABLE_NAME = "VECTOR_STORE_TEST";
  private static EmbeddingStore<TextSegment> embeddingStore;
  private static OracleVectorStoreRepository vectorStore;

  @BeforeAll
  static void setUpVectorStore() throws Exception {
    // Create DataSource from container connection
    OracleDataSource dataSource = new OracleDataSource();
    dataSource.setURL(getJdbcUrl());
    dataSource.setUser(getUsername());
    dataSource.setPassword(getPassword());

    // Create LangChain4j Oracle embedding store
    // LangChain4j will create/manage the table structure itself
    embeddingStore =
        OracleEmbeddingStore.builder()
            .dataSource(dataSource)
            .embeddingTable(TABLE_NAME, CreateOption.CREATE_OR_REPLACE)
            .build();

    vectorStore = new OracleVectorStoreRepository(embeddingStore);
  }

  @BeforeEach
  void cleanUp() throws Exception {
    // Clean all data between tests
    try (Connection conn = getConnection()) {
      conn.createStatement().execute("DELETE FROM " + TABLE_NAME);
      conn.commit();
    } catch (Exception e) {
      // Table may not exist yet, ignore
    }
  }

  @Test
  @DisplayName("Should store and retrieve chunk from Oracle 23ai")
  void store_ThenSearch_ReturnsMatchingChunk() {
    // Given: A runbook chunk with 768-dimensional embedding
    float[] embedding = createRandomEmbedding(768);
    RunbookChunk chunk =
        new RunbookChunk(
            "chunk-001",
            "runbooks/memory-troubleshooting.md",
            "Memory Investigation",
            "Check free memory with: free -h",
            List.of("memory", "linux"),
            List.of("VM.*"),
            embedding);

    // When: Store the chunk
    vectorStore.store(chunk);

    // When: Search with same embedding (should have perfect similarity)
    List<ScoredChunk> results = vectorStore.search(embedding, 5);

    // Then: Chunk is returned with high similarity score
    assertThat(results).isNotEmpty();
    assertThat(results.get(0).chunk().id()).isEqualTo("chunk-001");
    assertThat(results.get(0).chunk().content()).isEqualTo("Check free memory with: free -h");
    assertThat(results.get(0).similarityScore()).isGreaterThan(0.9);
  }

  @Test
  @DisplayName("Should store batch and return top-K results")
  void storeBatch_ThenSearch_ReturnsTopKChunks() {
    // Given: Multiple runbook chunks with distinct embeddings
    float[] cpuEmbedding = createCategoryEmbedding(768, 0); // CPU category
    float[] memEmbedding = createCategoryEmbedding(768, 1); // Memory category
    float[] diskEmbedding = createCategoryEmbedding(768, 2); // Disk category

    List<RunbookChunk> chunks =
        List.of(
            createChunk("chunk-cpu-1", "CPU troubleshooting", cpuEmbedding),
            createChunk("chunk-mem-1", "Memory troubleshooting", memEmbedding),
            createChunk("chunk-disk-1", "Disk troubleshooting", diskEmbedding));

    // When: Store batch
    vectorStore.storeBatch(chunks);

    // When: Search for memory-related content (using memory embedding)
    List<ScoredChunk> results = vectorStore.search(memEmbedding, 2);

    // Then: Memory chunk is top result
    assertThat(results).hasSize(2);
    assertThat(results.get(0).chunk().id()).isEqualTo("chunk-mem-1");
  }

  @Test
  @DisplayName("Should delete chunks by runbook path")
  void delete_RemovesChunksByRunbookPath() {
    // Given: Chunks from two different runbooks
    vectorStore.store(
        createChunk(
            "chunk-a1", "runbooks/runbook-a.md", "Runbook A content", createRandomEmbedding(768)));
    vectorStore.store(
        createChunk(
            "chunk-b1", "runbooks/runbook-b.md", "Runbook B content", createRandomEmbedding(768)));

    // Verify both are stored
    float[] queryEmbedding = createRandomEmbedding(768);
    assertThat(vectorStore.search(queryEmbedding, 10).size()).isGreaterThanOrEqualTo(2);

    // When: Delete runbook A chunks
    vectorStore.delete("runbooks/runbook-a.md");

    // Then: Only runbook B chunks remain when searching
    List<ScoredChunk> results = vectorStore.search(queryEmbedding, 10);
    assertThat(results).allMatch(sc -> sc.chunk().runbookPath().equals("runbooks/runbook-b.md"));
  }

  @Test
  @DisplayName("Should verify Oracle container is healthy")
  void containerShouldBeHealthy() {
    assertThat(isContainerHealthy()).isTrue();
  }

  // --- Helper methods ---

  private RunbookChunk createChunk(String id, String content, float[] embedding) {
    return new RunbookChunk(
        id, "runbooks/test.md", "Section", content, List.of(), List.of(), embedding);
  }

  private RunbookChunk createChunk(
      String id, String runbookPath, String content, float[] embedding) {
    return new RunbookChunk(id, runbookPath, "Section", content, List.of(), List.of(), embedding);
  }

  /** Creates a random 768-dimensional embedding vector. */
  private float[] createRandomEmbedding(int dimensions) {
    float[] embedding = new float[dimensions];
    java.util.Random random = new java.util.Random();
    for (int i = 0; i < dimensions; i++) {
      embedding[i] = random.nextFloat();
    }
    // Normalize to unit vector
    float norm = 0;
    for (float v : embedding) {
      norm += v * v;
    }
    norm = (float) Math.sqrt(norm);
    for (int i = 0; i < dimensions; i++) {
      embedding[i] /= norm;
    }
    return embedding;
  }

  /** Creates a category-biased embedding for similarity testing. */
  private float[] createCategoryEmbedding(int dimensions, int category) {
    float[] embedding = new float[dimensions];
    // Set a spike at the category position
    int spikeStart = category * (dimensions / 4);
    for (int i = 0; i < dimensions; i++) {
      if (i >= spikeStart && i < spikeStart + dimensions / 4) {
        embedding[i] = 0.8f;
      } else {
        embedding[i] = 0.1f;
      }
    }
    return embedding;
  }
}
