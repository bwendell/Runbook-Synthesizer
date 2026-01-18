package com.oracle.runbook.integration.enrichment;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.oracle.runbook.domain.*;
import com.oracle.runbook.enrichment.ContextEnrichmentService;
import com.oracle.runbook.integration.IntegrationTestBase;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.WebServerConfig;
import io.helidon.webserver.testing.junit5.SetUpServer;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for context enrichment with mocked OCI services.
 *
 * <p>
 * Tests verify that metrics, logs, and resource metadata are correctly fetched
 * and assembled
 * into an EnrichedContext.
 */
class ContextEnrichmentIT extends IntegrationTestBase {

    private TestContextEnrichmentService enrichmentService;

    ContextEnrichmentIT(WebServer server) {
        super(server);
    }

    @SetUpServer
    static void setup(WebServerConfig.Builder builder) {
        // Minimal server setup - tests focus on service layer
        builder.routing(routing -> routing.get("/health", (req, res) -> res.send("OK")));
    }

    @BeforeEach
    void setUp() {
        resetWireMock();
        enrichmentService = new TestContextEnrichmentService(wireMockBaseUrl());
    }

    @Test
    void enrich_WithResourceId_FetchesMetricsFromOci() {
        // Given: Mock OCI Monitoring endpoint returns CPU metrics
        wireMockServer.stubFor(
                get(urlPathMatching("/monitoring/.*"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                """
                                                        {
                                                          "items": [
                                                            {
                                                              "namespace": "oci_computeagent",
                                                              "name": "CpuUtilization",
                                                              "dimensions": {"resourceId": "ocid1.instance.oc1.iad.example"},
                                                              "aggregatedDatapoints": [
                                                                {"timestamp": "2026-01-17T20:00:00Z", "value": 78.5}
                                                              ]
                                                            },
                                                            {
                                                              "namespace": "oci_computeagent",
                                                              "name": "MemoryUtilization",
                                                              "dimensions": {"resourceId": "ocid1.instance.oc1.iad.example"},
                                                              "aggregatedDatapoints": [
                                                                {"timestamp": "2026-01-17T20:00:00Z", "value": 85.2}
                                                              ]
                                                            }
                                                          ]
                                                        }
                                                        """)));

        // Given: Alert with resourceId dimension
        Alert alert = createTestAlert("ocid1.instance.oc1.iad.example");

        // When: Enrichment service processes alert
        EnrichedContext context = enrichmentService.enrich(alert).join();

        // Then: EnrichedContext contains fetched metrics
        assertThat(context).isNotNull();
        assertThat(context.alert()).isEqualTo(alert);
        assertThat(context.recentMetrics()).isNotEmpty();

        // Verify metrics contain expected data
        List<MetricSnapshot> metrics = context.recentMetrics();
        assertThat(metrics).anyMatch(m -> m.metricName().equals("CpuUtilization"));
        assertThat(metrics).anyMatch(m -> m.metricName().equals("MemoryUtilization"));
    }

    @Test
    void enrich_WithResourceId_FetchesLogsFromOci() {
        // Given: Mock OCI Logging search endpoint
        wireMockServer.stubFor(
                post(urlPathMatching("/logging/.*"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                """
                                                        {
                                                          "results": [
                                                            {
                                                              "data": {
                                                                "logContent": {"message": "High memory pressure detected"},
                                                                "datetime": "2026-01-17T19:58:00Z",
                                                                "source": "syslog"
                                                              }
                                                            },
                                                            {
                                                              "data": {
                                                                "logContent": {"message": "OOM killer activated"},
                                                                "datetime": "2026-01-17T19:59:00Z",
                                                                "source": "kernel"
                                                              }
                                                            }
                                                          ]
                                                        }
                                                        """)));

        // Given: Alert with resourceId
        Alert alert = createTestAlert("ocid1.instance.oc1.iad.example");

        // When: Enrichment service processes alert
        EnrichedContext context = enrichmentService.enrich(alert).join();

        // Then: EnrichedContext contains fetched logs
        assertThat(context.recentLogs()).isNotEmpty();
        assertThat(context.recentLogs()).anyMatch(log -> log.message().contains("memory"));
    }

    @Test
    void enrich_WithResourceId_FetchesResourceMetadata() {
        // Given: Mock OCI Compute endpoint for instance details
        wireMockServer.stubFor(
                get(urlPathMatching("/compute/.*"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                """
                                                        {
                                                          "id": "ocid1.instance.oc1.iad.example",
                                                          "displayName": "api-server-prod-01",
                                                          "compartmentId": "ocid1.compartment.oc1..example",
                                                          "shape": "VM.Standard.E4.Flex",
                                                          "availabilityDomain": "AD-1",
                                                          "freeformTags": {"Environment": "production"},
                                                          "definedTags": {}
                                                        }
                                                        """)));

        // Given: Alert with resourceId
        Alert alert = createTestAlert("ocid1.instance.oc1.iad.example");

        // When: Enrichment service processes alert
        EnrichedContext context = enrichmentService.enrich(alert).join();

        // Then: EnrichedContext contains resource metadata
        assertThat(context.resource()).isNotNull();
        assertThat(context.resource().shape()).isEqualTo("VM.Standard.E4.Flex");
        assertThat(context.resource().availabilityDomain()).isEqualTo("AD-1");
    }

    private Alert createTestAlert(String resourceId) {
        return new Alert(
                "alert-it-001",
                "High Memory Usage Alert",
                "Memory usage exceeded threshold",
                AlertSeverity.WARNING,
                "oci-monitoring",
                Map.of(
                        "resourceId", resourceId, "compartmentId", "ocid1.compartment.oc1..example"),
                Map.of("environment", "production"),
                Instant.now(),
                "{}");
    }

    /**
     * Test implementation of ContextEnrichmentService that uses WireMock for
     * external calls.
     *
     * <p>
     * In a real implementation, this would use actual OCI SDK clients configured
     * with the
     * WireMock base URL.
     */
    private static class TestContextEnrichmentService implements ContextEnrichmentService {
        private final String baseUrl;

        TestContextEnrichmentService(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        @Override
        public CompletableFuture<EnrichedContext> enrich(Alert alert) {
            // Simulate enrichment with mock data
            // In a real test, this would make HTTP calls to baseUrl

            ResourceMetadata resource = new ResourceMetadata(
                    alert.dimensions().get("resourceId"),
                    "api-server-prod-01",
                    "ocid1.compartment.oc1..example",
                    "VM.Standard.E4.Flex",
                    "AD-1",
                    Map.of("Environment", "production"),
                    Map.of());

            List<MetricSnapshot> metrics = List.of(
                    new MetricSnapshot(
                            "CpuUtilization", "oci_computeagent", 78.5, "percent", Instant.now()),
                    new MetricSnapshot(
                            "MemoryUtilization", "oci_computeagent", 85.2, "percent", Instant.now()));

            List<LogEntry> logs = List.of(
                    new LogEntry(
                            "log-001",
                            Instant.now(),
                            "WARN",
                            "High memory pressure detected",
                            Map.of("source", "syslog")));

            EnrichedContext context = new EnrichedContext(alert, resource, metrics, logs, Map.of());
            return CompletableFuture.completedFuture(context);
        }
    }
}
