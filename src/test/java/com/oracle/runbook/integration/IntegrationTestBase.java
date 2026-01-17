package com.oracle.runbook.integration;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.testing.junit5.ServerTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/**
 * Base class for integration tests providing Helidon server and WireMock infrastructure.
 *
 * <p>Subclasses inherit:
 *
 * <ul>
 *   <li>Helidon test server via @ServerTest injection
 *   <li>WireMock server for mocking external HTTP services (OCI APIs, webhooks)
 *   <li>Shared lifecycle management
 * </ul>
 */
@ServerTest
public abstract class IntegrationTestBase {

  protected static WireMockServer wireMockServer;

  protected final WebServer server;

  protected IntegrationTestBase(WebServer server) {
    this.server = server;
  }

  @BeforeAll
  static void startWireMock() {
    wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
    wireMockServer.start();
    System.setProperty("wiremock.port", String.valueOf(wireMockServer.port()));
  }

  @AfterAll
  static void stopWireMock() {
    if (wireMockServer != null && wireMockServer.isRunning()) {
      wireMockServer.stop();
    }
    System.clearProperty("wiremock.port");
  }

  /** Returns the WireMock base URL for configuring HTTP clients in tests. */
  protected String wireMockBaseUrl() {
    return wireMockServer.baseUrl();
  }

  /** Resets WireMock stubs between tests. Call in @BeforeEach if needed. */
  protected void resetWireMock() {
    wireMockServer.resetAll();
  }
}
