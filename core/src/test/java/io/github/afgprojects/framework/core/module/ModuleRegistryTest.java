package io.github.afgprojects.framework.core.module;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.module.exception.ModuleCircularDependencyException;
import io.github.afgprojects.framework.core.module.exception.ModuleDuplicateException;
import io.github.afgprojects.framework.core.module.exception.ModuleNotFoundException;
import io.github.afgprojects.framework.core.support.BaseUnitTest;
import io.github.afgprojects.framework.core.support.TestDataFactory;

/**
 * ModuleRegistry 单元测试
 */
@DisplayName("ModuleRegistry 测试")
class ModuleRegistryTest extends BaseUnitTest {

    private ModuleRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ModuleRegistry();
    }

    @Nested
    @DisplayName("register 测试")
    class RegisterTest {

        @Test
        @DisplayName("应该成功注册模块")
        void shouldRegisterModule() {
            // given
            ModuleDefinition definition = TestDataFactory.createModuleDefinition("module-1", "Module 1");

            // when
            registry.register(definition);

            // then
            assertTrue(registry.containsModule("module-1"));
            assertEquals(definition, registry.getModule("module-1"));
        }

        @Test
        @DisplayName("注册重复ID的模块应该抛出 ModuleDuplicateException")
        void shouldThrowExceptionWhenDuplicateId() {
            // given
            ModuleDefinition definition1 = TestDataFactory.createModuleDefinition("module-1", "Module 1");
            ModuleDefinition definition2 = TestDataFactory.createModuleDefinition("module-1", "Module 2");
            registry.register(definition1);

            // when & then
            assertThrows(ModuleDuplicateException.class, () -> registry.register(definition2));
        }

        @Test
        @DisplayName("注册带依赖的模块")
        void shouldRegisterModuleWithDependencies() {
            // given
            ModuleDefinition depModule = TestDataFactory.createModuleDefinition("dep-module", "Dep Module");
            ModuleDefinition mainModule =
                    TestDataFactory.createModuleDefinition("main-module", "Main Module", "dep-module");
            registry.register(depModule);

            // when
            registry.register(mainModule);

            // then
            assertTrue(registry.containsModule("main-module"));
        }

        @Test
        @DisplayName("注册模块时依赖不存在应该抛出 ModuleNotFoundException")
        void shouldThrowExceptionWhenDependencyNotFound() {
            // given
            ModuleDefinition module = TestDataFactory.createModuleDefinition("module-1", "Module 1", "non-existent");

            // when & then
            ModuleNotFoundException exception =
                    assertThrows(ModuleNotFoundException.class, () -> registry.register(module));
            assertTrue(exception.getMessage().contains("non-existent"));
        }
    }

    @Nested
    @DisplayName("getModule 测试")
    class GetModuleTest {

        @Test
        @DisplayName("应该返回已注册的模块")
        void shouldReturnRegisteredModule() {
            // given
            ModuleDefinition definition = TestDataFactory.createModuleDefinition("module-1", "Module 1");
            registry.register(definition);

            // when
            ModuleDefinition result = registry.getModule("module-1");

            // then
            assertNotNull(result);
            assertEquals(definition, result);
        }

        @Test
        @DisplayName("获取不存在的模块应该返回null")
        void shouldReturnNullForNonExistentModule() {
            // when
            ModuleDefinition result = registry.getModule("non-existent");

            // then
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("getAllModules 测试")
    class GetAllModulesTest {

        @Test
        @DisplayName("应该返回所有已注册的模块")
        void shouldReturnAllModules() {
            // given
            ModuleDefinition module1 = TestDataFactory.createModuleDefinition("module-1", "Module 1");
            ModuleDefinition module2 = TestDataFactory.createModuleDefinition("module-2", "Module 2");
            registry.register(module1);
            registry.register(module2);

            // when
            List<ModuleDefinition> modules = registry.getAllModules();

            // then
            assertEquals(2, modules.size());
        }

        @Test
        @DisplayName("没有注册模块时应该返回空列表")
        void shouldReturnEmptyListWhenNoModules() {
            // when
            List<ModuleDefinition> modules = registry.getAllModules();

            // then
            assertNotNull(modules);
            assertTrue(modules.isEmpty());
        }
    }

    @Nested
    @DisplayName("getSortedModules 测试")
    class GetSortedModulesTest {

        @Test
        @DisplayName("应该按依赖顺序返回模块")
        void shouldReturnModulesInDependencyOrder() {
            // given
            ModuleDefinition baseModule = TestDataFactory.createModuleDefinition("base", "Base Module");
            ModuleDefinition middleModule = TestDataFactory.createModuleDefinition("middle", "Middle Module", "base");
            ModuleDefinition topModule = TestDataFactory.createModuleDefinition("top", "Top Module", "middle");

            registry.register(baseModule);
            registry.register(middleModule);
            registry.register(topModule);

            // when
            List<ModuleDefinition> sorted = registry.getSortedModules();

            // then
            assertEquals(3, sorted.size());
            // base 应该在 middle 之前
            assertTrue(sorted.indexOf(baseModule) < sorted.indexOf(middleModule));
            // middle 应该在 top 之前
            assertTrue(sorted.indexOf(middleModule) < sorted.indexOf(topModule));
        }

        @Test
        @DisplayName("独立模块应该按ID排序")
        void independentModulesShouldBeSortedById() {
            // given
            ModuleDefinition moduleC = TestDataFactory.createModuleDefinition("module-c", "Module C");
            ModuleDefinition moduleA = TestDataFactory.createModuleDefinition("module-a", "Module A");
            ModuleDefinition moduleB = TestDataFactory.createModuleDefinition("module-b", "Module B");

            registry.register(moduleC);
            registry.register(moduleA);
            registry.register(moduleB);

            // when
            List<ModuleDefinition> sorted = registry.getSortedModules();

            // then
            assertEquals("module-a", sorted.get(0).id());
            assertEquals("module-b", sorted.get(1).id());
            assertEquals("module-c", sorted.get(2).id());
        }

        @Test
        @DisplayName("检测到循环依赖应该抛出异常")
        void shouldDetectCircularDependency() {
            // given - 创建循环依赖: A -> B -> C -> A
            ModuleDefinition moduleA = TestDataFactory.createModuleDefinition("module-a", "Module A", "module-c");
            ModuleDefinition moduleB = TestDataFactory.createModuleDefinition("module-b", "Module B", "module-a");
            ModuleDefinition moduleC = TestDataFactory.createModuleDefinition("module-c", "Module C", "module-b");

            // 先注册模块（跳过依赖检查）
            registry.registerWithoutDependencyCheck(moduleA);
            registry.registerWithoutDependencyCheck(moduleB);
            registry.registerWithoutDependencyCheck(moduleC);

            // when & then
            assertThrows(ModuleCircularDependencyException.class, () -> registry.getSortedModules());
        }
    }

    @Nested
    @DisplayName("containsModule 测试")
    class ContainsModuleTest {

        @Test
        @DisplayName("应该返回true当模块存在时")
        void shouldReturnTrueWhenModuleExists() {
            // given
            ModuleDefinition module = TestDataFactory.createModuleDefinition("module-1", "Module 1");
            registry.register(module);

            // when & then
            assertTrue(registry.containsModule("module-1"));
        }

        @Test
        @DisplayName("应该返回false当模块不存在时")
        void shouldReturnFalseWhenModuleNotExists() {
            // when & then
            assertFalse(registry.containsModule("non-existent"));
        }
    }

    @Nested
    @DisplayName("unregister 测试")
    class UnregisterTest {

        @Test
        @DisplayName("应该成功注销模块")
        void shouldUnregisterModule() {
            // given
            ModuleDefinition module = TestDataFactory.createModuleDefinition("module-1", "Module 1");
            registry.register(module);

            // when
            registry.unregister("module-1");

            // then
            assertFalse(registry.containsModule("module-1"));
        }

        @Test
        @DisplayName("注销被依赖的模块应该抛出异常")
        void shouldThrowExceptionWhenUnregisteringDependedModule() {
            // given
            ModuleDefinition baseModule = TestDataFactory.createModuleDefinition("base", "Base Module");
            ModuleDefinition dependentModule =
                    TestDataFactory.createModuleDefinition("dependent", "Dependent Module", "base");
            registry.register(baseModule);
            registry.register(dependentModule);

            // when & then
            IllegalStateException exception =
                    assertThrows(IllegalStateException.class, () -> registry.unregister("base"));
            assertTrue(exception.getMessage().contains("dependent"));
        }
    }

    @Nested
    @DisplayName("clear 测试")
    class ClearTest {

        @Test
        @DisplayName("应该清空所有模块")
        void shouldClearAllModules() {
            // given
            registry.register(TestDataFactory.createModuleDefinition("module-1", "Module 1"));
            registry.register(TestDataFactory.createModuleDefinition("module-2", "Module 2"));

            // when
            registry.clear();

            // then
            assertTrue(registry.getAllModules().isEmpty());
        }
    }
}
