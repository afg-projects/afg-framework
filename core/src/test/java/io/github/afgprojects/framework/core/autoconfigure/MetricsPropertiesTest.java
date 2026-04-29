package io.github.afgprojects.framework.core.autoconfigure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MetricsProperties 测试
 */
@DisplayName("MetricsProperties 测试")
class MetricsPropertiesTest {

    @Test
    @DisplayName("应该使用默认值创建 MetricsProperties")
    void shouldCreateWithDefaults() {
        MetricsProperties properties = new MetricsProperties();
        assertTrue(properties.isEnabled());
        assertTrue(properties.isAnnotationsEnabled());
        assertNull(properties.getTags());
        assertNull(properties.getCommonTags());
        assertNotNull(properties.getHistogram());
        assertNotNull(properties.getCustom());
    }

    @Test
    @DisplayName("应该正确设置 enabled")
    void shouldSetEnabled() {
        MetricsProperties properties = new MetricsProperties();
        properties.setEnabled(false);
        assertFalse(properties.isEnabled());
    }

    @Test
    @DisplayName("应该正确设置 annotationsEnabled")
    void shouldSetAnnotationsEnabled() {
        MetricsProperties properties = new MetricsProperties();
        properties.setAnnotationsEnabled(false);
        assertFalse(properties.isAnnotationsEnabled());
    }

    @Test
    @DisplayName("应该正确设置 tags")
    void shouldSetTags() {
        MetricsProperties properties = new MetricsProperties();
        Map<String, String> tags = Map.of("env", "dev", "region", "cn");
        properties.setTags(tags);
        assertEquals(tags, properties.getTags());
    }

    @Test
    @DisplayName("应该正确设置 commonTags")
    void shouldSetCommonTags() {
        MetricsProperties properties = new MetricsProperties();
        Map<String, String> commonTags = Map.of("app", "myapp");
        properties.setCommonTags(commonTags);
        assertEquals(commonTags, properties.getCommonTags());
    }

    @Test
    @DisplayName("HistogramConfigProperties 应该有默认值")
    void histogramConfigShouldHaveDefaults() {
        MetricsProperties.HistogramConfigProperties histogram = new MetricsProperties.HistogramConfigProperties();
        assertTrue(histogram.isEnabled());
        assertTrue(histogram.isPercentileHistogram());
        assertArrayEquals(new double[]{0.5, 0.95, 0.99}, histogram.getPercentiles());
        assertEquals(Duration.ofMillis(1), histogram.getMinimumExpectedValue());
        assertEquals(Duration.ofSeconds(10), histogram.getMaximumExpectedValue());
    }

    @Test
    @DisplayName("HistogramConfigProperties 应该正确设置属性")
    void histogramConfigShouldSetProperties() {
        MetricsProperties.HistogramConfigProperties histogram = new MetricsProperties.HistogramConfigProperties();
        histogram.setEnabled(false);
        histogram.setPercentileHistogram(false);
        histogram.setPercentiles(new double[]{0.5, 0.99});
        histogram.setMinimumExpectedValue(Duration.ofMillis(10));
        histogram.setMaximumExpectedValue(Duration.ofSeconds(30));

        assertFalse(histogram.isEnabled());
        assertFalse(histogram.isPercentileHistogram());
        assertArrayEquals(new double[]{0.5, 0.99}, histogram.getPercentiles());
        assertEquals(Duration.ofMillis(10), histogram.getMinimumExpectedValue());
        assertEquals(Duration.ofSeconds(30), histogram.getMaximumExpectedValue());
    }

    @Test
    @DisplayName("CustomMetricsConfig 应该有默认值")
    void customMetricsConfigShouldHaveDefaults() {
        MetricsProperties.CustomMetricsConfig custom = new MetricsProperties.CustomMetricsConfig();
        assertTrue(custom.isEnabled());
        assertTrue(custom.getCounters().isEmpty());
        assertTrue(custom.getGauges().isEmpty());
    }

    @Test
    @DisplayName("CustomMetricsConfig 应该正确设置属性")
    void customMetricsConfigShouldSetProperties() {
        MetricsProperties.CustomMetricsConfig custom = new MetricsProperties.CustomMetricsConfig();
        custom.setEnabled(false);

        MetricsProperties.CounterConfig counter = new MetricsProperties.CounterConfig();
        counter.setName("requests");
        counter.setDescription("Total requests");
        counter.setTags(Map.of("type", "api"));
        custom.setCounters(List.of(counter));

        MetricsProperties.GaugeConfig gauge = new MetricsProperties.GaugeConfig();
        gauge.setName("queue.size");
        gauge.setDescription("Queue size");
        gauge.setTags(Map.of("queue", "main"));
        custom.setGauges(List.of(gauge));

        assertFalse(custom.isEnabled());
        assertEquals(1, custom.getCounters().size());
        assertEquals("requests", custom.getCounters().get(0).getName());
        assertEquals("Total requests", custom.getCounters().get(0).getDescription());
        assertEquals(1, custom.getGauges().size());
        assertEquals("queue.size", custom.getGauges().get(0).getName());
    }

    @Test
    @DisplayName("CounterConfig 应该正确设置属性")
    void counterConfigShouldSetProperties() {
        MetricsProperties.CounterConfig counter = new MetricsProperties.CounterConfig();
        counter.setName("orders.created");
        counter.setDescription("Total orders created");
        counter.setTags(Map.of("source", "web"));

        assertEquals("orders.created", counter.getName());
        assertEquals("Total orders created", counter.getDescription());
        assertEquals(Map.of("source", "web"), counter.getTags());
    }

    @Test
    @DisplayName("GaugeConfig 应该正确设置属性")
    void gaugeConfigShouldSetProperties() {
        MetricsProperties.GaugeConfig gauge = new MetricsProperties.GaugeConfig();
        gauge.setName("memory.used");
        gauge.setDescription("Memory used in bytes");
        gauge.setTags(Map.of("type", "heap"));

        assertEquals("memory.used", gauge.getName());
        assertEquals("Memory used in bytes", gauge.getDescription());
        assertEquals(Map.of("type", "heap"), gauge.getTags());
    }
}
