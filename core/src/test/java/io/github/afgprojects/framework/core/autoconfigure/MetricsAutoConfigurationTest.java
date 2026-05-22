package io.github.afgprojects.framework.core.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;
import io.github.afgprojects.framework.core.metrics.CustomMetrics;
import io.github.afgprojects.framework.core.metrics.DefaultMetricsTagProvider;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;

/**
 * MetricsAutoConfiguration 单元测试。
 * 测试指标自动配置类的 Bean 创建功能。
 *
 * @see MetricsAutoConfiguration
 */
@DisplayName("MetricsAutoConfiguration 测试")
class MetricsAutoConfigurationTest {

    private MetricsAutoConfiguration configuration;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        configuration = new MetricsAutoConfiguration();
        meterRegistry = mock(MeterRegistry.class);
    }

    /**
     * 指标切面配置测试。
     * 验证 metricsAspect Bean 的创建。
     */
    @Nested
    @DisplayName("metricsAspect 配置测试")
    class MetricsAspectTests {

        /**
         * 测试创建指标切面。
         */
        @Test
        @DisplayName("应该创建指标切面")
        void shouldCreateMetricsAspect() {
            AfgCoreProperties properties = new AfgCoreProperties();

            var aspect = configuration.metricsAspect(meterRegistry, properties);

            assertThat(aspect).isNotNull();
        }
    }

    /**
     * 通用标签过滤器配置测试。
     * 验证 commonTagsMeterFilter Bean 的创建和标签合并功能。
     */
    @Nested
    @DisplayName("commonTagsMeterFilter 配置测试")
    class CommonTagsMeterFilterTests {

        /**
         * 测试创建通用标签过滤器。
         */
        @Test
        @DisplayName("应该创建通用标签过滤器")
        void shouldCreateCommonTagsMeterFilter() {
            AfgCoreProperties properties = new AfgCoreProperties();
            properties.getMetrics().setTags(Map.of("app", "test-app", "env", "test"));

            MeterFilter filter = configuration.commonTagsMeterFilter(properties);

            assertThat(filter).isNotNull();
        }

        /**
         * 测试空标签时创建过滤器。
         */
        @Test
        @DisplayName("空标签时应该创建过滤器")
        void shouldCreateFilterWithEmptyTags() {
            AfgCoreProperties properties = new AfgCoreProperties();

            MeterFilter filter = configuration.commonTagsMeterFilter(properties);

            assertThat(filter).isNotNull();
        }
    }

    /**
     * 直方图过滤器配置测试。
     * 验证 histogramMeterFilter Bean 的创建。
     */
    @Nested
    @DisplayName("histogramMeterFilter 配置测试")
    class HistogramMeterFilterTests {

        /**
         * 测试启用 histogram 时创建过滤器。
         */
        @Test
        @DisplayName("启用 histogram 时应该创建过滤器")
        void shouldCreateHistogramFilterWhenEnabled() {
            AfgCoreProperties properties = new AfgCoreProperties();
            properties.getMetrics().getHistogram().setEnabled(true);

            MeterFilter filter = configuration.histogramMeterFilter(properties);

            assertThat(filter).isNotNull();
        }

        /**
         * 测试禁用 histogram 时创建 accept 过滤器。
         */
        @Test
        @DisplayName("禁用 histogram 时应该创建 accept 过滤器")
        void shouldCreateAcceptFilterWhenDisabled() {
            AfgCoreProperties properties = new AfgCoreProperties();
            properties.getMetrics().getHistogram().setEnabled(false);

            MeterFilter filter = configuration.histogramMeterFilter(properties);

            assertThat(filter).isNotNull();
        }
    }

    /**
     * 指标标签提供者配置测试。
     * 验证 metricsTagProvider Bean 的创建。
     */
    @Nested
    @DisplayName("metricsTagProvider 配置测试")
    class MetricsTagProviderTests {

        /**
         * 测试创建默认标签提供者。
         */
        @Test
        @DisplayName("应该创建默认标签提供者")
        void shouldCreateDefaultMetricsTagProvider() {
            var provider = configuration.metricsTagProvider();

            assertThat(provider).isNotNull();
            assertThat(provider).isInstanceOf(DefaultMetricsTagProvider.class);
        }
    }

    /**
     * 自定义指标配置测试。
     * 验证 customMetrics Bean 的创建。
     */
    @Nested
    @DisplayName("customMetrics 配置测试")
    class CustomMetricsTests {

        /**
         * 测试创建自定义指标。
         */
        @Test
        @DisplayName("应该创建自定义指标")
        void shouldCreateCustomMetrics() {
            AfgCoreProperties properties = new AfgCoreProperties();

            CustomMetrics customMetrics = configuration.customMetrics(meterRegistry, properties);

            assertThat(customMetrics).isNotNull();
        }
    }
}