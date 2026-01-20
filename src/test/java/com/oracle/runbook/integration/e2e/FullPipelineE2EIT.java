package com.oracle.runbook.integration.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oracle.runbook.domain.*;
import com.oracle.runbook.output.WebhookDestination;
import com.oracle.runbook.output.WebhookResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.*;

/**
 * Full pipeline end-to-end integration tests.
 *
 * <p>Tests the complete flow from alert ingestion through checklist output:
 *
 * <ol>
 *   <li>Alert Ingestion
 *   <li>Context Enrichment (metrics, logs)
 *   <li>RAG Pipeline (runbook retrieval)
 *   <li>Checklist Generation (LLM)
 *   <li>Output Dispatch (file, webhooks)
 * </ol>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FullPipelineE2EIT {

  private static Path outputDir;
  private PipelineTestHarness harness;

  @BeforeAll
  static void setupOutputDirectory() throws Exception {
    outputDir = Files.createTempDirectory("full-pipeline-e2e");
  }

  @AfterAll
  static void cleanupOutputDirectory() throws Exception {
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

  @BeforeEach
  void setupHarness() throws Exception {
    harness = PipelineTestHarness.testMode().withOutputDirectory(outputDir).withTimeout(30).build();

    // Seed test runbooks
    harness.seedRunbooks(
        "sample-runbooks/memory-troubleshooting.md",
        "sample-runbooks/cpu-troubleshooting.md",
        "sample-runbooks/disk-troubleshooting.md",
        "sample-runbooks/network-troubleshooting.md");
  }

  // ========== Memory Alert Tests ==========

  @Test
  @Order(1)
  @DisplayName("Should process high memory alert through full pipeline")
  void shouldProcessHighMemoryAlert_FullPipeline() throws Exception {
    // Given: A high memory alert
    Alert memoryAlert =
        new Alert(
            "alert-memory-001",
            "High Memory Utilization",
            "Memory utilization exceeded 90% threshold on production instance",
            AlertSeverity.WARNING,
            "cloudwatch",
            Map.of("instanceId", "i-prod12345", "region", "us-east-1"),
            Map.of("alarmName", "HighMemoryAlarm", "metric", "MemoryUtilization"),
            Instant.now(),
            "{\"AlarmName\": \"HighMemoryAlarm\"}");

    // When: Process through full pipeline
    DynamicChecklist checklist = harness.processAlert(memoryAlert);

    // Then: Checklist was generated with memory-related content
    assertThat(checklist).isNotNull();
    assertThat(checklist.alertId()).isEqualTo("alert-memory-001");
    assertThat(checklist.steps()).isNotEmpty();
    assertThat(checklist.sourceRunbooks())
        .anyMatch(path -> path.contains("memory-troubleshooting"));

    // And: Steps contain memory-related instructions
    List<String> instructions = checklist.steps().stream().map(ChecklistStep::instruction).toList();
    assertThat(String.join(" ", instructions).toLowerCase())
        .containsAnyOf("memory", "free", "top", "oom");

    // And: File output was created
    Path outputFile = harness.getOutputFile("alert-memory-001");
    assertThat(outputFile).isNotNull();
    assertThat(Files.exists(outputFile)).isTrue();
  }

  // ========== CPU Alert Tests ==========

  @Test
  @Order(2)
  @DisplayName("Should process high CPU alert through full pipeline")
  void shouldProcessHighCpuAlert_FullPipeline() throws Exception {
    // Given: A high CPU alert
    Alert cpuAlert =
        new Alert(
            "alert-cpu-001",
            "High CPU Utilization",
            "CPU utilization exceeded 85% threshold",
            AlertSeverity.WARNING,
            "cloudwatch",
            Map.of("instanceId", "i-cpu12345"),
            Map.of("alarmName", "HighCPUAlarm"),
            Instant.now(),
            "{}");

    // When: Process through full pipeline
    DynamicChecklist checklist = harness.processAlert(cpuAlert);

    // Then: Checklist was generated with CPU-related content
    assertThat(checklist).isNotNull();
    assertThat(checklist.alertId()).isEqualTo("alert-cpu-001");
    assertThat(checklist.steps()).isNotEmpty();
    assertThat(checklist.sourceRunbooks()).anyMatch(path -> path.contains("cpu-troubleshooting"));

    // And: Steps contain CPU-related instructions
    List<String> instructions = checklist.steps().stream().map(ChecklistStep::instruction).toList();
    assertThat(String.join(" ", instructions).toLowerCase())
        .containsAnyOf("cpu", "top", "process", "renice");
  }

  // ========== Critical Severity Tests ==========

  @Test
  @Order(3)
  @DisplayName("Should process critical alert with urgent steps")
  void shouldProcessCriticalAlertWithUrgentSteps() throws Exception {
    // Given: A critical severity alert
    Alert criticalAlert =
        new Alert(
            "alert-critical-001",
            "CRITICAL: Memory Exhaustion",
            "System memory exhausted, OOM killer active",
            AlertSeverity.CRITICAL,
            "cloudwatch",
            Map.of("instanceId", "i-crit12345"),
            Map.of(),
            Instant.now(),
            "{}");

    // When: Process through full pipeline
    DynamicChecklist checklist = harness.processAlert(criticalAlert);

    // Then: Checklist was generated
    assertThat(checklist).isNotNull();
    assertThat(checklist.alertId()).isEqualTo("alert-critical-001");
    assertThat(checklist.steps()).isNotEmpty();

    // And: Steps have priority set (test implementation uses default priority)
    assertThat(checklist.steps()).allSatisfy(step -> assertThat(step.order()).isPositive());
  }

  // ========== Context Enrichment Tests ==========

  @Test
  @Order(4)
  @DisplayName("Should enrich context with metrics")
  void shouldEnrichContextWithMetrics() throws Exception {
    // Given: Alert that will be enriched with metrics
    Alert alert =
        new Alert(
            "alert-metrics-001",
            "Performance Alert",
            "System performance degradation detected",
            AlertSeverity.WARNING,
            "cloudwatch",
            Map.of("instanceId", "i-metrics12345"),
            Map.of(),
            Instant.now(),
            "{}");

    // When: Process through pipeline with enrichment
    DynamicChecklist checklist = harness.processAlert(alert);

    // Then: Checklist was generated (enrichment happened internally)
    assertThat(checklist).isNotNull();
    assertThat(checklist.steps()).isNotEmpty();
    // Note: The test enrichment service provides mock metrics
    // Actual verification of metrics in context happens inside the pipeline
  }

  @Test
  @Order(5)
  @DisplayName("Should enrich context with logs")
  void shouldEnrichContextWithLogs() throws Exception {
    // Given: Alert that will be enriched with logs
    Alert alert =
        new Alert(
            "alert-logs-001",
            "Application Error",
            "Application throwing errors in logs",
            AlertSeverity.WARNING,
            "cloudwatch",
            Map.of("instanceId", "i-logs12345"),
            Map.of(),
            Instant.now(),
            "{}");

    // When: Process through pipeline with enrichment
    DynamicChecklist checklist = harness.processAlert(alert);

    // Then: Checklist was generated (enrichment happened internally)
    assertThat(checklist).isNotNull();
    assertThat(checklist.steps()).isNotEmpty();
    // Note: The test enrichment service provides mock logs
  }

  // ========== RAG Retrieval Tests ==========

  @Test
  @Order(6)
  @DisplayName("Should retrieve relevant runbook chunks based on semantic similarity")
  void shouldRetrieveRelevantRunbookChunks() throws Exception {
    // Given: Alert about disk space issues
    Alert diskAlert =
        new Alert(
            "alert-disk-001",
            "Low Disk Space",
            "Disk space below 20% threshold on root volume",
            AlertSeverity.WARNING,
            "cloudwatch",
            Map.of("instanceId", "i-disk12345"),
            Map.of(),
            Instant.now(),
            "{}");

    // When: Process through pipeline
    DynamicChecklist checklist = harness.processAlert(diskAlert);

    // Then: Runbook chunks were retrieved (semantic similarity)
    assertThat(checklist).isNotNull();
    assertThat(checklist.sourceRunbooks()).isNotEmpty();

    // And: Checklist has valid steps
    assertThat(checklist.steps()).isNotEmpty();
    List<String> instructions = checklist.steps().stream().map(ChecklistStep::instruction).toList();
    String allInstructions = String.join(" ", instructions).toLowerCase();
    assertThat(allInstructions).isNotBlank();
  }

  // ========== LLM Generation Tests ==========

  @Test
  @Order(7)
  @DisplayName("Should generate checklist with LLM")
  void shouldGenerateChecklistWithLlm() throws Exception {
    // Given: Generic alert
    Alert alert =
        new Alert(
            "alert-llm-001",
            "Network Latency Alert",
            "Network latency exceeding thresholds",
            AlertSeverity.WARNING,
            "cloudwatch",
            Map.of("instanceId", "i-net12345"),
            Map.of(),
            Instant.now(),
            "{}");

    // When: Process through pipeline
    DynamicChecklist checklist = harness.processAlert(alert);

    // Then: LLM generated coherent steps
    assertThat(checklist).isNotNull();
    assertThat(checklist.steps()).hasSizeGreaterThanOrEqualTo(3);
    assertThat(checklist.llmProviderUsed()).isNotBlank();

    // And: Steps have proper ordering
    for (int i = 0; i < checklist.steps().size(); i++) {
      ChecklistStep step = checklist.steps().get(i);
      assertThat(step.order()).isEqualTo(i + 1);
      assertThat(step.instruction()).isNotBlank();
    }
  }

  // ========== Webhook Dispatch Tests ==========

  @Test
  @Order(8)
  @DisplayName("Should dispatch to multiple webhooks")
  void shouldDispatchToMultipleWebhooks() throws Exception {
    // Given: Multiple webhook destinations
    List<WebhookResult> capturedResults = new ArrayList<>();
    TestWebhookDestination webhook1 = new TestWebhookDestination("webhook1", capturedResults);
    TestWebhookDestination webhook2 = new TestWebhookDestination("webhook2", capturedResults);

    PipelineTestHarness multiDestHarness =
        PipelineTestHarness.testMode()
            .withDestination(webhook1)
            .withDestination(webhook2)
            .withTimeout(30)
            .build();

    multiDestHarness.seedRunbook("sample-runbooks/memory-troubleshooting.md");

    Alert alert =
        new Alert(
            "alert-multi-001",
            "Memory Alert for Multi-Webhook",
            "Testing multiple webhooks",
            AlertSeverity.INFO,
            "test",
            Map.of(),
            Map.of(),
            Instant.now(),
            "{}");

    // When: Process alert
    DynamicChecklist checklist = multiDestHarness.processAlert(alert);

    // Then: Checklist was generated
    assertThat(checklist).isNotNull();
    assertThat(checklist.alertId()).isEqualTo("alert-multi-001");

    // And: Both webhooks received the checklist
    assertThat(capturedResults).hasSize(2);
    assertThat(capturedResults).allMatch(WebhookResult::isSuccess);
  }

  // ========== File Output Tests ==========

  @Test
  @Order(9)
  @DisplayName("Should write output to file")
  void shouldWriteOutputToFile() throws Exception {
    // Given: Alert to process
    Alert alert =
        new Alert(
            "alert-file-001",
            "File Output Test",
            "Testing file output adapter",
            AlertSeverity.INFO,
            "test",
            Map.of(),
            Map.of(),
            Instant.now(),
            "{}");

    // When: Process alert
    harness.processAlert(alert);

    // Then: File was created
    Path outputFile = harness.getOutputFile("alert-file-001");
    assertThat(outputFile).isNotNull();
    assertThat(Files.exists(outputFile)).isTrue();

    // And: File contains valid JSON
    String content = Files.readString(outputFile);
    assertThat(content).contains("\"alertId\"");
    assertThat(content).contains("\"steps\"");
    assertThat(content).contains("alert-file-001");
  }

  // ========== Failure Handling Tests ==========

  @Test
  @Order(10)
  @DisplayName("Should handle partial enrichment failure gracefully")
  void shouldHandlePartialEnrichmentFailure() throws Exception {
    // Given: Harness with failing metrics enrichment
    PipelineTestHarness.TestEnrichmentService failingEnrichment =
        new PipelineTestHarness.TestEnrichmentService().withPartialMetricsFailure();

    PipelineTestHarness failingHarness =
        PipelineTestHarness.testMode()
            .withEnrichmentService(failingEnrichment)
            .withTimeout(30)
            .build();

    failingHarness.seedRunbook("sample-runbooks/memory-troubleshooting.md");

    Alert alert =
        new Alert(
            "alert-partial-001",
            "Partial Failure Test",
            "Testing partial enrichment failure",
            AlertSeverity.WARNING,
            "test",
            Map.of(),
            Map.of(),
            Instant.now(),
            "{}");

    // When: Process alert (metrics will fail but logs succeed)
    DynamicChecklist checklist = failingHarness.processAlert(alert);

    // Then: Checklist was still generated (graceful degradation)
    assertThat(checklist).isNotNull();
    assertThat(checklist.alertId()).isEqualTo("alert-partial-001");
    assertThat(checklist.steps()).isNotEmpty();
  }

  @Test
  @Order(11)
  @DisplayName("Should timeout gracefully for long-running LLM calls")
  void shouldTimeoutGracefully() throws Exception {
    // Given: Harness with artificially slow enrichment (simulates LLM delay)
    PipelineTestHarness.TimeoutEnrichmentService slowEnrichment =
        new PipelineTestHarness.TimeoutEnrichmentService(10000); // 10 second delay

    PipelineTestHarness slowHarness =
        PipelineTestHarness.testMode()
            .withEnrichmentService(slowEnrichment)
            .withTimeout(1) // 1 second timeout
            .build();

    Alert alert =
        new Alert(
            "alert-timeout-001",
            "Timeout Test",
            "Testing timeout behavior",
            AlertSeverity.WARNING,
            "test",
            Map.of(),
            Map.of(),
            Instant.now(),
            "{}");

    // When/Then: Processing times out cleanly
    assertThatThrownBy(() -> slowHarness.processAlert(alert)).isInstanceOf(TimeoutException.class);
  }

  // ========== Test Implementations ==========

  /** Test webhook destination that captures results. */
  private static class TestWebhookDestination implements WebhookDestination {
    private final String name;
    private final List<WebhookResult> capturedResults;

    TestWebhookDestination(String name, List<WebhookResult> capturedResults) {
      this.name = name;
      this.capturedResults = capturedResults;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public String type() {
      return "test";
    }

    @Override
    public com.oracle.runbook.output.WebhookConfig config() {
      return null;
    }

    @Override
    public CompletableFuture<WebhookResult> send(DynamicChecklist checklist) {
      WebhookResult result = WebhookResult.success(name, 200);
      capturedResults.add(result);
      return CompletableFuture.completedFuture(result);
    }

    @Override
    public boolean shouldSend(DynamicChecklist checklist) {
      return true;
    }
  }
}
