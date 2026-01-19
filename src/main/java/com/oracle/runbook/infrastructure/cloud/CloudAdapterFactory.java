package com.oracle.runbook.infrastructure.cloud;

import com.oracle.runbook.enrichment.LogSourceAdapter;
import com.oracle.runbook.enrichment.MetricsSourceAdapter;
import com.oracle.runbook.enrichment.OciLoggingAdapter;
import com.oracle.runbook.enrichment.OciMonitoringAdapter;
import com.oracle.runbook.infrastructure.cloud.aws.AwsBedrockLlmProvider;
import com.oracle.runbook.infrastructure.cloud.aws.AwsCloudWatchLogsAdapter;
import com.oracle.runbook.infrastructure.cloud.aws.AwsCloudWatchMetricsAdapter;
import com.oracle.runbook.infrastructure.cloud.aws.AwsEc2MetadataAdapter;
import com.oracle.runbook.infrastructure.cloud.aws.AwsOpenSearchVectorStoreRepository;
import com.oracle.runbook.infrastructure.cloud.aws.AwsS3StorageAdapter;
import com.oracle.runbook.infrastructure.cloud.aws.AwsSnsAlertSourceAdapter;
import com.oracle.runbook.infrastructure.cloud.local.InMemoryVectorStoreRepository;
import com.oracle.runbook.infrastructure.cloud.oci.OciComputeMetadataAdapter;
import com.oracle.runbook.infrastructure.cloud.oci.OciObjectStorageAdapter;
import com.oracle.runbook.infrastructure.cloud.oci.OciVectorStoreRepository;
import com.oracle.runbook.ingestion.AlertSourceAdapter;
import com.oracle.runbook.rag.LlmProvider;
import io.helidon.config.Config;
import java.util.Objects;
import java.util.Set;

/**
 * Factory for creating cloud provider-specific adapters based on configuration.
 *
 * <p>Reads the {@code cloud.provider} configuration property to determine which cloud provider's
 * adapters to use. Supports OCI (default) and AWS providers.
 *
 * <p>Example configuration:
 *
 * <pre>
 * cloud:
 *   provider: oci  # or "aws"
 * </pre>
 */
public class CloudAdapterFactory {

  private static final String CONFIG_KEY_PROVIDER = "cloud.provider";
  private static final String CONFIG_KEY_VECTOR_STORE_PROVIDER = "vectorStore.provider";
  private static final String DEFAULT_PROVIDER = "oci";
  private static final String DEFAULT_VECTOR_STORE_PROVIDER = "local";
  private static final Set<String> SUPPORTED_PROVIDERS = Set.of("oci", "aws");
  private static final Set<String> SUPPORTED_VECTOR_STORE_PROVIDERS = Set.of("local", "oci", "aws");

  private final String providerType;
  private final Config config;

  /**
   * Creates a new CloudAdapterFactory with the specified configuration.
   *
   * @param config the Helidon configuration containing cloud provider settings
   * @throws NullPointerException if config is null
   * @throws IllegalArgumentException if the configured provider is not supported
   */
  public CloudAdapterFactory(Config config) {
    this.config = Objects.requireNonNull(config, "config cannot be null");
    this.providerType = config.get(CONFIG_KEY_PROVIDER).asString().orElse(DEFAULT_PROVIDER);

    if (!SUPPORTED_PROVIDERS.contains(providerType)) {
      throw new IllegalArgumentException(
          String.format(
              "Unsupported cloud provider: '%s'. Supported providers are: %s",
              providerType, SUPPORTED_PROVIDERS));
    }
  }

  /**
   * Returns the configured cloud provider type.
   *
   * @return the provider type (e.g., "oci" or "aws")
   */
  public String getProviderType() {
    return providerType;
  }

  /**
   * Returns the class of the storage adapter for the configured provider.
   *
   * <p>This method is primarily used for testing and type verification. To create actual adapter
   * instances, use {@link #createStorageAdapter(Object...)} (when implemented).
   *
   * @return the storage adapter class for the configured provider
   */
  public Class<? extends CloudStorageAdapter> getStorageAdapterClass() {
    return switch (providerType) {
      case "oci" -> OciObjectStorageAdapter.class;
      case "aws" -> AwsS3StorageAdapter.class;
      default -> throw new IllegalStateException("Unknown provider: " + providerType);
    };
  }

