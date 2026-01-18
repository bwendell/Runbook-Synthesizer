package com.oracle.runbook;

import com.oracle.runbook.api.AlertResource;
import com.oracle.runbook.api.HealthResource;
import com.oracle.runbook.api.RunbookResource;
import com.oracle.runbook.api.WebhookResource;
import io.helidon.config.Config;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import java.util.logging.Logger;

/**
 * Main entry point for Runbook-Synthesizer application.
 *
 * <p>A Helidon SE 4.x application that provides dynamic SOP generation for OCI using RAG (Retrieval
 * Augmented Generation).
 */
public class RunbookSynthesizerApp {

  private static final Logger LOGGER = Logger.getLogger(RunbookSynthesizerApp.class.getName());

  /**
   * Application main entry point.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    // Load configuration from application.yaml
    Config config = Config.create();
    Config serverConfig = config.get("server");

    // Build and start the web server
    WebServer server =
        WebServer.builder()
            .config(serverConfig)
            .routing(routing -> configureRouting(routing, config))
            .build();

    // Register shutdown hook for graceful shutdown
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  LOGGER.info("Shutting down Runbook-Synthesizer...");
                  server.stop();
                  LOGGER.info("Runbook-Synthesizer stopped.");
                }));

    // Start the server
    server.start();

    LOGGER.info(
        () -> String.format("Runbook-Synthesizer started on http://localhost:%d", server.port()));
    LOGGER.info(
        "API endpoints: /api/v1/health, /api/v1/alerts, /api/v1/webhooks, /api/v1/runbooks");
  }

  /**
   * Configure HTTP routing for the application.
   *
   * @param routing the routing builder
   * @param config the application configuration
   */
  static void configureRouting(HttpRouting.Builder routing, Config config) {
    // Root endpoint
    routing.get("/", (req, res) -> res.send("Runbook-Synthesizer is running"));

    // Check if stub mode is enabled (defaults to true for backward compatibility)
    boolean stubMode = config.get("app.stub-mode").asBoolean().orElse(true);
    LOGGER.info("AlertResource configured with stubMode=" + stubMode);

    // API v1 endpoints
    routing.register("/api/v1/health", new HealthResource());

    if (stubMode) {
      // Use no-arg constructor for stub mode (existing tests/simple deployment)
      routing.register("/api/v1/alerts", new AlertResource());
    } else {
      // TODO: Wire real RagPipelineService and WebhookDispatcher when dependencies
      // are ready
      // For now, fall back to stub mode even if config says real mode
      LOGGER.warning(
          "Real mode requested but dependencies not yet wired. Using stub mode instead.");
      routing.register("/api/v1/alerts", new AlertResource());
    }

    routing.register("/api/v1/webhooks", new WebhookResource());
    routing.register("/api/v1/runbooks", new RunbookResource());
  }
}
