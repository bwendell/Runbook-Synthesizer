package com.oracle.runbook.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link OciConfig} configuration record.
 */
class OciConfigTest {

	@Test
	@DisplayName("OciConfig should hold all configuration fields")
	void shouldHoldConfigurationFields() {
		OciConfig config = new OciConfig("ocid1.compartment.oc1..abc", "us-ashburn-1", "~/.oci/config", "DEFAULT");

		assertEquals("ocid1.compartment.oc1..abc", config.compartmentId());
		assertEquals("us-ashburn-1", config.region());
		assertEquals("~/.oci/config", config.configFilePath());
		assertEquals("DEFAULT", config.profile());
	}

	@Test
	@DisplayName("OciConfig should reject null compartmentId")
	void shouldRejectNullCompartmentId() {
		assertThrows(NullPointerException.class, () -> new OciConfig(null, "us-ashburn-1", "~/.oci/config", "DEFAULT"));
	}

	@Test
	@DisplayName("OciConfig should allow null optional fields")
	void shouldAllowNullOptionalFields() {
		OciConfig config = new OciConfig("ocid1.compartment.oc1..abc", null, // region optional
				null, // configFilePath optional
				null // profile optional
		);

		assertEquals("ocid1.compartment.oc1..abc", config.compartmentId());
		assertNull(config.region());
		assertNull(config.configFilePath());
		assertNull(config.profile());
	}

	@Test
	@DisplayName("OciConfig should provide factory method with defaults")
	void shouldProvideFactoryWithDefaults() {
		OciConfig config = OciConfig.withDefaults("ocid1.compartment.oc1..abc");

		assertEquals("ocid1.compartment.oc1..abc", config.compartmentId());
		assertNull(config.region());
		assertEquals("~/.oci/config", config.configFilePath());
		assertEquals("DEFAULT", config.profile());
	}
}
