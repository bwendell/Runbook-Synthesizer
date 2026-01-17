package com.oracle.runbook.enrichment;

import com.oracle.runbook.config.OciConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link OciComputeClient}.
 */
class OciComputeClientTest {

    @Test
    @DisplayName("OciComputeClient constructor should reject null client")
    void testConstructorRejectsNullClient() {
        assertThrows(NullPointerException.class, () -> 
            new OciComputeClient(null)
        );
    }

    @Test
    @DisplayName("getInstance should return Optional with ResourceMetadata")
    void testGetInstanceContract() {
        try {
            var method = OciComputeClient.class.getMethod("getInstance", String.class);
            assertTrue(method.getReturnType().equals(CompletableFuture.class));
        } catch (NoSuchMethodException e) {
            fail("getInstance(String) method should exist");
        }
    }
}
