package com.oracle.runbook.enrichment;

import static org.assertj.core.api.Assertions.assertThat;

import com.oracle.runbook.domain.*;
import com.oracle.runbook.infrastructure.cloud.ComputeMetadataAdapter;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DefaultContextEnrichmentService}.
 *
 * <p>Tests cover:
 *
 * <ul>
 *   <li>Successful enrichment with all adapters returning data
 *   <li>Parallel execution of adapter calls
 *   <li>Graceful handling of partial failures
 *   <li>Complete failure handling
 * </ul>
 */
class DefaultContextEnrichmentServiceTest {

  @Nested
  @DisplayName("Success Cases")
  class SuccessCases {

    @Test
    @DisplayName("enrich returns EnrichedContext with metrics and logs when all adapters succeed")
    void enrich_allAdaptersSucceed_returnsCompleteContext() throws Exception {
      // Arrange
      var metricsAdapter = new StubMetricsSourceAdapter(createTestMetrics());
      var logsAdapter = new StubLogSourceAdapter(createTestLogs());
      var metadataAdapter = new StubComputeMetadataAdapter(createTestMetadata());

      var service =
          new DefaultContextEnrichmentService(metadataAdapter, metricsAdapter, logsAdapter);
      var alert = createTestAlert();

      // Act
      EnrichedContext context = service.enrich(alert).get(5, TimeUnit.SECONDS);

      // Assert
      assertThat(context).isNotNull();
      assertThat(context.alert().id()).isEqualTo(alert.id());
      assertThat(context.resource()).isNotNull();
      assertThat(context.resource().displayName()).isEqualTo("test-instance");
      assertThat(context.recentMetrics()).hasSize(2);
      assertThat(context.recentLogs()).hasSize(2);
    }

    @Test
    @DisplayName("enrich returns context with empty resource when metadata not found")
    void enrich_metadataNotFound_returnsContextWithNullResource() throws Exception {
      // Arrange
      var metricsAdapter = new StubMetricsSourceAdapter(createTestMetrics());
      var logsAdapter = new StubLogSourceAdapter(createTestLogs());
      var metadataAdapter = new StubComputeMetadataAdapter(Optional.empty());

      var service =
          new DefaultContextEnrichmentService(metadataAdapter, metricsAdapter, logsAdapter);
      var alert = createTestAlert();

      // Act
      EnrichedContext context = service.enrich(alert).get(5, TimeUnit.SECONDS);

      // Assert
      assertThat(context).isNotNull();
      assertThat(context.alert().id()).isEqualTo(alert.id());
      assertThat(context.resource()).isNull();
      assertThat(context.recentMetrics()).hasSize(2);
      assertThat(context.recentLogs()).hasSize(2);
    }
  }

  @Nested
  @DisplayName("Parallel Execution")
  class ParallelExecution {

    @Test
    @DisplayName("enrich calls all adapters in parallel for efficiency")
    void enrich_callsAdaptersInParallel() throws Exception {
      // Arrange - adapters with delays to detect serial vs parallel
      var callOrder = new AtomicInteger(0);
      var metricsAdapter = new DelayedMetricsAdapter(100, callOrder);
      var logsAdapter = new DelayedLogAdapter(100, callOrder);
      var metadataAdapter = new DelayedMetadataAdapter(100, callOrder);

      var service =
          new DefaultContextEnrichmentService(metadataAdapter, metricsAdapter, logsAdapter);
      var alert = createTestAlert();

      // Act
      long start = System.currentTimeMillis();
      service.enrich(alert).get(5, TimeUnit.SECONDS);
      long elapsed = System.currentTimeMillis() - start;

      // Assert - if parallel, should complete in ~100ms, if serial in ~300ms
      // Using 200ms as threshold to account for overhead
      assertThat(elapsed).as("Adapters should run in parallel").isLessThan(200);
    }
  }

  @Nested
  @DisplayName("Partial Failure Handling")
  class PartialFailureHandling {

    @Test
    @DisplayName("enrich returns logs when metrics adapter fails")
    void enrich_metricsAdapterFails_returnsLogsWithEmptyMetrics() throws Exception {
      // Arrange
      var metricsAdapter = new FailingMetricsAdapter();
      var logsAdapter = new StubLogSourceAdapter(createTestLogs());
      var metadataAdapter = new StubComputeMetadataAdapter(createTestMetadata());

      var service =
          new DefaultContextEnrichmentService(metadataAdapter, metricsAdapter, logsAdapter);
      var alert = createTestAlert();

      // Act
      EnrichedContext context = service.enrich(alert).get(5, TimeUnit.SECONDS);

      // Assert
      assertThat(context).isNotNull();
      assertThat(context.recentMetrics()).isEmpty();
      assertThat(context.recentLogs()).hasSize(2);
      assertThat(context.resource()).isNotNull();
    }

