package com.oracle.runbook.api;

import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
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
    String timestamp = Instant.now().toString();
    String json = String.format("{\"status\":\"UP\",\"timestamp\":\"%s\"}", timestamp);
    res.send(json);
  }
}
