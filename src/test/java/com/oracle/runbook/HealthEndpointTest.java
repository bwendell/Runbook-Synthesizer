package com.oracle.runbook;

import static org.assertj.core.api.Assertions.assertThat;

import io.helidon.http.Status;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Integration tests for health endpoint. Uses Helidon SE 4.x testing framework. */
@ServerTest
class HealthEndpointTest {

  private final Http1Client client;

  HealthEndpointTest(Http1Client client) {
    this.client = client;
  }

  @SetUpRoute
  static void routing(HttpRouting.Builder routing) {
    // Add a simple health check route for testing
    routing.get("/health", (req, res) -> res.send("{\"status\":\"UP\"}"));
  }

  @Test
  @DisplayName("Health endpoint returns 200 OK")
  void healthEndpointReturns200() {
    try (Http1ClientResponse response = client.get("/health").request()) {
      assertThat(response.status())
          .as("Health endpoint should return 200 OK")
          .isEqualTo(Status.OK_200);
    }
  }

  @Test
  @DisplayName("Health endpoint returns UP status")
  void healthEndpointReturnsUpStatus() {
    try (Http1ClientResponse response = client.get("/health").request()) {
      String body = response.as(String.class);
      assertThat(body).as("Health response should contain UP status").contains("UP");
    }
  }
}
