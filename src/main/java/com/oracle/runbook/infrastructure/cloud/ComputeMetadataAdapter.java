package com.oracle.runbook.infrastructure.cloud;

import com.oracle.runbook.domain.ResourceMetadata;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Port interface for fetching compute instance metadata from cloud providers.
 *
 * <p>This interface defines the contract for compute metadata adapters in the Hexagonal
 * Architecture. Implementations provide concrete integrations with specific cloud compute services
 * like OCI Compute, AWS EC2, etc.
 *
 * <p>All implementations must be non-blocking and return CompletableFuture to support Helidon SE's
 * reactive patterns.
 *
 * @see ResourceMetadata
 * @see com.oracle.runbook.infrastructure.cloud.oci.OciComputeMetadataAdapter
 * @see com.oracle.runbook.infrastructure.cloud.aws.AwsEc2MetadataAdapter
 */
public interface ComputeMetadataAdapter {

  /**
   * Returns the cloud provider type for this adapter.
   *
   * <p>Examples: "oci", "aws"
   *
   * @return the provider type identifier, never null
   */
  String providerType();

  /**
   * Gets metadata for a compute instance by its provider-specific identifier.
   *
   * <p>The implementation should fetch instance details from the cloud provider and map them to the
   * domain {@link ResourceMetadata} model.
   *
   * @param instanceId the provider-specific instance identifier (OCID for OCI, instance ID for AWS)
   * @return a CompletableFuture containing the instance metadata, or empty if not found
   */
  CompletableFuture<Optional<ResourceMetadata>> getInstance(String instanceId);
}
