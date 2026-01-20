package com.oracle.runbook.config;

import com.oracle.runbook.enrichment.ContextEnrichmentService;
import com.oracle.runbook.enrichment.DefaultContextEnrichmentService;
import com.oracle.runbook.enrichment.LogSourceAdapter;
import com.oracle.runbook.enrichment.MetricsSourceAdapter;
import com.oracle.runbook.infrastructure.cloud.CloudAdapterFactory;
import com.oracle.runbook.infrastructure.cloud.ComputeMetadataAdapter;
import com.oracle.runbook.infrastructure.cloud.VectorStoreRepository;
import com.oracle.runbook.infrastructure.cloud.aws.AwsCloudWatchLogsAdapter;
import com.oracle.runbook.infrastructure.cloud.aws.AwsCloudWatchMetricsAdapter;
import com.oracle.runbook.infrastructure.cloud.aws.AwsEc2MetadataAdapter;
import com.oracle.runbook.infrastructure.cloud.local.InMemoryVectorStoreRepository;
import com.oracle.runbook.infrastructure.llm.OllamaConfig;
import com.oracle.runbook.infrastructure.llm.OllamaLlmProvider;
import com.oracle.runbook.output.WebhookConfig;
import com.oracle.runbook.output.WebhookDestination;
import com.oracle.runbook.output.WebhookDispatcher;
import com.oracle.runbook.output.adapters.FileOutputAdapter;
import com.oracle.runbook.output.adapters.FileOutputConfig;
import com.oracle.runbook.output.adapters.GenericWebhookDestination;
import com.oracle.runbook.rag.ChecklistGenerator;
import com.oracle.runbook.rag.DefaultChecklistGenerator;
import com.oracle.runbook.rag.DefaultEmbeddingService;
import com.oracle.runbook.rag.DefaultRunbookRetriever;
import com.oracle.runbook.rag.EmbeddingService;
import com.oracle.runbook.rag.LlmProvider;
import com.oracle.runbook.rag.RagPipelineService;
import com.oracle.runbook.rag.RunbookRetriever;
import io.helidon.config.Config;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Factory for creating and wiring production services from Helidon Config.
 *
 * <p>This factory reads configuration to create all the required services for the RAG pipeline,
 * including LLM providers, vector stores, context enrichment services, and webhook dispatchers.
 *
 * <p>Services are created lazily on first request and cached for reuse.
 *
 * @see RagPipelineService
 * @see WebhookDispatcher
 */
public class ServiceFactory {

  private static final Logger LOGGER = Logger.getLogger(ServiceFactory.class.getName());

  private final Config config;
  private final CloudAdapterFactory cloudAdapterFactory;
  private final WebhookConfigLoader webhookConfigLoader;

  // Cached instances for reuse
  private LlmProvider cachedLlmProvider;
  private EmbeddingService cachedEmbeddingService;
  private VectorStoreRepository cachedVectorStore;
  private ContextEnrichmentService cachedEnrichmentService;
  private RunbookRetriever cachedRetriever;
  private ChecklistGenerator cachedGenerator;

  /**
   * Creates a new ServiceFactory with the given configuration.
   *
   * @param config the Helidon configuration
   * @throws NullPointerException if config is null
   */
  public ServiceFactory(Config config) {
    this.config = Objects.requireNonNull(config, "config cannot be null");
    this.cloudAdapterFactory = new CloudAdapterFactory(config);
    this.webhookConfigLoader = new WebhookConfigLoader(config);
  }

  /**
   * Creates the complete RagPipelineService with all required dependencies.
   *
   * @return the configured RagPipelineService
   */
  public RagPipelineService createRagPipelineService() {
    return new RagPipelineService(
        createContextEnrichmentService(), createRunbookRetriever(), createChecklistGenerator());
  }

