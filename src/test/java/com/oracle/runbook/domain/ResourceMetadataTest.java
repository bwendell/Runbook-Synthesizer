package com.oracle.runbook.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ResourceMetadata} record.
 */
class ResourceMetadataTest {

    @Test
    @DisplayName("ResourceMetadata construction with all fields succeeds")
    void constructionWithAllFieldsSucceeds() {
        Map<String, String> freeformTags = Map.of("env", "production");
        Map<String, String> definedTags = Map.of("Oracle-Tags.CreatedBy", "user@example.com");

        ResourceMetadata metadata = new ResourceMetadata(
            "ocid1.instance.oc1.iad.xxx",
            "web-server-01",
            "ocid1.compartment.oc1..xxx",
            "VM.Standard2.4",
            "AD-1",
            freeformTags,
            definedTags
        );

        assertEquals("ocid1.instance.oc1.iad.xxx", metadata.ocid());
        assertEquals("web-server-01", metadata.displayName());
        assertEquals("ocid1.compartment.oc1..xxx", metadata.compartmentId());
        assertEquals("VM.Standard2.4", metadata.shape());
        assertEquals("AD-1", metadata.availabilityDomain());
        assertEquals(freeformTags, metadata.freeformTags());
        assertEquals(definedTags, metadata.definedTags());
    }

    @Test
    @DisplayName("ResourceMetadata throws NullPointerException for null ocid")
    void throwsForNullOcid() {
        assertThrows(NullPointerException.class, () -> new ResourceMetadata(
            null,
            "name",
            "compartment",
            "shape",
            "ad",
            Map.of(),
            Map.of()
        ));
    }

    @Test
    @DisplayName("ResourceMetadata freeformTags map is immutable")
    void freeformTagsMapIsImmutable() {
        Map<String, String> mutableTags = new HashMap<>();
        mutableTags.put("key", "value");

        ResourceMetadata metadata = new ResourceMetadata(
            "ocid1.instance.oc1.iad.xxx",
            "name",
            "compartment",
            "shape",
            "ad",
            mutableTags,
            Map.of()
        );

        // Modifying original should not affect record
        mutableTags.put("newKey", "newValue");
        assertFalse(metadata.freeformTags().containsKey("newKey"));

        // Record's map should be unmodifiable
        assertThrows(UnsupportedOperationException.class, 
            () -> metadata.freeformTags().put("another", "value"));
    }

    @Test
    @DisplayName("ResourceMetadata definedTags map is immutable")
    void definedTagsMapIsImmutable() {
        Map<String, String> mutableTags = new HashMap<>();
        mutableTags.put("key", "value");

        ResourceMetadata metadata = new ResourceMetadata(
            "ocid1.instance.oc1.iad.xxx",
            "name",
            "compartment",
            "shape",
            "ad",
            Map.of(),
            mutableTags
        );

        // Modifying original should not affect record
        mutableTags.put("newKey", "newValue");
        assertFalse(metadata.definedTags().containsKey("newKey"));

        // Record's map should be unmodifiable
        assertThrows(UnsupportedOperationException.class, 
            () -> metadata.definedTags().put("another", "value"));
    }
}
