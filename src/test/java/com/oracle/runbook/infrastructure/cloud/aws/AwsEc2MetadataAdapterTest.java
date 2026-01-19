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
              .placement(Placement.builder().availabilityZone("us-east-1a").build())
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
      assertThat(result.get().availabilityDomain()).isEqualTo("us-east-1a");
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
}
