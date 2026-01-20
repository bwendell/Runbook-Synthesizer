package com.oracle.runbook.integration.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.oracle.runbook.api.AlertResource;
import com.oracle.runbook.domain.*;
import com.oracle.runbook.enrichment.ContextEnrichmentService;
import com.oracle.runbook.infrastructure.cloud.local.InMemoryVectorStoreRepository;
import com.oracle.runbook.output.WebhookDispatcher;
import com.oracle.runbook.output.adapters.FileOutputAdapter;
import com.oracle.runbook.output.adapters.FileOutputConfig;
import com.oracle.runbook.rag.*;
import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.*;

/**
 * Integration tests for manual alarm trigger scenarios.
 *
 * <p>Tests the complete flow of triggering alerts via HTTP POST to the /api/v1/alerts endpoint
 * using various alarm formats (standard, high-memory, CPU, minimal).
 *
 * <p>These tests are demo-ready and validate that the pipeline can process alerts from HTTP POST.
 * Note: CloudWatch/SNS envelope parsing is handled by {@link
 * com.oracle.runbook.infrastructure.cloud.aws.AwsSnsAlertSourceAdapter} and tested separately.
 */
@ServerTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ManualAlarmTriggerIT {

  private static Path outputDir;
  private static InMemoryVectorStoreRepository vectorStore;
  private final Http1Client client;

  ManualAlarmTriggerIT(Http1Client client) {
    this.client = client;
  }

  @SetUpRoute
  static void route(HttpRouting.Builder routing) {
    try {
      // Create temp output directory
      outputDir = Files.createTempDirectory("manual-trigger-output");

      // Initialize vector store with sample data
      vectorStore = new InMemoryVectorStoreRepository();
      seedVectorStore();

      // Create test services
      TestLlmProvider llmProvider = new TestLlmProvider();
      TestEmbeddingService embeddingService = new TestEmbeddingService();
      TestEnrichmentService enrichmentService = new TestEnrichmentService();
      DefaultRunbookRetriever retriever =
          new DefaultRunbookRetriever(embeddingService, vectorStore);
      DefaultChecklistGenerator generator = new DefaultChecklistGenerator(llmProvider);
      RagPipelineService ragPipeline =
          new RagPipelineService(enrichmentService, retriever, generator);

      // Create file output
      FileOutputConfig fileConfig = new FileOutputConfig(outputDir.toString(), "manual-test");
      FileOutputAdapter fileAdapter = new FileOutputAdapter(fileConfig);
      WebhookDispatcher dispatcher = new WebhookDispatcher(List.of(fileAdapter));

      // Register alert resource in real mode
      routing.register("/api/v1/alerts", new AlertResource(ragPipeline, dispatcher, false));
    } catch (Exception e) {
      throw new RuntimeException("Failed to set up routes", e);
    }
  }

  private static void seedVectorStore() {
    vectorStore.store(
        new RunbookChunk(
            "chunk-memory-001",
            "memory-troubleshooting.md",
            "Memory Troubleshooting",
            "Check memory usage with free -h. Monitor swap usage.",
            List.of("memory", "oom"),
            List.of("*"),
            new float[] {0.9f, 0.1f, 0.0f, 0.0f}));
    vectorStore.store(
        new RunbookChunk(
            "chunk-cpu-001",
            "cpu-troubleshooting.md",
            "CPU Troubleshooting",
            "Use top or htop to identify CPU-intensive processes.",
            List.of("cpu", "load"),
            List.of("*"),
            new float[] {0.1f, 0.9f, 0.0f, 0.0f}));
  }

  @AfterAll
  static void cleanup() throws Exception {
    if (outputDir != null) {
      Files.walk(outputDir)
          .sorted((a, b) -> -a.compareTo(b))
          .forEach(
              path -> {
                try {
                  Files.deleteIfExists(path);
                } catch (Exception ignored) {
                }
              });
    }
  }

  @Test
  @Order(1)
  @DisplayName("Should trigger alarm via HTTP POST with high memory alert")
  void shouldTriggerAlarmViaHttpPost() {
    // Given - Standard alert request format
    String alertPayload =
        """
        {
          "title": "High Memory Utilization",
          "message": "Memory utilization exceeded 90% threshold on test-instance",
          "severity": "CRITICAL",
          "sourceService": "cloudwatch",
          "dimensions": {
            "instanceId": "i-test12345678",
            "region": "us-west-2"
          }
        }
        """;

    // When - POST to alerts endpoint
    try (Http1ClientResponse response =
        client
            .post("/api/v1/alerts")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit(alertPayload)) {

      // Then - should return 200 OK
      assertThat(response.status()).isEqualTo(Status.OK_200);

      // Then - response should contain checklist
      String body = response.as(String.class);
      JsonObject json = parseJson(body);
      assertThat(json.containsKey("alertId")).isTrue();
      assertThat(json.containsKey("steps")).isTrue();
      assertThat(json.getJsonArray("steps")).isNotEmpty();
    }
  }

  @Test
  @Order(2)
  @DisplayName("Should trigger alarm via HTTP POST with CPU alert")
  void shouldTriggerAlarmViaCpuPayload() {
    // Given - CPU alert in standard format
    String cpuPayload =
        """
        {
          "title": "High CPU Utilization",
          "message": "CPU usage exceeded 85% threshold",
          "severity": "WARNING",
          "sourceService": "cloudwatch",
          "dimensions": {
            "instanceId": "i-cpu12345678",
            "metricName": "CPUUtilization"
          }
        }
        """;

    // When - POST to alerts endpoint
    try (Http1ClientResponse response =
        client
            .post("/api/v1/alerts")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit(cpuPayload)) {

      // Then - should return 200 OK
      assertThat(response.status()).isEqualTo(Status.OK_200);

      // Then - response should contain valid checklist
      String body = response.as(String.class);
      JsonObject json = parseJson(body);
      assertThat(json.containsKey("alertId")).isTrue();
      assertThat(json.getJsonArray("steps")).isNotEmpty();
    }
  }

  @Test
  @Order(3)
  @DisplayName("Should trigger alarm with OCI-style alert payload")
  void shouldTriggerAlarmWithOciStylePayload() {
    // Given - OCI-style alert in standard format
    String ociPayload =
        """
        {
          "title": "OCI High CPU Usage Alarm",
          "message": "CPU usage has exceeded 90% threshold for the past 5 minutes",
          "severity": "CRITICAL",
          "sourceService": "oci-monitoring",
          "dimensions": {
            "resourceId": "ocid1.instance.oc1.iad.example",
            "availabilityDomain": "AD-1"
          }
        }
        """;

    // When - POST to alerts endpoint
    try (Http1ClientResponse response =
        client
            .post("/api/v1/alerts")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit(ociPayload)) {

      // Then - should return 200 OK
      assertThat(response.status()).isEqualTo(Status.OK_200);

      // Then - response should contain checklist
      String body = response.as(String.class);
      JsonObject json = parseJson(body);
      assertThat(json.containsKey("alertId")).isTrue();
      assertThat(json.containsKey("steps")).isTrue();
    }
  }

  @Test
  @Order(4)
  @DisplayName("Should trigger alarm with minimal required payload fields")
  void shouldTriggerAlarmWithMinimalPayload() {
    // Given - Minimal alert request with just required fields
    String minimalPayload =
        """
        {
          "title": "Minimal Test Alert",
          "message": "This is a minimal test alert",
          "severity": "WARNING"
        }
        """;

    // When - POST to alerts endpoint
    try (Http1ClientResponse response =
        client
            .post("/api/v1/alerts")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit(minimalPayload)) {

      // Then - should return 200 OK
      assertThat(response.status()).isEqualTo(Status.OK_200);

      // Then - response should contain checklist
      String body = response.as(String.class);
      JsonObject json = parseJson(body);
      assertThat(json.containsKey("alertId")).isTrue();
      assertThat(json.containsKey("steps")).isTrue();
    }
  }

  @Test
  @Order(5)
  @DisplayName("Should create output file for triggered alarm")
  void shouldCreateOutputFileForTriggeredAlarm() throws Exception {
    // Given - an alarm that should generate a file
    String alertPayload =
        """
        {
          "title": "Disk Space Alert",
          "message": "Disk utilization exceeded 95% threshold",
          "severity": "CRITICAL",
          "sourceService": "monitoring"
        }
        """;

    // When - POST to alerts endpoint
    try (Http1ClientResponse response =
        client
            .post("/api/v1/alerts")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit(alertPayload)) {

      assertThat(response.status()).isEqualTo(Status.OK_200);
    }

    // Then - verify file was created
    Path[] outputFiles =
        Files.list(outputDir)
            .filter(p -> p.getFileName().toString().startsWith("checklist-"))
            .toArray(Path[]::new);

    assertThat(outputFiles).hasSizeGreaterThanOrEqualTo(1);

    // Then - verify file contains valid JSON
    String lastFileContent = Files.readString(outputFiles[outputFiles.length - 1]);
    assertThat(lastFileContent).contains("\"alertId\"");
    assertThat(lastFileContent).contains("\"steps\"");
  }

  @Test
  @Order(6)
  @DisplayName("AlertTriggerHelper should create valid CloudWatch alarm payloads")
  void alertTriggerHelperShouldCreateValidCloudWatchPayload() {
    // Given - CloudWatch alarm created by helper
    String alarm = AlertTriggerHelper.createCloudWatchAlarm("TestAlarm", "MemoryUtilization", 90.0);

    // Then - should be valid SNS-wrapped JSON
    JsonObject snsEnvelope = parseJson(alarm);
    assertThat(snsEnvelope.getString("Type")).isEqualTo("Notification");
    assertThat(snsEnvelope.containsKey("Message")).isTrue();

    // And - inner message should be valid CloudWatch alarm
    JsonObject cwAlarm = AlertTriggerHelper.parseFromSnsMessage(alarm);
    assertThat(cwAlarm.getString("AlarmName")).isEqualTo("TestAlarm");
    assertThat(cwAlarm.getString("NewStateValue")).isEqualTo("ALARM");
  }

  @Test
  @Order(7)
  @DisplayName("AlertTriggerHelper should create valid OCI alarm payloads")
  void alertTriggerHelperShouldCreateValidOciPayload() {
    // Given - OCI alarm created by helper
    String alarm =
        AlertTriggerHelper.createOciMonitoringAlarm("TestOciAlarm", "ocid1.instance.test");

    // Then - should be valid OCI event JSON
    JsonObject event = parseJson(alarm);
    assertThat(event.getString("type")).isEqualTo("com.oraclecloud.monitoring.alarmstatechange");
    assertThat(event.getJsonObject("data").getString("alarmName")).isEqualTo("TestOciAlarm");
    assertThat(event.getJsonObject("data").getString("currentState")).isEqualTo("FIRING");
  }

  // ========== Helper Methods ==========

  private JsonObject parseJson(String json) {
    try (JsonReader reader = Json.createReader(new StringReader(json))) {
      return reader.readObject();
    }
  }

  // ========== Test Implementations ==========

  private static class TestLlmProvider implements LlmProvider {
    private final String response =
        "Troubleshooting Checklist\n\n"
            + "Step 1: Check system metrics with monitoring tools\n"
            + "Step 2: Review recent application logs\n"
            + "Step 3: Identify resource-intensive processes\n"
            + "Step 4: Apply appropriate remediation steps";

    @Override
    public String providerId() {
      return "test-llm";
    }

    @Override
    public CompletableFuture<String> generateText(String prompt, GenerationConfig config) {
      return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<float[]> generateEmbedding(String text) {
      return CompletableFuture.completedFuture(new float[] {0.5f, 0.5f, 0.0f, 0.0f});
    }

    @Override
    public CompletableFuture<List<float[]>> generateEmbeddings(List<String> texts) {
      return CompletableFuture.completedFuture(
          texts.stream().map(t -> new float[] {0.5f, 0.5f, 0.0f, 0.0f}).toList());
    }
  }

  private static class TestEmbeddingService implements EmbeddingService {
    @Override
    public CompletableFuture<float[]> embed(String text) {
      return CompletableFuture.completedFuture(new float[] {0.85f, 0.15f, 0.0f, 0.0f});
    }

    @Override
    public CompletableFuture<List<float[]>> embedBatch(List<String> texts) {
      return CompletableFuture.completedFuture(texts.stream().map(t -> embed(t).join()).toList());
    }

    @Override
    public CompletableFuture<float[]> embedContext(EnrichedContext context) {
      return embed(context.alert().title() + " " + context.alert().message());
    }
  }

  private static class TestEnrichmentService implements ContextEnrichmentService {
    @Override
    public CompletableFuture<EnrichedContext> enrich(Alert alert) {
      ResourceMetadata resource =
          new ResourceMetadata(
              alert.dimensions().getOrDefault("instanceId", "test-instance"),
              "test-server",
              null,
              "t3.medium",
              "us-east-1a",
              Map.of("environment", "test"),
              Map.of());

      List<MetricSnapshot> metrics =
          List.of(
              new MetricSnapshot("CPUUtilization", "AWS/EC2", 75.0, "%", Instant.now()),
              new MetricSnapshot("MemoryUtilization", "AWS/EC2", 65.0, "%", Instant.now()));

      List<LogEntry> logs =
          List.of(
              new LogEntry(
                  "log-test-001",
                  Instant.now().minusSeconds(60),
                  "INFO",
                  "Test log entry",
                  Map.of("hostname", "test-server")));

      return CompletableFuture.completedFuture(
          new EnrichedContext(alert, resource, metrics, logs, Map.of()));
    }
  }
}