  /**
   * Creates the WebhookDispatcher with all configured destinations.
   *
   * <p>Includes both configured webhooks and file output adapter (if enabled).
   *
   * @return the configured WebhookDispatcher
   */
  public WebhookDispatcher createWebhookDispatcher() {
    List<WebhookDestination> destinations = new ArrayList<>();

    // Add configured webhooks (enabled ones only)
    List<WebhookConfig> webhookConfigs = webhookConfigLoader.loadEnabledWebhookConfigs();
    for (WebhookConfig webhookConfig : webhookConfigs) {
      destinations.add(new GenericWebhookDestination(webhookConfig));
      LOGGER.info("Added webhook destination: " + webhookConfig.name());
    }

    // Add file output adapter if enabled
    if (isFileOutputEnabled()) {
      FileOutputConfig fileConfig = createFileOutputConfig();
      destinations.add(new FileOutputAdapter(fileConfig));
      LOGGER.info("Added file output destination: " + fileConfig.name());
    }

    LOGGER.info("Created WebhookDispatcher with " + destinations.size() + " destination(s)");
    return new WebhookDispatcher(destinations);
  }

  /**
   * Creates the LLM provider based on configuration.
   *
   * <p>Currently only supports Ollama provider for MVP. AWS Bedrock support is available via
   * CloudAdapterFactory for production use.
   *
   * @return the configured LlmProvider
   * @throws IllegalStateException if required Ollama configuration is missing
   */
  public LlmProvider createLlmProvider() {
    if (cachedLlmProvider != null) {
      return cachedLlmProvider;
    }

    String provider = config.get("llm.provider").asString().orElse("ollama");

    if ("ollama".equals(provider)) {
      OllamaConfig ollamaConfig = createOllamaConfig();
      cachedLlmProvider = new OllamaLlmProvider(ollamaConfig);
      LOGGER.info("Created Ollama LLM provider");
    } else {
      throw new IllegalStateException("Unsupported LLM provider: " + provider);
    }

    return cachedLlmProvider;
  }

  /**
   * Creates the embedding service using the configured LLM provider.
   *
   * @return the configured EmbeddingService
   */
  public EmbeddingService createEmbeddingService() {
    if (cachedEmbeddingService != null) {
      return cachedEmbeddingService;
    }

    cachedEmbeddingService = new DefaultEmbeddingService(createLlmProvider());
    LOGGER.info("Created DefaultEmbeddingService");
    return cachedEmbeddingService;
  }

  /**
   * Creates the vector store repository based on configuration.
   *
   * <p>Currently only supports "local" (in-memory) provider for MVP.
   *
   * @return the configured VectorStoreRepository
   */
  public VectorStoreRepository createVectorStoreRepository() {
    if (cachedVectorStore != null) {
      return cachedVectorStore;
    }

    String provider = config.get("vectorStore.provider").asString().orElse("local");

    if ("local".equals(provider)) {
      cachedVectorStore = new InMemoryVectorStoreRepository();
      LOGGER.info("Created InMemoryVectorStoreRepository");
    } else {
      // For now, default to local for unsupported providers
      LOGGER.warning("Unsupported vector store provider '" + provider + "', using local");
      cachedVectorStore = new InMemoryVectorStoreRepository();
    }

    return cachedVectorStore;
  }

  /**
   * Creates the context enrichment service with cloud-specific adapters.
   *
   * @return the configured ContextEnrichmentService
   */
  public ContextEnrichmentService createContextEnrichmentService() {
    if (cachedEnrichmentService != null) {
      return cachedEnrichmentService;
    }

    ComputeMetadataAdapter metadataAdapter = createComputeMetadataAdapter();
    MetricsSourceAdapter metricsAdapter = createMetricsAdapter();
    LogSourceAdapter logsAdapter = createLogsAdapter();

    cachedEnrichmentService =
        new DefaultContextEnrichmentService(metadataAdapter, metricsAdapter, logsAdapter);
    LOGGER.info("Created DefaultContextEnrichmentService");
    return cachedEnrichmentService;
  }

  /**
   * Creates the runbook retriever with embedding service and vector store.
   *
   * @return the configured RunbookRetriever
   */
  public RunbookRetriever createRunbookRetriever() {
    if (cachedRetriever != null) {
      return cachedRetriever;
    }

    cachedRetriever =
        new DefaultRunbookRetriever(createEmbeddingService(), createVectorStoreRepository());
    LOGGER.info("Created DefaultRunbookRetriever");
    return cachedRetriever;
  }

