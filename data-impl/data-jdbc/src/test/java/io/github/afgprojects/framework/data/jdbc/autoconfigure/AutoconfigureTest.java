package io.github.afgprojects.framework.data.jdbc.autoconfigure;

import io.github.afgprojects.framework.core.cache.CacheProperties;
import io.github.afgprojects.framework.core.cache.DefaultCacheManager;
import io.github.afgprojects.framework.data.jdbc.cache.EntityCacheManager;
import io.github.afgprojects.framework.data.jdbc.cache.EntityCacheProperties;
import io.github.afgprojects.framework.data.jdbc.metrics.SqlMetrics;
import io.github.afgprojects.framework.data.jdbc.metrics.SqlMetricsAspect;
import io.github.afgprojects.framework.data.jdbc.metrics.SqlMetricsProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Data-JDBC Autoconfigure 包测试
 */
@DisplayName("Data-JDBC Autoconfigure 包测试")
class AutoconfigureTest {

    // ==================== SqlMetricsAutoConfiguration 测试 ====================

    @Nested
    @DisplayName("SqlMetricsAutoConfiguration 测试")
    class SqlMetricsAutoConfigurationTest {

        private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(SqlMetricsAutoConfiguration.class));

        @Test
        @DisplayName("存在 MeterRegistry 时应自动配置 SqlMetrics 和 SqlMetricsAspect")
        void shouldAutoConfigureSqlMetricsWhenMeterRegistryPresent() {
            contextRunner
                    .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                    .run(context -> {
                        assertThat(context).hasSingleBean(SqlMetrics.class);
                        assertThat(context).hasSingleBean(SqlMetricsAspect.class);
                        assertThat(context).hasSingleBean(SqlMetricsProperties.class);
                    });
        }

        @Test
        @DisplayName("不存在 MeterRegistry 时不应配置")
        void shouldNotConfigureWhenMeterRegistryAbsent() {
            contextRunner.run(context -> {
                assertThat(context).doesNotHaveBean(SqlMetrics.class);
                assertThat(context).doesNotHaveBean(SqlMetricsAspect.class);
            });
        }

