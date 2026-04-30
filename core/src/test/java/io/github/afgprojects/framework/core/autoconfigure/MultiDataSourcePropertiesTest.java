package io.github.afgprojects.framework.core.autoconfigure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MultiDataSourceProperties 测试
 */
@DisplayName("MultiDataSourceProperties 测试")
class MultiDataSourcePropertiesTest {

    @Test
    @DisplayName("应该使用默认值创建 MultiDataSourceProperties")
    void shouldCreateWithDefaults() {
        MultiDataSourceProperties properties = new MultiDataSourceProperties();
        assertFalse(properties.isEnabled());
        assertEquals("master", properties.getPrimary());
        assertFalse(properties.isStrict());
        assertNotNull(properties.getDatasources());
        assertNotNull(properties.getReadWriteSeparation());
    }

    @Test
    @DisplayName("应该正确设置基本属性")
    void shouldSetBasicProperties() {
        MultiDataSourceProperties properties = new MultiDataSourceProperties();
        properties.setEnabled(true);
        properties.setPrimary("main");
        properties.setStrict(true);

        assertTrue(properties.isEnabled());
        assertEquals("main", properties.getPrimary());
        assertTrue(properties.isStrict());
    }

    @Test
    @DisplayName("DataSourceConfig 应该有默认值")
    void dataSourceConfigShouldHaveDefaults() {
        MultiDataSourceProperties.DataSourceConfig config = new MultiDataSourceProperties.DataSourceConfig();
        assertNull(config.getUrl());
        assertNull(config.getUsername());
        assertNull(config.getPassword());
        assertNull(config.getDriverClassName());
        assertFalse(config.isLazyInit());
        assertNotNull(config.getPoolConfig());
    }

    @Test
    @DisplayName("DataSourceConfig 应该正确设置属性")
    void dataSourceConfigShouldSetProperties() {
        MultiDataSourceProperties.DataSourceConfig config = new MultiDataSourceProperties.DataSourceConfig();
        config.setUrl("jdbc:mysql://localhost:3306/db");
        config.setUsername("user");
        config.setPassword("pass");
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setLazyInit(true);
        config.setPoolConfig(Map.of("maxPoolSize", 10));

        assertEquals("jdbc:mysql://localhost:3306/db", config.getUrl());
        assertEquals("user", config.getUsername());
        assertEquals("pass", config.getPassword());
        assertEquals("com.mysql.cj.jdbc.Driver", config.getDriverClassName());
        assertTrue(config.isLazyInit());
        assertEquals(10, config.getPoolConfig().get("maxPoolSize"));
    }

    @Test
    @DisplayName("ReadWriteSeparationConfig 应该有默认值")
    void readWriteSeparationConfigShouldHaveDefaults() {
        MultiDataSourceProperties.ReadWriteSeparationConfig config = new MultiDataSourceProperties.ReadWriteSeparationConfig();
        assertFalse(config.isEnabled());
        assertNotNull(config.getReadDatasources());
        assertNull(config.getWriteDatasource());
        assertNotNull(config.getLoadBalance());
    }

    @Test
    @DisplayName("ReadWriteSeparationConfig 应该正确设置属性")
    void readWriteSeparationConfigShouldSetProperties() {
        MultiDataSourceProperties.ReadWriteSeparationConfig config = new MultiDataSourceProperties.ReadWriteSeparationConfig();
        config.setEnabled(true);
        config.setReadDatasources(List.of("read1", "read2"));
        config.setWriteDatasource("write");

        assertTrue(config.isEnabled());
        assertEquals(2, config.getReadDatasources().size());
        assertEquals("write", config.getWriteDatasource());
    }

    @Test
    @DisplayName("LoadBalanceConfig 应该有默认值")
    void loadBalanceConfigShouldHaveDefaults() {
        MultiDataSourceProperties.LoadBalanceConfig config = new MultiDataSourceProperties.LoadBalanceConfig();
        assertEquals(MultiDataSourceProperties.LoadBalanceStrategyType.ROUND_ROBIN, config.getStrategy());
        assertEquals(30000L, config.getHealthCheckInterval());
        assertTrue(config.isHealthCheckEnabled());
        assertNotNull(config.getWeights());
    }

    @Test
    @DisplayName("LoadBalanceConfig 应该正确设置属性")
    void loadBalanceConfigShouldSetProperties() {
        MultiDataSourceProperties.LoadBalanceConfig config = new MultiDataSourceProperties.LoadBalanceConfig();
        config.setStrategy(MultiDataSourceProperties.LoadBalanceStrategyType.WEIGHTED);
        config.setHealthCheckInterval(60000L);
        config.setHealthCheckEnabled(false);
        config.setWeights(Map.of("ds1", 3, "ds2", 1));

        assertEquals(MultiDataSourceProperties.LoadBalanceStrategyType.WEIGHTED, config.getStrategy());
        assertEquals(60000L, config.getHealthCheckInterval());
        assertFalse(config.isHealthCheckEnabled());
        assertEquals(3, config.getWeights().get("ds1"));
    }

    @Test
    @DisplayName("LoadBalanceStrategyType 应该包含所有策略")
    void loadBalanceStrategyTypeShouldContainAllStrategies() {
        MultiDataSourceProperties.LoadBalanceStrategyType[] strategies = MultiDataSourceProperties.LoadBalanceStrategyType.values();
        assertEquals(3, strategies.length);
        assertEquals(MultiDataSourceProperties.LoadBalanceStrategyType.ROUND_ROBIN, MultiDataSourceProperties.LoadBalanceStrategyType.valueOf("ROUND_ROBIN"));
        assertEquals(MultiDataSourceProperties.LoadBalanceStrategyType.WEIGHTED, MultiDataSourceProperties.LoadBalanceStrategyType.valueOf("WEIGHTED"));
        assertEquals(MultiDataSourceProperties.LoadBalanceStrategyType.LEAST_CONNECTIONS, MultiDataSourceProperties.LoadBalanceStrategyType.valueOf("LEAST_CONNECTIONS"));
    }
}
