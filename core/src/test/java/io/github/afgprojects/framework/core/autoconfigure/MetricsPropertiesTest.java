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
 * MetricsProperties 单元测试。
 * 测试指标配置属性类的默认值和属性设置。
 *
 * @see MetricsProperties
 */
@DisplayName("MetricsProperties 测试")
class MetricsPropertiesTest {

    /**
     * 默认值测试。
     * 验证配置属性的默认初始化值。
     */
    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTests {

        /**
         * 测试主配置类的默认值。
         */
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

    /**
     * HistogramConfigProperties 内嵌类测试。
     * 验证直方图配置的默认值。
     */
    @Nested
    @DisplayName("HistogramConfigProperties 测试")
    class HistogramConfigTests {

        /**
         * 测试 HistogramConfigProperties 的默认值。
         */
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

    /**
     * CustomMetricsConfig 内嵌类测试。
     * 验证自定义指标配置的默认值。
     */
    @Nested
    @DisplayName("CustomMetricsConfig 测试")
    class CustomMetricsConfigTests {

        /**
         * 测试 CustomMetricsConfig 的默认值。
         */
        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            MetricsProperties.CustomMetricsConfig custom = new MetricsProperties.CustomMetricsConfig();

            assertThat(custom.isEnabled()).isTrue();
            assertThat(custom.getCounters()).isEmpty();
            assertThat(custom.getGauges()).isEmpty();
        }
    }

    /**
     * CounterConfig 内嵌类测试。
     * 验证计数器配置的属性设置。
     */
    @Nested
    @DisplayName("CounterConfig 测试")
    class CounterConfigTests {

        /**
         * 测试 CounterConfig 的属性设置。
         */
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

    /**
     * GaugeConfig 内嵌类测试。
     * 验证仪表盘配置的属性设置。
     */
    @Nested
    @DisplayName("GaugeConfig 测试")
    class GaugeConfigTests {

        /**
         * 测试 GaugeConfig 的属性设置。
         */
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

    /**
     * 设置属性测试。
     * 验证主配置类的属性设置。
     */
    @Nested
    @DisplayName("设置属性测试")
    class SetPropertiesTests {

        /**
         * 测试设置所有属性。
         */
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
