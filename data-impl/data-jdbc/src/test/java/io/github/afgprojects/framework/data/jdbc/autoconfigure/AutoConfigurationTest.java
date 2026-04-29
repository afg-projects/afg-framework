package io.github.afgprojects.framework.data.jdbc.autoconfigure;

import io.github.afgprojects.framework.core.cache.CacheProperties;
import io.github.afgprojects.framework.core.cache.DefaultCacheManager;
import io.github.afgprojects.framework.data.jdbc.cache.EntityCacheManager;
import io.github.afgprojects.framework.data.jdbc.cache.EntityCacheProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 自动配置测试
 */
@DisplayName("自动配置测试")
class AutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(EntityCacheAutoConfiguration.class));

    private static DefaultCacheManager createDefaultCacheManager() {
        CacheProperties props = new CacheProperties();
        props.setEnabled(true);
        return new DefaultCacheManager(props);
    }

    @Nested
    @DisplayName("EntityCacheAutoConfiguration 测试")
    class EntityCacheAutoConfigurationTests {

        @Test
        @DisplayName("满足条件时应创建 EntityCacheManager")
        void shouldCreateEntityCacheManagerWhenConditionsMet() {
            contextRunner
                .withBean(DefaultCacheManager.class, AutoConfigurationTest::createDefaultCacheManager)
                .withPropertyValues("afg.jdbc.cache.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(EntityCacheManager.class);
                    assertThat(context).hasSingleBean(EntityCacheProperties.class);
                });
        }

        @Test
        @DisplayName("未启用缓存属性时不应创建 EntityCacheManager")
        void shouldNotCreateEntityCacheManagerWhenPropertyNotEnabled() {
            contextRunner
                .withBean(DefaultCacheManager.class, AutoConfigurationTest::createDefaultCacheManager)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(EntityCacheManager.class);
                });
        }

        @Test
        @DisplayName("缺少 DefaultCacheManager 时不应创建 EntityCacheManager")
        void shouldNotCreateEntityCacheManagerWhenNoCacheManager() {
            contextRunner
                .withPropertyValues("afg.jdbc.cache.enabled=true")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(EntityCacheManager.class);
                });
        }
    }

    @Nested
    @DisplayName("SqlMetricsAutoConfiguration 测试")
    class SqlMetricsAutoConfigurationTests {

        private final ApplicationContextRunner metricsContextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SqlMetricsAutoConfiguration.class));

        @Test
        @DisplayName("满足条件时应创建 SqlMetrics 相关 Bean")
        void shouldCreateSqlMetricsBeansWhenConditionsMet() {
            metricsContextRunner
                .withBean(MeterRegistry.class, () -> new SimpleMeterRegistry())
                .withPropertyValues("afg.jdbc.metrics.enabled=true")
                .run(context -> {
                    assertThat(context).hasBean("sqlMetrics");
                    assertThat(context).hasBean("sqlMetricsAspect");
                });
        }

        @Test
        @DisplayName("缺少 MeterRegistry 时不应创建 SqlMetrics Bean")
        void shouldNotCreateSqlMetricsWhenNoMeterRegistry() {
            metricsContextRunner
                .withPropertyValues("afg.jdbc.metrics.enabled=true")
                .run(context -> {
                    assertThat(context).doesNotHaveBean("sqlMetrics");
                    assertThat(context).doesNotHaveBean("sqlMetricsAspect");
                });
        }

        @Test
        @DisplayName("禁用指标时不应创建 SqlMetrics Bean")
        void shouldNotCreateSqlMetricsWhenDisabled() {
            metricsContextRunner
                .withBean(MeterRegistry.class, () -> new SimpleMeterRegistry())
                .withPropertyValues("afg.jdbc.metrics.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean("sqlMetrics");
                    assertThat(context).doesNotHaveBean("sqlMetricsAspect");
                });
        }
    }
}
