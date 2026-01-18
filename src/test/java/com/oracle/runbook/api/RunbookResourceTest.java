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

/** Unit tests for RunbookResource. */
@ServerTest
class RunbookResourceTest {

  private final Http1Client client;

  RunbookResourceTest(Http1Client client) {
    this.client = client;
  }

  @SetUpRoute
  static void route(HttpRouting.Builder routing) {
    routing.register("/api/v1/runbooks", new RunbookResource());
  }

  @Test
  void testPostSync_NoBody_StartsFullSync() {
    try (Http1ClientResponse response =
        client
            .post("/api/v1/runbooks/sync")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit("{}")) {
      assertThat(response.status()).isEqualTo(Status.ACCEPTED_202);
      String body = response.as(String.class);
      assertThat(body).as("Response should contain status").contains("\"status\"");
      assertThat(body).as("Status should be STARTED").contains("\"STARTED\"");
      assertThat(body).as("Response should contain requestId").contains("\"requestId\"");
    }
  }

  @Test
  void testPostSync_WithBucket_StartsFilteredSync() {
    String syncRequest =
        """
                {
                  "bucketName": "my-runbooks",
                  "prefix": "ops/",
                  "forceRefresh": true
                }
                """;

    try (Http1ClientResponse response =
        client
            .post("/api/v1/runbooks/sync")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit(syncRequest)) {
      assertThat(response.status()).isEqualTo(Status.ACCEPTED_202);
      String body = response.as(String.class);
      assertThat(body).as("Status should be STARTED").contains("\"STARTED\"");
    }
  }

  @Test
  void testPostSync_ReturnsRequestId() {
    try (Http1ClientResponse response =
        client
            .post("/api/v1/runbooks/sync")
            .header(HeaderNames.CONTENT_TYPE, "application/json")
            .submit("{}")) {
      String body = response.as(String.class);
      assertThat(body).as("Response should contain requestId").contains("\"requestId\"");
      // Verify requestId looks like a UUID
      assertThat(body)
          .as("requestId should be a UUID")
          .matches(".*\"requestId\":\\s*\"[a-f0-9-]+\".*");
    }
  }
}
