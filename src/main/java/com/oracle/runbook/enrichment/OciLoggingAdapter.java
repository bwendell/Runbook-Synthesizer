package com.oracle.runbook.enrichment;

import com.oracle.bmc.loggingsearch.LogSearchClient;
import com.oracle.bmc.loggingsearch.model.SearchLogsDetails;
import com.oracle.bmc.loggingsearch.model.SearchResult;
import com.oracle.bmc.loggingsearch.requests.SearchLogsRequest;
import com.oracle.bmc.loggingsearch.responses.SearchLogsResponse;
import com.oracle.runbook.config.OciConfig;
import com.oracle.runbook.domain.LogEntry;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * OCI Logging adapter implementing {@link LogSourceAdapter}.
 *
 * <p>Fetches logs from OCI Logging Search API and converts them to domain model objects. Uses the
 * OCI Java SDK LogSearchClient for API calls.
 */
public class OciLoggingAdapter implements LogSourceAdapter {

  private static final String SOURCE_TYPE = "oci-logging";
  private static final int DEFAULT_LIMIT = 100;

  private final LogSearchClient logSearchClient;
  private final OciConfig config;

  /**
   * Creates a new OciLoggingAdapter.
   *
   * @param logSearchClient the OCI Log Search client
   * @param config the OCI configuration
   */
  public OciLoggingAdapter(LogSearchClient logSearchClient, OciConfig config) {
    this.logSearchClient =
        Objects.requireNonNull(logSearchClient, "logSearchClient cannot be null");
    this.config = Objects.requireNonNull(config, "config cannot be null");
  }

  @Override
  public String sourceType() {
    return SOURCE_TYPE;
  }

  @Override
  public CompletableFuture<List<LogEntry>> fetchLogs(
      String resourceId, Duration lookback, String query) {
    return CompletableFuture.supplyAsync(
        () -> {
          Instant endTime = Instant.now();
          Instant startTime = endTime.minus(lookback);

          // Build search query combining resourceId filter with user query
          String searchQuery = buildSearchQuery(resourceId, query);

          SearchLogsDetails details =
              SearchLogsDetails.builder()
                  .searchQuery(searchQuery)
                  .timeStart(Date.from(startTime))
                  .timeEnd(Date.from(endTime))
                  .isReturnFieldInfo(false)
                  .build();

          SearchLogsRequest request =
              SearchLogsRequest.builder().searchLogsDetails(details).limit(DEFAULT_LIMIT).build();

          SearchLogsResponse response = logSearchClient.searchLogs(request);

          return convertToLogEntries(response.getSearchResponse().getResults());
        });
  }

  /** Builds the OCI Logging search query. */
  private String buildSearchQuery(String resourceId, String userQuery) {
    StringBuilder queryBuilder = new StringBuilder();

    // Add resource filter
    queryBuilder.append("search \"").append(config.compartmentId()).append("\"");

    if (resourceId != null && !resourceId.isBlank()) {
      queryBuilder.append(" | where resource = '").append(resourceId).append("'");
    }

    // Append user query if provided
    if (userQuery != null && !userQuery.isBlank()) {
      queryBuilder.append(" | ").append(userQuery);
    }

    return queryBuilder.toString();
  }

  /** Converts OCI SearchResult list to domain LogEntry list. */
  private List<LogEntry> convertToLogEntries(List<SearchResult> results) {
    if (results == null || results.isEmpty()) {
      return List.of();
    }

    List<LogEntry> entries = new ArrayList<>();
    int idx = 0;

    for (SearchResult result : results) {
      Object dataObj = result.getData();
      if (dataObj instanceof Map<?, ?> rawMap) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) rawMap;

        String id = extractString(data, "logId", "log-" + idx++);
        Instant timestamp = extractTimestamp(data);
        String level = extractString(data, "level", "INFO");
        String message = extractString(data, "message", "");
        Map<String, String> metadata = extractMetadata(data);

        entries.add(new LogEntry(id, timestamp, level, message, metadata));
      }
    }

    return entries;
  }

  private String extractString(Map<String, Object> data, String key, String defaultValue) {
    Object value = data.get(key);
    return value != null ? value.toString() : defaultValue;
  }

  private Instant extractTimestamp(Map<String, Object> data) {
    Object timeValue = data.get("datetime");
    if (timeValue instanceof Date date) {
      return date.toInstant();
    } else if (timeValue instanceof String str) {
      try {
        return Instant.parse(str);
      } catch (Exception e) {
        return Instant.now();
      }
    }
    return Instant.now();
  }

  private Map<String, String> extractMetadata(Map<String, Object> data) {
    Map<String, String> metadata = new HashMap<>();
    for (Map.Entry<String, Object> entry : data.entrySet()) {
      String key = entry.getKey();
      // Skip known fields
      if (!key.equals("logId")
          && !key.equals("datetime")
          && !key.equals("level")
          && !key.equals("message")) {
        metadata.put(key, entry.getValue() != null ? entry.getValue().toString() : "");
      }
    }
    return metadata;
  }
}
