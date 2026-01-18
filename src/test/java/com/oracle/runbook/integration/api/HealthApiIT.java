package com.oracle.runbook.integration.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.oracle.runbook.api.HealthResource;
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
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * API-level integration tests for the Health endpoint.
 *
 * <p>Tests exercise the real Helidon server HTTP endpoints for health checks, validating:
 *
 * <ul>
 *   <li>HTTP 200 status codes
 *   <li>JSON response structure with status and timestamp fields
 *   <li>Timestamp format is valid ISO-8601
 * </ul>
 */
@ServerTest
class HealthApiIT {

  private final Http1Client client;

  HealthApiIT(Http1Client client) {
    this.client = client;
  }

  @SetUpRoute
  static void route(HttpRouting.Builder routing) {
    routing.register("/api/v1/health", new HealthResource());
  }

  // ========== Task 3.2: GET /api/v1/health → 200 + {"status": "UP", "timestamp": ...} ==========

  @Test
  void healthCheckReturnsUp_ResponseContainsStatusAndTimestamp() {
    try (Http1ClientResponse response = client.get("/api/v1/health").request()) {
      assertThat(response.status()).isEqualTo(Status.OK_200);

      String body = response.as(String.class);
      JsonObject json = parseJson(body);

      // Verify status field
      assertThat(json.containsKey("status")).as("Response should contain status field").isTrue();
      assertThat(json.getString("status")).isEqualTo("UP");

      // Verify timestamp field
      assertThat(json.containsKey("timestamp"))
          .as("Response should contain timestamp field")
          .isTrue();
      String timestamp = json.getString("timestamp");
      assertThat(timestamp)
          .as("timestamp should be valid ISO-8601 format")
          .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*");

      // Verify timestamp is parseable as Instant
      Instant parsed = Instant.parse(timestamp);
      assertThat(parsed)
          .as("timestamp should be a recent time")
          .isAfter(Instant.now().minusSeconds(60));
    }
  }

  @Test
  void healthCheckReturnsUp_ValidJsonStructure() {
    try (Http1ClientResponse response = client.get("/api/v1/health").request()) {
      assertThat(response.status()).isEqualTo(Status.OK_200);

      String body = response.as(String.class);

      // Verify it's valid JSON
      JsonObject json = parseJson(body);
      assertThat(json).isNotNull();

      // Verify only expected keys are present
      assertThat(json.keySet()).containsExactlyInAnyOrder("status", "timestamp");
    }
  }

  @Test
  void healthCheckReturnsUp_MultipleRequestsConsistent() {
    // Make multiple requests to verify consistency
    for (int i = 0; i < 3; i++) {
      try (Http1ClientResponse response = client.get("/api/v1/health").request()) {
        assertThat(response.status()).isEqualTo(Status.OK_200);

        String body = response.as(String.class);
        JsonObject json = parseJson(body);

        assertThat(json.getString("status")).isEqualTo("UP");
        assertThat(json.containsKey("timestamp")).isTrue();
      }
    }
  }

  // ========== Helper Methods ==========

  private JsonObject parseJson(String body) {
    try (JsonReader reader = Json.createReader(new StringReader(body))) {
      return reader.readObject();
    }
  }
}
