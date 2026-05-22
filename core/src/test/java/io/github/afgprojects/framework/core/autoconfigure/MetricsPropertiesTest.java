package io.github.afgprojects.framework.core.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;

/**
 * MetricsConfig 单元测试。
 * 测试指标配置属性类的默认值和属性设置。
 *
 * @see AfgCoreProperties.MetricsConfig
 */
@DisplayName("MetricsConfig 测试")
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
            AfgCoreProperties.MetricsConfig props = new AfgCoreProperties.MetricsConfig();

            assertThat(props.isEnabled()).isTrue();
            assertThat(props.isAnnotationsEnabled()).isTrue();
            assertThat(props.getHistogram()).isNotNull();
        }
    }

    /**
     * HistogramConfig 内嵌类测试。
     * 验证直方图配置的默认值。
     */
    @Nested
    @DisplayName("HistogramConfig 测试")
    class HistogramConfigTests {

        /**
         * 测试 HistogramConfig 的默认值。
         */
        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            AfgCoreProperties.MetricsConfig.HistogramConfig histogram = new AfgCoreProperties.MetricsConfig.HistogramConfig();

            assertThat(histogram.isEnabled()).isTrue();
            assertThat(histogram.isPercentileHistogram()).isTrue();
            assertThat(histogram.getPercentiles()).containsExactly(0.5, 0.95, 0.99);
            assertThat(histogram.getMinimumExpectedValue()).isEqualTo(Duration.ofMillis(1));
            assertThat(histogram.getMaximumExpectedValue()).isEqualTo(Duration.ofSeconds(10));
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
            AfgCoreProperties.MetricsConfig props = new AfgCoreProperties.MetricsConfig();
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