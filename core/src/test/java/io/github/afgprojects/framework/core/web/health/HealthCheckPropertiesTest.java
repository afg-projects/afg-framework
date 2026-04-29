package io.github.afgprojects.framework.core.web.health;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * HealthCheckProperties 单元测试
 */
@DisplayName("HealthCheckProperties 测试")
class HealthCheckPropertiesTest {

    private HealthCheckProperties properties;

    @BeforeEach
    void setUp() {
        properties = new HealthCheckProperties();
    }

    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTest {

        @Test
        @DisplayName("存活探针应该默认启用")
        void livenessShouldBeEnabledByDefault() {
            assertTrue(properties.isLivenessEnabled());
        }

        @Test
        @DisplayName("就绪探针应该默认启用")
        void readinessShouldBeEnabledByDefault() {
            assertTrue(properties.isReadinessEnabled());
        }

        @Test
        @DisplayName("深度检查应该默认启用")
        void deepShouldBeEnabledByDefault() {
            assertTrue(properties.isDeepEnabled());
        }

        @Test
        @DisplayName("存活探针配置应该有正确的默认值")
        void livenessConfigShouldHaveCorrectDefaults() {
            assertTrue(properties.getLiveness().isDeadlockDetectionEnabled());
            assertEquals(Duration.ofSeconds(5), properties.getLiveness().getDeadlockDetectionTimeout());
            assertTrue(properties.getLiveness().isMemoryCheckEnabled());
            assertEquals(80, properties.getLiveness().getMemoryWarningThreshold());
            assertEquals(95, properties.getLiveness().getMemoryCriticalThreshold());
        }

        @Test
        @DisplayName("就绪探针配置应该有正确的默认值")
        void readinessConfigShouldHaveCorrectDefaults() {
            assertTrue(properties.getReadiness().isDatabaseCheckEnabled());
            assertEquals(Duration.ofSeconds(3), properties.getReadiness().getDatabaseCheckTimeout());
            assertTrue(properties.getReadiness().isRedisCheckEnabled());
            assertEquals(Duration.ofSeconds(3), properties.getReadiness().getRedisCheckTimeout());
            assertTrue(properties.getReadiness().isModuleCheckEnabled());
            assertTrue(properties.getReadiness().getRequiredModules().isEmpty());
        }

        @Test
        @DisplayName("深度检查配置应该有正确的默认值")
        void deepConfigShouldHaveCorrectDefaults() {
            assertEquals(Duration.ofSeconds(10), properties.getDeep().getTimeout());
            assertTrue(properties.getDeep().isExternalServiceCheckEnabled());
            assertTrue(properties.getDeep().isConfigCenterCheckEnabled());
            assertEquals(Duration.ofSeconds(5), properties.getDeep().getExternalServiceTimeout());
        }
    }

    @Nested
    @DisplayName("Setter 测试")
    class SetterTest {

        @Test
        @DisplayName("应该能设置存活探针启用状态")
        void shouldSetLivenessEnabled() {
            properties.setLivenessEnabled(false);
            assertFalse(properties.isLivenessEnabled());
        }

        @Test
        @DisplayName("应该能设置就绪探针启用状态")
        void shouldSetReadinessEnabled() {
            properties.setReadinessEnabled(false);
            assertFalse(properties.isReadinessEnabled());
        }

        @Test
        @DisplayName("应该能设置深度检查启用状态")
        void shouldSetDeepEnabled() {
            properties.setDeepEnabled(false);
            assertFalse(properties.isDeepEnabled());
        }

        @Test
        @DisplayName("应该能设置存活探针配置")
        void shouldSetLivenessConfig() {
            properties.getLiveness().setDeadlockDetectionEnabled(false);
            properties.getLiveness().setMemoryWarningThreshold(70);
            properties.getLiveness().setMemoryCriticalThreshold(90);

            assertFalse(properties.getLiveness().isDeadlockDetectionEnabled());
            assertEquals(70, properties.getLiveness().getMemoryWarningThreshold());
            assertEquals(90, properties.getLiveness().getMemoryCriticalThreshold());
        }

        @Test
        @DisplayName("应该能设置就绪探针配置")
        void shouldSetReadinessConfig() {
            properties.getReadiness().setDatabaseCheckEnabled(false);
            properties.getReadiness().setRedisCheckEnabled(false);

            assertFalse(properties.getReadiness().isDatabaseCheckEnabled());
            assertFalse(properties.getReadiness().isRedisCheckEnabled());
        }
    }
}
