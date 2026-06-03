package io.github.afgprojects.framework.core.properties.metrics;

import java.time.Duration;

import lombok.Data;

/**
 * 直方图配置。
 */
@Data
public class AfgCoreMetricsHistogramProperties {

    private boolean enabled = true;
    private boolean percentileHistogram = true;
    private double[] percentiles = {0.5, 0.95, 0.99};
    private Duration minimumExpectedValue = Duration.ofMillis(1);
    private Duration maximumExpectedValue = Duration.ofSeconds(10);
}