  /**
   * Returns the class of the compute metadata adapter for the configured provider.
   *
   * <p>This method is primarily used for testing and type verification. To create actual adapter
   * instances, use {@link #createComputeMetadataAdapter(Object...)} (when implemented).
   *
   * @return the compute metadata adapter class for the configured provider
   */
  public Class<? extends ComputeMetadataAdapter> getComputeMetadataAdapterClass() {
    return switch (providerType) {
      case "oci" -> OciComputeMetadataAdapter.class;
      case "aws" -> AwsEc2MetadataAdapter.class;
      default -> throw new IllegalStateException("Unknown provider: " + providerType);
    };
  }

  /**
   * Returns the class of the metrics adapter for the configured provider.
   *
   * @return the metrics adapter class for the configured provider
   */
  public Class<? extends MetricsSourceAdapter> getMetricsAdapterClass() {
    return switch (providerType) {
      case "oci" -> OciMonitoringAdapter.class;
      case "aws" -> AwsCloudWatchMetricsAdapter.class;
      default -> throw new IllegalStateException("Unknown provider: " + providerType);
    };
  }

  /**
   * Returns the class of the logs adapter for the configured provider.
   *
   * @return the logs adapter class for the configured provider
   */
  public Class<? extends LogSourceAdapter> getLogsAdapterClass() {
    return switch (providerType) {
      case "oci" -> OciLoggingAdapter.class;
      case "aws" -> AwsCloudWatchLogsAdapter.class;
      default -> throw new IllegalStateException("Unknown provider: " + providerType);
    };
  }

  /**
   * Returns the class of the alert source adapter for the configured provider.
   *
   * <p>Note: OCI alert source adapter is not yet implemented.
   *
   * @return the alert source adapter class for the configured provider
   * @throws IllegalStateException if the configured provider does not support alert adapters
   */
  public Class<? extends AlertSourceAdapter> getAlertSourceAdapterClass() {
    return switch (providerType) {
      case "aws" -> AwsSnsAlertSourceAdapter.class;
      case "oci" ->
          throw new IllegalStateException(
              "Alert source adapter for 'oci' is not supported. Use 'aws' provider.");
      default -> throw new IllegalStateException("Unknown provider: " + providerType);
    };
  }

  /**
   * Returns the class of the LLM provider for the configured cloud provider.
   *
   * <p>Note: OCI LLM provider is not yet implemented.
   *
   * @return the LLM provider class for the configured provider
   * @throws IllegalStateException if the configured provider does not support LLM
   */
  public Class<? extends LlmProvider> getLlmProviderClass() {
    return switch (providerType) {
      case "aws" -> AwsBedrockLlmProvider.class;
      case "oci" ->
          throw new IllegalStateException(
              "LLM provider for 'oci' is not supported. Use 'aws' provider.");
      default -> throw new IllegalStateException("Unknown provider: " + providerType);
    };
  }

  /**
   * Returns the class of the vector store repository for the configured provider.
   *
   * <p>Vector store provider is configured separately from cloud provider via {@code
   * vectorStore.provider} config key, defaulting to "local".
   *
   * @return the vector store repository class for the configured provider
   * @throws IllegalArgumentException if the configured vector store provider is not supported
   */
  public Class<? extends VectorStoreRepository> getVectorStoreClass() {
    String vectorStoreProvider =
        config
            .get(CONFIG_KEY_VECTOR_STORE_PROVIDER)
            .asString()
            .orElse(DEFAULT_VECTOR_STORE_PROVIDER);

    if (!SUPPORTED_VECTOR_STORE_PROVIDERS.contains(vectorStoreProvider)) {
      throw new IllegalArgumentException(
          String.format(
              "Unsupported vector store provider: '%s'. Supported providers are: %s",
              vectorStoreProvider, SUPPORTED_VECTOR_STORE_PROVIDERS));
    }

    return switch (vectorStoreProvider) {
      case "local" -> InMemoryVectorStoreRepository.class;
      case "oci" -> OciVectorStoreRepository.class;
      case "aws" -> AwsOpenSearchVectorStoreRepository.class;
      default ->
          throw new IllegalStateException("Unknown vector store provider: " + vectorStoreProvider);
    };
  }
}