    @Test
    @DisplayName("enrich returns metrics when logs adapter fails")
    void enrich_logsAdapterFails_returnsMetricsWithEmptyLogs() throws Exception {
      // Arrange
      var metricsAdapter = new StubMetricsSourceAdapter(createTestMetrics());
      var logsAdapter = new FailingLogAdapter();
      var metadataAdapter = new StubComputeMetadataAdapter(createTestMetadata());

      var service =
          new DefaultContextEnrichmentService(metadataAdapter, metricsAdapter, logsAdapter);
      var alert = createTestAlert();

      // Act
      EnrichedContext context = service.enrich(alert).get(5, TimeUnit.SECONDS);

      // Assert
      assertThat(context).isNotNull();
      assertThat(context.recentMetrics()).hasSize(2);
      assertThat(context.recentLogs()).isEmpty();
      assertThat(context.resource()).isNotNull();
    }

    @Test
    @DisplayName("enrich returns partial data when metadata adapter fails")
    void enrich_metadataAdapterFails_returnsContextWithNullResource() throws Exception {
      // Arrange
      var metricsAdapter = new StubMetricsSourceAdapter(createTestMetrics());
      var logsAdapter = new StubLogSourceAdapter(createTestLogs());
      var metadataAdapter = new FailingMetadataAdapter();

      var service =
          new DefaultContextEnrichmentService(metadataAdapter, metricsAdapter, logsAdapter);
      var alert = createTestAlert();

      // Act
      EnrichedContext context = service.enrich(alert).get(5, TimeUnit.SECONDS);

      // Assert
      assertThat(context).isNotNull();
      assertThat(context.resource()).isNull();
      assertThat(context.recentMetrics()).hasSize(2);
      assertThat(context.recentLogs()).hasSize(2);
    }
  }

  @Nested
  @DisplayName("Complete Failure Handling")
  class CompleteFailureHandling {

    @Test
    @DisplayName("enrich returns minimal context when all adapters fail")
    void enrich_allAdaptersFail_returnsMinimalContext() throws Exception {
      // Arrange
      var metricsAdapter = new FailingMetricsAdapter();
      var logsAdapter = new FailingLogAdapter();
      var metadataAdapter = new FailingMetadataAdapter();

      var service =
          new DefaultContextEnrichmentService(metadataAdapter, metricsAdapter, logsAdapter);
      var alert = createTestAlert();

      // Act
      EnrichedContext context = service.enrich(alert).get(5, TimeUnit.SECONDS);

      // Assert
      assertThat(context).isNotNull();
      assertThat(context.alert().id()).isEqualTo(alert.id());
      assertThat(context.resource()).isNull();
      assertThat(context.recentMetrics()).isEmpty();
      assertThat(context.recentLogs()).isEmpty();
    }
  }

  // ====== Test Data Factories ======

  private Alert createTestAlert() {
    return new Alert(
        "alert-001",
        "High Memory Usage",
        "Memory utilization exceeded 90% threshold",
        AlertSeverity.WARNING,
        "aws-cloudwatch",
        Map.of("resourceId", "i-1234567890abcdef0"),
        Map.of(),
        Instant.now(),
        "{}");
  }

  private List<MetricSnapshot> createTestMetrics() {
    return List.of(
        new MetricSnapshot("CPUUtilization", "AWS/EC2", 85.5, "Percent", Instant.now()),
        new MetricSnapshot("MemoryUtilization", "CWAgent", 92.0, "Percent", Instant.now()));
  }

  private List<LogEntry> createTestLogs() {
    return List.of(
        new LogEntry("log-001", Instant.now(), "ERROR", "Out of memory error detected", Map.of()),
        new LogEntry("log-002", Instant.now(), "WARN", "High memory pressure", Map.of()));
  }

  private Optional<ResourceMetadata> createTestMetadata() {
    return Optional.of(
        new ResourceMetadata(
            "i-1234567890abcdef0",
            "test-instance",
            "123456789012",
            "t3.medium",
            "us-east-1a",
            Map.of("env", "test"),
            Map.of()));
  }

  // ====== Stub Adapters ======

  private static class StubMetricsSourceAdapter implements MetricsSourceAdapter {
    private final List<MetricSnapshot> metrics;

    StubMetricsSourceAdapter(List<MetricSnapshot> metrics) {
      this.metrics = metrics;
    }

