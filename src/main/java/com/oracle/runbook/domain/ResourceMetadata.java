package com.oracle.runbook.domain;

import java.util.Map;
import java.util.Objects;

/**
 * Container for OCI compute instance metadata. Used to enrich alert context with infrastructure
 * details.
 *
 * @param ocid the OCI resource identifier
 * @param displayName the human-readable instance name
 * @param compartmentId the compartment containing this resource
 * @param shape the compute shape (e.g., VM.Standard2.1, GPU3.4, BM.Standard2.52)
 * @param availabilityDomain the AD where this instance is located
 * @param freeformTags user-defined freeform tags
 * @param definedTags namespace-scoped defined tags
 */
public record ResourceMetadata(
    String ocid,
    String displayName,
    String compartmentId,
    String shape,
    String availabilityDomain,
    Map<String, String> freeformTags,
    Map<String, String> definedTags) {
  /** Compact constructor with validation and defensive copies. */
  public ResourceMetadata {
    Objects.requireNonNull(ocid, "ResourceMetadata ocid cannot be null");

    // Defensive copies for immutability
    freeformTags = freeformTags != null ? Map.copyOf(freeformTags) : Map.of();
    definedTags = definedTags != null ? Map.copyOf(definedTags) : Map.of();
  }
}
