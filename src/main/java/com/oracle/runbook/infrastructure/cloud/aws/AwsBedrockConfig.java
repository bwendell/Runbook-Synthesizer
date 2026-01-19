package com.oracle.runbook.infrastructure.cloud.aws;

import java.util.Objects;

/**
 * Configuration for AWS Bedrock LLM provider.
 *
 * @param region the AWS region (e.g., "us-west-2")
 * @param textModelId the model ID for text generation (e.g.,
 *     "anthropic.claude-3-haiku-20240307-v1:0")
 * @param embeddingModelId the model ID for embeddings (e.g., "cohere.embed-english-v3")
 */
public record AwsBedrockConfig(String region, String textModelId, String embeddingModelId) {
  /** Compact constructor with validation. */
  public AwsBedrockConfig {
    Objects.requireNonNull(region, "region cannot be null");
    Objects.requireNonNull(textModelId, "textModelId cannot be null");
    Objects.requireNonNull(embeddingModelId, "embeddingModelId cannot be null");
  }
}
