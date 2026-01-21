package com.oracle.runbook.config;

import io.helidon.config.Config;

/**
 * Configuration for runbook ingestion settings.
 *
 * <p>This record holds configuration for the S3 bucket containing runbooks and whether to
 * automatically ingest runbooks at application startup.
 *
 * @param bucket the S3 bucket name containing runbook markdown files
 * @param ingestOnStartup whether to automatically ingest runbooks when the app starts
 */
public record RunbookConfig(String bucket, boolean ingestOnStartup) {

  /** Default bucket name used when not configured. */
  public static final String DEFAULT_BUCKET = "runbook-synthesizer-runbooks";

  /** Default value for ingestOnStartup when not configured. */
  public static final boolean DEFAULT_INGEST_ON_STARTUP = true;

  /**
   * Creates a RunbookConfig from Helidon Config.
   *
   * <p>Reads the following configuration keys:
   *
   * <ul>
   *   <li>{@code runbooks.bucket} - S3 bucket name (default: "runbook-synthesizer-runbooks")
   *   <li>{@code runbooks.ingestOnStartup} - Whether to ingest at startup (default: true)
   * </ul>
   *
   * @param config the Helidon configuration
   */
  public RunbookConfig(Config config) {
    this(
        config.get("runbooks.bucket").asString().orElse(DEFAULT_BUCKET),
        config.get("runbooks.ingestOnStartup").asBoolean().orElse(DEFAULT_INGEST_ON_STARTUP));
  }
}
