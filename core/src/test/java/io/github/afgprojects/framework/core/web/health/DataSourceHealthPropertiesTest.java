package io.github.afgprojects.framework.core.web.health;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;

/**
 * DataSourceHealthConfig 单元测试
 */
@DisplayName("DataSourceHealthConfig 测试")
class DataSourceHealthPropertiesTest {

    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTest {

        @Test
        @DisplayName("默认启用状态应该为 true")
        void defaultEnabledShouldBeTrue() {
            AfgCoreProperties.HealthConfig.DataSourceHealthConfig properties = new AfgCoreProperties.HealthConfig.DataSourceHealthConfig();
            assertTrue(properties.isEnabled());
        }

        @Test
        @DisplayName("默认验证查询应该为 SELECT 1")
        void defaultValidationQueryShouldBeSelectOne() {
            AfgCoreProperties.HealthConfig.DataSourceHealthConfig properties = new AfgCoreProperties.HealthConfig.DataSourceHealthConfig();
            assertEquals("SELECT 1", properties.getValidationQuery());
        }

        @Test
        @DisplayName("默认连接池使用率警告阈值应该为 70%")
        void defaultPoolUsageWarningThresholdShouldBe70() {
            AfgCoreProperties.HealthConfig.DataSourceHealthConfig properties = new AfgCoreProperties.HealthConfig.DataSourceHealthConfig();
            assertEquals(70, properties.getPoolUsageWarningThreshold());
        }

        @Test
        @DisplayName("默认连接池使用率严重阈值应该为 90%")
        void defaultPoolUsageCriticalThresholdShouldBe90() {
            AfgCoreProperties.HealthConfig.DataSourceHealthConfig properties = new AfgCoreProperties.HealthConfig.DataSourceHealthConfig();
            assertEquals(90, properties.getPoolUsageCriticalThreshold());
        }

        @Test
        @DisplayName("默认等待线程数警告阈值应该为 5")
        void defaultThreadsAwaitingWarningThresholdShouldBe5() {
            AfgCoreProperties.HealthConfig.DataSourceHealthConfig properties = new AfgCoreProperties.HealthConfig.DataSourceHealthConfig();
            assertEquals(5, properties.getThreadsAwaitingWarningThreshold());
        }

        @Test
        @DisplayName("默认等待线程数严重阈值应该为 10")
        void defaultThreadsAwaitingCriticalThresholdShouldBe10() {
            AfgCoreProperties.HealthConfig.DataSourceHealthConfig properties = new AfgCoreProperties.HealthConfig.DataSourceHealthConfig();
            assertEquals(10, properties.getThreadsAwaitingCriticalThreshold());
        }

        @Test
        @DisplayName("默认连接超时应该为 3000ms")
        void defaultConnectionTimeoutShouldBe3000() {
            AfgCoreProperties.HealthConfig.DataSourceHealthConfig properties = new AfgCoreProperties.HealthConfig.DataSourceHealthConfig();
            assertEquals(3000, properties.getConnectionTimeout());
        }
    }

    @Nested
    @DisplayName("Setter 测试")
    class SetterTest {

        @Test
        @DisplayName("应该能够设置启用状态")
        void shouldSetEnabled() {
            AfgCoreProperties.HealthConfig.DataSourceHealthConfig properties = new AfgCoreProperties.HealthConfig.DataSourceHealthConfig();
            properties.setEnabled(false);
            assertFalse(properties.isEnabled());
        }

        @Test
        @DisplayName("应该能够设置验证查询")
        void shouldSetValidationQuery() {
            AfgCoreProperties.HealthConfig.DataSourceHealthConfig properties = new AfgCoreProperties.HealthConfig.DataSourceHealthConfig();
            properties.setValidationQuery("SELECT 1 FROM DUAL");
            assertEquals("SELECT 1 FROM DUAL", properties.getValidationQuery());
        }

        @Test
        @DisplayName("应该能够设置连接池使用率警告阈值")
        void shouldSetPoolUsageWarningThreshold() {
            AfgCoreProperties.HealthConfig.DataSourceHealthConfig properties = new AfgCoreProperties.HealthConfig.DataSourceHealthConfig();
            properties.setPoolUsageWarningThreshold(80);
            assertEquals(80, properties.getPoolUsageWarningThreshold());
        }

        @Test
        @DisplayName("应该能够设置连接池使用率严重阈值")
        void shouldSetPoolUsageCriticalThreshold() {
            AfgCoreProperties.HealthConfig.DataSourceHealthConfig properties = new AfgCoreProperties.HealthConfig.DataSourceHealthConfig();
            properties.setPoolUsageCriticalThreshold(95);
            assertEquals(95, properties.getPoolUsageCriticalThreshold());
        }

        @Test
        @DisplayName("应该能够设置等待线程数警告阈值")
        void shouldSetThreadsAwaitingWarningThreshold() {
            AfgCoreProperties.HealthConfig.DataSourceHealthConfig properties = new AfgCoreProperties.HealthConfig.DataSourceHealthConfig();
            properties.setThreadsAwaitingWarningThreshold(8);
            assertEquals(8, properties.getThreadsAwaitingWarningThreshold());
        }

        @Test
        @DisplayName("应该能够设置等待线程数严重阈值")
        void shouldSetThreadsAwaitingCriticalThreshold() {
            AfgCoreProperties.HealthConfig.DataSourceHealthConfig properties = new AfgCoreProperties.HealthConfig.DataSourceHealthConfig();
            properties.setThreadsAwaitingCriticalThreshold(15);
            assertEquals(15, properties.getThreadsAwaitingCriticalThreshold());
        }

        @Test
        @DisplayName("应该能够设置连接超时")
        void shouldSetConnectionTimeout() {
            AfgCoreProperties.HealthConfig.DataSourceHealthConfig properties = new AfgCoreProperties.HealthConfig.DataSourceHealthConfig();
            properties.setConnectionTimeout(5000);
            assertEquals(5000, properties.getConnectionTimeout());
        }
    }
}