package io.github.afgprojects.framework.core.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * MultiDataSourceProperties 测试
 */
@DisplayName("MultiDataSourceProperties 测试")
class MultiDataSourcePropertiesTest {

    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTests {

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            MultiDataSourceProperties props = new MultiDataSourceProperties();

            assertThat(props.isEnabled()).isFalse();
            assertThat(props.getPrimary()).isEqualTo("master");
            assertThat(props.isStrict()).isFalse();
            assertThat(props.getDatasources()).isEmpty();
            assertThat(props.getReadWriteSeparation()).isNotNull();
        }
    }

    @Nested
    @DisplayName("DataSourceConfig 测试")
    class DataSourceConfigTests {

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            MultiDataSourceProperties.DataSourceConfig config = new MultiDataSourceProperties.DataSourceConfig();

            assertThat(config.isLazyInit()).isFalse();
            assertThat(config.getPoolConfig()).isEmpty();
        }

        @Test
        @DisplayName("应该正确设置属性")
        void shouldSetProperties() {
            MultiDataSourceProperties.DataSourceConfig config = new MultiDataSourceProperties.DataSourceConfig();
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

    @Nested
    @DisplayName("ReadWriteSeparationConfig 测试")
    class ReadWriteSeparationConfigTests {

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            MultiDataSourceProperties.ReadWriteSeparationConfig config = new MultiDataSourceProperties.ReadWriteSeparationConfig();

            assertThat(config.isEnabled()).isFalse();
            assertThat(config.getReadDatasources()).isEmpty();
            assertThat(config.getLoadBalance()).isNotNull();
        }

        @Test
        @DisplayName("应该正确设置属性")
        void shouldSetProperties() {
            MultiDataSourceProperties.ReadWriteSeparationConfig config = new MultiDataSourceProperties.ReadWriteSeparationConfig();
            config.setEnabled(true);
            config.setReadDatasources(List.of("slave1", "slave2"));
            config.setWriteDatasource("master");

            assertThat(config.isEnabled()).isTrue();
            assertThat(config.getReadDatasources()).containsExactly("slave1", "slave2");
            assertThat(config.getWriteDatasource()).isEqualTo("master");
        }
    }

    @Nested
    @DisplayName("LoadBalanceConfig 测试")
    class LoadBalanceConfigTests {

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            MultiDataSourceProperties.LoadBalanceConfig config = new MultiDataSourceProperties.LoadBalanceConfig();

            assertThat(config.getStrategy()).isEqualTo(MultiDataSourceProperties.LoadBalanceStrategyType.ROUND_ROBIN);
            assertThat(config.getHealthCheckInterval()).isEqualTo(30000L);
            assertThat(config.isHealthCheckEnabled()).isTrue();
            assertThat(config.getWeights()).isEmpty();
        }

        @Test
        @DisplayName("应该正确设置属性")
        void shouldSetProperties() {
            MultiDataSourceProperties.LoadBalanceConfig config = new MultiDataSourceProperties.LoadBalanceConfig();
            config.setStrategy(MultiDataSourceProperties.LoadBalanceStrategyType.WEIGHTED);
            config.setHealthCheckInterval(60000L);
            config.setHealthCheckEnabled(false);

            Map<String, Integer> weights = new HashMap<>();
            weights.put("slave1", 70);
            weights.put("slave2", 30);
            config.setWeights(weights);

            assertThat(config.getStrategy()).isEqualTo(MultiDataSourceProperties.LoadBalanceStrategyType.WEIGHTED);
            assertThat(config.getHealthCheckInterval()).isEqualTo(60000L);
            assertThat(config.isHealthCheckEnabled()).isFalse();
            assertThat(config.getWeights()).containsEntry("slave1", 70);
        }
    }

    @Nested
    @DisplayName("LoadBalanceStrategyType 枚举测试")
    class LoadBalanceStrategyTypeTests {

        @Test
        @DisplayName("应该包含所有策略类型")
        void shouldContainAllTypes() {
            MultiDataSourceProperties.LoadBalanceStrategyType[] types = MultiDataSourceProperties.LoadBalanceStrategyType.values();

            assertThat(types).hasSize(3);
            assertThat(types).contains(
                    MultiDataSourceProperties.LoadBalanceStrategyType.ROUND_ROBIN,
                    MultiDataSourceProperties.LoadBalanceStrategyType.WEIGHTED,
                    MultiDataSourceProperties.LoadBalanceStrategyType.LEAST_CONNECTIONS
            );
        }
    }
}
