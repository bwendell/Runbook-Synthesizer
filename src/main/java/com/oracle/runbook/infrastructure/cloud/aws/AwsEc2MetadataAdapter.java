package com.oracle.runbook.infrastructure.cloud.aws;

import com.oracle.runbook.domain.ResourceMetadata;
import com.oracle.runbook.infrastructure.cloud.ComputeMetadataAdapter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.services.ec2.Ec2AsyncClient;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.Tag;

/**
 * AWS EC2 implementation of {@link ComputeMetadataAdapter}.
 *
 * <p>Provides compute instance metadata retrieval using AWS Ec2AsyncClient for non-blocking
 * operations compatible with Helidon SE's reactive patterns.
 */
public class AwsEc2MetadataAdapter implements ComputeMetadataAdapter {

  private final Ec2AsyncClient ec2Client;

  /**
   * Creates a new AwsEc2MetadataAdapter.
   *
   * @param ec2Client the AWS EC2 async client
   * @throws NullPointerException if ec2Client is null
   */
  public AwsEc2MetadataAdapter(Ec2AsyncClient ec2Client) {
    this.ec2Client = Objects.requireNonNull(ec2Client, "ec2Client cannot be null");
  }

  @Override
  public String providerType() {
    return "aws";
  }

  @Override
  public CompletableFuture<Optional<ResourceMetadata>> getInstance(String instanceId) {
    var request = DescribeInstancesRequest.builder().instanceIds(instanceId).build();

    return ec2Client
        .describeInstances(request)
        .thenApply(
            response -> {
              List<Reservation> reservations = response.reservations();
              if (reservations == null || reservations.isEmpty()) {
                return Optional.empty();
              }

              List<Instance> instances = reservations.get(0).instances();
              if (instances == null || instances.isEmpty()) {
                return Optional.empty();
              }

              return Optional.of(convertToResourceMetadata(instances.get(0)));
            });
  }

  /** Converts AWS EC2 Instance to domain ResourceMetadata. */
  private ResourceMetadata convertToResourceMetadata(Instance instance) {
    String name = extractNameFromTags(instance.tags());
    String az = instance.placement() != null ? instance.placement().availabilityZone() : null;

    return new ResourceMetadata(
        instance.instanceId(),
        name,
        null, // AWS doesn't have compartmentId concept
        instance.instanceType() != null ? instance.instanceType().toString() : null,
        az,
        extractFreeformTags(instance.tags()),
        Map.of() // AWS doesn't have defined tags concept like OCI
        );
  }

  /** Extracts Name tag value from EC2 tags. */
  private String extractNameFromTags(List<Tag> tags) {
    if (tags == null) {
      return null;
    }
    return tags.stream()
        .filter(t -> "Name".equals(t.key()))
        .findFirst()
        .map(Tag::value)
        .orElse(null);
  }

  /** Converts EC2 tags to freeform tags map. */
  private Map<String, String> extractFreeformTags(List<Tag> tags) {
    if (tags == null || tags.isEmpty()) {
      return Map.of();
    }

    Map<String, String> freeformTags = new HashMap<>();
    for (Tag tag : tags) {
      freeformTags.put(tag.key(), tag.value());
    }
    return freeformTags;
  }
}
