package com.oracle.runbook.enrichment;

import com.oracle.bmc.core.ComputeClient;
import com.oracle.bmc.core.model.Instance;
import com.oracle.bmc.core.requests.GetInstanceRequest;
import com.oracle.bmc.core.responses.GetInstanceResponse;
import com.oracle.bmc.model.BmcException;
import com.oracle.runbook.domain.ResourceMetadata;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * OCI Compute client wrapper for fetching instance metadata.
 * <p>
 * Retrieves compute instance details and converts them to domain model objects.
 * Uses the OCI Java SDK ComputeClient for API calls.
 */
public class OciComputeClient {

    private final ComputeClient computeClient;

    /**
     * Creates a new OciComputeClient.
     *
     * @param computeClient the OCI Compute client
     */
    public OciComputeClient(ComputeClient computeClient) {
        this.computeClient = Objects.requireNonNull(computeClient, "computeClient cannot be null");
    }

    /**
     * Gets an OCI compute instance by its OCID.
     *
     * @param instanceOcid the instance OCID
     * @return a CompletableFuture containing the instance metadata, or empty if not found
     */
    public CompletableFuture<Optional<ResourceMetadata>> getInstance(String instanceOcid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                GetInstanceRequest request = GetInstanceRequest.builder()
                    .instanceId(instanceOcid)
                    .build();

                GetInstanceResponse response = computeClient.getInstance(request);
                Instance instance = response.getInstance();

                return Optional.of(convertToResourceMetadata(instance));
            } catch (BmcException e) {
                if (e.getStatusCode() == 404) {
                    // Instance not found
                    return Optional.empty();
                }
                throw e;
            }
        });
    }

    /**
     * Converts OCI Instance to domain ResourceMetadata.
     */
    private ResourceMetadata convertToResourceMetadata(Instance instance) {
        return new ResourceMetadata(
            instance.getId(),
            instance.getDisplayName(),
            instance.getCompartmentId(),
            instance.getShape(),
            instance.getAvailabilityDomain(),
            instance.getFreeformTags() != null ? instance.getFreeformTags() : Map.of(),
            flattenDefinedTags(instance.getDefinedTags())
        );
    }

    /**
     * Flattens OCI defined tags from nested map to flat map.
     * <p>
     * OCI defined tags are structured as Map<String, Map<String, Object>>.
     * This method flattens them to Map<String, String> with "namespace.key" format.
     */
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
