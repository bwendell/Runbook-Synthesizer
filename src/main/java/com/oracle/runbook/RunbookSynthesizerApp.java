package com.oracle.runbook;

import io.helidon.config.Config;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;

import java.util.logging.Logger;

/**
 * Main entry point for Runbook-Synthesizer application.
 * <p>
 * A Helidon SE 4.x application that provides dynamic SOP generation for OCI
 * using RAG (Retrieval Augmented Generation).
 */
public class RunbookSynthesizerApp {

	private static final Logger LOGGER = Logger.getLogger(RunbookSynthesizerApp.class.getName());

	/**
	 * Application main entry point.
	 *
	 * @param args
	 *            command line arguments
	 */
	public static void main(String[] args) {
		// Load configuration from application.yaml
		Config config = Config.create();
		Config serverConfig = config.get("server");

		// Build and start the web server
		WebServer server = WebServer.builder().config(serverConfig).routing(RunbookSynthesizerApp::configureRouting)
				.build();

		// Register shutdown hook for graceful shutdown
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			LOGGER.info("Shutting down Runbook-Synthesizer...");
			server.stop();
			LOGGER.info("Runbook-Synthesizer stopped.");
		}));

		// Start the server
		server.start();

		LOGGER.info(() -> String.format("Runbook-Synthesizer started on http://localhost:%d", server.port()));
		LOGGER.info("Health endpoint available at /observe/health");
	}

	/**
	 * Configure HTTP routing for the application.
	 *
	 * @param routing
	 *            the routing builder
	 */
	private static void configureRouting(HttpRouting.Builder routing) {
		routing.get("/", (req, res) -> res.send("Runbook-Synthesizer is running"));
		routing.get("/health", (req, res) -> res.send("{\"status\":\"UP\"}"));
	}
}
