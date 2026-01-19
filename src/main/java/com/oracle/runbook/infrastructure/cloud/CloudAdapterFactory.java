package com.oracle.runbook.infrastructure.cloud;

import com.oracle.runbook.infrastructure.cloud.aws.AwsEc2MetadataAdapter;
import com.oracle.runbook.infrastructure.cloud.aws.AwsS3StorageAdapter;
import com.oracle.runbook.infrastructure.cloud.oci.OciComputeMetadataAdapter;
import com.oracle.runbook.infrastructure.cloud.oci.OciObjectStorageAdapter;
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
  private static final String DEFAULT_PROVIDER = "oci";
  private static final Set<String> SUPPORTED_PROVIDERS = Set.of("oci", "aws");

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
}
