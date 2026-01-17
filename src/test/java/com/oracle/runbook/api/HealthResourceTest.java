package com.oracle.runbook.api;

import static org.junit.jupiter.api.Assertions.*;

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
      assertEquals(Status.OK_200, response.status());
      String body = response.as(String.class);
      assertTrue(body.contains("\"status\""), "Response should contain status field");
      assertTrue(body.contains("\"UP\""), "Status should be UP");
      assertTrue(body.contains("\"timestamp\""), "Response should contain timestamp field");
    }
  }
}
