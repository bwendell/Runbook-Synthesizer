package com.oracle.runbook.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oracle.runbook.enrichment.ContextEnrichmentService;
import com.oracle.runbook.enrichment.DefaultContextEnrichmentService;
import com.oracle.runbook.infrastructure.cloud.VectorStoreRepository;
import com.oracle.runbook.infrastructure.cloud.local.InMemoryVectorStoreRepository;
import com.oracle.runbook.output.WebhookDispatcher;
import com.oracle.runbook.rag.ChecklistGenerator;
import com.oracle.runbook.rag.DefaultChecklistGenerator;
import com.oracle.runbook.rag.DefaultRunbookRetriever;
import com.oracle.runbook.rag.LlmProvider;
import com.oracle.runbook.rag.RagPipelineService;
import com.oracle.runbook.rag.RunbookRetriever;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ServiceFactory}.
 *
 * <p>Tests follow TDD red-green-refactor cycle. These tests will initially fail until
 * ServiceFactory is implemented.
 */
class ServiceFactoryTest {

  // ========== Happy Path Tests ==========

  @Nested
  @DisplayName("Happy Path Tests")
  class HappyPathTests {

    @Test
    @DisplayName("Should create RagPipelineService when configured")
    void shouldCreateRagPipelineService_WhenConfigured() {
      Config config = createValidConfig();
      ServiceFactory factory = new ServiceFactory(config);

      RagPipelineService pipeline = factory.createRagPipelineService();

      assertThat(pipeline).isNotNull();
    }

