package com.oracle.runbook.integration;

import java.net.URI;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsAsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * Base class for integration tests that require LocalStack AWS services.
 *
 * <p>This class provides:
 *
 * <ul>
 *   <li>Automatic LocalStack container lifecycle management via Testcontainers
 *   <li>Factory methods for creating AWS SDK async clients pointing to LocalStack
 *   <li>S3, CloudWatch, and CloudWatch Logs service support
 * </ul>
 *
 * <p>Usage: Extend this class in your integration tests. The LocalStack container will be started
 * before all tests and stopped after all tests complete.
 *
 * <pre>{@code
 * class AwsS3StorageAdapterIT extends LocalStackContainerBase {
 *     @Test
 *     void shouldListRunbooks() throws Exception {
 *         S3AsyncClient s3 = createS3Client();
 *         // Test S3 operations
 *     }
 * }
 * }</pre>
 */
public abstract class LocalStackContainerBase {

  /** LocalStack Docker image. */
  private static final DockerImageName LOCALSTACK_IMAGE =
      DockerImageName.parse("localstack/localstack:3.4");

  /** LocalStack container instance. */
  protected static LocalStackContainer localstack;

  /**
   * Starts the LocalStack container before all tests.
   *
   * <p>Initializes the container with S3, CloudWatch, and CloudWatch Logs services.
   *
   * @throws AssertionError if Docker is not available
   */
  @BeforeAll
  static void startLocalStackContainer() {
    assertDockerAvailable();

    localstack =
        new LocalStackContainer(LOCALSTACK_IMAGE)
            .withServices(Service.S3, Service.CLOUDWATCH, Service.CLOUDWATCHLOGS);

    localstack.start();
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
          ║ LocalStack integration tests require Docker to be running.                   ║
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

  /** Stops the LocalStack container and cleans up resources after all tests. */
  @AfterAll
  static void stopLocalStackContainer() {
    if (localstack != null) {
      localstack.stop();
    }
  }

  /**
   * Creates an S3AsyncClient configured to use the LocalStack endpoint.
   *
   * @return a new S3AsyncClient pointing to LocalStack
   */
  protected static S3AsyncClient createS3Client() {
    return S3AsyncClient.builder()
        .endpointOverride(getServiceEndpoint(Service.S3))
        .region(Region.of(localstack.getRegion()))
        .credentialsProvider(getCredentialsProvider())
        .forcePathStyle(true) // Required for LocalStack
        .build();
  }

  /**
   * Creates a CloudWatchAsyncClient configured to use the LocalStack endpoint.
   *
   * @return a new CloudWatchAsyncClient pointing to LocalStack
   */
  protected static CloudWatchAsyncClient createCloudWatchClient() {
    return CloudWatchAsyncClient.builder()
        .endpointOverride(getServiceEndpoint(Service.CLOUDWATCH))
        .region(Region.of(localstack.getRegion()))
        .credentialsProvider(getCredentialsProvider())
        .build();
  }

  /**
   * Creates a CloudWatchLogsAsyncClient configured to use the LocalStack endpoint.
   *
   * @return a new CloudWatchLogsAsyncClient pointing to LocalStack
   */
  protected static CloudWatchLogsAsyncClient createCloudWatchLogsClient() {
    return CloudWatchLogsAsyncClient.builder()
        .endpointOverride(getServiceEndpoint(Service.CLOUDWATCHLOGS))
        .region(Region.of(localstack.getRegion()))
        .credentialsProvider(getCredentialsProvider())
        .build();
  }

  /**
   * Gets the endpoint URI for a specific LocalStack service.
   *
   * @param service the LocalStack service
   * @return the endpoint URI
   */
  protected static URI getServiceEndpoint(Service service) {
    return localstack.getEndpointOverride(service);
  }

  /**
   * Gets the credentials provider for LocalStack access.
   *
   * @return a StaticCredentialsProvider with LocalStack test credentials
   */
  protected static StaticCredentialsProvider getCredentialsProvider() {
    return StaticCredentialsProvider.create(
        AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey()));
  }

  /**
   * Gets the region configured for the LocalStack container.
   *
   * @return the region string
   */
  protected static String getRegion() {
    return localstack.getRegion();
  }

  /**
   * Verifies that the LocalStack container is running.
   *
   * @return true if the container is running
   */
  protected static boolean isContainerRunning() {
    return localstack != null && localstack.isRunning();
  }
}
