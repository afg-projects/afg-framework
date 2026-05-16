package io.github.afgprojects.framework.ai.core.memory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Message 单元测试
 */
class MessageTest {

    @Test
    @DisplayName("创建用户消息")
    void user() {
        Message msg = Message.user("Hello");

        assertThat(msg.role()).isEqualTo(Message.Role.USER);
        assertThat(msg.content()).isEqualTo("Hello");
    }

    @Test
    @DisplayName("创建助手消息")
    void assistant() {
        Message msg = Message.assistant("Hi there!");

        assertThat(msg.role()).isEqualTo(Message.Role.ASSISTANT);
        assertThat(msg.content()).isEqualTo("Hi there!");
    }

    @Test
    @DisplayName("创建系统消息")
    void system() {
        Message msg = Message.system("You are helpful");

        assertThat(msg.role()).isEqualTo(Message.Role.SYSTEM);
        assertThat(msg.content()).isEqualTo("You are helpful");
    }

    @Test
    @DisplayName("创建带工具调用的助手消息")
    void assistantWithTools() {
        var toolCall = new io.github.afgprojects.framework.ai.core.tool.ToolCall(
                "call-1", "test_tool", Map.of("arg", "value")
        );
        Message msg = Message.assistantWithTools("Thinking...", List.of(toolCall));

        assertThat(msg.role()).isEqualTo(Message.Role.ASSISTANT);
        assertThat(msg.toolCalls()).hasSize(1);
    }

    @Test
    @DisplayName("创建工具结果消息")
    void tool() {
        var toolResult = new io.github.afgprojects.framework.ai.core.tool.ToolResult(
                "call-1", "test_tool", "result", null
        );
        Message msg = Message.tool(null, List.of(toolResult));

        assertThat(msg.role()).isEqualTo(Message.Role.TOOL);
        assertThat(msg.toolResults()).hasSize(1);
    }

    @Test
    @DisplayName("角色检查")
    void roleCheck() {
        Message user = Message.user("Hello");
        Message assistant = Message.assistant("Hi");
        Message system = Message.system("System");

        assertThat(user.role()).isEqualTo(Message.Role.USER);
        assertThat(assistant.role()).isEqualTo(Message.Role.ASSISTANT);
        assertThat(system.role()).isEqualTo(Message.Role.SYSTEM);
    }
}