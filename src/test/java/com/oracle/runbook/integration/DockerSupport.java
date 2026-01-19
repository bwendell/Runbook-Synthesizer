/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0.
 */
package com.oracle.runbook.integration;

import java.util.concurrent.TimeUnit;
import org.testcontainers.DockerClientFactory;

/**
 * Utility class that ensures Docker is available before running container-based tests.
 *
 * <p>This class provides a centralized Docker availability check that:
 *
 * <ul>
 *   <li>Checks if Docker is already running
 *   <li>Attempts to start Docker Desktop if not running (Windows/Mac)
 *   <li>Waits up to 60 seconds for Docker to become available
 *   <li>Fails fast with a clear error banner if Docker cannot be started
 * </ul>
 *
 * <p>Usage: Call {@link #ensureDockerAvailable()} in {@code @BeforeAll} of any container-based
 * test.
 */
public final class DockerSupport {

  private static final int DOCKER_START_TIMEOUT_SECONDS = 60;
  private static final int DOCKER_POLL_INTERVAL_MS = 2000;

  /** Flag to track if Docker startup was already attempted this test run. */
  private static volatile boolean dockerStartAttempted = false;

  private DockerSupport() {
    // Utility class, no instantiation
  }

  /**
   * Ensures Docker is available for container-based tests.
   *
   * <p>This method:
   *
   * <ol>
   *   <li>Checks if Docker is already running (fast path)
   *   <li>If not, attempts to start Docker Desktop (Windows/Mac) or daemon (Linux)
   *   <li>Waits up to 60 seconds for Docker to become available with retry logic
   *   <li>Fails fast with a clear error banner if Docker cannot be started
   * </ol>
   *
   * <p>This method is thread-safe and idempotent - multiple calls will only attempt to start Docker
   * once per test run.
   *
   * @throws AssertionError if Docker is not available and cannot be started
   */
  public static synchronized void ensureDockerAvailable() {
    // Fast path: Docker is already running
    if (isDockerRunning()) {
      System.out.println("[DockerSupport] ✓ Docker is available.");
      return;
    }

    // Only attempt to start Docker once per test run
    if (dockerStartAttempted) {
      failWithDockerError("Docker was not available and previous start attempt failed.");
    }

    dockerStartAttempted = true;
    System.out.println("[DockerSupport] Docker is not running. Attempting to start...");

    if (!attemptStartDocker()) {
      failWithDockerError("Failed to launch Docker Desktop. Please start it manually.");
    }

    // Wait for Docker to become available with retry logic
    long startTime = System.currentTimeMillis();
    long timeoutMs = DOCKER_START_TIMEOUT_SECONDS * 1000L;

    while (System.currentTimeMillis() - startTime < timeoutMs) {
      if (isDockerRunning()) {
        long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
        System.out.printf(
            "[DockerSupport] ✓ Docker started successfully after %d seconds.%n", elapsedSeconds);
        return;
      }

      long remainingSeconds = (timeoutMs - (System.currentTimeMillis() - startTime)) / 1000;
      System.out.printf(
          "[DockerSupport] Waiting for Docker to start... (%d seconds remaining)%n",
          remainingSeconds);

      try {
        Thread.sleep(DOCKER_POLL_INTERVAL_MS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        failWithDockerError("Interrupted while waiting for Docker to start.");
      }
    }

    failWithDockerError(
        String.format("Docker did not start within %d seconds.", DOCKER_START_TIMEOUT_SECONDS));
  }

  /**
   * Checks if Docker daemon is currently running using Testcontainers' built-in check.
   *
   * @return true if Docker is running and responsive, false otherwise
   */
  private static boolean isDockerRunning() {
    try {
      return DockerClientFactory.instance().isDockerAvailable();
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Attempts to start Docker Desktop on Windows or Mac, or the Docker daemon on Linux.
   *
   * @return true if the start command was issued successfully, false otherwise
   */
  private static boolean attemptStartDocker() {
    String os = System.getProperty("os.name", "").toLowerCase();

    try {
      ProcessBuilder pb;
      if (os.contains("win")) {
        // Windows: Start Docker Desktop
        pb =
            new ProcessBuilder(
                "cmd",
                "/c",
                "start",
                "",
                "\"C:\\Program Files\\Docker\\Docker\\Docker Desktop.exe\"");
      } else if (os.contains("mac")) {
        // macOS: Open Docker Desktop app
        pb = new ProcessBuilder("open", "-a", "Docker");
      } else {
        // Linux: Try to start Docker daemon via systemctl
        pb = new ProcessBuilder("sudo", "systemctl", "start", "docker");
      }

      pb.redirectErrorStream(true);
      Process process = pb.start();
      process.waitFor(10, TimeUnit.SECONDS);
      return true;
    } catch (Exception e) {
      System.err.printf("[DockerSupport] Failed to start Docker: %s%n", e.getMessage());
      return false;
    }
  }

  /**
   * Fails the test with a prominent Docker error message.
   *
   * @param reason the specific reason for the failure
   */
  private static void failWithDockerError(String reason) {
    String errorMessage =
        """

        ╔══════════════════════════════════════════════════════════════════════════════╗
        ║                      🐳 DOCKER IS NOT AVAILABLE 🐳                            ║
        ╠══════════════════════════════════════════════════════════════════════════════╣
        ║                                                                              ║
        ║  Reason: %s
        ║                                                                              ║
        ║  Container-based tests require Docker to be running.                         ║
        ║                                                                              ║
        ║  To fix this:                                                                ║
        ║    1. Start Docker Desktop (Windows/Mac) or Docker daemon (Linux)            ║
        ║    2. Wait for Docker to fully initialize (~30 seconds)                      ║
        ║    3. Verify with: docker info                                               ║
        ║    4. Re-run the tests                                                       ║
        ║                                                                              ║
        ║  To skip container tests:                                                    ║
        ║    Run without -Pe2e-containers or -Dtest.use.containers=true                ║
        ║                                                                              ║
        ╚══════════════════════════════════════════════════════════════════════════════╝

        """
            .formatted(reason);

    System.err.println(errorMessage);
    throw new AssertionError("DOCKER NOT AVAILABLE: " + reason);
  }
}
