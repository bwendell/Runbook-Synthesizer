package com.oracle.runbook.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;
import org.junit.jupiter.api.Test;

/**
 * Integration test verifying all API routes are properly configured. This test boots a server with
 * the full API routing to verify all endpoints respond correctly.
 */
@ServerTest
class ApiRoutingIntegrationTest {

  private final Http1Client client;

  ApiRoutingIntegrationTest(Http1Client client) {
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

  @Test
  void testHealthEndpoint_Returns200() {
    try (Http1ClientResponse response = client.get("/api/v1/health").request()) {
      assertThat(response.status()).isEqualTo(Status.OK_200);
      String body = response.as(String.class);
      assertThat(body).as("Health should return UP status").contains("\"UP\"");
    }
  }

  @Test
  void testAlertsEndpoint_ValidRequest_Returns200() {
    String validRequest =
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
            .submit(validRequest)) {
      assertThat(response.status()).isEqualTo(Status.OK_200);
    }
  }

  @Test
  void testAlertsEndpoint_InvalidRequest_Returns400() {
    String invalidRequest =
        """
                {
                  "message": "missing title"
                }
                """;

    try (Http1ClientResponse response =
        client
            .post("/api/v1/alerts")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit(invalidRequest)) {
      assertThat(response.status()).isEqualTo(Status.BAD_REQUEST_400);
    }
  }

  @Test
  void testWebhooksEndpoint_GetReturnsEmptyList() {
    try (Http1ClientResponse response = client.get("/api/v1/webhooks").request()) {
      assertThat(response.status()).isEqualTo(Status.OK_200);
      assertThat(response.as(String.class)).isEqualTo("[]");
    }
  }

  @Test
  void testRunbooksSyncEndpoint_Returns202() {
    try (Http1ClientResponse response =
        client
            .post("/api/v1/runbooks/sync")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit("{}")) {
      assertThat(response.status()).isEqualTo(Status.ACCEPTED_202);
      String body = response.as(String.class);
      assertThat(body).as("Sync should return STARTED status").contains("\"STARTED\"");
    }
  }
}
