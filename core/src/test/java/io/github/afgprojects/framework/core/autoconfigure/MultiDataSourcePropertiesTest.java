package io.github.afgprojects.framework.core.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;

/**
 * DataSourceConfig 单元测试。
 * 测试多数据源配置属性类的默认值和属性设置。
 *
 * @see AfgCoreProperties.DataSourceConfig
 */
@DisplayName("DataSourceConfig 测试")
class MultiDataSourcePropertiesTest {

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
            AfgCoreProperties.DataSourceConfig props = new AfgCoreProperties.DataSourceConfig();

            assertThat(props.isEnabled()).isFalse();
            assertThat(props.getPrimary()).isEqualTo("master");
            assertThat(props.isStrict()).isFalse();
            assertThat(props.getDatasources()).isEmpty();
            assertThat(props.getReadWriteSeparation()).isNotNull();
        }
    }

    /**
     * DataSourceInstanceConfig 内嵌类测试。
     * 验证数据源配置的默认值和属性设置。
     */
    @Nested
    @DisplayName("DataSourceInstanceConfig 测试")
    class DataSourceConfigTests {

        /**
         * 测试 DataSourceInstanceConfig 的默认值。
         */
        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            AfgCoreProperties.DataSourceConfig.DataSourceInstanceConfig config = new AfgCoreProperties.DataSourceConfig.DataSourceInstanceConfig();

            assertThat(config.isLazyInit()).isFalse();
            assertThat(config.getPoolConfig()).isEmpty();
        }

        /**
         * 测试 DataSourceInstanceConfig 的属性设置。
         */
        @Test
        @DisplayName("应该正确设置属性")
        void shouldSetProperties() {
            AfgCoreProperties.DataSourceConfig.DataSourceInstanceConfig config = new AfgCoreProperties.DataSourceConfig.DataSourceInstanceConfig();
            config.setUrl("jdbc:mysql://localhost:3306/test");
            config.setUsername("root");
            config.setPassword("password");
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            config.setLazyInit(true);

            Map<String, Object> poolConfig = new HashMap<>();
            poolConfig.put("maxPoolSize", 10);
            config.setPoolConfig(poolConfig);

            assertThat(config.getUrl()).isEqualTo("jdbc:mysql://localhost:3306/test");
            assertThat(config.getUsername()).isEqualTo("root");
            assertThat(config.getPassword()).isEqualTo("password");
            assertThat(config.getDriverClassName()).isEqualTo("com.mysql.cj.jdbc.Driver");
            assertThat(config.isLazyInit()).isTrue();
            assertThat(config.getPoolConfig()).containsEntry("maxPoolSize", 10);
        }
    }

    /**
     * ReadWriteSeparationConfig 内嵌类测试。
     * 验证读写分离配置的默认值和属性设置。
     */
    @Nested
    @DisplayName("ReadWriteSeparationConfig 测试")
    class ReadWriteSeparationConfigTests {

        /**
         * 测试 ReadWriteSeparationConfig 的默认值。
         */
        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            AfgCoreProperties.DataSourceConfig.ReadWriteSeparationConfig config = new AfgCoreProperties.DataSourceConfig.ReadWriteSeparationConfig();

            assertThat(config.isEnabled()).isFalse();
            assertThat(config.getReadDatasources()).isEmpty();
            assertThat(config.getLoadBalance()).isNotNull();
        }

        /**
         * 测试 ReadWriteSeparationConfig 的属性设置。
         */
        @Test
        @DisplayName("应该正确设置属性")
        void shouldSetProperties() {
            AfgCoreProperties.DataSourceConfig.ReadWriteSeparationConfig config = new AfgCoreProperties.DataSourceConfig.ReadWriteSeparationConfig();
            config.setEnabled(true);
            config.setReadDatasources(List.of("slave1", "slave2"));
            config.setWriteDatasource("master");

            assertThat(config.isEnabled()).isTrue();
            assertThat(config.getReadDatasources()).containsExactly("slave1", "slave2");
            assertThat(config.getWriteDatasource()).isEqualTo("master");
        }
    }

    /**
     * LoadBalanceConfig 内嵌类测试。
     * 验证负载均衡配置的默认值和属性设置。
     */
    @Nested
    @DisplayName("LoadBalanceConfig 测试")
    class LoadBalanceConfigTests {

        /**
         * 测试 LoadBalanceConfig 的默认值。
         */
        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            AfgCoreProperties.DataSourceConfig.LoadBalanceConfig config = new AfgCoreProperties.DataSourceConfig.LoadBalanceConfig();

            assertThat(config.getStrategy()).isEqualTo(AfgCoreProperties.DataSourceConfig.LoadBalanceStrategyType.ROUND_ROBIN);
            assertThat(config.getHealthCheckInterval()).isEqualTo(30000L);
            assertThat(config.isHealthCheckEnabled()).isTrue();
            assertThat(config.getWeights()).isEmpty();
        }

        /**
         * 测试 LoadBalanceConfig 的属性设置。
         */
        @Test
        @DisplayName("应该正确设置属性")
        void shouldSetProperties() {
            AfgCoreProperties.DataSourceConfig.LoadBalanceConfig config = new AfgCoreProperties.DataSourceConfig.LoadBalanceConfig();
            config.setStrategy(AfgCoreProperties.DataSourceConfig.LoadBalanceStrategyType.WEIGHTED);
            config.setHealthCheckInterval(60000L);
            config.setHealthCheckEnabled(false);

            Map<String, Integer> weights = new HashMap<>();
            weights.put("slave1", 70);
            weights.put("slave2", 30);
            config.setWeights(weights);

            assertThat(config.getStrategy()).isEqualTo(AfgCoreProperties.DataSourceConfig.LoadBalanceStrategyType.WEIGHTED);
            assertThat(config.getHealthCheckInterval()).isEqualTo(60000L);
            assertThat(config.isHealthCheckEnabled()).isFalse();
            assertThat(config.getWeights()).containsEntry("slave1", 70);
        }
    }

    /**
     * LoadBalanceStrategyType 枚举测试。
     * 验证负载均衡策略枚举类型的完整性。
     */
    @Nested
    @DisplayName("LoadBalanceStrategyType 枚举测试")
    class LoadBalanceStrategyTypeTests {

        /**
         * 测试枚举包含所有策略类型。
         */
        @Test
        @DisplayName("应该包含所有策略类型")
        void shouldContainAllTypes() {
            AfgCoreProperties.DataSourceConfig.LoadBalanceStrategyType[] types = AfgCoreProperties.DataSourceConfig.LoadBalanceStrategyType.values();

            assertThat(types).hasSize(3);
            assertThat(types).contains(
                    AfgCoreProperties.DataSourceConfig.LoadBalanceStrategyType.ROUND_ROBIN,
                    AfgCoreProperties.DataSourceConfig.LoadBalanceStrategyType.WEIGHTED,
                    AfgCoreProperties.DataSourceConfig.LoadBalanceStrategyType.LEAST_CONNECTIONS
            );
        }
    }
}
