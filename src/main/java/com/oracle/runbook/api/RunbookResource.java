package com.oracle.runbook.api;

import com.oracle.runbook.api.dto.SyncResponse;
import com.oracle.runbook.api.dto.SyncResponse.SyncStatus;
import io.helidon.http.Status;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.json.Json;
import java.util.List;
import java.util.UUID;

/**
 * Runbook sync resource providing POST /api/v1/runbooks/sync endpoint.
 *
 * <p>Triggers re-indexing of runbooks from OCI Object Storage.
 */
public class RunbookResource implements HttpService {

  @Override
  public void routing(HttpRules rules) {
    rules.post("/sync", this::handlePostSync);
  }

  private void handlePostSync(ServerRequest req, ServerResponse res) {
    // Generate a request ID for tracking
    String requestId = UUID.randomUUID().toString();

    // Create response indicating sync has started (stub implementation)
    var response = new SyncResponse(SyncStatus.STARTED, 0, List.of(), requestId);

    res.status(Status.ACCEPTED_202);
    res.send(toJson(response));
  }

  private String toJson(SyncResponse response) {
    var errorsBuilder = Json.createArrayBuilder();
    response.errors().forEach(errorsBuilder::add);

    return Json.createObjectBuilder()
        .add("status", response.status().name())
        .add("documentsProcessed", response.documentsProcessed())
        .add("errors", errorsBuilder)
        .add("requestId", response.requestId())
        .build()
        .toString();
  }
}
