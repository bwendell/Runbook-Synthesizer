package com.oracle.runbook.infrastructure.cloud;

/**
 * Base configuration interface for cloud provider settings.
 *
 * <p>Defines the common configuration contract that all cloud provider configurations must
 * implement. This enables polymorphic configuration handling across AWS, OCI, and future cloud
 * providers.
 *
 * @see com.oracle.runbook.infrastructure.cloud.oci.OciConfig
 * @see com.oracle.runbook.infrastructure.cloud.aws.AwsConfig
 */
public interface CloudConfig {

  /**
   * Returns the cloud provider identifier.
   *
   * <p>Examples: "oci", "aws", "azure", "gcp"
   *
   * @return the provider identifier, never null
   */
  String provider();

  /**
   * Returns the cloud region identifier.
   *
   * <p>Examples: "us-ashburn-1" (OCI), "us-east-1" (AWS)
   *
   * @return the region identifier, may be null if not configured
   */
  String region();
}
