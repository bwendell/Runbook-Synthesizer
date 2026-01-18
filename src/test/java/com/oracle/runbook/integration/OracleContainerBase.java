package com.oracle.runbook.integration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.oracle.OracleContainer;

/**
 * Base class for integration tests that require an Oracle 23ai database with vector support.
 *
 * <p>This class provides:
 *
 * <ul>
 *   <li>Automatic container lifecycle management via Testcontainers
 *   <li>JDBC connection details for the running Oracle instance
 *   <li>Vector store schema initialization
 *   <li>Shared network for multi-container tests (e.g., Ollama + Oracle)
 * </ul>
 *
 * <p>Usage: Extend this class in your integration tests. The Oracle container will be started
 * before all tests and stopped after all tests complete.
 *
 * <pre>{@code
 * class VectorStoreIT extends OracleContainerBase {
 *     @Test
 *     void shouldStoreAndRetrieveVectors() {
 *         try (Connection conn = getConnection()) {
 *             // Test vector operations
 *         }
 *     }
 * }
 * }</pre>
 */
public abstract class OracleContainerBase {

  /** Oracle 23ai Free container image. */
  private static final String ORACLE_IMAGE = "gvenzl/oracle-free:23-slim";

  /** SQL schema script for vector store initialization. */
  private static final String SCHEMA_SCRIPT = "schema/oracle-vector-schema.sql";

  /** Shared network for multi-container communication. */
  protected static Network sharedNetwork;

  /** Oracle 23ai container instance. */
  protected static OracleContainer oracle;

  /**
   * Starts the Oracle 23ai container before all tests.
   *
   * <p>Initializes the container with:
   *
   * <ul>
   *   <li>Oracle 23ai Free slim image (gvenzl/oracle-free:23-slim)
   *   <li>Vector schema initialization script
   *   <li>Shared network for multi-container tests
   * </ul>
   *
   * @throws AssertionError if Docker is not available
   */
  @BeforeAll
  static void startContainer() {
    // Explicit Docker availability check with clear error message
    assertDockerAvailable();

    sharedNetwork = Network.newNetwork();

    oracle =
        new OracleContainer(ORACLE_IMAGE)
            .withNetwork(sharedNetwork)
            .withNetworkAliases("oracle")
            .withInitScript(SCHEMA_SCRIPT);

    oracle.start();
  }

  /**
   * Asserts that Docker is available and accessible.
   *
   * @throws AssertionError if Docker is not available with actionable error message
   */
  private static void assertDockerAvailable() {
    if (!DockerClientFactory.instance().isDockerAvailable()) {
      throw new AssertionError(
          """

          ╔══════════════════════════════════════════════════════════════════════════════╗
          ║                         DOCKER NOT AVAILABLE                                  ║
          ╠══════════════════════════════════════════════════════════════════════════════╣
          ║ Container tests require Docker to be running.                                ║
          ║                                                                              ║
          ║ To fix this:                                                                 ║
          ║   1. Start Docker Desktop (Windows/Mac) or Docker daemon (Linux)             ║
          ║   2. Wait for Docker to fully initialize                                     ║
          ║   3. Re-run the tests                                                        ║
          ║                                                                              ║
          ║ To skip container tests:                                                     ║
          ║   Run without -Pe2e-containers or -Dtest.use.containers=true                 ║
          ╚══════════════════════════════════════════════════════════════════════════════╝
          """);
    }
  }

  /** Stops the Oracle container and cleans up resources after all tests. */
  @AfterAll
  static void stopContainer() {
    if (oracle != null) {
      oracle.stop();
    }
    if (sharedNetwork != null) {
      sharedNetwork.close();
    }
  }

  /**
   * Gets a JDBC connection to the running Oracle container.
   *
   * @return a new Connection to the Oracle database
   * @throws Exception if connection fails
   */
  protected static Connection getConnection() throws Exception {
    return DriverManager.getConnection(
        oracle.getJdbcUrl(), oracle.getUsername(), oracle.getPassword());
  }

  /**
   * Gets the JDBC URL for the running Oracle container.
   *
   * @return the JDBC URL
   */
  protected static String getJdbcUrl() {
    return oracle.getJdbcUrl();
  }

  /**
   * Gets the database username for the running Oracle container.
   *
   * @return the username
   */
  protected static String getUsername() {
    return oracle.getUsername();
  }

  /**
   * Gets the database password for the running Oracle container.
   *
   * @return the password
   */
  protected static String getPassword() {
    return oracle.getPassword();
  }

  /**
   * Gets the shared network for multi-container communication.
   *
   * @return the Testcontainers Network
   */
  protected static Network getSharedNetwork() {
    return sharedNetwork;
  }

  /**
   * Verifies that the Oracle container is running and accessible.
   *
   * @return true if the container is running and a connection can be established
   */
  protected static boolean isContainerHealthy() {
    try (Connection conn = getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT 1 FROM DUAL")) {
      return rs.next();
    } catch (Exception e) {
      return false;
    }
  }
}
