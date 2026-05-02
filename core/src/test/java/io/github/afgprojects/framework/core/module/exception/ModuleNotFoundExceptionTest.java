package io.github.afgprojects.framework.core.module.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * ModuleNotFoundException 单元测试
 */
@DisplayName("ModuleNotFoundException 单元测试")
class ModuleNotFoundExceptionTest {

    @Nested
    @DisplayName("构造函数测试")
    class ConstructorTests {

        @Test
        @DisplayName("应该使用 moduleId 创建异常")
        void shouldCreateWithModuleId() {
            // when
            ModuleNotFoundException exception = new ModuleNotFoundException("missing-module");

            // then
            assertThat(exception.getModuleId()).isEqualTo("missing-module");
            assertThat(exception.getMessage()).contains("missing-module");
        }

        @Test
        @DisplayName("应该使用 moduleId 和 dependentModuleId 创建异常")
        void shouldCreateWithModuleIdAndDependentModuleId() {
            // when
            ModuleNotFoundException exception = new ModuleNotFoundException("missing-module", "dependent-module");

            // then
            assertThat(exception.getModuleId()).isEqualTo("missing-module");
            assertThat(exception.getMessage()).contains("missing-module");
            assertThat(exception.getMessage()).contains("dependent-module");
        }
    }
}