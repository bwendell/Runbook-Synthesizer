package com.oracle.runbook.integration;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.helidon.webserver.WebServer;
import io.helidon.webserver.WebServerConfig;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpServer;
import org.junit.jupiter.api.Test;

/** Minimal integration test to verify Helidon test infrastructure is working. */
@ServerTest
class SmokeIT {

  private final WebServer server;

  SmokeIT(WebServer server) {
    this.server = server;
  }

  @SetUpServer
  static void setup(WebServerConfig.Builder builder) {
    builder.routing(routing -> routing.get("/health", (req, res) -> res.send("OK")));
  }

  @Test
  void serverStartsSuccessfully() {
    assertNotNull(server, "Server should be available");
  }
}
