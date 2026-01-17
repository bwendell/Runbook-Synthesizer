package com.oracle.runbook.api;

import static org.junit.jupiter.api.Assertions.*;

import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;
import org.junit.jupiter.api.Test;

/** Unit tests for AlertResource. */
@ServerTest
class AlertResourceTest {

  private final Http1Client client;

  AlertResourceTest(Http1Client client) {
    this.client = client;
  }

  @SetUpRoute
  static void route(HttpRouting.Builder routing) {
    routing.register("/api/v1/alerts", new AlertResource());
  }

  @Test
  void testPostAlert_ValidRequest_ReturnsChecklist() {
    String validRequest =
        """
                {
                  "title": "High CPU Usage",
                  "message": "CPU utilization is above 95%",
                  "severity": "CRITICAL",
                  "sourceService": "oci-monitoring",
                  "dimensions": {"compartmentId": "test"},
                  "labels": {}
                }
                """;

    try (Http1ClientResponse response =
        client
            .post("/api/v1/alerts")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit(validRequest)) {
      assertEquals(Status.OK_200, response.status());
      String body = response.as(String.class);
      assertTrue(body.contains("\"alertId\""), "Response should contain alertId");
      assertTrue(body.contains("\"summary\""), "Response should contain summary");
      assertTrue(body.contains("\"steps\""), "Response should contain steps");
    }
  }

  @Test
  void testPostAlert_MissingTitle_ReturnsBadRequest() {
    String invalidRequest =
        """
                {
                  "message": "test message",
                  "severity": "CRITICAL"
                }
                """;

    try (Http1ClientResponse response =
        client
            .post("/api/v1/alerts")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit(invalidRequest)) {
      assertEquals(Status.BAD_REQUEST_400, response.status());
      String body = response.as(String.class);
      assertTrue(body.contains("\"errorCode\""), "Response should contain errorCode");
    }
  }

  @Test
  void testPostAlert_InvalidSeverity_ReturnsBadRequest() {
    String invalidRequest =
        """
                {
                  "title": "Test Alert",
                  "message": "test message",
                  "severity": "INVALID_SEVERITY"
                }
                """;

    try (Http1ClientResponse response =
        client
            .post("/api/v1/alerts")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit(invalidRequest)) {
      assertEquals(Status.BAD_REQUEST_400, response.status());
      String body = response.as(String.class);
      assertTrue(body.contains("\"errorCode\""), "Response should contain errorCode");
    }
  }
}
