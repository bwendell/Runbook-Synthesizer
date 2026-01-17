package com.oracle.runbook.api.dto;

import java.util.List;
import java.util.Objects;

/**
 * Response body for POST /api/v1/runbooks/sync endpoint.
 *
 * @param status the current sync status
 * @param documentsProcessed number of documents processed so far
 * @param errors list of error messages if any
 * @param requestId unique identifier for tracking this sync request
 */
public record SyncResponse(
    SyncStatus status, int documentsProcessed, List<String> errors, String requestId) {

  /** Compact constructor with defensive copy. */
  public SyncResponse {
    Objects.requireNonNull(requestId, "requestId is required");
    errors = errors != null ? List.copyOf(errors) : List.of();
  }

  /** Sync operation status. */
  public enum SyncStatus {
    STARTED,
    COMPLETED,
    FAILED
  }
}
