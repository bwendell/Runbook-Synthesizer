package com.oracle.runbook.integration.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.oracle.runbook.api.AlertResource;
import com.oracle.runbook.api.HealthResource;
import com.oracle.runbook.api.RunbookResource;
import com.oracle.runbook.api.WebhookResource;
import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.io.StringReader;
import java.util.Objects;
import org.junit.jupiter.api.Test;

/**
 * API-level integration tests for the Alert endpoint.
 *
 * <p>Tests exercise the real Helidon server HTTP endpoints, validating:
 *
 * <ul>
 *   <li>Routing configuration
 *   <li>JSON serialization/deserialization
 *   <li>HTTP status codes
 *   <li>Response DTO structure
 * </ul>
 */
@ServerTest
class AlertApiIT {

  private final Http1Client client;

  AlertApiIT(Http1Client client) {
    this.client = client;
  }

  @SetUpRoute
  static void route(HttpRouting.Builder routing) {
    // Wire all resources as they would be in production
    routing.register("/api/v1/health", new HealthResource());
    routing.register("/api/v1/alerts", new AlertResource());
    routing.register("/api/v1/webhooks", new WebhookResource());
    routing.register("/api/v1/runbooks", new RunbookResource());
  }

  // ========== Task 2.2: POST with valid JSON → 200 + ChecklistResponse ==========

  @Test
  void postValidAlert_WithAllFields_Returns200AndChecklist() {
    String validRequest =
        """
        {
          "title": "High Memory Utilization",
          "message": "Memory utilization has exceeded 90% threshold",
          "severity": "WARNING",
          "sourceService": "oci-monitoring",
          "dimensions": {"compartmentId": "ocid1.compartment.oc1..abc", "resourceId": "ocid1.instance.oc1..xyz"},
          "labels": {"environment": "production"}
        }
        """;

    try (Http1ClientResponse response =
        client
            .post("/api/v1/alerts")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit(validRequest)) {

      assertThat(response.status()).isEqualTo(Status.OK_200);

      String body = response.as(String.class);
      JsonObject json = Objects.requireNonNull(parseJson(body));

      // Verify ChecklistResponse structure
      assertThat(json.containsKey("alertId")).as("Response should contain alertId").isTrue();
      assertThat(json.getString("alertId")).as("alertId should not be empty").isNotEmpty();

      assertThat(json.containsKey("steps")).as("Response should contain steps").isTrue();
      JsonArray steps = json.getJsonArray("steps");
      assertThat(steps).as("Steps array should not be empty").isNotEmpty();

      assertThat(json.containsKey("generatedAt"))
          .as("Response should contain generatedAt")
          .isTrue();
      assertThat(json.getString("generatedAt"))
          .as("generatedAt should be valid ISO-8601")
          .matches("\\d{4}-\\d{2}-\\d{2}T.*");
    }
  }

  @Test
  void postValidAlert_MinimalFields_Returns200AndChecklist() {
    String minimalRequest =
        """
        {
          "title": "Test Alert",
          "severity": "INFO"
        }
        """;

    try (Http1ClientResponse response =
        client
            .post("/api/v1/alerts")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit(minimalRequest)) {

      assertThat(response.status()).isEqualTo(Status.OK_200);

      String body = response.as(String.class);
      JsonObject json = Objects.requireNonNull(parseJson(body));

      assertThat(json.containsKey("alertId")).isTrue();
      assertThat(json.containsKey("steps")).isTrue();
      assertThat(json.containsKey("sourceRunbooks")).isTrue();
    }
  }

  // ========== Task 2.3: POST with invalid severity → 400 + ErrorResponse ==========

  @Test
  void postInvalidSeverity_Returns400AndValidationError() {
    String invalidRequest =
        """
        {
          "title": "Test Alert",
          "severity": "SUPER_CRITICAL_INVALID"
        }
        """;

    try (Http1ClientResponse response =
        client
            .post("/api/v1/alerts")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit(invalidRequest)) {

      assertThat(response.status()).isEqualTo(Status.BAD_REQUEST_400);

      String body = response.as(String.class);
      JsonObject json = Objects.requireNonNull(parseJson(body));

      assertThat(json.containsKey("errorCode")).as("Response should contain errorCode").isTrue();
      assertThat(json.getString("errorCode")).isEqualTo("VALIDATION_ERROR");

      assertThat(json.containsKey("message")).as("Response should contain message").isTrue();
      assertThat(json.getString("message").toLowerCase()).contains("severity");
    }
  }

  // ========== Task 2.4: POST with missing required fields → 400 + validation error ==========

