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
      assertThat(response.status()).isEqualTo(Status.OK_200);
      String body = response.as(String.class);
      assertThat(body).isEqualTo("[]");
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
      assertThat(response.status()).isEqualTo(Status.CREATED_201);
      String body = response.as(String.class);
      assertThat(body).as("Response should contain name").contains("\"name\"");
      assertThat(body).as("Response should contain the webhook name").contains("slack-notify");
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
      assertThat(response.status()).isEqualTo(Status.BAD_REQUEST_400);
      String body = response.as(String.class);
      assertThat(body).as("Response should contain errorCode").contains("\"errorCode\"");
    }
  }
}
