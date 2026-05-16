package io.github.afgprojects.framework.ai.core.tool;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ToolCall 单元测试
 */
class ToolCallTest {

    @Test
    @DisplayName("创建 ToolCall")
    void create_success() {
        ToolCall call = new ToolCall("call-123", "test_tool", Map.of("arg1", "value1"));

        assertThat(call.id()).isEqualTo("call-123");
        assertThat(call.name()).isEqualTo("test_tool");
        assertThat(call.arguments()).containsEntry("arg1", "value1");
    }

    @Test
    @DisplayName("使用 of 方法创建（无参数）")
    void of_noArgs_success() {
        ToolCall call = ToolCall.of("call-1", "tool");

        assertThat(call.id()).isEqualTo("call-1");
        assertThat(call.name()).isEqualTo("tool");
        assertThat(call.arguments()).isEmpty();
    }

    @Test
    @DisplayName("使用 of 方法创建（单参数）")
    void of_singleArg_success() {
        ToolCall call = ToolCall.of("call-1", "tool", "key", "value");

        assertThat(call.id()).isEqualTo("call-1");
        assertThat(call.name()).isEqualTo("tool");
        assertThat(call.arguments()).containsEntry("key", "value");
    }

    @Test
    @DisplayName("空 ID 抛出异常")
    void nullId_throwsException() {
        assertThatThrownBy(() -> new ToolCall(null, "tool", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id cannot be null");
    }

    @Test
    @DisplayName("空白 ID 抛出异常")
    void blankId_throwsException() {
        assertThatThrownBy(() -> new ToolCall("  ", "tool", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id cannot be null");
    }

    @Test
    @DisplayName("空名称抛出异常")
    void nullName_throwsException() {
        assertThatThrownBy(() -> new ToolCall("id", null, Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null");
    }

    @Test
    @DisplayName("空白名称抛出异常")
    void blankName_throwsException() {
        assertThatThrownBy(() -> new ToolCall("id", "  ", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null");
    }

    @Test
    @DisplayName("空参数默认为空 Map")
    void nullArguments_defaultsToEmptyMap() {
        ToolCall call = new ToolCall("id", "tool", null);

        assertThat(call.arguments()).isNotNull().isEmpty();
    }
}