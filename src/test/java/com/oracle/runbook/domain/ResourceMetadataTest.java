package com.oracle.runbook.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link ResourceMetadata} record. */
class ResourceMetadataTest {

  @Test
  @DisplayName("ResourceMetadata construction with all fields succeeds")
  void constructionWithAllFieldsSucceeds() {
    Map<String, String> freeformTags = Map.of("env", "production");
    Map<String, String> definedTags = Map.of("Oracle-Tags.CreatedBy", "user@example.com");

    ResourceMetadata metadata =
        new ResourceMetadata(
            "ocid1.instance.oc1.iad.xxx",
            "web-server-01",
            "ocid1.compartment.oc1..xxx",
            "VM.Standard2.4",
            "AD-1",
            freeformTags,
            definedTags);

    assertThat(metadata.ocid()).isEqualTo("ocid1.instance.oc1.iad.xxx");
    assertThat(metadata.displayName()).isEqualTo("web-server-01");
    assertThat(metadata.compartmentId()).isEqualTo("ocid1.compartment.oc1..xxx");
    assertThat(metadata.shape()).isEqualTo("VM.Standard2.4");
    assertThat(metadata.availabilityDomain()).isEqualTo("AD-1");
    assertThat(metadata.freeformTags()).isEqualTo(freeformTags);
    assertThat(metadata.definedTags()).isEqualTo(definedTags);
  }

  @Test
  @DisplayName("ResourceMetadata throws NullPointerException for null ocid")
  void throwsForNullOcid() {
    assertThatThrownBy(
            () ->
                new ResourceMetadata(
                    null, "name", "compartment", "shape", "ad", Map.of(), Map.of()))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("ResourceMetadata freeformTags map is immutable")
  void freeformTagsMapIsImmutable() {
    Map<String, String> mutableTags = new HashMap<>();
    mutableTags.put("key", "value");

    ResourceMetadata metadata =
        new ResourceMetadata(
            "ocid1.instance.oc1.iad.xxx",
            "name",
            "compartment",
            "shape",
            "ad",
            mutableTags,
            Map.of());

    // Modifying original should not affect record
    mutableTags.put("newKey", "newValue");
    assertThat(metadata.freeformTags()).doesNotContainKey("newKey");

    // Record's map should be unmodifiable
    assertThatThrownBy(() -> metadata.freeformTags().put("another", "value"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("ResourceMetadata definedTags map is immutable")
  void definedTagsMapIsImmutable() {
    Map<String, String> mutableTags = new HashMap<>();
    mutableTags.put("key", "value");

    ResourceMetadata metadata =
        new ResourceMetadata(
            "ocid1.instance.oc1.iad.xxx",
            "name",
            "compartment",
            "shape",
            "ad",
            Map.of(),
            mutableTags);

    // Modifying original should not affect record
    mutableTags.put("newKey", "newValue");
    assertThat(metadata.definedTags()).doesNotContainKey("newKey");

    // Record's map should be unmodifiable
    assertThatThrownBy(() -> metadata.definedTags().put("another", "value"))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
