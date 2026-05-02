package io.github.afgprojects.framework.core.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * MetricsProperties 测试
 */
@DisplayName("MetricsProperties 测试")
class MetricsPropertiesTest {

    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTests {

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            MetricsProperties props = new MetricsProperties();

            assertThat(props.isEnabled()).isTrue();
            assertThat(props.isAnnotationsEnabled()).isTrue();
            assertThat(props.getHistogram()).isNotNull();
            assertThat(props.getCustom()).isNotNull();
        }
    }

    @Nested
    @DisplayName("HistogramConfigProperties 测试")
    class HistogramConfigTests {

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            MetricsProperties.HistogramConfigProperties histogram = new MetricsProperties.HistogramConfigProperties();

            assertThat(histogram.isEnabled()).isTrue();
            assertThat(histogram.isPercentileHistogram()).isTrue();
            assertThat(histogram.getPercentiles()).containsExactly(0.5, 0.95, 0.99);
            assertThat(histogram.getMinimumExpectedValue()).isEqualTo(Duration.ofMillis(1));
            assertThat(histogram.getMaximumExpectedValue()).isEqualTo(Duration.ofSeconds(10));
        }
    }

    @Nested
    @DisplayName("CustomMetricsConfig 测试")
    class CustomMetricsConfigTests {

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            MetricsProperties.CustomMetricsConfig custom = new MetricsProperties.CustomMetricsConfig();

            assertThat(custom.isEnabled()).isTrue();
            assertThat(custom.getCounters()).isEmpty();
            assertThat(custom.getGauges()).isEmpty();
        }
    }

    @Nested
    @DisplayName("CounterConfig 测试")
    class CounterConfigTests {

        @Test
        @DisplayName("应该正确设置属性")
        void shouldSetProperties() {
            MetricsProperties.CounterConfig config = new MetricsProperties.CounterConfig();
            config.setName("test.counter");
            config.setDescription("Test counter");
            Map<String, String> tags = new HashMap<>();
            tags.put("env", "test");
            config.setTags(tags);

            assertThat(config.getName()).isEqualTo("test.counter");
            assertThat(config.getDescription()).isEqualTo("Test counter");
            assertThat(config.getTags()).containsEntry("env", "test");
        }
    }

    @Nested
    @DisplayName("GaugeConfig 测试")
    class GaugeConfigTests {

        @Test
        @DisplayName("应该正确设置属性")
        void shouldSetProperties() {
            MetricsProperties.GaugeConfig config = new MetricsProperties.GaugeConfig();
            config.setName("test.gauge");
            config.setDescription("Test gauge");
            Map<String, String> tags = new HashMap<>();
            tags.put("type", "memory");
            config.setTags(tags);

            assertThat(config.getName()).isEqualTo("test.gauge");
            assertThat(config.getDescription()).isEqualTo("Test gauge");
            assertThat(config.getTags()).containsEntry("type", "memory");
        }
    }

    @Nested
    @DisplayName("设置属性测试")
    class SetPropertiesTests {

        @Test
        @DisplayName("应该正确设置所有属性")
        void shouldSetAllProperties() {
            MetricsProperties props = new MetricsProperties();
            props.setEnabled(false);
            props.setAnnotationsEnabled(false);

            Map<String, String> tags = new HashMap<>();
            tags.put("app", "test");
            props.setTags(tags);

            assertThat(props.isEnabled()).isFalse();
            assertThat(props.isAnnotationsEnabled()).isFalse();
            assertThat(props.getTags()).containsEntry("app", "test");
        }
    }
}
