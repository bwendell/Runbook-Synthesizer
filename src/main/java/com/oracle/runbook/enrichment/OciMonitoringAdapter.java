package com.oracle.runbook.enrichment;

import com.oracle.bmc.monitoring.MonitoringClient;
import com.oracle.bmc.monitoring.model.AggregatedDatapoint;
import com.oracle.bmc.monitoring.model.MetricData;
import com.oracle.bmc.monitoring.model.SummarizeMetricsDataDetails;
import com.oracle.bmc.monitoring.requests.SummarizeMetricsDataRequest;
import com.oracle.bmc.monitoring.responses.SummarizeMetricsDataResponse;
import com.oracle.runbook.config.OciConfig;
import com.oracle.runbook.domain.MetricSnapshot;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * OCI Monitoring adapter implementing {@link MetricsSourceAdapter}.
 * <p>
 * Fetches metrics from OCI Monitoring API and converts them to domain model
 * objects. Uses the OCI Java SDK MonitoringClient for API calls.
 */
public class OciMonitoringAdapter implements MetricsSourceAdapter {

	private static final String SOURCE_TYPE = "oci-monitoring";
	private static final String DEFAULT_NAMESPACE = "oci_computeagent";

	private final MonitoringClient monitoringClient;
	private final OciConfig config;

	/**
	 * Creates a new OciMonitoringAdapter.
	 *
	 * @param monitoringClient
	 *            the OCI Monitoring client
	 * @param config
	 *            the OCI configuration
	 */
	public OciMonitoringAdapter(MonitoringClient monitoringClient, OciConfig config) {
		this.monitoringClient = Objects.requireNonNull(monitoringClient, "monitoringClient cannot be null");
		this.config = Objects.requireNonNull(config, "config cannot be null");
	}

	@Override
	public String sourceType() {
		return SOURCE_TYPE;
	}

	@Override
	public CompletableFuture<List<MetricSnapshot>> fetchMetrics(String resourceId, Duration lookback) {
		return CompletableFuture.supplyAsync(() -> {
			Instant endTime = Instant.now();
			Instant startTime = endTime.minus(lookback);

			// Build MQL query for the resource
			String query = String.format("resourceId = \"%s\"", resourceId);

			SummarizeMetricsDataDetails details = SummarizeMetricsDataDetails.builder().namespace(DEFAULT_NAMESPACE)
					.query(query).startTime(Date.from(startTime)).endTime(Date.from(endTime)).build();

			SummarizeMetricsDataRequest request = SummarizeMetricsDataRequest.builder()
					.compartmentId(config.compartmentId()).summarizeMetricsDataDetails(details).build();

			SummarizeMetricsDataResponse response = monitoringClient.summarizeMetricsData(request);

			return convertToSnapshots(response.getItems());
		});
	}

	/**
	 * Converts OCI MetricData list to domain MetricSnapshot list.
	 */
	private List<MetricSnapshot> convertToSnapshots(List<MetricData> metricDataList) {
		if (metricDataList == null || metricDataList.isEmpty()) {
			return List.of();
		}

		List<MetricSnapshot> snapshots = new ArrayList<>();

		for (MetricData metricData : metricDataList) {
			List<AggregatedDatapoint> datapoints = metricData.getAggregatedDatapoints();
			if (datapoints != null) {
				// Extract unit from metadata if available
				String unit = extractUnit(metricData);

				for (AggregatedDatapoint dp : datapoints) {
					snapshots.add(new MetricSnapshot(metricData.getName(), metricData.getNamespace(), dp.getValue(),
							unit, dp.getTimestamp().toInstant()));
				}
			}
		}

		return snapshots;
	}

	/**
	 * Extracts the unit from MetricData metadata. OCI stores unit in the metadata
	 * map with key "unit".
	 */
	private String extractUnit(MetricData metricData) {
		if (metricData.getMetadata() != null) {
			Object unitValue = metricData.getMetadata().get("unit");
			if (unitValue != null) {
				return unitValue.toString();
			}
		}
		// Default unit if not specified in metadata
		return "count";
	}
}
