package com.oracle.runbook.infrastructure.cloud.aws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.oracle.runbook.domain.ResourceMetadata;
import com.oracle.runbook.infrastructure.cloud.ComputeMetadataAdapter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.ec2.Ec2AsyncClient;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Placement;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.Tag;

/**
 * Unit tests for {@link AwsEc2MetadataAdapter}.
 *
 * <p>Uses mocked Ec2AsyncClient per testing-patterns-java for external SDK dependencies.
 */
class AwsEc2MetadataAdapterTest {

  @Nested
  @DisplayName("ComputeMetadataAdapter interface implementation")
  class InterfaceImplementationTests {

    @Test
    @DisplayName("AwsEc2MetadataAdapter should implement ComputeMetadataAdapter")
    void shouldImplementComputeMetadataAdapter() {
      Ec2AsyncClient mockClient = mock(Ec2AsyncClient.class);
      AwsEc2MetadataAdapter adapter = new AwsEc2MetadataAdapter(mockClient);

      assertThat(adapter)
          .as("AwsEc2MetadataAdapter must implement ComputeMetadataAdapter")
          .isInstanceOf(ComputeMetadataAdapter.class);
    }

    @Test
    @DisplayName("providerType() should return 'aws'")
    void providerTypeShouldReturnAws() {
      Ec2AsyncClient mockClient = mock(Ec2AsyncClient.class);
      AwsEc2MetadataAdapter adapter = new AwsEc2MetadataAdapter(mockClient);

      assertThat(adapter.providerType())
          .as("providerType() must return 'aws' for EC2 adapter")
          .isEqualTo("aws");
    }
  }

  @Nested
  @DisplayName("Constructor validation")
  class ConstructorValidationTests {

    @Test
    @DisplayName("Should reject null Ec2AsyncClient")
    void shouldRejectNullClient() {
      assertThatThrownBy(() -> new AwsEc2MetadataAdapter(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("ec2Client");
    }
  }

  @Nested
  @DisplayName("getInstance()")
  class GetInstanceTests {

    @Test
    @DisplayName("Should return instance metadata when instance exists")
    void shouldReturnMetadataWhenInstanceExists() throws Exception {
      Ec2AsyncClient mockClient = mock(Ec2AsyncClient.class);

      Instance mockInstance =
          Instance.builder()
              .instanceId("i-1234567890abcdef0")
              .tags(Tag.builder().key("Name").value("test-instance").build())
              .instanceType("t3.medium")
              .placement(Placement.builder().availabilityZone("us-west-2a").build())
              .build();

      DescribeInstancesResponse mockResponse =
          DescribeInstancesResponse.builder()
              .reservations(Reservation.builder().instances(mockInstance).build())
              .build();

      when(mockClient.describeInstances(any(DescribeInstancesRequest.class)))
          .thenReturn(CompletableFuture.completedFuture(mockResponse));

      AwsEc2MetadataAdapter adapter = new AwsEc2MetadataAdapter(mockClient);

      Optional<ResourceMetadata> result = adapter.getInstance("i-1234567890abcdef0").get();

      assertThat(result).isPresent();
      assertThat(result.get().ocid()).isEqualTo("i-1234567890abcdef0");
      assertThat(result.get().shape()).isEqualTo("t3.medium");
      assertThat(result.get().availabilityDomain()).isEqualTo("us-west-2a");
    }

    @Test
    @DisplayName("Should return empty when instance not found")
    void shouldReturnEmptyWhenInstanceNotFound() throws Exception {
      Ec2AsyncClient mockClient = mock(Ec2AsyncClient.class);

      DescribeInstancesResponse mockResponse =
          DescribeInstancesResponse.builder().reservations(List.of()).build();

      when(mockClient.describeInstances(any(DescribeInstancesRequest.class)))
          .thenReturn(CompletableFuture.completedFuture(mockResponse));

      AwsEc2MetadataAdapter adapter = new AwsEc2MetadataAdapter(mockClient);

      Optional<ResourceMetadata> result = adapter.getInstance("i-nonexistent").get();

      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("Exception handling")
  class ExceptionHandlingTests {

    @Test
    @DisplayName("Should wrap Ec2Exception in CompletionException")
    void shouldWrapEc2ExceptionInCompletionException() {
      Ec2AsyncClient mockClient = mock(Ec2AsyncClient.class);

      // Ec2Exception.builder() returns Ec2Exception for EC2 service errors
      CompletableFuture<DescribeInstancesResponse> failedFuture = new CompletableFuture<>();
      failedFuture.completeExceptionally(
          software.amazon.awssdk.services.ec2.model.Ec2Exception.builder()
              .message("Request has expired")
              .statusCode(400)
              .awsErrorDetails(
                  software.amazon.awssdk.awscore.exception.AwsErrorDetails.builder()
                      .errorCode("RequestExpired")
                      .errorMessage("Request has expired")
                      .build())
              .build());

      when(mockClient.describeInstances(any(DescribeInstancesRequest.class)))
          .thenReturn(failedFuture);

      AwsEc2MetadataAdapter adapter = new AwsEc2MetadataAdapter(mockClient);

      // Following aws-sdk-java pattern: async exceptions wrapped in CompletionException
      assertThatThrownBy(() -> adapter.getInstance("i-expired").get())
          .isInstanceOf(java.util.concurrent.ExecutionException.class)
          .hasRootCauseInstanceOf(software.amazon.awssdk.services.ec2.model.Ec2Exception.class)
          .hasMessageContaining("Request has expired");
    }

    @Test
    @DisplayName("Should handle empty reservations list gracefully")
    void shouldHandleEmptyReservationsGracefully() throws Exception {
      Ec2AsyncClient mockClient = mock(Ec2AsyncClient.class);

      // Response with no reservations (null or empty handled same way)
      DescribeInstancesResponse mockResponse =
          DescribeInstancesResponse.builder().build(); // No reservations set

      when(mockClient.describeInstances(any(DescribeInstancesRequest.class)))
          .thenReturn(CompletableFuture.completedFuture(mockResponse));

      AwsEc2MetadataAdapter adapter = new AwsEc2MetadataAdapter(mockClient);

      Optional<ResourceMetadata> result = adapter.getInstance("i-nonexistent").get();

      assertThat(result).as("Should return empty when no reservations").isEmpty();
    }

    @Test
    @DisplayName("Should handle null tags in instance gracefully")
    void shouldHandleNullTagsGracefully() throws Exception {
      Ec2AsyncClient mockClient = mock(Ec2AsyncClient.class);

      // Instance with no tags set (null)
      Instance mockInstance =
          Instance.builder()
              .instanceId("i-notags")
              .instanceType("t3.micro")
              // No tags set - should handle null gracefully
              .build();

      DescribeInstancesResponse mockResponse =
          DescribeInstancesResponse.builder()
              .reservations(Reservation.builder().instances(mockInstance).build())
              .build();

      when(mockClient.describeInstances(any(DescribeInstancesRequest.class)))
          .thenReturn(CompletableFuture.completedFuture(mockResponse));

      AwsEc2MetadataAdapter adapter = new AwsEc2MetadataAdapter(mockClient);

      Optional<ResourceMetadata> result = adapter.getInstance("i-notags").get();

      assertThat(result).isPresent();
      assertThat(result.get().displayName()).isNull(); // No Name tag
      assertThat(result.get().freeformTags()).isEmpty(); // No freeform tags
    }
  }
}
