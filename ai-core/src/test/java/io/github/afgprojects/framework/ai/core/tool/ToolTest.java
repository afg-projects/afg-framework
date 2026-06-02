package io.github.afgprojects.framework.ai.core.api.tool.

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link Tool} 接口测试
 */
@DisplayName("Tool")
class ToolTest {

    // ── 辅助方法 ───────────────────────────────────────────────────────────────

    private Tool<Map<String, Object>, String> createEchoTool() {
        return new Tool<>() {
            @Override
            public @NonNull String name() {
                return "echo";
            }

            @Override
            public @NonNull String description() {
                return "Echoes the input";
            }

            @Override
            public @NonNull String inputSchema() {
                return "{\"type\":\"object\",\"properties\":{\"message\":{\"type\":\"string\"}}}";
            }

            @Override
            public @NonNull String execute(Map<String, Object> input) {
                return String.valueOf(input.get("message"));
            }
        };
    }

    private Tool<Map<String, Object>, Double> createMathTool() {
        return new Tool<>() {
            @Override
            public @NonNull String name() {
                return "add";
            }

            @Override
            public @NonNull String description() {
                return "Adds two numbers";
            }

            @Override
            public @NonNull String inputSchema() {
                return "{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"number\"},\"b\":{\"type\":\"number\"}}}";
            }

            @Override
            public @NonNull Double execute(Map<String, Object> input) {
                double a = ((Number) input.get("a")).doubleValue();
                double b = ((Number) input.get("b")).doubleValue();
                return a + b;
            }
        };
    }

    // ── 基本属性 ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("基本属性")
    class BasicProperties {

        @Test
        @DisplayName("应返回正确的名称")
        void shouldReturnName() {
            Tool<?, ?> tool = createEchoTool();
            assertThat(tool.name()).isEqualTo("echo");
        }

        @Test
        @DisplayName("应返回正确的描述")
        void shouldReturnDescription() {
            Tool<?, ?> tool = createEchoTool();
            assertThat(tool.description()).isEqualTo("Echoes the input");
        }

        @Test
        @DisplayName("应返回正确的 inputSchema")
        void shouldReturnInputSchema() {
            Tool<?, ?> tool = createEchoTool();
            assertThat(tool.inputSchema()).isNotBlank();
        }
    }

    // ── 执行 ───────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("执行")
    class Execution {

        @Test
        @DisplayName("应正确执行 echo 工具")
        void shouldExecuteEcho() {
            Tool<Map<String, Object>, String> tool = createEchoTool();
            String result = tool.execute(Map.of("message", "hello"));
            assertThat(result).isEqualTo("hello");
        }

        @Test
        @DisplayName("应正确执行数学工具")
        void shouldExecuteMath() {
            Tool<Map<String, Object>, Double> tool = createMathTool();
            Double result = tool.execute(Map.of("a", 3, "b", 4));
            assertThat(result).isEqualTo(7.0);
        }

        @Test
        @DisplayName("inputType 默认返回 null")
        void shouldReturnNullInputTypeByDefault() {
            Tool<?, ?> tool = createEchoTool();
            assertThat(tool.inputType()).isNull();
        }
    }
}