  @Test
  void postMissingTitle_Returns400AndValidationError() {
    String missingTitleRequest =
        """
        {
          "message": "Some message",
          "severity": "WARNING"
        }
        """;

    try (Http1ClientResponse response =
        client
            .post("/api/v1/alerts")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit(missingTitleRequest)) {

      assertThat(response.status()).isEqualTo(Status.BAD_REQUEST_400);

      String body = response.as(String.class);
      JsonObject json = Objects.requireNonNull(parseJson(body));

      assertThat(json.containsKey("errorCode")).isTrue();
    }
  }

  @Test
  void postMissingSeverity_Returns400AndValidationError() {
    String missingSeverityRequest =
        """
        {
          "title": "Test Alert",
          "message": "Some message"
        }
        """;

    try (Http1ClientResponse response =
        client
            .post("/api/v1/alerts")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit(missingSeverityRequest)) {

      assertThat(response.status()).isEqualTo(Status.BAD_REQUEST_400);

      String body = response.as(String.class);
      JsonObject json = Objects.requireNonNull(parseJson(body));

      assertThat(json.containsKey("errorCode")).isTrue();
    }
  }

  @Test
  void postEmptyBody_Returns400() {
    try (Http1ClientResponse response =
        client
            .post("/api/v1/alerts")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit("{}")) {

      assertThat(response.status()).isEqualTo(Status.BAD_REQUEST_400);
    }
  }

  // ========== Task 2.5: Verify JSON response structure matches DTOs ==========

  @Test
  void postValidAlert_ResponseMatchesChecklistResponseDto() {
    String validRequest =
        """
        {
          "title": "CPU High",
          "message": "CPU at 95%",
          "severity": "CRITICAL",
          "sourceService": "oci-monitoring"
        }
        """;

    try (Http1ClientResponse response =
        client
            .post("/api/v1/alerts")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit(validRequest)) {

      assertThat(response.status()).isEqualTo(Status.OK_200);

      String body = response.as(String.class);
      JsonObject json = Objects.requireNonNull(parseJson(body));

      // Verify all ChecklistResponse fields exist
      assertThat(json.containsKey("alertId")).as("ChecklistResponse.alertId").isTrue();
      assertThat(json.containsKey("summary")).as("ChecklistResponse.summary").isTrue();
      assertThat(json.containsKey("steps")).as("ChecklistResponse.steps").isTrue();
      assertThat(json.containsKey("sourceRunbooks"))
          .as("ChecklistResponse.sourceRunbooks")
          .isTrue();
      assertThat(json.containsKey("generatedAt")).as("ChecklistResponse.generatedAt").isTrue();
      assertThat(json.containsKey("llmProviderUsed"))
          .as("ChecklistResponse.llmProviderUsed")
          .isTrue();

      // Verify step structure
      JsonArray steps = json.getJsonArray("steps");
      if (!steps.isEmpty()) {
        JsonObject firstStep = steps.getJsonObject(0);
        assertThat(firstStep.containsKey("order")).as("ChecklistStepResponse.order").isTrue();
        assertThat(firstStep.containsKey("instruction"))
            .as("ChecklistStepResponse.instruction")
            .isTrue();
        assertThat(firstStep.containsKey("rationale"))
            .as("ChecklistStepResponse.rationale")
            .isTrue();
        assertThat(firstStep.containsKey("currentValue"))
            .as("ChecklistStepResponse.currentValue")
            .isTrue();
        assertThat(firstStep.containsKey("expectedValue"))
            .as("ChecklistStepResponse.expectedValue")
            .isTrue();
        assertThat(firstStep.containsKey("priority")).as("ChecklistStepResponse.priority").isTrue();
        assertThat(firstStep.containsKey("commands")).as("ChecklistStepResponse.commands").isTrue();
      }
    }
  }

  @Test
  void postInvalidAlert_ResponseMatchesErrorResponseDto() {
    String invalidRequest =
        """
        {
          "title": "Test",
          "severity": "BOGUS"
        }
        """;

    try (Http1ClientResponse response =
        client
            .post("/api/v1/alerts")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit(invalidRequest)) {

      assertThat(response.status()).isEqualTo(Status.BAD_REQUEST_400);

      String body = response.as(String.class);
      JsonObject json = Objects.requireNonNull(parseJson(body));

      // Verify all ErrorResponse fields exist
      assertThat(json.containsKey("correlationId")).as("ErrorResponse.correlationId").isTrue();
      assertThat(json.containsKey("errorCode")).as("ErrorResponse.errorCode").isTrue();
      assertThat(json.containsKey("message")).as("ErrorResponse.message").isTrue();
      assertThat(json.containsKey("timestamp")).as("ErrorResponse.timestamp").isTrue();
      assertThat(json.containsKey("details")).as("ErrorResponse.details").isTrue();
    }
  }

  // ========== Helper Methods ==========

  private JsonObject parseJson(String body) {
    try (JsonReader reader = Json.createReader(new StringReader(body))) {
      return reader.readObject();
    }
  }
}
