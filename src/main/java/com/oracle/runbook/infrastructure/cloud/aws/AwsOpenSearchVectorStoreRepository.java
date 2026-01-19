package com.oracle.runbook.infrastructure.cloud.aws;

import com.oracle.runbook.domain.RunbookChunk;
import com.oracle.runbook.infrastructure.cloud.VectorStoreRepository;
import com.oracle.runbook.rag.ScoredChunk;
import java.util.List;

/**
 * AWS OpenSearch implementation of {@link VectorStoreRepository}.
 *
 * <p>This is a stub implementation for future AWS OpenSearch Serverless integration. All methods
 * throw {@link UnsupportedOperationException} until implemented.
 *
 * @see VectorStoreRepository
 */
public class AwsOpenSearchVectorStoreRepository implements VectorStoreRepository {

  @Override
  public String providerType() {
    return "aws";
  }

  @Override
  public void store(RunbookChunk chunk) {
    throw new UnsupportedOperationException(
        "AWS OpenSearch vector store is not yet implemented. Use 'local' or 'oci' provider.");
  }

  @Override
  public void storeBatch(List<RunbookChunk> chunks) {
    throw new UnsupportedOperationException(
        "AWS OpenSearch vector store is not yet implemented. Use 'local' or 'oci' provider.");
  }

  @Override
  public List<ScoredChunk> search(float[] queryEmbedding, int topK) {
    throw new UnsupportedOperationException(
        "AWS OpenSearch vector store is not yet implemented. Use 'local' or 'oci' provider.");
  }

  @Override
  public void delete(String runbookPath) {
    throw new UnsupportedOperationException(
        "AWS OpenSearch vector store is not yet implemented. Use 'local' or 'oci' provider.");
  }
}
