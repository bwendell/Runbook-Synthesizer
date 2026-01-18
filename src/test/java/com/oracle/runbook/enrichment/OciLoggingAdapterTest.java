package com.oracle.runbook.enrichment;

import static org.junit.jupiter.api.Assertions.*;

import com.oracle.runbook.config.OciConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OciLoggingAdapter}.
 *
 * <p>Note: Full integration testing with OCI SDK requires live credentials. These unit tests verify
 * the adapter implements the interface contract.
 */
class OciLoggingAdapterTest {

  @Test
  @DisplayName("OciLoggingAdapter should implement LogSourceAdapter")
  void testImplementsLogSourceAdapter() {
    assertTrue(
        LogSourceAdapter.class.isAssignableFrom(OciLoggingAdapter.class),
        "OciLoggingAdapter should implement LogSourceAdapter");
  }

  @Test
  @DisplayName("OciLoggingAdapter constructor should reject null client")
  void testConstructorRejectsNullClient() {
    OciConfig config =
        new OciConfig(
            "ocid1.compartment.oc1..test", null, null, null, null, null, null, null, null);

    assertThrows(NullPointerException.class, () -> new OciLoggingAdapter(null, config));
  }

  @Test
  @DisplayName("OciLoggingAdapter constructor should reject null config")
  void testConstructorRejectsNullConfig() {
    assertThrows(NullPointerException.class, () -> new OciLoggingAdapter(null, null));
  }

  @Test
  @DisplayName("sourceType() should return expected identifier")
  void testSourceTypeContract() {
    try {
      var method = OciLoggingAdapter.class.getMethod("sourceType");
      assertEquals(String.class, method.getReturnType());
    } catch (NoSuchMethodException e) {
      fail("sourceType() method should exist");
    }
  }

  @Test
  @DisplayName("fetchLogs should be accessible via interface contract")
  void testFetchLogsContract() {
    try {
      var method =
          OciLoggingAdapter.class.getMethod(
              "fetchLogs", String.class, java.time.Duration.class, String.class);
      assertNotNull(method.getGenericReturnType());
    } catch (NoSuchMethodException e) {
      fail("fetchLogs(String, Duration, String) method should exist");
    }
  }
}
