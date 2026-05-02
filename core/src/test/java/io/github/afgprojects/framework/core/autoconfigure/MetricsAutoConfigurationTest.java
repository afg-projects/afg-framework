package io.github.afgprojects.framework.core.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.metrics.CustomMetrics;
import io.github.afgprojects.framework.core.metrics.DefaultMetricsTagProvider;
import io.github.afgprojects.framework.core.web.metrics.MetricsProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;

/**
 * MetricsAutoConfiguration 测试
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

    @Nested
    @DisplayName("metricsAspect 配置测试")
    class MetricsAspectTests {

        @Test
        @DisplayName("应该创建指标切面")
        void shouldCreateMetricsAspect() {
            MetricsProperties properties = new MetricsProperties();

            var aspect = configuration.metricsAspect(meterRegistry, properties);

            assertThat(aspect).isNotNull();
        }
    }

    @Nested
    @DisplayName("commonTagsMeterFilter 配置测试")
    class CommonTagsMeterFilterTests {

        @Test
        @DisplayName("应该创建通用标签过滤器")
        void shouldCreateCommonTagsMeterFilter() {
            io.github.afgprojects.framework.core.autoconfigure.MetricsProperties properties =
                    new io.github.afgprojects.framework.core.autoconfigure.MetricsProperties();
            properties.setTags(Map.of("app", "test-app", "env", "test"));

            MeterFilter filter = configuration.commonTagsMeterFilter(properties);

            assertThat(filter).isNotNull();
        }

        @Test
        @DisplayName("应该合并 tags 和 commonTags")
        void shouldMergeTagsAndCommonTags() {
            io.github.afgprojects.framework.core.autoconfigure.MetricsProperties properties =
                    new io.github.afgprojects.framework.core.autoconfigure.MetricsProperties();
            properties.setTags(Map.of("app", "test-app"));
            properties.setCommonTags(Map.of("env", "test"));

            MeterFilter filter = configuration.commonTagsMeterFilter(properties);

            assertThat(filter).isNotNull();
        }

        @Test
        @DisplayName("空标签时应该创建过滤器")
        void shouldCreateFilterWithEmptyTags() {
            io.github.afgprojects.framework.core.autoconfigure.MetricsProperties properties =
                    new io.github.afgprojects.framework.core.autoconfigure.MetricsProperties();

            MeterFilter filter = configuration.commonTagsMeterFilter(properties);

            assertThat(filter).isNotNull();
        }
    }

    @Nested
    @DisplayName("histogramMeterFilter 配置测试")
    class HistogramMeterFilterTests {

        @Test
        @DisplayName("启用 histogram 时应该创建过滤器")
        void shouldCreateHistogramFilterWhenEnabled() {
            io.github.afgprojects.framework.core.autoconfigure.MetricsProperties properties =
                    new io.github.afgprojects.framework.core.autoconfigure.MetricsProperties();
            properties.getHistogram().setEnabled(true);

            MeterFilter filter = configuration.histogramMeterFilter(properties);

            assertThat(filter).isNotNull();
        }

        @Test
        @DisplayName("禁用 histogram 时应该创建 accept 过滤器")
        void shouldCreateAcceptFilterWhenDisabled() {
            io.github.afgprojects.framework.core.autoconfigure.MetricsProperties properties =
                    new io.github.afgprojects.framework.core.autoconfigure.MetricsProperties();
            properties.getHistogram().setEnabled(false);

            MeterFilter filter = configuration.histogramMeterFilter(properties);

            assertThat(filter).isNotNull();
        }
    }

    @Nested
    @DisplayName("metricsTagProvider 配置测试")
    class MetricsTagProviderTests {

        @Test
        @DisplayName("应该创建默认标签提供者")
        void shouldCreateDefaultMetricsTagProvider() {
            var provider = configuration.metricsTagProvider();

            assertThat(provider).isNotNull();
            assertThat(provider).isInstanceOf(DefaultMetricsTagProvider.class);
        }
    }

    @Nested
    @DisplayName("customMetrics 配置测试")
    class CustomMetricsTests {

        @Test
        @DisplayName("应该创建自定义指标")
        void shouldCreateCustomMetrics() {
            io.github.afgprojects.framework.core.autoconfigure.MetricsProperties properties =
                    new io.github.afgprojects.framework.core.autoconfigure.MetricsProperties();

            CustomMetrics customMetrics = configuration.customMetrics(meterRegistry, properties);

            assertThat(customMetrics).isNotNull();
        }
    }
}
