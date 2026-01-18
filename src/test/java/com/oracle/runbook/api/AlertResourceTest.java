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
      assertThat(response.status()).isEqualTo(Status.OK_200);
      String body = response.as(String.class);
      assertThat(body).as("Response should contain alertId").contains("\"alertId\"");
      assertThat(body).as("Response should contain summary").contains("\"summary\"");
      assertThat(body).as("Response should contain steps").contains("\"steps\"");
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
      assertThat(response.status()).isEqualTo(Status.BAD_REQUEST_400);
      String body = response.as(String.class);
      assertThat(body).as("Response should contain errorCode").contains("\"errorCode\"");
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
      assertThat(response.status()).isEqualTo(Status.BAD_REQUEST_400);
      String body = response.as(String.class);
      assertThat(body).as("Response should contain errorCode").contains("\"errorCode\"");
    }
  }
}
