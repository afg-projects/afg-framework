package io.github.afgprojects.framework.core.web.health;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * HealthCheckLevel 单元测试
 */
@DisplayName("HealthCheckLevel 测试")
class HealthCheckLevelTest {

    @Nested
    @DisplayName("枚举值测试")
    class EnumValuesTest {

        @Test
        @DisplayName("应该包含三个级别")
        void shouldHaveThreeLevels() {
            HealthCheckLevel[] levels = HealthCheckLevel.values();
            assertEquals(3, levels.length);
        }

        @Test
        @DisplayName("LIVENESS 级别应该是 1")
        void livenessLevelShouldBeOne() {
            assertEquals(1, HealthCheckLevel.LIVENESS.getLevel());
        }

        @Test
        @DisplayName("READINESS 级别应该是 2")
        void readinessLevelShouldBeTwo() {
            assertEquals(2, HealthCheckLevel.READINESS.getLevel());
        }

        @Test
        @DisplayName("DEEP 级别应该是 3")
        void deepLevelShouldBeThree() {
            assertEquals(3, HealthCheckLevel.DEEP.getLevel());
        }
    }

    @Nested
    @DisplayName("includes 测试")
    class IncludesTest {

        @Test
        @DisplayName("LIVENESS 不应包含 READINESS")
        void livenessShouldNotIncludeReadiness() {
            assertFalse(HealthCheckLevel.LIVENESS.includes(HealthCheckLevel.READINESS));
        }

        @Test
        @DisplayName("LIVENESS 不应包含 DEEP")
        void livenessShouldNotIncludeDeep() {
            assertFalse(HealthCheckLevel.LIVENESS.includes(HealthCheckLevel.DEEP));
        }

        @Test
        @DisplayName("LIVENESS 应包含自身")
        void livenessShouldIncludeItself() {
            assertTrue(HealthCheckLevel.LIVENESS.includes(HealthCheckLevel.LIVENESS));
        }

        @Test
        @DisplayName("READINESS 应包含 LIVENESS")
        void readinessShouldIncludeLiveness() {
            assertTrue(HealthCheckLevel.READINESS.includes(HealthCheckLevel.LIVENESS));
        }

        @Test
        @DisplayName("READINESS 应包含自身")
        void readinessShouldIncludeItself() {
            assertTrue(HealthCheckLevel.READINESS.includes(HealthCheckLevel.READINESS));
        }

        @Test
        @DisplayName("READINESS 不应包含 DEEP")
        void readinessShouldNotIncludeDeep() {
            assertFalse(HealthCheckLevel.READINESS.includes(HealthCheckLevel.DEEP));
        }

        @Test
        @DisplayName("DEEP 应包含所有级别")
        void deepShouldIncludeAllLevels() {
            assertTrue(HealthCheckLevel.DEEP.includes(HealthCheckLevel.LIVENESS));
            assertTrue(HealthCheckLevel.DEEP.includes(HealthCheckLevel.READINESS));
            assertTrue(HealthCheckLevel.DEEP.includes(HealthCheckLevel.DEEP));
        }
    }

    @Nested
    @DisplayName("getDescription 测试")
    class GetDescriptionTest {

        @Test
        @DisplayName("应该返回中文描述")
        void shouldReturnChineseDescription() {
            assertEquals("存活检查", HealthCheckLevel.LIVENESS.getDescription());
            assertEquals("就绪检查", HealthCheckLevel.READINESS.getDescription());
            assertEquals("深度检查", HealthCheckLevel.DEEP.getDescription());
        }
    }
}
