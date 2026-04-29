package io.github.afgprojects.framework.core.web.health;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import io.github.afgprojects.framework.core.module.ModuleDefinition;
import io.github.afgprojects.framework.core.module.ModuleRegistry;
import io.github.afgprojects.framework.core.support.BaseUnitTest;
import io.github.afgprojects.framework.core.support.TestDataFactory;

/**
 * ModuleHealthIndicator 单元测试
 */
@DisplayName("ModuleHealthIndicator 测试")
class ModuleHealthIndicatorTest extends BaseUnitTest {

    private ModuleRegistry moduleRegistry;
    private ModuleHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        moduleRegistry = new ModuleRegistry();
        healthIndicator = new ModuleHealthIndicator(moduleRegistry);
    }

    @Nested
    @DisplayName("health 测试")
    class HealthTest {

        @Test
        @DisplayName("所有模块注册时应该报告 UP 状态")
        void shouldReportUpWhenAllModulesRegistered() {
            // given
            ModuleDefinition module1 = TestDataFactory.createModuleDefinition("module-1", "Module 1");
            ModuleDefinition module2 = TestDataFactory.createModuleDefinition("module-2", "Module 2");
            moduleRegistry.register(module1);
            moduleRegistry.register(module2);

            // when
            Health health = healthIndicator.health();

            // then
            assertEquals(Status.UP, health.getStatus());
            assertEquals(2, health.getDetails().get("moduleCount"));
        }

        @Test
        @DisplayName("没有模块时应该报告 UP 状态")
        void shouldReportUpWhenNoModules() {
            // given - 空的 ModuleRegistry

            // when
            Health health = healthIndicator.health();

            // then
            assertEquals(Status.UP, health.getStatus());
            assertEquals(0, health.getDetails().get("moduleCount"));
        }

        @Test
        @DisplayName("应该包含模块详情")
        void shouldIncludeModuleDetails() {
            // given
            ModuleDefinition baseModule = TestDataFactory.createModuleDefinition("base", "Base Module");
            ModuleDefinition dependentModule =
                    TestDataFactory.createModuleDefinition("dependent", "Dependent Module", "base");
            moduleRegistry.register(baseModule);
            moduleRegistry.register(dependentModule);

            // when
            Health health = healthIndicator.health();

            // then
            @SuppressWarnings("unchecked")
            List<ModuleHealthDetails> modules =
                    (List<ModuleHealthDetails>) health.getDetails().get("modules");

            assertNotNull(modules);
            assertEquals(2, modules.size());

            // 验证模块详情
            ModuleHealthDetails baseDetails = modules.stream()
                    .filter(m -> "base".equals(m.id()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(baseDetails);
            assertEquals("Base Module", baseDetails.name());
            assertEquals("UP", baseDetails.status());
            assertTrue(baseDetails.dependencies().isEmpty());

            ModuleHealthDetails dependentDetails = modules.stream()
                    .filter(m -> "dependent".equals(m.id()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(dependentDetails);
            assertEquals("Dependent Module", dependentDetails.name());
            assertEquals("UP", dependentDetails.status());
            assertEquals(List.of("base"), dependentDetails.dependencies());
        }

        @Test
        @DisplayName("应该包含检查级别信息")
        void shouldIncludeCheckLevelInfo() {
            // given
            ModuleDefinition module = TestDataFactory.createModuleDefinition("test", "Test Module");
            moduleRegistry.register(module);

            // when
            Health health = healthIndicator.health();

            // then
            assertEquals("READINESS", health.getDetails().get("checkLevel"));
        }
    }

    @Nested
    @DisplayName("健康检查级别测试")
    class CheckLevelTest {

        @Test
        @DisplayName("默认应该是 READINESS 级别")
        void defaultCheckLevelShouldBeReadiness() {
            assertEquals(HealthCheckLevel.READINESS, healthIndicator.getCheckLevel());
        }

        @Test
        @DisplayName("LIVENESS 级别应该总是返回 UP")
        void livenessLevelShouldAlwaysReturnUp() {
            // given
            healthIndicator.setCheckLevel(HealthCheckLevel.LIVENESS);
            ModuleDefinition module = TestDataFactory.createModuleDefinition("test", "Test Module");
            moduleRegistry.register(module);

            // when
            Health health = healthIndicator.health();

            // then
            assertEquals(Status.UP, health.getStatus());
            assertEquals("LIVENESS", health.getDetails().get("checkLevel"));
        }

        @Test
        @DisplayName("READINESS 级别应该检查模块状态")
        void readinessLevelShouldCheckModuleStatus() {
            // given
            healthIndicator.setCheckLevel(HealthCheckLevel.READINESS);
            ModuleDefinition module = TestDataFactory.createModuleDefinition("test", "Test Module");
            moduleRegistry.register(module);

            // when
            Health health = healthIndicator.health();

            // then
            assertEquals(Status.UP, health.getStatus());
            assertEquals("READINESS", health.getDetails().get("checkLevel"));
        }

        @Test
        @DisplayName("DEEP 级别应该包含额外信息")
        void deepLevelShouldIncludeExtraDetails() {
            // given
            healthIndicator.setCheckLevel(HealthCheckLevel.DEEP);
            ModuleDefinition base = TestDataFactory.createModuleDefinition("base", "Base");
            ModuleDefinition dependent = TestDataFactory.createModuleDefinition("dependent", "Dependent", "base");
            moduleRegistry.register(base);
            moduleRegistry.register(dependent);

            // when
            Health health = healthIndicator.health();

            // then
            assertEquals(Status.UP, health.getStatus());
            assertEquals("DEEP", health.getDetails().get("checkLevel"));
            assertEquals(2L, health.getDetails().get("upModules"));
            assertEquals(0L, health.getDetails().get("downModules"));
            assertEquals(1L, health.getDetails().get("modulesWithDependencies"));
        }

        @Test
        @DisplayName("构造函数可以指定检查级别")
        void constructorShouldAcceptCheckLevel() {
            // given
            ModuleHealthIndicator indicator = new ModuleHealthIndicator(moduleRegistry, HealthCheckLevel.LIVENESS);

            // when & then
            assertEquals(HealthCheckLevel.LIVENESS, indicator.getCheckLevel());
        }
    }

    @Nested
    @DisplayName("setCheckLevel 测试")
    class SetCheckLevelTest {

        @Test
        @DisplayName("应该能动态设置检查级别")
        void shouldSetCheckLevelDynamically() {
            // when
            healthIndicator.setCheckLevel(HealthCheckLevel.DEEP);

            // then
            assertEquals(HealthCheckLevel.DEEP, healthIndicator.getCheckLevel());
        }
    }
}