    @Override
    public String sourceType() {
      return "stub-metrics";
    }

    @Override
    public CompletableFuture<List<MetricSnapshot>> fetchMetrics(
        String resourceId, Duration lookback) {
      return CompletableFuture.completedFuture(metrics);
    }
  }

  private static class StubLogSourceAdapter implements LogSourceAdapter {
    private final List<LogEntry> logs;

    StubLogSourceAdapter(List<LogEntry> logs) {
      this.logs = logs;
    }

    @Override
    public String sourceType() {
      return "stub-logs";
    }

    @Override
    public CompletableFuture<List<LogEntry>> fetchLogs(
        String resourceId, Duration lookback, String query) {
      return CompletableFuture.completedFuture(logs);
    }
  }

  private static class StubComputeMetadataAdapter implements ComputeMetadataAdapter {
    private final Optional<ResourceMetadata> metadata;

    StubComputeMetadataAdapter(Optional<ResourceMetadata> metadata) {
      this.metadata = metadata;
    }

    @Override
    public String providerType() {
      return "stub-metadata";
    }

    @Override
    public CompletableFuture<Optional<ResourceMetadata>> getInstance(String instanceId) {
      return CompletableFuture.completedFuture(metadata);
    }
  }

  // ====== Failing Adapters ======

  private static class FailingMetricsAdapter implements MetricsSourceAdapter {
    @Override
    public String sourceType() {
      return "failing-metrics";
    }

    @Override
    public CompletableFuture<List<MetricSnapshot>> fetchMetrics(
        String resourceId, Duration lookback) {
      return CompletableFuture.failedFuture(new RuntimeException("Metrics service unavailable"));
    }
  }

  private static class FailingLogAdapter implements LogSourceAdapter {
    @Override
    public String sourceType() {
      return "failing-logs";
    }

    @Override
    public CompletableFuture<List<LogEntry>> fetchLogs(
        String resourceId, Duration lookback, String query) {
      return CompletableFuture.failedFuture(new RuntimeException("Log service unavailable"));
    }
  }

  private static class FailingMetadataAdapter implements ComputeMetadataAdapter {
    @Override
    public String providerType() {
      return "failing-metadata";
    }

    @Override
    public CompletableFuture<Optional<ResourceMetadata>> getInstance(String instanceId) {
      return CompletableFuture.failedFuture(new RuntimeException("Metadata service unavailable"));
    }
  }

  // ====== Delayed Adapters (for parallel execution testing) ======

  private static class DelayedMetricsAdapter implements MetricsSourceAdapter {
    private final long delayMs;
    private final AtomicInteger callOrder;

    DelayedMetricsAdapter(long delayMs, AtomicInteger callOrder) {
      this.delayMs = delayMs;
      this.callOrder = callOrder;
    }

    @Override
    public String sourceType() {
      return "delayed-metrics";
    }

    @Override
    public CompletableFuture<List<MetricSnapshot>> fetchMetrics(
        String resourceId, Duration lookback) {
      return CompletableFuture.supplyAsync(
          () -> {
            try {
              Thread.sleep(delayMs);
              callOrder.incrementAndGet();
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
            return List.of();
          });
    }
  }

  private static class DelayedLogAdapter implements LogSourceAdapter {
    private final long delayMs;
    private final AtomicInteger callOrder;

    DelayedLogAdapter(long delayMs, AtomicInteger callOrder) {
      this.delayMs = delayMs;
      this.callOrder = callOrder;
    }

    @Override
    public String sourceType() {
      return "delayed-logs";
    }

    @Override
    public CompletableFuture<List<LogEntry>> fetchLogs(
        String resourceId, Duration lookback, String query) {
      return CompletableFuture.supplyAsync(
          () -> {
            try {
              Thread.sleep(delayMs);
              callOrder.incrementAndGet();
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
            return List.of();
          });
    }
  }

  private static class DelayedMetadataAdapter implements ComputeMetadataAdapter {
    private final long delayMs;
    private final AtomicInteger callOrder;

    DelayedMetadataAdapter(long delayMs, AtomicInteger callOrder) {
      this.delayMs = delayMs;
      this.callOrder = callOrder;
    }

    @Override
    public String providerType() {
      return "delayed-metadata";
    }

    @Override
    public CompletableFuture<Optional<ResourceMetadata>> getInstance(String instanceId) {
      return CompletableFuture.supplyAsync(
          () -> {
            try {
              Thread.sleep(delayMs);
              callOrder.incrementAndGet();
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
            return Optional.empty();
          });
    }
  }
}