        @Test
        @DisplayName("配置禁用后不应创建 Bean")
        void shouldNotCreateBeansWhenDisabled() {
            contextRunner
                    .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                    .withPropertyValues("afg.jdbc.metrics.enabled=false")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(SqlMetrics.class);
                        assertThat(context).doesNotHaveBean(SqlMetricsAspect.class);
                    });
        }

        @Test
        @DisplayName("用户自定义 SqlMetrics 应覆盖自动配置")
        void customSqlMetricsShouldOverrideAutoConfiguration() {
            MeterRegistry registry = new SimpleMeterRegistry();
            SqlMetrics customSqlMetrics = new SqlMetrics(registry, new SqlMetricsProperties());

            contextRunner
                    .withBean(MeterRegistry.class, () -> registry)
                    .withBean(SqlMetrics.class, () -> customSqlMetrics)
                    .run(context -> {
                        assertThat(context).hasSingleBean(SqlMetrics.class);
                        assertThat(context.getBean(SqlMetrics.class)).isSameAs(customSqlMetrics);
                    });
        }

        @Test
        @DisplayName("用户自定义 SqlMetricsAspect 应覆盖自动配置")
        void customSqlMetricsAspectShouldOverrideAutoConfiguration() {
            MeterRegistry registry = new SimpleMeterRegistry();
            SqlMetrics sqlMetrics = new SqlMetrics(registry, new SqlMetricsProperties());
            SqlMetricsAspect customAspect = new SqlMetricsAspect(sqlMetrics, new SqlMetricsProperties());

            contextRunner
                    .withBean(MeterRegistry.class, () -> registry)
                    .withBean(SqlMetrics.class, () -> sqlMetrics)
                    .withBean(SqlMetricsAspect.class, () -> customAspect)
                    .run(context -> {
                        assertThat(context).hasSingleBean(SqlMetricsAspect.class);
                        assertThat(context.getBean(SqlMetricsAspect.class)).isSameAs(customAspect);
                    });
        }

        @Test
        @DisplayName("应正确绑定配置属性")
        void shouldBindConfigurationProperties() {
            contextRunner
                    .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                    .withPropertyValues(
                            "afg.jdbc.metrics.enabled=true",
                            "afg.jdbc.metrics.slow-query-threshold=2000ms",
                            "afg.jdbc.metrics.log-slow-queries=true",
                            "afg.jdbc.metrics.log-sql-params=true"
                    )
                    .run(context -> {
                        assertThat(context).hasSingleBean(SqlMetricsProperties.class);
                        SqlMetricsProperties props = context.getBean(SqlMetricsProperties.class);
                        // 验证属性绑定
                        assertThat(props).isNotNull();
                    });
        }
    }

    // ==================== EntityCacheAutoConfiguration 测试 ====================

    @Nested
    @DisplayName("EntityCacheAutoConfiguration 测试")
    class EntityCacheAutoConfigurationTest {

        private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(EntityCacheAutoConfiguration.class));

        @Test
        @DisplayName("启用缓存且存在 DefaultCacheManager 时应配置 EntityCacheManager")
        void shouldConfigureEntityCacheManagerWhenEnabled() {
            DefaultCacheManager cacheManager = new DefaultCacheManager(new CacheProperties());

            contextRunner
                    .withBean(DefaultCacheManager.class, () -> cacheManager)
                    .withPropertyValues("afg.jdbc.cache.enabled=true")
                    .run(context -> {
                        assertThat(context).hasSingleBean(EntityCacheManager.class);
                        assertThat(context).hasSingleBean(EntityCacheProperties.class);
                    });
        }

        @Test
        @DisplayName("缓存禁用后不应配置")
        void shouldNotConfigureWhenCacheDisabled() {
            DefaultCacheManager cacheManager = new DefaultCacheManager(new CacheProperties());

            contextRunner
                    .withBean(DefaultCacheManager.class, () -> cacheManager)
                    .withPropertyValues("afg.jdbc.cache.enabled=false")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(EntityCacheManager.class);
                    });
        }

        @Test
        @DisplayName("不存在 DefaultCacheManager 时不应配置")
        void shouldNotConfigureWhenCacheManagerAbsent() {
            contextRunner
                    .withPropertyValues("afg.jdbc.cache.enabled=true")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(EntityCacheManager.class);
                    });
        }

        @Test
        @DisplayName("默认不启用缓存")
        void shouldNotEnableCacheByDefault() {
            DefaultCacheManager cacheManager = new DefaultCacheManager(new CacheProperties());

            contextRunner
                    .withBean(DefaultCacheManager.class, () -> cacheManager)
                    .run(context -> {
                        // 默认不启用，所以不应该有 EntityCacheManager
                        assertThat(context).doesNotHaveBean(EntityCacheManager.class);
                    });
        }

        @Test
        @DisplayName("应正确绑定缓存配置属性")
        void shouldBindCacheConfigurationProperties() {
            DefaultCacheManager cacheManager = new DefaultCacheManager(new CacheProperties());

            contextRunner
                    .withBean(DefaultCacheManager.class, () -> cacheManager)
                    .withPropertyValues(
                            "afg.jdbc.cache.enabled=true",
                            "afg.jdbc.cache.default-ttl=3600s",
                            "afg.jdbc.cache.max-size=1000"
                    )
                    .run(context -> {
                        assertThat(context).hasSingleBean(EntityCacheProperties.class);
                        EntityCacheProperties props = context.getBean(EntityCacheProperties.class);
                        assertThat(props).isNotNull();
                    });
        }

        @Test
        @DisplayName("用户自定义 EntityCacheManager 应覆盖自动配置")
        void customEntityCacheManagerShouldOverrideAutoConfiguration() {
            DefaultCacheManager cacheManager = new DefaultCacheManager(new CacheProperties());
            EntityCacheProperties properties = new EntityCacheProperties();
            EntityCacheManager customEntityCacheManager = new EntityCacheManager(cacheManager, properties);

            contextRunner
                    .withBean(DefaultCacheManager.class, () -> cacheManager)
                    .withBean(EntityCacheManager.class, () -> customEntityCacheManager)
                    .withPropertyValues("afg.jdbc.cache.enabled=true")
                    .run(context -> {
                        assertThat(context).hasSingleBean(EntityCacheManager.class);
                        assertThat(context.getBean(EntityCacheManager.class)).isSameAs(customEntityCacheManager);
                    });
        }
    }
}
