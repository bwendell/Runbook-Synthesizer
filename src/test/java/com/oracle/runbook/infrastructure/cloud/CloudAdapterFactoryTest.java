package com.oracle.runbook.infrastructure.cloud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oracle.runbook.enrichment.LogSourceAdapter;
import com.oracle.runbook.enrichment.MetricsSourceAdapter;
import com.oracle.runbook.enrichment.OciLoggingAdapter;
import com.oracle.runbook.enrichment.OciMonitoringAdapter;
import com.oracle.runbook.infrastructure.cloud.aws.AwsBedrockLlmProvider;
import com.oracle.runbook.infrastructure.cloud.aws.AwsCloudWatchLogsAdapter;
import com.oracle.runbook.infrastructure.cloud.aws.AwsCloudWatchMetricsAdapter;
import com.oracle.runbook.infrastructure.cloud.aws.AwsOpenSearchVectorStoreRepository;
import com.oracle.runbook.infrastructure.cloud.aws.AwsS3StorageAdapter;
import com.oracle.runbook.infrastructure.cloud.aws.AwsSnsAlertSourceAdapter;
import com.oracle.runbook.infrastructure.cloud.local.InMemoryVectorStoreRepository;
import com.oracle.runbook.infrastructure.cloud.oci.OciObjectStorageAdapter;
import com.oracle.runbook.infrastructure.cloud.oci.OciVectorStoreRepository;
import com.oracle.runbook.ingestion.AlertSourceAdapter;
import com.oracle.runbook.rag.LlmProvider;
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
  @DisplayName("Storage adapter creation")
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
  @DisplayName("Metrics adapter creation")
  class MetricsAdapterTests {

    @Test
    @DisplayName("Should return OciMonitoringAdapter.class when provider is 'oci'")
    void shouldReturnOciMetricsAdapterForOci() {
      Config config =
          Config.builder().sources(ConfigSources.create(Map.of("cloud.provider", "oci"))).build();

      CloudAdapterFactory factory = new CloudAdapterFactory(config);

      assertThat(factory.getMetricsAdapterClass())
          .as("Metrics adapter class should be OciMonitoringAdapter for OCI provider")
          .isEqualTo(OciMonitoringAdapter.class);
    }

    @Test
    @DisplayName("Should return AwsCloudWatchMetricsAdapter.class when provider is 'aws'")
    void shouldReturnAwsMetricsAdapterForAws() {
      Config config =
          Config.builder().sources(ConfigSources.create(Map.of("cloud.provider", "aws"))).build();

      CloudAdapterFactory factory = new CloudAdapterFactory(config);

      assertThat(factory.getMetricsAdapterClass())
          .as("Metrics adapter class should be AwsCloudWatchMetricsAdapter for AWS provider")
          .isEqualTo(AwsCloudWatchMetricsAdapter.class);
    }

    @Test
    @DisplayName("Should return class implementing MetricsSourceAdapter interface")
    void shouldReturnMetricsSourceAdapterImplementation() {
      Config config =
          Config.builder().sources(ConfigSources.create(Map.of("cloud.provider", "aws"))).build();

      CloudAdapterFactory factory = new CloudAdapterFactory(config);

      assertThat(MetricsSourceAdapter.class.isAssignableFrom(factory.getMetricsAdapterClass()))
          .as("Returned class should implement MetricsSourceAdapter")
          .isTrue();
    }
  }

  @Nested
  @DisplayName("Logs adapter creation")
  class LogsAdapterTests {

    @Test
    @DisplayName("Should return OciLoggingAdapter.class when provider is 'oci'")
    void shouldReturnOciLogsAdapterForOci() {
      Config config =
          Config.builder().sources(ConfigSources.create(Map.of("cloud.provider", "oci"))).build();

      CloudAdapterFactory factory = new CloudAdapterFactory(config);

      assertThat(factory.getLogsAdapterClass())
          .as("Logs adapter class should be OciLoggingAdapter for OCI provider")
          .isEqualTo(OciLoggingAdapter.class);
    }

    @Test
    @DisplayName("Should return AwsCloudWatchLogsAdapter.class when provider is 'aws'")
    void shouldReturnAwsLogsAdapterForAws() {
      Config config =
          Config.builder().sources(ConfigSources.create(Map.of("cloud.provider", "aws"))).build();

      CloudAdapterFactory factory = new CloudAdapterFactory(config);

      assertThat(factory.getLogsAdapterClass())
          .as("Logs adapter class should be AwsCloudWatchLogsAdapter for AWS provider")
          .isEqualTo(AwsCloudWatchLogsAdapter.class);
    }

    @Test
    @DisplayName("Should return class implementing LogSourceAdapter interface")
    void shouldReturnLogSourceAdapterImplementation() {
      Config config =
          Config.builder().sources(ConfigSources.create(Map.of("cloud.provider", "aws"))).build();

      CloudAdapterFactory factory = new CloudAdapterFactory(config);

      assertThat(LogSourceAdapter.class.isAssignableFrom(factory.getLogsAdapterClass()))
          .as("Returned class should implement LogSourceAdapter")
          .isTrue();
    }
  }

  @Nested
  @DisplayName("Alert source adapter creation")
  class AlertSourceAdapterTests {

    @Test
    @DisplayName("Should return AwsSnsAlertSourceAdapter.class when provider is 'aws'")
    void shouldReturnAwsAlertAdapterForAws() {
      Config config =
          Config.builder().sources(ConfigSources.create(Map.of("cloud.provider", "aws"))).build();

      CloudAdapterFactory factory = new CloudAdapterFactory(config);

      assertThat(factory.getAlertSourceAdapterClass())
          .as("Alert adapter class should be AwsSnsAlertSourceAdapter for AWS provider")
          .isEqualTo(AwsSnsAlertSourceAdapter.class);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when provider is 'oci' (not implemented)")
    void shouldThrowForOciAlertAdapter() {
      Config config =
          Config.builder().sources(ConfigSources.create(Map.of("cloud.provider", "oci"))).build();

      CloudAdapterFactory factory = new CloudAdapterFactory(config);

      assertThatThrownBy(factory::getAlertSourceAdapterClass)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("oci")
          .hasMessageContaining("not supported");
    }

    @Test
    @DisplayName("Should return class implementing AlertSourceAdapter interface")
    void shouldReturnAlertSourceAdapterImplementation() {
      Config config =
          Config.builder().sources(ConfigSources.create(Map.of("cloud.provider", "aws"))).build();

      CloudAdapterFactory factory = new CloudAdapterFactory(config);

      assertThat(AlertSourceAdapter.class.isAssignableFrom(factory.getAlertSourceAdapterClass()))
          .as("Returned class should implement AlertSourceAdapter")
          .isTrue();
    }
  }

  @Nested
  @DisplayName("LLM provider creation")
  class LlmProviderTests {

    @Test
    @DisplayName("Should return AwsBedrockLlmProvider.class when provider is 'aws'")
    void shouldReturnAwsLlmProviderForAws() {
      Config config =
          Config.builder().sources(ConfigSources.create(Map.of("cloud.provider", "aws"))).build();

      CloudAdapterFactory factory = new CloudAdapterFactory(config);

      assertThat(factory.getLlmProviderClass())
          .as("LLM provider class should be AwsBedrockLlmProvider for AWS provider")
          .isEqualTo(AwsBedrockLlmProvider.class);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when provider is 'oci' (not implemented)")
    void shouldThrowForOciLlmProvider() {
      Config config =
          Config.builder().sources(ConfigSources.create(Map.of("cloud.provider", "oci"))).build();

      CloudAdapterFactory factory = new CloudAdapterFactory(config);

      assertThatThrownBy(factory::getLlmProviderClass)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("oci")
          .hasMessageContaining("not supported");
    }

    @Test
    @DisplayName("Should return class implementing LlmProvider interface")
    void shouldReturnLlmProviderImplementation() {
      Config config =
          Config.builder().sources(ConfigSources.create(Map.of("cloud.provider", "aws"))).build();

      CloudAdapterFactory factory = new CloudAdapterFactory(config);

      assertThat(LlmProvider.class.isAssignableFrom(factory.getLlmProviderClass()))
          .as("Returned class should implement LlmProvider")
          .isTrue();
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

  @Nested
  @DisplayName("Vector store adapter creation")
  class VectorStoreAdapterTests {

    @Test
    @DisplayName(
        "Should return InMemoryVectorStoreRepository.class when vectorStore.provider=local")
    void shouldReturnLocalVectorStoreForLocal() {
      Config config =
          Config.builder()
              .sources(ConfigSources.create(Map.of("vectorStore.provider", "local")))
              .build();

      CloudAdapterFactory factory = new CloudAdapterFactory(config);

      assertThat(factory.getVectorStoreClass())
          .as("Vector store class should be InMemoryVectorStoreRepository for local provider")
          .isEqualTo(InMemoryVectorStoreRepository.class);
    }

    @Test
    @DisplayName("Should return OciVectorStoreRepository.class when vectorStore.provider=oci")
    void shouldReturnOciVectorStoreForOci() {
      Config config =
          Config.builder()
              .sources(ConfigSources.create(Map.of("vectorStore.provider", "oci")))
              .build();

      CloudAdapterFactory factory = new CloudAdapterFactory(config);

      assertThat(factory.getVectorStoreClass())
          .as("Vector store class should be OciVectorStoreRepository for oci provider")
          .isEqualTo(OciVectorStoreRepository.class);
    }

    @Test
    @DisplayName(
        "Should return AwsOpenSearchVectorStoreRepository.class when vectorStore.provider=aws")
    void shouldReturnAwsVectorStoreForAws() {
      Config config =
          Config.builder()
              .sources(ConfigSources.create(Map.of("vectorStore.provider", "aws")))
              .build();

      CloudAdapterFactory factory = new CloudAdapterFactory(config);

      assertThat(factory.getVectorStoreClass())
          .as("Vector store class should be AwsOpenSearchVectorStoreRepository for aws provider")
          .isEqualTo(AwsOpenSearchVectorStoreRepository.class);
    }

    @Test
    @DisplayName("Should default to local when vectorStore.provider is not set")
    void shouldDefaultToLocalWhenNotConfigured() {
      Config config = Config.builder().sources(ConfigSources.create(Map.of())).build();

      CloudAdapterFactory factory = new CloudAdapterFactory(config);

      assertThat(factory.getVectorStoreClass())
          .as("Vector store should default to InMemoryVectorStoreRepository when not configured")
          .isEqualTo(InMemoryVectorStoreRepository.class);
    }

    @Test
    @DisplayName("Should return class implementing VectorStoreRepository interface")
    void shouldReturnVectorStoreRepositoryImplementation() {
      Config config =
          Config.builder()
              .sources(ConfigSources.create(Map.of("vectorStore.provider", "local")))
              .build();

      CloudAdapterFactory factory = new CloudAdapterFactory(config);

      assertThat(VectorStoreRepository.class.isAssignableFrom(factory.getVectorStoreClass()))
          .as("Returned class should implement VectorStoreRepository")
          .isTrue();
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for unsupported vector store provider")
    void shouldThrowForUnsupportedVectorStoreProvider() {
      Config config =
          Config.builder()
              .sources(ConfigSources.create(Map.of("vectorStore.provider", "unknown")))
              .build();

      CloudAdapterFactory factory = new CloudAdapterFactory(config);

      assertThatThrownBy(factory::getVectorStoreClass)
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("unknown")
          .hasMessageContaining("Unsupported vector store provider");
    }
  }
}
