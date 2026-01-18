package com.oracle.runbook.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.helidon.http.Status;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;
import org.junit.jupiter.api.Test;

/** Unit tests for HealthResource. */
@ServerTest
class HealthResourceTest {

  private final Http1Client client;

  HealthResourceTest(Http1Client client) {
    this.client = client;
  }

  @SetUpRoute
  static void route(HttpRouting.Builder routing) {
    routing.register("/api/v1/health", new HealthResource());
  }

  @Test
  void testHealthEndpoint_ReturnsUpStatus() {
    try (Http1ClientResponse response = client.get("/api/v1/health").request()) {
      assertThat(response.status()).isEqualTo(Status.OK_200);
      String body = response.as(String.class);
      assertThat(body).as("Response should contain status field").contains("\"status\"");
      assertThat(body).as("Status should be UP").contains("\"UP\"");
      assertThat(body).as("Response should contain timestamp field").contains("\"timestamp\"");
    }
  }
}
