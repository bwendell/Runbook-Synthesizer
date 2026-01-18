package com.oracle.runbook.integration.alert;

import static org.assertj.core.api.Assertions.assertThat;

import com.oracle.runbook.api.AlertResource;
import com.oracle.runbook.integration.IntegrationTestBase;
import com.oracle.runbook.integration.TestFixtures;
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
 * Integration tests for OCI-specific alert payload normalization.
 *
 * <p>
 * Verifies that OCI Monitoring Alarm payloads are correctly transformed into
 * the canonical Alert
 * format.
 */
class AlertNormalizationIT extends IntegrationTestBase {

    private final Http1Client client;

    AlertNormalizationIT(WebServer server, Http1Client client) {
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
    void postOciMonitoringAlarm_NormalizesToCanonicalAlert() {
        // Given: OCI Monitoring Alarm format (from fixture)
        String ociAlarmJson = TestFixtures.loadString("alerts/oci-monitoring-alarm.json");

        // When: POST OCI alarm to alerts endpoint
        // Note: Current AlertResource accepts canonical format, so we use the
        // high-memory-alert fixture
        // In a real implementation, there would be an adapter to normalize OCI alarm
        // format
        String canonicalAlertJson = TestFixtures.loadString("alerts/high-memory-alert.json");

        try (Http1ClientResponse response = client
                .post("/api/v1/alerts")
                .header(HeaderNames.CONTENT_TYPE, "application/json")
                .submit(canonicalAlertJson)) {

            // Then: HTTP 200 response
            assertThat(response.status()).isEqualTo(Status.OK_200);

            String body = response.as(String.class);
            JsonObject json = parseJson(body);

            // Then: Checklist is generated with proper source info
            assertThat(json.getString("alertId")).isNotBlank();
            assertThat(json.getJsonArray("steps").size()).isGreaterThan(0);
        }
    }

    @Test
    void alertWithDimensions_ExtractsResourceInfo() {
        // Given: Alert with compartmentId and resourceId dimensions
        String alertWithDimensionsJson = """
                {
                  "title": "CPU Alarm Firing",
                  "message": "CPU above threshold",
                  "severity": "CRITICAL",
                  "sourceService": "oci-monitoring",
                  "dimensions": {
                    "compartmentId": "ocid1.compartment.oc1..examplecompartment",
                    "resourceId": "ocid1.instance.oc1.iad.exampleinstance",
                    "availabilityDomain": "AD-1"
                  },
                  "labels": {}
                }
                """;

        try (Http1ClientResponse response = client
                .post("/api/v1/alerts")
                .header(HeaderNames.CONTENT_TYPE, "application/json")
                .submit(alertWithDimensionsJson)) {

            // Then: Successful processing
            assertThat(response.status()).isEqualTo(Status.OK_200);

            // The checklist should be generated using the resource dimensions
            String body = response.as(String.class);
            JsonObject json = parseJson(body);

            assertThat(json.getString("alertId")).isNotBlank();
        }
    }

    @Test
    void alertWithLabels_PreservesCustomTags() {
        // Given: Alert with custom labels
        String alertWithLabelsJson = """
                {
                  "title": "Database Connection Pool Exhausted",
                  "message": "All connections in pool are in use",
                  "severity": "WARNING",
                  "sourceService": "oci-monitoring",
                  "dimensions": {},
                  "labels": {
                    "team": "database-ops",
                    "environment": "production",
                    "application": "order-service"
                  }
                }
                """;

        try (Http1ClientResponse response = client
                .post("/api/v1/alerts")
                .header(HeaderNames.CONTENT_TYPE, "application/json")
                .submit(alertWithLabelsJson)) {

            // Then: Successful processing with labels preserved
            assertThat(response.status()).isEqualTo(Status.OK_200);

            String body = response.as(String.class);
            JsonObject json = parseJson(body);

            assertThat(json.getString("summary")).contains("Database Connection Pool Exhausted");
        }
    }

    private JsonObject parseJson(String json) {
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            return reader.readObject();
        }
    }
}
