package io.github.afgprojects.framework.core.metrics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HistogramConfig 测试
 */
@DisplayName("HistogramConfig 测试")
class HistogramConfigTest {

    @Test
    @DisplayName("应该使用默认值创建 HistogramConfig")
    void shouldCreateWithDefaults() {
        HistogramConfig config = new HistogramConfig();
        assertTrue(config.isEnabled());
        assertTrue(config.isPercentileHistogram());
        assertArrayEquals(new double[]{0.5, 0.95, 0.99}, config.getPercentiles());
        assertEquals(Duration.ofMillis(1), config.getMinimumExpectedValue());
        assertEquals(Duration.ofSeconds(10), config.getMaximumExpectedValue());
        assertNull(config.getSla());
    }

    @Test
    @DisplayName("应该正确设置 enabled")
    void shouldSetEnabled() {
        HistogramConfig config = new HistogramConfig();
        config.setEnabled(false);
        assertFalse(config.isEnabled());
    }

    @Test
    @DisplayName("应该正确设置 percentileHistogram")
    void shouldSetPercentileHistogram() {
        HistogramConfig config = new HistogramConfig();
        config.setPercentileHistogram(false);
        assertFalse(config.isPercentileHistogram());
    }

    @Test
    @DisplayName("应该正确设置 percentiles")
    void shouldSetPercentiles() {
        HistogramConfig config = new HistogramConfig();
        double[] newPercentiles = {0.5, 0.75, 0.9, 0.95, 0.99};
        config.setPercentiles(newPercentiles);
        assertArrayEquals(newPercentiles, config.getPercentiles());
    }

    @Test
    @DisplayName("应该正确设置 minimumExpectedValue")
    void shouldSetMinimumExpectedValue() {
        HistogramConfig config = new HistogramConfig();
        Duration min = Duration.ofMillis(10);
        config.setMinimumExpectedValue(min);
        assertEquals(min, config.getMinimumExpectedValue());
    }

    @Test
    @DisplayName("应该正确设置 maximumExpectedValue")
    void shouldSetMaximumExpectedValue() {
        HistogramConfig config = new HistogramConfig();
        Duration max = Duration.ofSeconds(30);
        config.setMaximumExpectedValue(max);
        assertEquals(max, config.getMaximumExpectedValue());
    }

    @Test
    @DisplayName("应该正确设置 sla")
    void shouldSetSla() {
        HistogramConfig config = new HistogramConfig();
        Duration[] sla = {Duration.ofMillis(50), Duration.ofMillis(100), Duration.ofMillis(200)};
        config.setSla(sla);
        assertArrayEquals(sla, config.getSla());
    }
}
