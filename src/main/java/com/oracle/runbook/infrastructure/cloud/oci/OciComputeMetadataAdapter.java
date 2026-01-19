package com.oracle.runbook.infrastructure.cloud.oci;

import com.oracle.bmc.core.ComputeClient;
import com.oracle.bmc.core.model.Instance;
import com.oracle.bmc.core.requests.GetInstanceRequest;
import com.oracle.bmc.core.responses.GetInstanceResponse;
import com.oracle.bmc.model.BmcException;
import com.oracle.runbook.domain.ResourceMetadata;
import com.oracle.runbook.infrastructure.cloud.ComputeMetadataAdapter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * OCI Compute implementation of {@link ComputeMetadataAdapter}.
 *
 * <p>Retrieves compute instance details and converts them to domain model objects. Uses the OCI
 * Java SDK ComputeClient for API calls.
 */
public class OciComputeMetadataAdapter implements ComputeMetadataAdapter {

  private final ComputeClient computeClient;

  /**
   * Creates a new OciComputeMetadataAdapter.
   *
   * @param computeClient the OCI Compute client
   * @throws NullPointerException if computeClient is null
   */
  public OciComputeMetadataAdapter(ComputeClient computeClient) {
    this.computeClient = Objects.requireNonNull(computeClient, "computeClient cannot be null");
  }

  @Override
  public String providerType() {
    return "oci";
  }

  @Override
  public CompletableFuture<Optional<ResourceMetadata>> getInstance(String instanceId) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            GetInstanceRequest request =
                GetInstanceRequest.builder().instanceId(instanceId).build();

            GetInstanceResponse response = computeClient.getInstance(request);
            Instance instance = response.getInstance();

            return Optional.of(convertToResourceMetadata(instance));
          } catch (BmcException e) {
            if (e.getStatusCode() == 404) {
              return Optional.empty();
            }
            throw e;
          }
        });
  }

  /** Converts OCI Instance to domain ResourceMetadata. */
  private ResourceMetadata convertToResourceMetadata(Instance instance) {
    return new ResourceMetadata(
        instance.getId(),
        instance.getDisplayName(),
        instance.getCompartmentId(),
        instance.getShape(),
        instance.getAvailabilityDomain(),
        instance.getFreeformTags() != null ? instance.getFreeformTags() : Map.of(),
        flattenDefinedTags(instance.getDefinedTags()));
  }

  /** Flattens OCI defined tags from nested map to flat map. */
  private Map<String, String> flattenDefinedTags(Map<String, Map<String, Object>> definedTags) {
    if (definedTags == null || definedTags.isEmpty()) {
      return Map.of();
    }

    Map<String, String> flattened = new HashMap<>();
    for (Map.Entry<String, Map<String, Object>> namespaceEntry : definedTags.entrySet()) {
      String namespace = namespaceEntry.getKey();
      Map<String, Object> tags = namespaceEntry.getValue();
      if (tags != null) {
        for (Map.Entry<String, Object> tagEntry : tags.entrySet()) {
          String key = namespace + "." + tagEntry.getKey();
          String value = tagEntry.getValue() != null ? tagEntry.getValue().toString() : "";
          flattened.put(key, value);
        }
      }
    }
    return flattened;
  }
}
