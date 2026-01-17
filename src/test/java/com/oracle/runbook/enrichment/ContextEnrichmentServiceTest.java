package com.oracle.runbook.enrichment;

import static org.junit.jupiter.api.Assertions.*;

import com.oracle.runbook.domain.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for the ContextEnrichmentService interface contract. */
class ContextEnrichmentServiceTest {

  @Test
  @DisplayName("enrich accepts Alert and returns CompletableFuture<EnrichedContext>")
  void enrich_acceptsAlert_returnsEnrichedContextFuture() throws Exception {
    // Arrange
    ContextEnrichmentService service = new TestContextEnrichmentService();
    Alert alert = createTestAlert();

    // Act
    CompletableFuture<EnrichedContext> future = service.enrich(alert);
    EnrichedContext context = future.get();

    // Assert
    assertNotNull(future);
    assertNotNull(context);
    assertEquals(alert.id(), context.alert().id());
    assertNotNull(context.recentMetrics());
    assertNotNull(context.recentLogs());
  }

  private Alert createTestAlert() {
    return new Alert(
        "alert-001",
        "High Memory Usage",
        "Memory utilization exceeded 90% threshold",
        AlertSeverity.WARNING,
        "oci-monitoring",
        Map.of("resourceId", "ocid1.instance.oc1..example"),
        Map.of(),
        Instant.now(),
        "{}");
  }

  /** Test implementation of ContextEnrichmentService for verifying interface contract. */
  private static class TestContextEnrichmentService implements ContextEnrichmentService {
    @Override
    public CompletableFuture<EnrichedContext> enrich(Alert alert) {
      ResourceMetadata resource =
          new ResourceMetadata(
              alert.dimensions().get("resourceId"),
              "test-server",
              "ocid1.compartment.oc1..example",
              "VM.Standard2.1",
              "AD-1",
              Map.of(),
              Map.of());

      MetricSnapshot metric =
          new MetricSnapshot("CpuUtilization", "oci_computeagent", 45.5, "percent", Instant.now());

      EnrichedContext context =
          new EnrichedContext(alert, resource, List.of(metric), List.of(), Map.of());
      return CompletableFuture.completedFuture(context);
    }
  }
}
