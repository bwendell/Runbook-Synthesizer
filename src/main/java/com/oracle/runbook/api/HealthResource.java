package com.oracle.runbook.api;

import io.helidon.http.HeaderNames;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.json.Json;
import java.time.Instant;

/**
 * Health check resource providing /api/v1/health endpoint.
 *
 * <p>Returns JSON with status and timestamp for Kubernetes probes and monitoring.
 */
public class HealthResource implements HttpService {

  @Override
  public void routing(HttpRules rules) {
    rules.get("/", this::handleGet);
  }

  private void handleGet(ServerRequest req, ServerResponse res) {
    String json =
        Json.createObjectBuilder()
            .add("status", "UP")
            .add("timestamp", Instant.now().toString())
            .build()
            .toString();
    res.header(HeaderNames.CONTENT_TYPE, "application/json");
    res.send(json);
  }
}
