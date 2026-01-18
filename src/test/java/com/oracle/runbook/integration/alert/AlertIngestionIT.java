package com.oracle.runbook.integration.alert;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.oracle.runbook.api.AlertResource;
import com.oracle.runbook.integration.IntegrationTestBase;
import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.WebServerConfig;
import io.helidon.webserver.testing.junit5.SetUpServer;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.io.StringReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for alert ingestion flow.
 *
 * <p>Tests the full flow: POST alert → context enrichment → RAG pipeline → checklist response.
 */
class AlertIngestionIT extends IntegrationTestBase {

  private final Http1Client client;

  AlertIngestionIT(WebServer server, Http1Client client) {
    super(server);
    this.client = client;
  }

  @SetUpServer
  static void setup(WebServerConfig.Builder builder) {
    builder.routing(routing -> routing.register("/api/v1/alerts", new AlertResource()));
  }

  @BeforeEach
  void resetMocks() {
    resetWireMock();
  }

  @Test
  void postValidAlert_ReturnsChecklist() {
    // Given: A valid OCI alarm JSON payload
    String validAlertJson =
        """
                {
                  "title": "High Memory Usage Alert",
                  "message": "Memory usage on host has exceeded 85% threshold",
                  "severity": "WARNING",
                  "sourceService": "oci-monitoring",
                  "dimensions": {
                    "compartmentId": "ocid1.compartment.oc1..example",
                    "resourceId": "ocid1.instance.oc1.iad.example"
                  },
                  "labels": {
                    "environment": "production"
                  }
                }
                """;

    // When: POST to /api/v1/alerts
    try (Http1ClientResponse response =
        client
            .post("/api/v1/alerts")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit(validAlertJson)) {

      // Then: HTTP 200 response
      assertThat(response.status()).isEqualTo(Status.OK_200);

      // Then: Response contains DynamicChecklist structure
      String body = response.as(String.class);
      JsonObject json = parseJson(body);

      assertThat(json.containsKey("alertId")).isTrue();
      assertThat(json.containsKey("summary")).isTrue();
      assertThat(json.containsKey("steps")).isTrue();
      assertThat(json.getJsonArray("steps")).isNotEmpty();
      assertThat(json.containsKey("sourceRunbooks")).isTrue();
      assertThat(json.containsKey("generatedAt")).isTrue();
      assertThat(json.containsKey("llmProviderUsed")).isTrue();
    }
  }

  @Test
  void postValidAlert_WithMockedOciServices_ReturnsEnrichedChecklist() {
    // Given: Mock OCI Monitoring metrics endpoint
    wireMockServer.stubFor(
        get(urlPathMatching("/20180401/metrics/actions/summarizeMetricsData"))
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
                                                                {"timestamp": "2026-01-17T20:00:00Z", "value": 45.2}
                                                              ]
                                                            }
                                                          ]
                                                        }
                                                        """)));

    // Given: Mock OCI Logging search endpoint
    wireMockServer.stubFor(
        post(urlPathMatching("/20190909/search"))
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
                                                                "logContent": "Memory pressure detected",
                                                                "timestamp": "2026-01-17T19:58:00Z"
                                                              }
                                                            }
                                                          ]
                                                        }
                                                        """)));

    // Given: Mock OCI GenAI generate endpoint
    wireMockServer.stubFor(
        post(urlPathMatching("/20231130/actions/generateText"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                                                        {
                                                          "modelId": "cohere.command",
                                                          "inferenceResponse": {
                                                            "generatedTexts": [
                                                              {
                                                                "text": "## Checklist\\n1. Check memory usage\\n2. Restart service"
                                                              }
                                                            ]
                                                          }
                                                        }
                                                        """)));

    // When: POST valid alert
    String validAlertJson =
        """
                {
                  "title": "High Memory Usage Alert",
                  "message": "Memory usage on host has exceeded 85% threshold",
                  "severity": "WARNING",
                  "sourceService": "oci-monitoring",
                  "dimensions": {
                    "compartmentId": "ocid1.compartment.oc1..example",
                    "resourceId": "ocid1.instance.oc1.iad.example"
                  },
                  "labels": {}
                }
                """;

    try (Http1ClientResponse response =
        client
            .post("/api/v1/alerts")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit(validAlertJson)) {

      // Then: HTTP 200 with checklist
      assertThat(response.status()).isEqualTo(Status.OK_200);

      String body = response.as(String.class);
      JsonObject json = parseJson(body);

      assertThat(json.getString("alertId")).isNotBlank();
      assertThat(json.getJsonArray("steps").size()).isGreaterThan(0);
    }
  }

  private JsonObject parseJson(String json) {
    try (JsonReader reader = Json.createReader(new StringReader(json))) {
      return reader.readObject();
    }
  }
}