  /**
   * Creates the checklist generator with LLM provider.
   *
   * @return the configured ChecklistGenerator
   */
  public ChecklistGenerator createChecklistGenerator() {
    if (cachedGenerator != null) {
      return cachedGenerator;
    }

    cachedGenerator = new DefaultChecklistGenerator(createLlmProvider());
    LOGGER.info("Created DefaultChecklistGenerator");
    return cachedGenerator;
  }

  // ========== Private Helper Methods ==========

  private OllamaConfig createOllamaConfig() {
    Config ollamaConfig = config.get("llm.ollama");

    String baseUrl =
        ollamaConfig
            .get("baseUrl")
            .asString()
            .orElseThrow(
                () -> new IllegalStateException("Ollama baseUrl is required in configuration"));

    String textModel =
        ollamaConfig
            .get("textModel")
            .asString()
            .orElseThrow(
                () -> new IllegalStateException("Ollama textModel is required in configuration"));

    String embeddingModel =
        ollamaConfig
            .get("embeddingModel")
            .asString()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Ollama embeddingModel is required in configuration"));

    return new OllamaConfig(baseUrl, textModel, embeddingModel);
  }

  private boolean isFileOutputEnabled() {
    return config.get("output.file.enabled").asBoolean().orElse(false);
  }

  private FileOutputConfig createFileOutputConfig() {
    Config fileConfig = config.get("output.file");
    String outputDirectory = fileConfig.get("outputDirectory").asString().orElse("./output");
    String name = fileConfig.get("name").asString().orElse("file-output");
    return new FileOutputConfig(outputDirectory, name);
  }

  private String getAwsRegion() {
    return config.get("cloud.aws.region").asString().orElse("us-west-2");
  }

  private ComputeMetadataAdapter createComputeMetadataAdapter() {
    String provider = cloudAdapterFactory.getProviderType();
    if ("aws".equals(provider)) {
      software.amazon.awssdk.services.ec2.Ec2AsyncClient ec2Client =
          software.amazon.awssdk.services.ec2.Ec2AsyncClient.builder()
              .region(software.amazon.awssdk.regions.Region.of(getAwsRegion()))
              .build();
      return new AwsEc2MetadataAdapter(ec2Client);
    }
    // Default to AWS for now
    software.amazon.awssdk.services.ec2.Ec2AsyncClient ec2Client =
        software.amazon.awssdk.services.ec2.Ec2AsyncClient.builder()
            .region(software.amazon.awssdk.regions.Region.of(getAwsRegion()))
            .build();
    return new AwsEc2MetadataAdapter(ec2Client);
  }

  private MetricsSourceAdapter createMetricsAdapter() {
    String provider = cloudAdapterFactory.getProviderType();
    if ("aws".equals(provider)) {
      software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient cloudWatchClient =
          software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient.builder()
              .region(software.amazon.awssdk.regions.Region.of(getAwsRegion()))
              .build();
      return new AwsCloudWatchMetricsAdapter(cloudWatchClient);
    }
    // Default to AWS for now
    software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient cloudWatchClient =
        software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient.builder()
            .region(software.amazon.awssdk.regions.Region.of(getAwsRegion()))
            .build();
    return new AwsCloudWatchMetricsAdapter(cloudWatchClient);
  }

  private LogSourceAdapter createLogsAdapter() {
    String provider = cloudAdapterFactory.getProviderType();
    String logGroupName =
        config.get("cloud.aws.logs.logGroupName").asString().orElse("/aws/ec2/syslog");
    if ("aws".equals(provider)) {
      software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsAsyncClient logsClient =
          software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsAsyncClient.builder()
              .region(software.amazon.awssdk.regions.Region.of(getAwsRegion()))
              .build();
      return new AwsCloudWatchLogsAdapter(logsClient, logGroupName);
    }
    // Default to AWS for now
    software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsAsyncClient logsClient =
        software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsAsyncClient.builder()
            .region(software.amazon.awssdk.regions.Region.of(getAwsRegion()))
            .build();
    return new AwsCloudWatchLogsAdapter(logsClient, logGroupName);
  }
}
