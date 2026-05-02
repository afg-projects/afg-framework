package io.github.afgprojects.framework.core.trace;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * ExceptionLogLevel 测试
 */
@DisplayName("ExceptionLogLevel 测试")
class ExceptionLogLevelTest {

    @Test
    @DisplayName("应该包含所有日志级别")
    void shouldContainAllLevels() {
        ExceptionLogLevel[] levels = ExceptionLogLevel.values();

        assertThat(levels).hasSize(3);
        assertThat(levels).contains(
                ExceptionLogLevel.NONE,
                ExceptionLogLevel.MESSAGE,
                ExceptionLogLevel.STACK_TRACE
        );
    }

    @Test
    @DisplayName("应该正确获取枚举名称")
    void shouldGetName() {
        assertThat(ExceptionLogLevel.NONE.name()).isEqualTo("NONE");
        assertThat(ExceptionLogLevel.MESSAGE.name()).isEqualTo("MESSAGE");
        assertThat(ExceptionLogLevel.STACK_TRACE.name()).isEqualTo("STACK_TRACE");
    }
}
