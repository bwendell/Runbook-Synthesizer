package com.oracle.runbook.integration.containers;

import com.oracle.runbook.domain.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Factory for creating test data objects for E2E tests.
 *
 * <p>Follows the testing-patterns skill: factory functions with sensible defaults and property
 * overrides.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * // Create with defaults
 * Alert alert = E2EDataFactory.createMemoryAlert();
 *
 * // Create with overrides
 * Alert customAlert = E2EDataFactory.createAlert(
 *     AlertBuilder.base().withSeverity(AlertSeverity.CRITICAL)
 * );
 * }</pre>
 */
public final class E2EDataFactory {

  private E2EDataFactory() {
    // Utility class
  }

  // ===== Alert Factories =====

  /**
   * Creates a high memory utilization alert with sensible defaults.
   *
   * @return Alert for memory troubleshooting scenario
   */
  public static Alert createMemoryAlert() {
    return createMemoryAlert("alert-mem-001");
  }

  /**
   * Creates a high memory utilization alert with custom ID.
   *
   * @param id the alert ID
   * @return Alert for memory troubleshooting scenario
   */
  public static Alert createMemoryAlert(String id) {
    return new Alert(
        id,
        "High Memory Utilization",
        "Memory utilization has exceeded 90% threshold on prod-app-server-01",
        AlertSeverity.WARNING,
        "oci-monitoring",
        Map.of(
            "resourceId", "ocid1.instance.oc1..e2e-test-instance",
            "compartmentId", "ocid1.compartment.oc1..e2e-test-compartment"),
        Map.of("alarmName", "HighMemoryAlarm", "metricName", "MemoryUtilization"),
        Instant.now(),
        "{}");
  }

  /**
   * Creates a high CPU utilization alert with sensible defaults.
   *
   * @return Alert for CPU troubleshooting scenario
   */
  public static Alert createCpuAlert() {
    return createCpuAlert("alert-cpu-001");
  }

  /**
   * Creates a high CPU utilization alert with custom ID.
   *
   * @param id the alert ID
   * @return Alert for CPU troubleshooting scenario
   */
  public static Alert createCpuAlert(String id) {
    return new Alert(
        id,
        "High CPU Utilization",
        "CPU utilization has exceeded 85% threshold on prod-app-server-02",
        AlertSeverity.WARNING,
        "oci-monitoring",
        Map.of(
            "resourceId", "ocid1.instance.oc1..e2e-cpu-instance",
            "compartmentId", "ocid1.compartment.oc1..e2e-test-compartment"),
        Map.of("alarmName", "HighCpuAlarm", "metricName", "CpuUtilization"),
        Instant.now(),
        "{}");
  }

  /**
   * Creates a critical disk space alert.
   *
   * @return Alert for disk troubleshooting scenario
   */
  public static Alert createDiskSpaceAlert() {
    return new Alert(
        "alert-disk-001",
        "Critical Disk Space",
        "Disk usage has exceeded 95% on /var partition",
        AlertSeverity.CRITICAL,
        "oci-monitoring",
        Map.of(
            "resourceId", "ocid1.instance.oc1..e2e-disk-instance",
            "compartmentId", "ocid1.compartment.oc1..e2e-test-compartment"),
        Map.of("alarmName", "DiskSpaceAlarm", "mountPoint", "/var"),
        Instant.now(),
        "{}");
  }

  // ===== EnrichedContext Factories =====