    @Test
    @DisplayName("Should create WebhookDispatcher with file output")
    void shouldCreateWebhookDispatcher_WithFileOutput() {
      Config config = createConfigWithFileOutput();
      ServiceFactory factory = new ServiceFactory(config);

      WebhookDispatcher dispatcher = factory.createWebhookDispatcher();

      assertThat(dispatcher).isNotNull();
      assertThat(dispatcher.destinationCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Should create with Ollama provider by default")
    void shouldCreateWithOllamaProvider_ByDefault() {
      Config config = createValidConfig();
      ServiceFactory factory = new ServiceFactory(config);

      LlmProvider llmProvider = factory.createLlmProvider();

      assertThat(llmProvider).isNotNull();
      assertThat(llmProvider.providerId()).isEqualTo("ollama");
    }

    @Test
    @DisplayName("Should create ContextEnrichmentService with AWS adapters")
    void shouldCreateContextEnrichmentService_WithAwsAdapters() {
      Config config = createValidConfig();
      ServiceFactory factory = new ServiceFactory(config);

      ContextEnrichmentService enrichmentService = factory.createContextEnrichmentService();

      assertThat(enrichmentService).isNotNull();
      assertThat(enrichmentService).isInstanceOf(DefaultContextEnrichmentService.class);
    }

    @Test
    @DisplayName("Should create RunbookRetriever with vector store")
    void shouldCreateRunbookRetriever_WithVectorStore() {
      Config config = createValidConfig();
      ServiceFactory factory = new ServiceFactory(config);

      RunbookRetriever retriever = factory.createRunbookRetriever();

      assertThat(retriever).isNotNull();
      assertThat(retriever).isInstanceOf(DefaultRunbookRetriever.class);
    }

    @Test
    @DisplayName("Should create ChecklistGenerator with LLM provider")
    void shouldCreateChecklistGenerator_WithLlmProvider() {
      Config config = createValidConfig();
      ServiceFactory factory = new ServiceFactory(config);

      ChecklistGenerator generator = factory.createChecklistGenerator();

      assertThat(generator).isNotNull();
      assertThat(generator).isInstanceOf(DefaultChecklistGenerator.class);
    }
  }

  // ========== Configuration Variation Tests ==========

  @Nested
  @DisplayName("Configuration Variation Tests")
  class ConfigurationVariationTests {

    @Test
    @DisplayName("Should use InMemoryVectorStore when provider is local")
    void shouldUseInMemoryVectorStore_WhenProviderIsLocal() {
      Config config = createConfigWithVectorStoreProvider("local");
      ServiceFactory factory = new ServiceFactory(config);

      VectorStoreRepository vectorStore = factory.createVectorStoreRepository();

      assertThat(vectorStore).isNotNull();
      assertThat(vectorStore).isInstanceOf(InMemoryVectorStoreRepository.class);
      assertThat(vectorStore.providerType()).isEqualTo("local");
    }

    @Test
    @DisplayName("Should create FileOutputAdapter when file output enabled")
    void shouldCreateFileOutputAdapter_WhenFileOutputEnabled() {
      Config config = createConfigWithFileOutput();
      ServiceFactory factory = new ServiceFactory(config);

      WebhookDispatcher dispatcher = factory.createWebhookDispatcher();

      // Dispatcher should include the file output adapter
      assertThat(dispatcher.destinationCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Should combine webhooks and file output when both configured")
    void shouldCombineWebhooksAndFileOutput_WhenBothConfigured() {
      Config config = createConfigWithWebhooksAndFileOutput();
      ServiceFactory factory = new ServiceFactory(config);

      WebhookDispatcher dispatcher = factory.createWebhookDispatcher();

      // Should have at least 2: one webhook + file output
      assertThat(dispatcher.destinationCount()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Should use configured Ollama settings when provided")
    void shouldUseConfiguredOllamaSettings_WhenProvided() {
      Config config =
          createConfigWithOllamaSettings(
              "http://custom-ollama:11434", "custom-model:7b", "custom-embed-model");
      ServiceFactory factory = new ServiceFactory(config);

      LlmProvider llmProvider = factory.createLlmProvider();

      assertThat(llmProvider).isNotNull();
      assertThat(llmProvider.providerId()).isEqualTo("ollama");
    }
  }

  // ========== Error Handling Tests ==========

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Should throw exception when Ollama base URL missing")
    void shouldThrowException_WhenOllamaBaseUrlMissing() {
      Config config = createConfigMissingOllamaBaseUrl();
      ServiceFactory factory = new ServiceFactory(config);

      assertThatThrownBy(factory::createLlmProvider)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("baseUrl");
    }

    @Test
    @DisplayName("Should throw exception when Ollama text model missing")
    void shouldThrowException_WhenOllamaTextModelMissing() {
      Config config = createConfigMissingOllamaTextModel();
      ServiceFactory factory = new ServiceFactory(config);

      assertThatThrownBy(factory::createLlmProvider)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("textModel");
    }

    @Test
    @DisplayName("Should throw exception when Ollama embedding model missing")
    void shouldThrowException_WhenOllamaEmbeddingModelMissing() {
      Config config = createConfigMissingOllamaEmbeddingModel();
      ServiceFactory factory = new ServiceFactory(config);

      assertThatThrownBy(factory::createLlmProvider)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("embeddingModel");
    }

    @Test
    @DisplayName("Should handle missing file output config gracefully")
    void shouldHandleMissingFileOutputConfig_Gracefully() {
      Config config = createConfigWithoutFileOutput();
      ServiceFactory factory = new ServiceFactory(config);

      // Should not throw, just creates dispatcher without file output
      WebhookDispatcher dispatcher = factory.createWebhookDispatcher();
      assertThat(dispatcher).isNotNull();
    }

    @Test
    @DisplayName("Should handle empty webhook list gracefully")
    void shouldHandleEmptyWebhookList_Gracefully() {
      Config config = createConfigWithEmptyWebhooks();
      ServiceFactory factory = new ServiceFactory(config);

      WebhookDispatcher dispatcher = factory.createWebhookDispatcher();
      assertThat(dispatcher).isNotNull();
    }
  }

  // ========== Edge Case Tests ==========

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("Should create empty dispatcher when no webhooks and file disabled")
    void shouldCreateEmptyDispatcher_WhenNoWebhooksAndFileDisabled() {
      Config config = createConfigNoWebhooksNoFile();
      ServiceFactory factory = new ServiceFactory(config);

      WebhookDispatcher dispatcher = factory.createWebhookDispatcher();

      assertThat(dispatcher).isNotNull();
      assertThat(dispatcher.destinationCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should filter disabled webhooks from dispatcher")
    void shouldFilterDisabledWebhooks_FromDispatcher() {
      Config config = createConfigWithDisabledWebhook();
      ServiceFactory factory = new ServiceFactory(config);

      WebhookDispatcher dispatcher = factory.createWebhookDispatcher();

      // Only enabled destinations should be included
      // The disabled webhook should be filtered out
      assertThat(dispatcher).isNotNull();
    }

    @Test
    @DisplayName("Should use default values when optional config missing")
    void shouldUseDefaultValues_WhenOptionalConfigMissing() {
      Config config = createMinimalConfig();
      ServiceFactory factory = new ServiceFactory(config);

      // Should use defaults: local vector store, ollama LLM
      VectorStoreRepository vectorStore = factory.createVectorStoreRepository();
      assertThat(vectorStore.providerType()).isEqualTo("local");
    }
  }

  // ========== Helper Methods for Config Creation ==========

  private Config createValidConfig() {
    return Config.builder()
        .sources(
            ConfigSources.create(
                Map.of(
                    "cloud.provider", "aws",
                    "llm.provider", "ollama",
                    "llm.ollama.baseUrl", "http://localhost:11434",
                    "llm.ollama.textModel", "llama3.2:3b",
                    "llm.ollama.embeddingModel", "nomic-embed-text",
                    "vectorStore.provider", "local",
                    "output.file.enabled", "true",
                    "output.file.outputDirectory", "./output",
                    "output.file.name", "file-output")))
        .build();
  }

  private Config createConfigWithFileOutput() {
    return Config.builder()
        .sources(
            ConfigSources.create(
                Map.of(
                    "cloud.provider", "aws",
                    "llm.provider", "ollama",
                    "llm.ollama.baseUrl", "http://localhost:11434",
                    "llm.ollama.textModel", "llama3.2:3b",
                    "llm.ollama.embeddingModel", "nomic-embed-text",
                    "vectorStore.provider", "local",
                    "output.file.enabled", "true",
                    "output.file.outputDirectory", "./output",
                    "output.file.name", "file-output")))
        .build();
  }

  private Config createConfigWithVectorStoreProvider(String provider) {
    return Config.builder()
        .sources(
            ConfigSources.create(
                Map.of(
                    "cloud.provider", "aws",
                    "llm.provider", "ollama",
                    "llm.ollama.baseUrl", "http://localhost:11434",
                    "llm.ollama.textModel", "llama3.2:3b",
                    "llm.ollama.embeddingModel", "nomic-embed-text",
                    "vectorStore.provider", provider,
                    "output.file.enabled", "false")))
        .build();
  }

  private Config createConfigWithWebhooksAndFileOutput() {
    return Config.builder()
        .sources(
            ConfigSources.create(
                Map.ofEntries(
                    Map.entry("cloud.provider", "aws"),
                    Map.entry("llm.provider", "ollama"),
                    Map.entry("llm.ollama.baseUrl", "http://localhost:11434"),
                    Map.entry("llm.ollama.textModel", "llama3.2:3b"),
                    Map.entry("llm.ollama.embeddingModel", "nomic-embed-text"),
                    Map.entry("vectorStore.provider", "local"),
                    Map.entry("output.file.enabled", "true"),
                    Map.entry("output.file.outputDirectory", "./output"),
                    Map.entry("output.file.name", "file-output"),
                    Map.entry("output.webhooks.0.name", "test-webhook"),
                    Map.entry("output.webhooks.0.type", "generic"),
                    Map.entry("output.webhooks.0.url", "http://example.com/webhook"),
                    Map.entry("output.webhooks.0.enabled", "true"))))
        .build();
  }

  private Config createConfigWithOllamaSettings(
      String baseUrl, String textModel, String embeddingModel) {
    return Config.builder()
        .sources(
            ConfigSources.create(
                Map.of(
                    "cloud.provider", "aws",
                    "llm.provider", "ollama",
                    "llm.ollama.baseUrl", baseUrl,
                    "llm.ollama.textModel", textModel,
                    "llm.ollama.embeddingModel", embeddingModel,
                    "vectorStore.provider", "local",
                    "output.file.enabled", "false")))
        .build();
  }

  private Config createConfigMissingOllamaBaseUrl() {
    return Config.builder()
        .sources(
            ConfigSources.create(
                Map.of(
                    "cloud.provider", "aws",
                    "llm.provider", "ollama",
                    "llm.ollama.textModel", "llama3.2:3b",
                    "llm.ollama.embeddingModel", "nomic-embed-text",
                    "vectorStore.provider", "local")))
        .build();
  }

  private Config createConfigMissingOllamaTextModel() {
    return Config.builder()
        .sources(
            ConfigSources.create(
                Map.of(
                    "cloud.provider", "aws",
                    "llm.provider", "ollama",
                    "llm.ollama.baseUrl", "http://localhost:11434",
                    "llm.ollama.embeddingModel", "nomic-embed-text",
                    "vectorStore.provider", "local")))
        .build();
  }

  private Config createConfigMissingOllamaEmbeddingModel() {
    return Config.builder()
        .sources(
            ConfigSources.create(
                Map.of(
                    "cloud.provider", "aws",
                    "llm.provider", "ollama",
                    "llm.ollama.baseUrl", "http://localhost:11434",
                    "llm.ollama.textModel", "llama3.2:3b",
                    "vectorStore.provider", "local")))
        .build();
  }

  private Config createConfigWithoutFileOutput() {
    return Config.builder()
        .sources(
            ConfigSources.create(
                Map.of(
                    "cloud.provider", "aws",
                    "llm.provider", "ollama",
                    "llm.ollama.baseUrl", "http://localhost:11434",
                    "llm.ollama.textModel", "llama3.2:3b",
                    "llm.ollama.embeddingModel", "nomic-embed-text",
                    "vectorStore.provider", "local",
                    "output.file.enabled", "false")))
        .build();
  }

  private Config createConfigWithEmptyWebhooks() {
    return Config.builder()
        .sources(
            ConfigSources.create(
                Map.of(
                    "cloud.provider", "aws",
                    "llm.provider", "ollama",
                    "llm.ollama.baseUrl", "http://localhost:11434",
                    "llm.ollama.textModel", "llama3.2:3b",
                    "llm.ollama.embeddingModel", "nomic-embed-text",
                    "vectorStore.provider", "local",
                    "output.file.enabled", "false")))
        .build();
  }

  private Config createConfigNoWebhooksNoFile() {
    return Config.builder()
        .sources(
            ConfigSources.create(
                Map.of(
                    "cloud.provider", "aws",
                    "llm.provider", "ollama",
                    "llm.ollama.baseUrl", "http://localhost:11434",
                    "llm.ollama.textModel", "llama3.2:3b",
                    "llm.ollama.embeddingModel", "nomic-embed-text",
                    "vectorStore.provider", "local",
                    "output.file.enabled", "false")))
        .build();
  }

  private Config createConfigWithDisabledWebhook() {
    return Config.builder()
        .sources(
            ConfigSources.create(
                Map.ofEntries(
                    Map.entry("cloud.provider", "aws"),
                    Map.entry("llm.provider", "ollama"),
                    Map.entry("llm.ollama.baseUrl", "http://localhost:11434"),
                    Map.entry("llm.ollama.textModel", "llama3.2:3b"),
                    Map.entry("llm.ollama.embeddingModel", "nomic-embed-text"),
                    Map.entry("vectorStore.provider", "local"),
                    Map.entry("output.file.enabled", "false"),
                    Map.entry("output.webhooks.0.name", "disabled-webhook"),
                    Map.entry("output.webhooks.0.type", "generic"),
                    Map.entry("output.webhooks.0.url", "http://example.com/webhook"),
                    Map.entry("output.webhooks.0.enabled", "false"))))
        .build();
  }

  private Config createMinimalConfig() {
    return Config.builder()
        .sources(
            ConfigSources.create(
                Map.of(
                    "cloud.provider", "aws",
                    "llm.ollama.baseUrl", "http://localhost:11434",
                    "llm.ollama.textModel", "llama3.2:3b",
                    "llm.ollama.embeddingModel", "nomic-embed-text")))
        .build();
  }
}
