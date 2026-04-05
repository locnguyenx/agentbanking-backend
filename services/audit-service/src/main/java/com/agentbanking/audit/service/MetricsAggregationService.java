package com.agentbanking.audit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public class MetricsAggregationService {

    private static final Logger log = LoggerFactory.getLogger(MetricsAggregationService.class);
    private static final int TIMEOUT_SECONDS = 5;
    private final Map<String, String> serviceUrls;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MetricsAggregationService(Map<String, String> serviceUrls) {
        this.serviceUrls = serviceUrls;
    }

    public Map<String, Object> getMetrics(String serviceName) {
        String baseUrl = serviceUrls.get(serviceName);
        if (baseUrl == null) {
            return Map.of("error", "Unknown service: " + serviceName);
        }
        try {
            double jvmMemoryUsed = getMetricValue(baseUrl, "jvm.memory.used", "VALUE") / (1024.0 * 1024.0);
            double jvmMemoryMax = getMetricValue(baseUrl, "jvm.memory.max", "VALUE") / (1024.0 * 1024.0);
            double jvmThreads = getMetricValue(baseUrl, "jvm.threads.live", "VALUE");
            double cpuUsage = getMetricValue(baseUrl, "system.cpu.usage", "VALUE") * 100;
            double uptime = getMetricValue(baseUrl, "process.uptime", "VALUE") / 1000.0;
            double httpRequests = getMetricValue(baseUrl, "http.server.requests", "COUNT");
            double httpErrors = 0;
            for (String code : new String[]{"500", "502", "503", "504"}) {
                httpErrors += getMetricValue(baseUrl, "http.server.requests", "COUNT", "status", code);
            }
            double httpAvgTime = getMetricValue(baseUrl, "http.server.requests", "TOTAL_TIME") /
                Math.max(httpRequests, 1) * 1000;

            return Map.of(
                "serviceName", serviceName,
                "jvm", Map.of(
                    "memoryUsedMb", Math.round(jvmMemoryUsed * 10) / 10.0,
                    "memoryMaxMb", Math.round(jvmMemoryMax * 10) / 10.0,
                    "threadsActive", (int) jvmThreads,
                    "cpuUsagePercent", Math.round(cpuUsage * 10) / 10.0,
                    "uptimeSeconds", (long) uptime
                ),
                "http", Map.of(
                    "requestsTotal", (long) httpRequests,
                    "errorsTotal", (long) httpErrors,
                    "avgResponseTimeMs", Math.round(httpAvgTime * 10) / 10.0
                ),
                "timestamp", Instant.now().toString()
            );
        } catch (Exception e) {
            log.warn("Failed to get metrics for {}: {}", serviceName, e.getMessage());
            return Map.of("error", "Metrics unavailable: " + e.getMessage());
        }
    }

    private double getMetricValue(String baseUrl, String metricName, String stat) throws Exception {
        return getMetricValue(baseUrl, metricName, stat, null, null);
    }

    private double getMetricValue(String baseUrl, String metricName, String stat, String tagKey, String tagValue)
            throws Exception {
        String url = baseUrl + "/actuator/metrics/" + metricName;
        if (tagKey != null) url += "?tag=" + tagKey + ":" + tagValue;
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url)).timeout(Duration.ofSeconds(TIMEOUT_SECONDS)).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) return 0;
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode measurements = root.get("measurements");
        if (measurements == null || !measurements.isArray()) return 0;
        for (JsonNode m : measurements) {
            if (stat.equals(m.get("statistic").asText())) return m.get("value").asDouble();
        }
        return 0;
    }
}