  /**
   * Creates an enriched context for an alert with default metadata.
   *
   * @param alert the alert to enrich
   * @return EnrichedContext with default resource metadata and metrics
   */
  public static EnrichedContext createEnrichedContext(Alert alert) {
    ResourceMetadata resource =
        new ResourceMetadata(
            alert.dimensions().getOrDefault("resourceId", "ocid1.instance.oc1..unknown"),
            "prod-app-server-01",
            alert.dimensions().getOrDefault("compartmentId", "ocid1.compartment.oc1..unknown"),
            "VM.Standard.E4.Flex",
            "AD-1",
            Map.of("environment", "production", "team", "platform"),
            Map.of("owner", "ops-team", "cost-center", "engineering"));

    List<MetricSnapshot> metrics =
        List.of(
            new MetricSnapshot("MemoryUtilization", "oci_computeagent", 92.5, "%", Instant.now()),
            new MetricSnapshot("CpuUtilization", "oci_computeagent", 45.2, "%", Instant.now()));

    List<LogEntry> logs =
        List.of(
            new LogEntry(
                "log-001",
                Instant.now().minusSeconds(60),
                "WARNING",
                "Memory pressure detected, swapping active",
                Map.of("hostname", "prod-app-server-01")),
            new LogEntry(
                "log-002",
                Instant.now().minusSeconds(30),
                "ERROR",
                "Application response time degraded",
                Map.of("hostname", "prod-app-server-01")));

    return new EnrichedContext(alert, resource, metrics, logs, Map.of());
  }

  // ===== Webhook Config Factories =====

  /**
   * Creates a generic webhook configuration.
   *
   * @param name webhook name
   * @param url webhook URL
   * @return configured WebhookConfig
   */
  public static WebhookConfig createWebhookConfig(String name, String url) {
    return WebhookConfig.builder().name(name).type("generic").url(url).build();
  }

  /**
   * Creates a Slack webhook configuration.
   *
   * @param url Slack webhook URL
   * @return configured WebhookConfig for Slack
   */
  public static WebhookConfig createSlackWebhookConfig(String url) {
    return WebhookConfig.builder().name("slack-notifications").type("slack").url(url).build();
  }

  // ===== RunbookChunk Factories =====

  /**
   * Creates a RunbookChunk with sensible defaults.
   *
   * @param id chunk identifier
   * @param content chunk text content
   * @param embedding the embedding vector
   * @return configured RunbookChunk
   */
  public static RunbookChunk createChunk(String id, String content, float[] embedding) {
    return new RunbookChunk(
        id, "runbooks/test.md", "Test Section", content, List.of("test"), List.of(), embedding);
  }

  /**
   * Creates a RunbookChunk with custom runbook path.
   *
   * @param id chunk identifier
   * @param runbookPath source runbook file path
   * @param content chunk text content
   * @param embedding the embedding vector
   * @return configured RunbookChunk
   */
  public static RunbookChunk createChunk(
      String id, String runbookPath, String content, float[] embedding) {
    return new RunbookChunk(
        id, runbookPath, "Section", content, List.of("test"), List.of(), embedding);
  }

  // ===== Utility Methods =====

  /**
   * Creates a random normalized embedding of the specified dimension.
   *
   * @param dimensions embedding dimension (typically 768)
   * @return normalized float array
   */
  public static float[] createRandomEmbedding(int dimensions) {
    float[] embedding = new float[dimensions];
    java.util.Random random = new java.util.Random();
    for (int i = 0; i < dimensions; i++) {
      embedding[i] = random.nextFloat();
    }
    normalize(embedding);
    return embedding;
  }

  /**
   * Creates a category-biased embedding for similarity testing.
   *
   * <p>Creates embeddings that are similar within category and dissimilar across categories.
   *
   * @param dimensions embedding dimension
   * @param category category index (0-3 for 4 categories)
   * @return biased float array
   */
  public static float[] createCategoryEmbedding(int dimensions, int category) {
    float[] embedding = new float[dimensions];
    int spikeStart = category * (dimensions / 4);
    for (int i = 0; i < dimensions; i++) {
      if (i >= spikeStart && i < spikeStart + dimensions / 4) {
        embedding[i] = 0.8f;
      } else {
        embedding[i] = 0.1f;
      }
    }
    return embedding;
  }

  private static void normalize(float[] embedding) {
    float norm = 0;
    for (float v : embedding) {
      norm += v * v;
    }
    norm = (float) Math.sqrt(norm);
    for (int i = 0; i < embedding.length; i++) {
      embedding[i] /= norm;
    }
  }
}
