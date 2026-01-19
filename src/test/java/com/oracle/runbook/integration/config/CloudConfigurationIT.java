package com.oracle.runbook.integration.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oracle.runbook.infrastructure.cloud.CloudAdapterFactory;
import com.oracle.runbook.infrastructure.cloud.aws.AwsS3StorageAdapter;
import com.oracle.runbook.infrastructure.cloud.oci.OciObjectStorageAdapter;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for cloud provider configuration binding.
 *
 * <p>These tests verify that Helidon configuration correctly binds to cloud provider selection and
 * that error handling provides actionable messages.
 */
class CloudConfigurationIT {

  @Nested
  @DisplayName("OCI Configuration (default)")
  class OciConfigurationTests {

    @Test
    @DisplayName("Should default to OCI when no provider specified")
    void shouldDefaultToOciWhenNoProviderSpecified() {
      Config config = Config.builder().sources(ConfigSources.create(Map.of())).build();

      CloudAdapterFactory factory = new CloudAdapterFactory(config);

      assertThat(factory.getProviderType()).as("Default provider should be 'oci'").isEqualTo("oci");

      assertThat(factory.getStorageAdapterClass())
          .as("Default storage adapter should be OCI")
          .isEqualTo(OciObjectStorageAdapter.class);
    }

    @Test
    @DisplayName("Should use OCI when explicitly configured")
    void shouldUseOciWhenExplicitlyConfigured() {
      Config config =
          Config.builder().sources(ConfigSources.create(Map.of("cloud.provider", "oci"))).build();

      CloudAdapterFactory factory = new CloudAdapterFactory(config);

      assertThat(factory.getProviderType()).isEqualTo("oci");
      assertThat(factory.getStorageAdapterClass()).isEqualTo(OciObjectStorageAdapter.class);
    }
  }

  @Nested
  @DisplayName("AWS Configuration")
  class AwsConfigurationTests {

    @Test
    @DisplayName("Should use AWS when configured")
    void shouldUseAwsWhenConfigured() {
      Config config =
          Config.builder().sources(ConfigSources.create(Map.of("cloud.provider", "aws"))).build();

      CloudAdapterFactory factory = new CloudAdapterFactory(config);

      assertThat(factory.getProviderType()).as("Provider should be 'aws'").isEqualTo("aws");

      assertThat(factory.getStorageAdapterClass())
          .as("Storage adapter should be AWS S3")
          .isEqualTo(AwsS3StorageAdapter.class);
    }

    @Test
    @DisplayName("Should support AWS configuration with additional settings")
    void shouldSupportAwsWithAdditionalSettings() {
      Config config =
          Config.builder()
              .sources(
                  ConfigSources.create(
                      Map.of(
                          "cloud.provider", "aws",
                          "cloud.aws.region", "us-west-2",
                          "cloud.aws.bucket", "my-runbooks")))
              .build();

      CloudAdapterFactory factory = new CloudAdapterFactory(config);

      assertThat(factory.getProviderType()).isEqualTo("aws");

      // Verify nested config is accessible
      assertThat(config.get("cloud.aws.region").asString().orElse("")).isEqualTo("us-west-2");
      assertThat(config.get("cloud.aws.bucket").asString().orElse("")).isEqualTo("my-runbooks");
    }
  }

  @Nested
  @DisplayName("Invalid Configuration")
  class InvalidConfigurationTests {

    @Test
    @DisplayName("Should fail fast with clear error for unknown provider")
    void shouldFailFastForUnknownProvider() {
      Config config =
          Config.builder().sources(ConfigSources.create(Map.of("cloud.provider", "azure"))).build();

      assertThatThrownBy(() -> new CloudAdapterFactory(config))
          .as("Unknown provider should fail with clear error message")
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Unsupported cloud provider")
          .hasMessageContaining("azure")
          .hasMessageContaining("oci")
          .hasMessageContaining("aws");
    }

    @Test
    @DisplayName("Should fail fast with clear error for empty provider")
    void shouldFailFastForEmptyProvider() {
      Config config =
          Config.builder().sources(ConfigSources.create(Map.of("cloud.provider", ""))).build();

      assertThatThrownBy(() -> new CloudAdapterFactory(config))
          .as("Empty provider should fail with clear error message")
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Unsupported cloud provider");
    }

    @Test
    @DisplayName("Should reject null configuration")
    void shouldRejectNullConfiguration() {
      assertThatThrownBy(() -> new CloudAdapterFactory(null))
          .as("Null config should throw NullPointerException")
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("config cannot be null");
    }
  }

  @Nested
  @DisplayName("Provider Switching")
  class ProviderSwitchingTests {

    @Test
    @DisplayName("Should support changing providers via configuration reload")
    void shouldSupportChangingProvidersViaConfigReload() {
      // Initial OCI configuration
      Config ociConfig =
          Config.builder().sources(ConfigSources.create(Map.of("cloud.provider", "oci"))).build();

      CloudAdapterFactory ociFactory = new CloudAdapterFactory(ociConfig);
      assertThat(ociFactory.getProviderType()).isEqualTo("oci");

      // Simulated config reload with AWS
      Config awsConfig =
          Config.builder().sources(ConfigSources.create(Map.of("cloud.provider", "aws"))).build();

      CloudAdapterFactory awsFactory = new CloudAdapterFactory(awsConfig);
      assertThat(awsFactory.getProviderType()).isEqualTo("aws");

      // Verify different adapter classes
      assertThat(ociFactory.getStorageAdapterClass()).isEqualTo(OciObjectStorageAdapter.class);
      assertThat(awsFactory.getStorageAdapterClass()).isEqualTo(AwsS3StorageAdapter.class);
    }
  }
}
