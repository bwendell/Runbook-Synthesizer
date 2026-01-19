package com.oracle.runbook.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oracle.runbook.infrastructure.cloud.CloudAdapterFactory;
import com.oracle.runbook.infrastructure.cloud.CloudStorageAdapter;
import com.oracle.runbook.infrastructure.cloud.ComputeMetadataAdapter;
import com.oracle.runbook.infrastructure.cloud.aws.AwsEc2MetadataAdapter;
import com.oracle.runbook.infrastructure.cloud.aws.AwsS3StorageAdapter;
import com.oracle.runbook.infrastructure.cloud.oci.OciComputeMetadataAdapter;
import com.oracle.runbook.infrastructure.cloud.oci.OciObjectStorageAdapter;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link CloudAdapterFactory} provider switching.
 *
 * <p>Verifies that the factory correctly selects OCI or AWS adapters based on the {@code
 * cloud.provider} configuration.
 */
class CloudProviderSwitchingIT {

  @Nested
  @DisplayName("OCI provider configuration")
  class OciProviderTests {

    @Test
    @DisplayName("Factory should return OCI adapters when cloud.provider=oci")
    void factoryShouldReturnOciAdaptersWhenProviderIsOci() {
      Config config = createConfig("oci");
      CloudAdapterFactory factory = new CloudAdapterFactory(config);

      assertThat(factory.getProviderType()).isEqualTo("oci");
      assertThat(factory.getStorageAdapterClass()).isEqualTo(OciObjectStorageAdapter.class);
      assertThat(factory.getComputeMetadataAdapterClass())
          .isEqualTo(OciComputeMetadataAdapter.class);
    }

    @Test
    @DisplayName("OCI storage adapter should implement CloudStorageAdapter")
    void ociStorageAdapterShouldImplementCloudStorageAdapter() {
      Config config = createConfig("oci");
      CloudAdapterFactory factory = new CloudAdapterFactory(config);

      Class<?> adapterClass = factory.getStorageAdapterClass();

      assertThat(CloudStorageAdapter.class).isAssignableFrom(adapterClass);
    }

    @Test
    @DisplayName("OCI compute metadata adapter should implement ComputeMetadataAdapter")
    void ociComputeMetadataAdapterShouldImplementComputeMetadataAdapter() {
      Config config = createConfig("oci");
      CloudAdapterFactory factory = new CloudAdapterFactory(config);

      Class<?> adapterClass = factory.getComputeMetadataAdapterClass();

      assertThat(ComputeMetadataAdapter.class).isAssignableFrom(adapterClass);
    }
  }

  @Nested
  @DisplayName("AWS provider configuration")
  class AwsProviderTests {

    @Test
    @DisplayName("Factory should return AWS adapters when cloud.provider=aws")
    void factoryShouldReturnAwsAdaptersWhenProviderIsAws() {
      Config config = createConfig("aws");
      CloudAdapterFactory factory = new CloudAdapterFactory(config);

      assertThat(factory.getProviderType()).isEqualTo("aws");
      assertThat(factory.getStorageAdapterClass()).isEqualTo(AwsS3StorageAdapter.class);
      assertThat(factory.getComputeMetadataAdapterClass()).isEqualTo(AwsEc2MetadataAdapter.class);
    }

    @Test
    @DisplayName("AWS storage adapter should implement CloudStorageAdapter")
    void awsStorageAdapterShouldImplementCloudStorageAdapter() {
      Config config = createConfig("aws");
      CloudAdapterFactory factory = new CloudAdapterFactory(config);

      Class<?> adapterClass = factory.getStorageAdapterClass();

      assertThat(CloudStorageAdapter.class).isAssignableFrom(adapterClass);
    }

    @Test
    @DisplayName("AWS compute metadata adapter should implement ComputeMetadataAdapter")
    void awsComputeMetadataAdapterShouldImplementComputeMetadataAdapter() {
      Config config = createConfig("aws");
      CloudAdapterFactory factory = new CloudAdapterFactory(config);

      Class<?> adapterClass = factory.getComputeMetadataAdapterClass();

      assertThat(ComputeMetadataAdapter.class).isAssignableFrom(adapterClass);
    }
  }

  @Nested
  @DisplayName("Default provider configuration")
  class DefaultProviderTests {

    @Test
    @DisplayName("Factory should default to OCI when provider not specified")
    void factoryShouldDefaultToOciWhenProviderNotSpecified() {
      Config config = Config.empty();
      CloudAdapterFactory factory = new CloudAdapterFactory(config);

      assertThat(factory.getProviderType()).isEqualTo("oci");
      assertThat(factory.getStorageAdapterClass()).isEqualTo(OciObjectStorageAdapter.class);
    }
  }

  @Nested
  @DisplayName("Invalid provider configuration")
  class InvalidProviderTests {

    @Test
    @DisplayName("Factory should throw exception for unsupported provider")
    void factoryShouldThrowForUnsupportedProvider() {
      Config config = createConfig("gcp");

      assertThatThrownBy(() -> new CloudAdapterFactory(config))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Unsupported cloud provider")
          .hasMessageContaining("gcp");
    }
  }

  /**
   * Creates a minimal Config with the specified cloud provider.
   *
   * @param provider the provider value (e.g., "oci", "aws")
   * @return a Config instance with cloud.provider set
   */
  private Config createConfig(String provider) {
    return Config.create(ConfigSources.create(Map.of("cloud.provider", provider)));
  }
}
