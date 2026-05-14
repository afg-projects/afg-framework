package io.github.afgprojects.framework.core.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;

import io.github.afgprojects.framework.core.module.ModuleRegistry;
import io.github.afgprojects.framework.core.web.health.DataSourceHealthProperties;
import io.github.afgprojects.framework.core.web.health.HealthCheckProperties;
import io.github.afgprojects.framework.core.web.health.LivenessHealthIndicator;
import io.github.afgprojects.framework.core.web.health.ModuleHealthIndicator;
import io.github.afgprojects.framework.core.web.health.ReadinessHealthIndicator;

/**
 * HealthAutoConfiguration 单元测试。
 * 测试健康检查自动配置类的 Bean 创建功能。
 *
 * @see HealthAutoConfiguration
 */
@DisplayName("HealthAutoConfiguration 测试")
class HealthAutoConfigurationTest {

    private HealthAutoConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new HealthAutoConfiguration();
    }

    /**
     * 模块健康指示器配置测试。
     * 验证 moduleHealthIndicator Bean 的创建。
     */
    @Nested
    @DisplayName("moduleHealthIndicator 配置测试")
    class ModuleHealthIndicatorTests {

        /**
         * 测试创建模块健康指示器。
         */
        @Test
        @DisplayName("应该创建模块健康指示器")
        void shouldCreateModuleHealthIndicator() {
            ModuleRegistry registry = new ModuleRegistry();

            ModuleHealthIndicator indicator = configuration.moduleHealthIndicator(registry);

            assertThat(indicator).isNotNull();
        }
    }

    /**
     * 数据源健康指示器配置测试。
     * 验证 dataSourceHealthIndicator Bean 的创建。
     */
    @Nested
    @DisplayName("dataSourceHealthIndicator 配置测试")
    class DataSourceHealthIndicatorTests {

        /**
         * 测试创建数据源健康指示器。
         */
        @Test
        @DisplayName("应该创建数据源健康指示器")
        void shouldCreateDataSourceHealthIndicator() {
            DataSource dataSource = mock(DataSource.class);
            DataSourceHealthProperties properties = new DataSourceHealthProperties();

            var indicator = configuration.dataSourceHealthIndicator(dataSource, properties);

            assertThat(indicator).isNotNull();
        }
    }

    /**
     * 存活探针健康指示器配置测试。
     * 验证 livenessHealthIndicator Bean 的创建。
     */
    @Nested
    @DisplayName("livenessHealthIndicator 配置测试")
    class LivenessHealthIndicatorTests {

        /**
         * 测试创建存活探针健康指示器。
         */
        @Test
        @DisplayName("应该创建存活探针健康指示器")
        void shouldCreateLivenessHealthIndicator() {
            HealthCheckProperties properties = new HealthCheckProperties();

            LivenessHealthIndicator indicator = configuration.livenessHealthIndicator(properties);

            assertThat(indicator).isNotNull();
        }
    }

    /**
     * 就绪探针健康指示器配置测试。
     * 验证 readinessHealthIndicator Bean 的创建。
     */
    @Nested
    @DisplayName("readinessHealthIndicator 配置测试")
    class ReadinessHealthIndicatorTests {

        /**
         * 测试创建就绪探针健康指示器。
         */
        @Test
        @DisplayName("应该创建就绪探针健康指示器")
        void shouldCreateReadinessHealthIndicator() {
            HealthCheckProperties properties = new HealthCheckProperties();

            ReadinessHealthIndicator indicator = configuration.readinessHealthIndicator(
                    properties, null, null, null);

            assertThat(indicator).isNotNull();
        }

        /**
         * 测试使用数据源创建就绪探针健康指示器。
         */
        @Test
        @DisplayName("应该使用数据源创建就绪探针健康指示器")
        void shouldCreateReadinessHealthIndicatorWithDataSource() {
            HealthCheckProperties properties = new HealthCheckProperties();
            DataSource dataSource = mock(DataSource.class);

            ReadinessHealthIndicator indicator = configuration.readinessHealthIndicator(
                    properties, dataSource, null, null);

            assertThat(indicator).isNotNull();
        }

        /**
         * 测试使用 RedissonClient 创建就绪探针健康指示器。
         */
        @Test
        @DisplayName("应该使用 RedissonClient 创建就绪探针健康指示器")
        void shouldCreateReadinessHealthIndicatorWithRedissonClient() {
            HealthCheckProperties properties = new HealthCheckProperties();
            RedissonClient redissonClient = mock(RedissonClient.class);

            ReadinessHealthIndicator indicator = configuration.readinessHealthIndicator(
                    properties, null, redissonClient, null);

            assertThat(indicator).isNotNull();
        }

        /**
         * 测试使用所有组件创建就绪探针健康指示器。
         */
        @Test
        @DisplayName("应该使用所有组件创建就绪探针健康指示器")
        void shouldCreateReadinessHealthIndicatorWithAllComponents() {
            HealthCheckProperties properties = new HealthCheckProperties();
            DataSource dataSource = mock(DataSource.class);
            RedissonClient redissonClient = mock(RedissonClient.class);
            ModuleRegistry moduleRegistry = new ModuleRegistry();

            ReadinessHealthIndicator indicator = configuration.readinessHealthIndicator(
                    properties, dataSource, redissonClient, moduleRegistry);

            assertThat(indicator).isNotNull();
        }
    }
}
