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

/** Unit tests for WebhookResource. */
@ServerTest
class WebhookResourceTest {

  private final Http1Client client;

  WebhookResourceTest(Http1Client client) {
    this.client = client;
  }

  @SetUpRoute
  static void route(HttpRouting.Builder routing) {
    routing.register("/api/v1/webhooks", new WebhookResource());
  }

  @Test
  void testGetWebhooks_ReturnsEmptyList() {
    try (Http1ClientResponse response = client.get("/api/v1/webhooks").request()) {
      assertEquals(Status.OK_200, response.status());
      String body = response.as(String.class);
      assertEquals("[]", body);
    }
  }

  @Test
  void testPostWebhook_ValidConfig_ReturnsCreated() {
    String validConfig =
        """
                {
                  "name": "slack-notify",
                  "type": "SLACK",
                  "url": "https://hooks.slack.com/test",
                  "enabled": true
                }
                """;

    try (Http1ClientResponse response =
        client
            .post("/api/v1/webhooks")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit(validConfig)) {
      assertEquals(Status.CREATED_201, response.status());
      String body = response.as(String.class);
      assertTrue(body.contains("\"name\""), "Response should contain name");
      assertTrue(body.contains("slack-notify"), "Response should contain the webhook name");
    }
  }

  @Test
  void testPostWebhook_MissingName_ReturnsBadRequest() {
    String invalidConfig =
        """
                {
                  "type": "SLACK",
                  "url": "https://hooks.slack.com/test"
                }
                """;

    try (Http1ClientResponse response =
        client
            .post("/api/v1/webhooks")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit(invalidConfig)) {
      assertEquals(Status.BAD_REQUEST_400, response.status());
      String body = response.as(String.class);
      assertTrue(body.contains("\"errorCode\""), "Response should contain errorCode");
    }
  }
}
