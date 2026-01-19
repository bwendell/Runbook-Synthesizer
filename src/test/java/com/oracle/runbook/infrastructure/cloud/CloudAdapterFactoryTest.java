package com.oracle.runbook.infrastructure.cloud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oracle.runbook.infrastructure.cloud.aws.AwsS3StorageAdapter;
import com.oracle.runbook.infrastructure.cloud.oci.OciObjectStorageAdapter;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CloudAdapterFactory}.
 *
 * <p>Tests follow TDD red-green-refactor cycle. Written FIRST before implementation.
 */
class CloudAdapterFactoryTest {

  @Nested
  @DisplayName("Provider selection based on configuration")
  class ProviderSelectionTests {

    @Test
    @DisplayName("Should return 'oci' as provider type when cloud.provider=oci")
    void shouldReturnOciProviderType() {
      Config config =
          Config.builder().sources(ConfigSources.create(Map.of("cloud.provider", "oci"))).build();

      CloudAdapterFactory factory = new CloudAdapterFactory(config);

      assertThat(factory.getProviderType())
          .as("Provider type should be 'oci' when cloud.provider=oci")
          .isEqualTo("oci");
    }

    @Test
    @DisplayName("Should return 'aws' as provider type when cloud.provider=aws")
    void shouldReturnAwsProviderType() {
      Config config =
          Config.builder().sources(ConfigSources.create(Map.of("cloud.provider", "aws"))).build();

      CloudAdapterFactory factory = new CloudAdapterFactory(config);

      assertThat(factory.getProviderType())
          .as("Provider type should be 'aws' when cloud.provider=aws")
          .isEqualTo("aws");
    }

    @Test
    @DisplayName("Should default to 'oci' when cloud.provider is not set")
    void shouldDefaultToOciWhenNotConfigured() {
      Config config = Config.builder().sources(ConfigSources.create(Map.of())).build();

      CloudAdapterFactory factory = new CloudAdapterFactory(config);

      assertThat(factory.getProviderType())
          .as("Provider type should default to 'oci' when not configured")
          .isEqualTo("oci");
    }
  }

  @Nested
  @DisplayName("Adapter creation")
  class AdapterCreationTests {

    @Test
    @DisplayName("Should create OCI storage adapter when provider is 'oci'")
    void shouldCreateOciStorageAdapterType() {
      Config config =
          Config.builder().sources(ConfigSources.create(Map.of("cloud.provider", "oci"))).build();

      CloudAdapterFactory factory = new CloudAdapterFactory(config);

      assertThat(factory.getStorageAdapterClass())
          .as("Storage adapter class should be OciObjectStorageAdapter for OCI provider")
          .isEqualTo(OciObjectStorageAdapter.class);
    }

    @Test
    @DisplayName("Should create AWS storage adapter when provider is 'aws'")
    void shouldCreateAwsStorageAdapterType() {
      Config config =
          Config.builder().sources(ConfigSources.create(Map.of("cloud.provider", "aws"))).build();

      CloudAdapterFactory factory = new CloudAdapterFactory(config);

      assertThat(factory.getStorageAdapterClass())
          .as("Storage adapter class should be AwsS3StorageAdapter for AWS provider")
          .isEqualTo(AwsS3StorageAdapter.class);
    }
  }

  @Nested
  @DisplayName("Error handling")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Should throw IllegalArgumentException for unknown provider")
    void shouldThrowForUnknownProvider() {
      Config config =
          Config.builder().sources(ConfigSources.create(Map.of("cloud.provider", "azure"))).build();

      assertThatThrownBy(() -> new CloudAdapterFactory(config))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("azure")
          .hasMessageContaining("Unsupported cloud provider");
    }

    @Test
    @DisplayName("Error message should list supported providers")
    void errorMessageShouldListSupportedProviders() {
      Config config =
          Config.builder().sources(ConfigSources.create(Map.of("cloud.provider", "gcp"))).build();

      assertThatThrownBy(() -> new CloudAdapterFactory(config))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("oci")
          .hasMessageContaining("aws");
    }

    @Test
    @DisplayName("Should throw NullPointerException for null config")
    void shouldThrowForNullConfig() {
      assertThatThrownBy(() -> new CloudAdapterFactory(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("config");
    }
  }
}
