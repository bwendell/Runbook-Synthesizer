package com.oracle.runbook.integration;

import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Ollama container support for E2E tests requiring local LLM inference.
 *
 * <p>This class provides an Ollama container that can be used alongside the Oracle container for
 * end-to-end testing of the RAG pipeline.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * class RagPipelineE2EIT extends OracleContainerBase {
 *     private static GenericContainer<?> ollama;
 *
 *     @BeforeAll
 *     static void startContainers() {
 *         OracleContainerBase.startContainer();
 *         ollama = OllamaContainerSupport.createContainer(getSharedNetwork());
 *         ollama.start();
 *     }
 * }
 * }</pre>
 */
public final class OllamaContainerSupport {

  /** Ollama container image. */
  private static final String OLLAMA_IMAGE = "ollama/ollama:latest";

  /** Ollama API port. */
  private static final int OLLAMA_PORT = 11434;

  /** Container startup timeout. */
  private static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(2);

  private OllamaContainerSupport() {
    // Utility class
  }

  /**
   * Creates an Ollama container configured to share a network with the Oracle container.
   *
   * <p>The container is configured with:
   *
   * <ul>
   *   <li>ollama/ollama:latest image
   *   <li>Port 11434 exposed and bound to host port 11434 (fixed port for demo stability)
   *   <li>Health check via /api/tags endpoint
   *   <li>Network alias "ollama" for container-to-container communication
   * </ul>
   *
   * @param network the shared Testcontainers Network (from OracleContainerBase.getSharedNetwork())
   * @return a configured GenericContainer for Ollama (not started)
   */
  public static GenericContainer<?> createContainer(Network network) {
    return new GenericContainer<>(OLLAMA_IMAGE)
        .withNetwork(network)
        .withNetworkAliases("ollama")
        .withExposedPorts(OLLAMA_PORT)
        .withCreateContainerCmdModifier(
            cmd -> {
              var hostConfig = cmd.getHostConfig();
              if (hostConfig != null) {
                cmd.withHostConfig(
                    hostConfig.withPortBindings(
                        new com.github.dockerjava.api.model.PortBinding(
                            com.github.dockerjava.api.model.Ports.Binding.bindPort(OLLAMA_PORT),
                            new com.github.dockerjava.api.model.ExposedPort(OLLAMA_PORT))));
              }
            })
        .waitingFor(Wait.forHttp("/api/tags").forPort(OLLAMA_PORT))
        .withStartupTimeout(STARTUP_TIMEOUT);
  }

  /**
   * Gets the Ollama API endpoint URL for a running container.
   *
   * @param ollama the running Ollama container
   * @return the API base URL (e.g., "http://localhost:32768")
   */
  public static String getOllamaUrl(GenericContainer<?> ollama) {
    return String.format("http://%s:%d", ollama.getHost(), ollama.getMappedPort(OLLAMA_PORT));
  }

  /**
   * Gets the internal Ollama URL for container-to-container communication.
   *
   * <p>Use this URL when other containers (like Oracle) need to call Ollama.
   *
   * @return the internal URL using the network alias
   */
  public static String getInternalOllamaUrl() {
    return "http://ollama:" + OLLAMA_PORT;
  }

  /**
   * Pulls a model into the Ollama container.
   *
   * @param ollama the running Ollama container
   * @param modelName the name of the model to pull (e.g., "llama3.2:1b")
   * @throws java.io.IOException if the execution fails
   * @throws InterruptedException if the execution is interrupted
   */
  public static void pullModel(GenericContainer<?> ollama, String modelName)
      throws java.io.IOException, InterruptedException {
    org.testcontainers.containers.Container.ExecResult result =
        ollama.execInContainer("ollama", "pull", modelName);
    if (result.getExitCode() != 0) {
      throw new java.io.IOException(
          "Failed to pull model " + modelName + ": " + result.getStderr() + result.getStdout());
    }
  }
}
