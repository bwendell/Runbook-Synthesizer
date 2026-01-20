package com.oracle.runbook.integration.e2e;

import com.oracle.runbook.integration.DockerSupport;
import com.oracle.runbook.integration.LocalStackContainerBase;
import com.oracle.runbook.integration.OllamaContainerSupport;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

/**
 * A utility "test" that spins up the full E2E environment (LocalStack + Ollama) and keeps it
 * running to allow manual interaction and demos.
 *
 * <p>Usage: Run this specific test method from your IDE or Maven to start the environment:
 *
 * <pre>{@code
 * mvn failsafe:integration-test -Dit.test=DemoEnvironmentIT -Pe2e-containers
 * }</pre>
 *
 * <p>Or directly via the exec plugin for an interactive session:
 *
 * <pre>{@code
 * mvn exec:java -Dexec.mainClass=com.oracle.runbook.integration.e2e.DemoEnvironmentIT
 * }</pre>
 *
 * <p>The environment will stay running until:
 *
 * <ul>
 *   <li>CTRL+C is pressed (graceful shutdown via shutdown hook)
 *   <li>The JVM is terminated
 * </ul>
 */
class DemoEnvironmentIT extends LocalStackContainerBase {

  @Test
  void runDemoEnvironment() throws Exception {
    System.out.println("==================================================================");
    System.out.println("STARTING DEMO ENVIRONMENT...");
    System.out.println("==================================================================");

    // 1. Check Docker
    DockerSupport.ensureDockerAvailable();

    // 2. Start LocalStack (handled by base class @BeforeAll -> startLocalStackContainer)
    // Note: Since we are not using standard JUnit lifecycle in a typical way here but running a
    // single test,
    // we need to ensure the container is started. The base class uses @BeforeAll which runs once
    // per class.
    // If running via 'mvn test', JUnit handles this.
    // Let's verify it's running.
    if (!isContainerRunning()) {
      startLocalStackContainer();
    }

    // 3. Start Ollama with its own network
    Network ollamaNetwork = Network.newNetwork();
    GenericContainer<?> ollama = OllamaContainerSupport.createContainer(ollamaNetwork);
    ollama.start();

    // Pull required models
    System.out.println("Pulling model: llama3.2:1b (this may take a while)...");
    OllamaContainerSupport.pullModel(ollama, "llama3.2:1b");
    System.out.println("Model pulled successfully.");

    System.out.println("Pulling model: nomic-embed-text...");
    OllamaContainerSupport.pullModel(ollama, "nomic-embed-text");
    System.out.println("Model pulled successfully.");

    // 4. Print Connection Info
    System.out.println("\n--- ENVIRONMENT READY ---");
    System.out.println("LocalStack Endpoint: " + localstack.getEndpoint());
    System.out.println("LocalStack Region: " + localstack.getRegion());
    System.out.println(
        "Ollama API URL: " + OllamaContainerSupport.getOllamaUrl((GenericContainer) ollama));
    System.out.println("Ollama Network ID: " + ollamaNetwork.getId());

    System.out.println("\n------------------------------------------------------------------");
    System.out.println("TO CONFIGURE YOUR APP (src/main/resources/application.yaml):");
    System.out.println("------------------------------------------------------------------");
    System.out.println("app:");
    System.out.println("  stub-mode: false");
    System.out.println("cloud:");
    System.out.println("  provider: aws");
    System.out.println("  aws:");
    System.out.println("    endpoint: " + localstack.getEndpoint());
    System.out.println("    region: " + localstack.getRegion());
    System.out.println("    credentials:");
    System.out.println("      access-key: " + localstack.getAccessKey());
    System.out.println("      secret-key: " + localstack.getSecretKey());
    System.out.println("vectorStore:");
    System.out.println("  provider: local");
    System.out.println("llm:");
    System.out.println("  provider: ollama");
    System.out.println("  ollama:");
    System.out.println(
        "    url: " + OllamaContainerSupport.getOllamaUrl((GenericContainer) ollama));
    System.out.println("------------------------------------------------------------------\n");

    System.out.println(">>> ENVIRONMENT RUNNING - PRESS CTRL+C TO SHUT DOWN <<<");
    System.out.println(">>> Or terminate from IDE/Maven to stop <<<");

    // Use CountDownLatch with shutdown hook for proper blocking
    // This works in both interactive and non-interactive contexts
    CountDownLatch shutdownLatch = new CountDownLatch(1);
    GenericContainer<?> ollamaRef = ollama;

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  System.out.println("\nShutdown signal received. Stopping containers...");
                  try {
                    ollamaRef.stop();
                  } catch (Exception e) {
                    System.err.println("Error stopping Ollama: " + e.getMessage());
                  }
                  shutdownLatch.countDown();
                }));

    // Block until shutdown signal
    try {
      shutdownLatch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      System.out.println("Interrupted. Stopping containers...");
      ollama.stop();
    }
    // LocalStack stopped by base class @AfterAll
  }
}
