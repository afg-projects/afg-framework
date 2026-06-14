package io.github.afgprojects.framework.ai.core.workflow.node.tool;

import io.github.afgprojects.framework.ai.core.api.tool.Tool;
import io.github.afgprojects.framework.ai.core.api.tool.ToolRegistry;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.NodeOutput;
import io.github.afgprojects.framework.ai.core.tool.DefaultToolRegistry;
import io.github.afgprojects.framework.ai.core.workflow.engine.DefaultExecutionContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * ToolNode 纯单元测试
 *
 * <p>测试工具执行节点的各种场景：
 * 无 ToolRegistry 降级、工具调用、工具不存在、参数校验。
 * 使用真实的 DefaultToolRegistry 和简单 Tool 实现，不使用 mock。
 */
@DisplayName("ToolNode")
class ToolNodeTest {

    private ExecutionContext createContext() {
        return new DefaultExecutionContext("wf-1", "conv-1", "user-1");
    }

    /**
     * 简单的测试工具实现 — 字符串大写转换。
     */
    private static class UpperCaseTool implements Tool<String, String> {
        @Override
        public String name() { return "toUpperCase"; }

        @Override
        public String description() { return "将字符串转为大写"; }

        @Override
        public String execute(String input) {
            return input != null ? input.toUpperCase() : null;
        }
    }

    /**
     * 抛出异常的工具实现 — 用于测试工具执行失败场景。
     */
    private static class FailingTool implements Tool<String, String> {
        @Override
        public String name() { return "failingTool"; }

        @Override
        public String description() { return "总是失败的工具"; }

        @Override
        public String execute(String input) {
            throw new RuntimeException("Tool execution failed intentionally");
        }
    }

    @Nested
    @DisplayName("基础属性")
    class BasicProperties {

        @Test
        @DisplayName("应返回正确的节点 ID 和类型")
        void shouldReturnNodeIdAndType() {
            ToolNode node = new ToolNode("tool-1");

            assertThat(node.getNodeId()).isEqualTo("tool-1");
            assertThat(node.getType()).isEqualTo("tool");
        }
    }

    @Nested
    @DisplayName("降级行为：无 ToolRegistry")
    class NoRegistryFallback {

        @Test
        @DisplayName("toolRegistry 为 null 时应返回错误说明工具未配置")
        void shouldReturnErrorOutput_whenNoToolRegistry() {
            ToolNode node = new ToolNode("tool-1");
            ExecutionContext context = createContext();

            NodeOutput output = node.execute(context, Map.of("toolName", "toUpperCase"));

            assertThat(output.data()).containsEntry("toolName", "toUpperCase");
            assertThat(output.data()).containsEntry("error", "No ToolRegistry available - tool invocation not configured");
        }

        @Test
        @DisplayName("null ToolRegistry 构造也应有相同降级行为")
        void shouldReturnErrorOutput_whenNullToolRegistry() {
            ToolNode node = new ToolNode("tool-1", null);
            ExecutionContext context = createContext();

            NodeOutput output = node.execute(context, Map.of("toolName", "toUpperCase"));

            assertThat(output.data()).containsKey("error");
        }
    }

    @Nested
    @DisplayName("工具调用")
    class ToolInvocation {

        @Test
        @DisplayName("应正确调用注册的工具并返回结果")
        void shouldInvokeRegisteredTool() {
            ToolRegistry registry = new DefaultToolRegistry();
            registry.register(new UpperCaseTool());
            ToolNode node = new ToolNode("tool-1", registry);
            ExecutionContext context = createContext();

            NodeOutput output = node.execute(context, Map.of(
                "toolName", "toUpperCase",
                "toolInput", "hello"
            ));

            assertThat(output.data()).containsEntry("toolName", "toUpperCase");
            assertThat(output.data()).containsEntry("result", "HELLO");
        }

        @Test
        @DisplayName("工具不存在时应返回错误输出")
        void shouldReturnErrorOutput_whenToolNotFound() {
            ToolRegistry registry = new DefaultToolRegistry();
            ToolNode node = new ToolNode("tool-1", registry);
            ExecutionContext context = createContext();

            NodeOutput output = node.execute(context, Map.of("toolName", "nonExistentTool"));

            // AbstractWorkflowNode catches the IllegalArgumentException
            assertThat(output.data()).containsKey("error");
        }

        @Test
        @DisplayName("工具执行异常时应返回错误信息")
        void shouldReturnErrorInfo_whenToolExecutionFails() {
            ToolRegistry registry = new DefaultToolRegistry();
            registry.register(new FailingTool());
            ToolNode node = new ToolNode("tool-1", registry);
            ExecutionContext context = createContext();

            NodeOutput output = node.execute(context, Map.of(
                "toolName", "failingTool",
                "toolInput", "test"
            ));

            assertThat(output.data()).containsEntry("toolName", "failingTool");
            assertThat(output.data()).containsEntry("error", "Tool execution failed intentionally");
        }
    }

    @Nested
    @DisplayName("参数校验")
    class ParameterValidation {

        @Test
        @DisplayName("缺少 toolName 参数时应返回错误输出")
        void shouldReturnErrorOutput_whenToolNameMissing() {
            ToolNode node = new ToolNode("tool-1");
            ExecutionContext context = createContext();

            NodeOutput output = node.execute(context, Map.of());

            assertThat(output.data()).containsKey("error");
        }

        @Test
        @DisplayName("null 参数应视为空 map 处理")
        void shouldHandleNullParams() {
            ToolNode node = new ToolNode("tool-1");
            ExecutionContext context = createContext();

            NodeOutput output = node.execute(context, null);

            assertThat(output.data()).containsKey("error");
        }
    }
}
