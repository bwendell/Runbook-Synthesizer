package com.oracle.runbook.infrastructure.cloud.aws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.oracle.runbook.domain.GenerationConfig;
import com.oracle.runbook.rag.LlmProvider;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;

/**
 * AWS Bedrock implementation of {@link LlmProvider}.
 *
 * <p>Provides LLM capabilities using AWS Bedrock for production deployments. Uses Claude 3 Haiku
 * for text generation and Cohere Embed v3 for embeddings.
 *
 * <p>Uses BedrockRuntimeAsyncClient for non-blocking operations compatible with Helidon SE's
 * reactive patterns.
 */
public class AwsBedrockLlmProvider implements LlmProvider {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final AwsBedrockConfig config;
  private final BedrockRuntimeAsyncClient bedrockClient;

  /**
   * Creates a new AwsBedrockLlmProvider.
   *
   * @param config the Bedrock configuration
   * @param bedrockClient the AWS Bedrock async client
   * @throws NullPointerException if config or bedrockClient is null
   */
  public AwsBedrockLlmProvider(AwsBedrockConfig config, BedrockRuntimeAsyncClient bedrockClient) {
    this.config = Objects.requireNonNull(config, "config cannot be null");
    this.bedrockClient = Objects.requireNonNull(bedrockClient, "bedrockClient cannot be null");
  }

  @Override
  public String providerId() {
    return "aws-bedrock";
  }

  @Override
  public CompletableFuture<String> generateText(String prompt, GenerationConfig genConfig) {
    String modelId = genConfig.modelOverride().orElse(config.textModelId());

    // Build Claude Messages API request
    String requestBody;
    try {
      ObjectNode request = OBJECT_MAPPER.createObjectNode();
      request.put("anthropic_version", "bedrock-2023-05-31");
      request.put("max_tokens", genConfig.maxTokens());
      request.put("temperature", genConfig.temperature());

      ArrayNode messages = request.putArray("messages");
      ObjectNode message = messages.addObject();
      message.put("role", "user");
      message.put("content", prompt);

      requestBody = OBJECT_MAPPER.writeValueAsString(request);
    } catch (Exception e) {
      return CompletableFuture.failedFuture(e);
    }

    InvokeModelRequest invokeRequest =
        InvokeModelRequest.builder()
            .modelId(modelId)
            .contentType("application/json")
            .accept("application/json")
            .body(SdkBytes.fromString(requestBody, StandardCharsets.UTF_8))
            .build();

    return bedrockClient
        .invokeModel(invokeRequest)
        .thenApply(
            response -> {
              try {
                String responseBody = response.body().asUtf8String();
                JsonNode node = OBJECT_MAPPER.readTree(responseBody);
                // Claude Messages API response format
                JsonNode content = node.get("content");
                if (content != null && content.isArray() && !content.isEmpty()) {
                  return content.get(0).get("text").asText();
                }
                throw new RuntimeException("Unexpected Claude response format");
              } catch (Exception e) {
                throw new RuntimeException("Failed to parse Claude response", e);
              }
            });
  }

  @Override
  public CompletableFuture<float[]> generateEmbedding(String text) {
    return generateEmbeddings(List.of(text))
        .thenApply(
            embeddings -> {
              if (embeddings.isEmpty()) {
                throw new RuntimeException("No embeddings returned");
              }
              return embeddings.get(0);
            });
  }

  @Override
  public CompletableFuture<List<float[]>> generateEmbeddings(List<String> texts) {
    if (texts.isEmpty()) {
      return CompletableFuture.completedFuture(List.of());
    }

    // Build Cohere Embed request
    String requestBody;
    try {
      ObjectNode request = OBJECT_MAPPER.createObjectNode();
      ArrayNode textsArray = request.putArray("texts");
      for (String text : texts) {
        textsArray.add(text);
      }
      request.put("input_type", "search_document");

      requestBody = OBJECT_MAPPER.writeValueAsString(request);
    } catch (Exception e) {
      return CompletableFuture.failedFuture(e);
    }

    InvokeModelRequest invokeRequest =
        InvokeModelRequest.builder()
            .modelId(config.embeddingModelId())
            .contentType("application/json")
            .accept("application/json")
            .body(SdkBytes.fromString(requestBody, StandardCharsets.UTF_8))
            .build();

    return bedrockClient
        .invokeModel(invokeRequest)
        .thenApply(
            response -> {
              try {
                String responseBody = response.body().asUtf8String();
                JsonNode node = OBJECT_MAPPER.readTree(responseBody);
                // Cohere Embed response format
                JsonNode embeddingsNode = node.get("embeddings");
                if (embeddingsNode == null || !embeddingsNode.isArray()) {
                  throw new RuntimeException("Unexpected Cohere response format");
                }

                List<float[]> embeddings = new ArrayList<>();
                for (JsonNode embeddingArray : embeddingsNode) {
                  float[] embedding = new float[embeddingArray.size()];
                  for (int i = 0; i < embeddingArray.size(); i++) {
                    embedding[i] = (float) embeddingArray.get(i).asDouble();
                  }
                  embeddings.add(embedding);
                }
                return embeddings;
              } catch (Exception e) {
                throw new RuntimeException("Failed to parse Cohere response", e);
              }
            });
  }
}
