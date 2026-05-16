// ai-core/src/test/java/io/github/afgprojects/framework/ai/core/tool/ToolTest.java
package io.github.afgprojects.framework.ai.core.tool;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ToolTest {

    @Test
    void tool_shouldHaveNameAndDescription() {
        Tool<String, String> tool = new Tool<>() {
            @Override
            public String name() {
                return "test_tool";
            }

            @Override
            public String description() {
                return "A test tool";
            }

            @Override
            public String execute(String input) {
                return "result: " + input;
            }
        };

        assertEquals("test_tool", tool.name());
        assertEquals("A test tool", tool.description());
        assertEquals("result: hello", tool.execute("hello"));
    }

    @Test
    void toolCall_shouldCreateWithAllFields() {
        ToolCall toolCall = new ToolCall(
            "call-123",
            "test_tool",
            java.util.Map.of("arg", "value")
        );

        assertEquals("call-123", toolCall.id());
        assertEquals("test_tool", toolCall.name());
        assertEquals("value", toolCall.arguments().get("arg"));
    }

    @Test
    void toolResult_shouldCreateWithAllFields() {
        ToolResult result = new ToolResult(
            "call-123",
            "test_tool",
            "tool output",
            null
        );

        assertEquals("call-123", result.toolCallId());
        assertEquals("test_tool", result.toolName());
        assertEquals("tool output", result.output());
        assertNull(result.error());
    }

    @Test
    void tool_shouldHaveDefaultInputSchema() {
        Tool<String, String> tool = new Tool<>() {
            @Override
            public String name() {
                return "test_tool";
            }

            @Override
            public String description() {
                return "A test tool";
            }

            @Override
            public String execute(String input) {
                return input;
            }
        };

        assertEquals("{}", tool.inputSchema());
    }

    @Test
    void toolCall_shouldCreateWithEmptyArguments() {
        ToolCall toolCall = ToolCall.of("call-456", "simple_tool");

        assertEquals("call-456", toolCall.id());
        assertEquals("simple_tool", toolCall.name());
        assertTrue(toolCall.arguments().isEmpty());
    }

    @Test
    void toolCall_shouldCreateWithSingleArgument() {
        ToolCall toolCall = ToolCall.of("call-789", "param_tool", "key", "value");

        assertEquals("call-789", toolCall.id());
        assertEquals("param_tool", toolCall.name());
        assertEquals("value", toolCall.arguments().get("key"));
    }

    @Test
    void toolCall_shouldRejectNullId() {
        assertThrows(IllegalArgumentException.class, () ->
            new ToolCall(null, "test_tool", java.util.Map.of())
        );
    }

    @Test
    void toolCall_shouldRejectBlankId() {
        assertThrows(IllegalArgumentException.class, () ->
            new ToolCall("", "test_tool", java.util.Map.of())
        );
    }

    @Test
    void toolCall_shouldRejectNullName() {
        assertThrows(IllegalArgumentException.class, () ->
            new ToolCall("call-123", null, java.util.Map.of())
        );
    }

    @Test
    void toolResult_shouldCreateSuccess() {
        ToolResult result = ToolResult.success("call-123", "test_tool", "output");

        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
        assertEquals("output", result.output());
        assertNull(result.error());
    }

    @Test
    void toolResult_shouldCreateFailure() {
        ToolResult result = ToolResult.failure("call-123", "test_tool", "error message");

        assertFalse(result.isSuccess());
        assertTrue(result.isFailure());
        assertNull(result.output());
        assertEquals("error message", result.error());
    }

    @Test
    void toolDefinition_shouldCreateFromTool() {
        Tool<String, String> tool = new Tool<>() {
            @Override
            public String name() {
                return "test_tool";
            }

            @Override
            public String description() {
                return "A test tool";
            }

            @Override
            public String execute(String input) {
                return input;
            }
        };

        ToolDefinition definition = ToolDefinition.from(tool);

        assertEquals("test_tool", definition.name());
        assertEquals("A test tool", definition.description());
        assertEquals("{}", definition.inputSchema());
    }

    @Test
    void toolDefinition_shouldCreateWithEmptySchema() {
        ToolDefinition definition = ToolDefinition.of("my_tool", "My description");

        assertEquals("my_tool", definition.name());
        assertEquals("My description", definition.description());
        assertEquals("{}", definition.inputSchema());
    }

    @Test
    void toolDefinition_shouldRejectNullName() {
        assertThrows(IllegalArgumentException.class, () ->
            new ToolDefinition(null, "description", "{}")
        );
    }

    @Test
    void toolExecutionException_shouldCreateWithMessage() {
        ToolExecutionException ex = new ToolExecutionException("Tool failed");

        assertEquals("Tool failed", ex.getMessage());
    }

    @Test
    void toolExecutionException_shouldCreateWithMessageAndCause() {
        RuntimeException cause = new RuntimeException("Underlying error");
        ToolExecutionException ex = new ToolExecutionException("Tool failed", cause);

        assertEquals("Tool failed", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }
}
