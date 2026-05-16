package io.github.afgprojects.framework.ai.core.tool;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ToolResult 单元测试
 */
class ToolResultTest {

    @Test
    @DisplayName("创建成功结果")
    void create_success() {
        ToolResult result = new ToolResult("call-1", "test_tool", "output", null);

        assertThat(result.toolCallId()).isEqualTo("call-1");
        assertThat(result.toolName()).isEqualTo("test_tool");
        assertThat(result.output()).isEqualTo("output");
        assertThat(result.error()).isNull();
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("创建错误结果")
    void create_error() {
        ToolResult result = new ToolResult("call-1", "test_tool", null, "Error message");

        assertThat(result.toolCallId()).isEqualTo("call-1");
        assertThat(result.toolName()).isEqualTo("test_tool");
        assertThat(result.output()).isNull();
        assertThat(result.error()).isEqualTo("Error message");
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("错误优先于输出判断成功")
    void isSuccess_errorTakesPrecedence() {
        ToolResult result = new ToolResult("call-1", "tool", "output", "error");

        // 有错误时，即使有输出也算失败
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("使用静态工厂方法创建成功结果")
    void staticSuccess() {
        ToolResult result = ToolResult.success("call-1", "tool", "output");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.output()).isEqualTo("output");
    }

    @Test
    @DisplayName("使用静态工厂方法创建失败结果")
    void staticFailure() {
        ToolResult result = ToolResult.failure("call-1", "tool", "error");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.error()).isEqualTo("error");
    }

    @Test
    @DisplayName("检查失败状态")
    void isFailure_returnsCorrectResult() {
        ToolResult success = ToolResult.success("id", "tool", "output");
        ToolResult failure = ToolResult.failure("id", "tool", "error");

        assertThat(success.isFailure()).isFalse();
        assertThat(failure.isFailure()).isTrue();
    }
}