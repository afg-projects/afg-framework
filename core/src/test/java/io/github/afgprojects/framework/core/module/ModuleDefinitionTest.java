package io.github.afgprojects.framework.core.module;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.support.BaseUnitTest;
import io.github.afgprojects.framework.core.support.TestDataFactory;

/**
 * ModuleDefinition 单元测试
 */
@DisplayName("ModuleDefinition 测试")
class ModuleDefinitionTest extends BaseUnitTest {

    @Nested
    @DisplayName("Builder 测试")
    class BuilderTest {

        @Test
        @DisplayName("应该成功创建 ModuleDefinition")
        void shouldCreateModuleDefinition() {
            // when
            ModuleDefinition definition = ModuleDefinition.builder()
                    .id("test-module")
                    .name("Test Module")
                    .build();

            // then
            assertNotNull(definition);
            assertEquals("test-module", definition.id());
            assertEquals("Test Module", definition.name());
            assertTrue(definition.dependencies().isEmpty());
            assertNull(definition.moduleInstance());
        }

        @Test
        @DisplayName("应该正确设置依赖列表")
        void shouldSetDependencies() {
            // given
            List<String> dependencies = Arrays.asList("module-a", "module-b");

            // when
            ModuleDefinition definition = ModuleDefinition.builder()
                    .id("test-module")
                    .name("Test Module")
                    .dependencies(dependencies)
                    .build();

            // then
            assertEquals(2, definition.dependencies().size());
            assertTrue(definition.dependencies().contains("module-a"));
            assertTrue(definition.dependencies().contains("module-b"));
        }

        @Test
        @DisplayName("应该正确设置模块实例")
        void shouldSetModuleInstance() {
            // given
            AfgModule module = TestDataFactory.createMockModule("test-module");

            // when
            ModuleDefinition definition = ModuleDefinition.builder()
                    .id("test-module")
                    .name("Test Module")
                    .moduleInstance(module)
                    .build();

            // then
            assertNotNull(definition.moduleInstance());
            assertEquals(module, definition.moduleInstance());
        }

        @Test
        @DisplayName("应该支持链式调用")
        void shouldSupportChainedCalls() {
            // when
            ModuleDefinition definition = ModuleDefinition.builder()
                    .id("test-module")
                    .name("Test Module")
                    .dependencies(Arrays.asList("dep1", "dep2"))
                    .moduleInstance(TestDataFactory.createMockModule("test-module"))
                    .build();

            // then
            assertNotNull(definition);
            assertEquals("test-module", definition.id());
        }
    }

    @Nested
    @DisplayName("Getter 测试")
    class GetterTest {

        @Test
        @DisplayName("getId 应该返回正确的ID")
        void getIdShouldReturnCorrectId() {
            // given
            ModuleDefinition definition = TestDataFactory.createModuleDefinition("my-module", "My Module");

            // when & then
            assertEquals("my-module", definition.id());
        }

        @Test
        @DisplayName("getName 应该返回正确的名称")
        void getNameShouldReturnCorrectName() {
            // given
            ModuleDefinition definition = TestDataFactory.createModuleDefinition("my-module", "My Module");

            // when & then
            assertEquals("My Module", definition.name());
        }

        @Test
        @DisplayName("getDependencies 应该返回正确的依赖列表")
        void getDependenciesShouldReturnCorrectDependencies() {
            // given
            ModuleDefinition definition =
                    TestDataFactory.createModuleDefinition("my-module", "My Module", "dep1", "dep2");

            // when
            List<String> dependencies = definition.dependencies();

            // then
            assertEquals(2, dependencies.size());
            assertTrue(dependencies.contains("dep1"));
            assertTrue(dependencies.contains("dep2"));
        }

        @Test
        @DisplayName("无依赖时 getDependencies 应该返回空列表")
        void getDependenciesShouldReturnEmptyListWhenNoDependencies() {
            // given
            ModuleDefinition definition = TestDataFactory.createModuleDefinition("my-module", "My Module");

            // when & then
            assertTrue(definition.dependencies().isEmpty());
        }
    }

    @Nested
    @DisplayName("equals 和 hashCode 测试")
    class EqualsAndHashCodeTest {

        @Test
        @DisplayName("相同ID的 ModuleDefinition 应该相等")
        void shouldBeEqualForSameId() {
            // given
            ModuleDefinition def1 =
                    ModuleDefinition.builder().id("module-1").name("Module 1").build();
            ModuleDefinition def2 =
                    ModuleDefinition.builder().id("module-1").name("Module 2").build();

            // when & then
            assertEquals(def1, def2);
            assertEquals(def1.hashCode(), def2.hashCode());
        }

        @Test
        @DisplayName("不同ID的 ModuleDefinition 应该不相等")
        void shouldNotBeEqualForDifferentId() {
            // given
            ModuleDefinition def1 =
                    ModuleDefinition.builder().id("module-1").name("Module 1").build();
            ModuleDefinition def2 =
                    ModuleDefinition.builder().id("module-2").name("Module 1").build();

            // when & then
            assertNotEquals(def1, def2);
        }

        @Test
        @DisplayName("应该与自身相等")
        void shouldBeEqualToItself() {
            // given
            ModuleDefinition definition =
                    ModuleDefinition.builder().id("module-1").name("Module 1").build();

            // when & then
            assertEquals(definition, definition);
        }

        @Test
        @DisplayName("不应该与null相等")
        void shouldNotBeEqualToNull() {
            // given
            ModuleDefinition definition =
                    ModuleDefinition.builder().id("module-1").name("Module 1").build();

            // when & then
            assertNotEquals(definition, null);
        }

        @Test
        @DisplayName("不应该与不同类型的对象相等")
        void shouldNotBeEqualToDifferentType() {
            // given
            ModuleDefinition definition =
                    ModuleDefinition.builder().id("module-1").name("Module 1").build();

            // when & then
            assertNotEquals(definition, "module-1");
        }
    }

    @Nested
    @DisplayName("toString 测试")
    class ToStringTest {

        @Test
        @DisplayName("toString 应该包含关键信息")
        void toStringShouldContainKeyInfo() {
            // given
            ModuleDefinition definition = ModuleDefinition.builder()
                    .id("test-module")
                    .name("Test Module")
                    .dependencies(Arrays.asList("dep1", "dep2"))
                    .build();

            // when
            String result = definition.toString();

            // then
            assertTrue(result.contains("test-module"));
            assertTrue(result.contains("Test Module"));
        }
    }
}
