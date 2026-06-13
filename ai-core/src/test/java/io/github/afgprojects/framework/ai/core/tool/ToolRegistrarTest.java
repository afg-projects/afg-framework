package io.github.afgprojects.framework.ai.core.tool;

import io.github.afgprojects.framework.ai.core.api.tool.Tool;
import io.github.afgprojects.framework.ai.core.api.tool.ToolExecutionException;
import io.github.afgprojects.framework.ai.core.api.tool.ToolRegistry;
import io.github.afgprojects.framework.ai.core.tool.annotation.ToolParam;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * ToolRegistrar 纯单元测试。
 *
 * <p>使用真实的 DefaultToolRegistry 进行测试，不使用 mock。
 */
@DisplayName("ToolRegistrar")
class ToolRegistrarTest {

    private final DefaultToolRegistry registry = new DefaultToolRegistry();
    private final ToolRegistrar registrar = new ToolRegistrar(registry);

    // ── 测试用 Bean 类 ──────────────────────────────────────────────────────────

    @org.springframework.stereotype.Service
    static class TestToolService {

        @io.github.afgprojects.framework.ai.core.tool.annotation.Tool(name = "weather", description = "查询天气")
        public String getWeather(@ToolParam(name = "city", description = "城市名称") String city) {
            return "Weather in " + city + ": sunny";
        }

        @io.github.afgprojects.framework.ai.core.tool.annotation.Tool(name = "calculate", description = "数学计算")
        public Integer calculate(@ToolParam(name = "a", description = "参数A") Integer a,
                                 @ToolParam(name = "b", description = "参数B", required = false, defaultValue = "0") Integer b) {
            return a + b;
        }

        @io.github.afgprojects.framework.ai.core.tool.annotation.Tool(name = "no-param-tool", description = "无参数工具")
        public String noParamMethod() {
            return "no params";
        }

        // 无 @Tool 注解的方法，应被忽略
        public String normalMethod() {
            return "normal";
        }
    }

    @org.springframework.stereotype.Service
    static class ExceptionToolService {

        @io.github.afgprojects.framework.ai.core.tool.annotation.Tool(name = "failing-tool", description = "总是失败的工具")
        public String alwaysFail() {
            throw new RuntimeException("Tool execution failed intentionally");
        }
    }

    // ── 测试 ────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("scanAndRegister")
    class ScanAndRegister {

        @Test
        @DisplayName("应扫描 @Tool 注解方法并注册到 ToolRegistry")
        void shouldRegisterToolMethods_whenScanningBeanWithToolAnnotations() {
            TestToolService bean = new TestToolService();

            int count = registrar.scanAndRegister(bean);

            assertThat(count).isEqualTo(3);
            assertThat(registry.exists("weather")).isTrue();
            assertThat(registry.exists("calculate")).isTrue();
            assertThat(registry.exists("no-param-tool")).isTrue();
        }

        @Test
        @DisplayName("应忽略无 @Tool 注解的方法")
        void shouldIgnoreMethodsWithoutToolAnnotation() {
            TestToolService bean = new TestToolService();

            int count = registrar.scanAndRegister(bean);

            assertThat(registry.getTool("normalMethod")).isEmpty();
        }

        @Test
        @DisplayName("无 @Tool 注解的 Bean 应注册 0 个 Tool")
        void shouldRegisterZeroTools_whenBeanHasNoToolAnnotation() {
            Object bean = new Object();

            int count = registrar.scanAndRegister(bean);

            assertThat(count).isZero();
        }
    }

    @Nested
    @DisplayName("AnnotatedMethodTool - name and description")
    class NameAndDescription {

        @Test
        @DisplayName("应使用注解的 name 作为 Tool 名称")
        void shouldUseAnnotationNameAsToolName() {
            TestToolService bean = new TestToolService();
            registrar.scanAndRegister(bean);

            Tool<?, ?> tool = registry.getTool("weather").orElseThrow();
            assertThat(tool.name()).isEqualTo("weather");
        }

        @Test
        @DisplayName("应使用注解的 description 作为 Tool 描述")
        void shouldUseAnnotationDescriptionAsToolDescription() {
            TestToolService bean = new TestToolService();
            registrar.scanAndRegister(bean);

            Tool<?, ?> tool = registry.getTool("weather").orElseThrow();
            assertThat(tool.description()).isEqualTo("查询天气");
        }
    }

    @Nested
    @DisplayName("AnnotatedMethodTool - inputSchema")
    class InputSchema {

        @Test
        @DisplayName("应从 @ToolParam 构建 JSON Schema")
        void shouldBuildJsonSchemaFromToolParam() {
            TestToolService bean = new TestToolService();
            registrar.scanAndRegister(bean);

            Tool<?, ?> tool = registry.getTool("weather").orElseThrow();
            String schema = tool.inputSchema();

            assertThat(schema).contains("\"city\"");
            assertThat(schema).contains("\"type\":\"string\"");
            assertThat(schema).contains("\"description\":\"城市名称\"");
            assertThat(schema).contains("\"required\"");
        }

        @Test
        @DisplayName("应标记 required 参数和非 required 参数")
        void shouldMarkRequiredAndOptionalParameters() {
            TestToolService bean = new TestToolService();
            registrar.scanAndRegister(bean);

            Tool<?, ?> tool = registry.getTool("calculate").orElseThrow();
            String schema = tool.inputSchema();

            assertThat(schema).contains("\"a\"");
            assertThat(schema).contains("\"b\"");
            assertThat(schema).contains("\"required\"");
        }

        @Test
        @DisplayName("无参数方法应返回空 JSON Schema")
        void shouldReturnEmptySchema_whenNoParams() {
            TestToolService bean = new TestToolService();
            registrar.scanAndRegister(bean);

            Tool<?, ?> tool = registry.getTool("no-param-tool").orElseThrow();
            assertThat(tool.inputSchema()).isEqualTo("{}");
        }
    }

    @Nested
    @DisplayName("AnnotatedMethodTool - execute")
    class Execute {

        @Test
        @DisplayName("应正确执行 @Tool 方法并返回结果")
        void shouldExecuteToolMethodAndReturnResult() {
            TestToolService bean = new TestToolService();
            registrar.scanAndRegister(bean);

            Tool<?, ?> tool = registry.getTool("weather").orElseThrow();

            @SuppressWarnings("unchecked")
            Tool<Map<String, Object>, Object> typedTool = (Tool<Map<String, Object>, Object>) tool;
            Object result = typedTool.execute(Map.of("city", "北京"));

            assertThat(result).isEqualTo("Weather in 北京: sunny");
        }

        @Test
        @DisplayName("应正确执行带多个参数的 @Tool 方法")
        void shouldExecuteToolWithMultipleParameters() {
            TestToolService bean = new TestToolService();
            registrar.scanAndRegister(bean);

            Tool<?, ?> tool = registry.getTool("calculate").orElseThrow();

            @SuppressWarnings("unchecked")
            Tool<Map<String, Object>, Object> typedTool = (Tool<Map<String, Object>, Object>) tool;
            Object result = typedTool.execute(Map.of("a", 5, "b", 3));

            assertThat(result).isEqualTo(8);
        }

        @Test
        @DisplayName("应使用 defaultValue 填充缺失的可选参数")
        void shouldUseDefaultValueForMissingOptionalParameter() {
            TestToolService bean = new TestToolService();
            registrar.scanAndRegister(bean);

            Tool<?, ?> tool = registry.getTool("calculate").orElseThrow();

            @SuppressWarnings("unchecked")
            Tool<Map<String, Object>, Object> typedTool = (Tool<Map<String, Object>, Object>) tool;
            Object result = typedTool.execute(Map.of("a", 10));

            assertThat(result).isEqualTo(10);
        }

        @Test
        @DisplayName("应正确执行无参数的 @Tool 方法")
        void shouldExecuteToolWithNoParameters() {
            TestToolService bean = new TestToolService();
            registrar.scanAndRegister(bean);

            Tool<?, ?> tool = registry.getTool("no-param-tool").orElseThrow();

            @SuppressWarnings("unchecked")
            Tool<Map<String, Object>, Object> typedTool = (Tool<Map<String, Object>, Object>) tool;
            Object result = typedTool.execute(Map.of());

            assertThat(result).isEqualTo("no params");
        }

        @Test
        @DisplayName("执行失败时应抛出 ToolExecutionException")
        void shouldThrowToolExecutionException_whenExecutionFails() {
            ExceptionToolService bean = new ExceptionToolService();
            registrar.scanAndRegister(bean);

            Tool<?, ?> tool = registry.getTool("failing-tool").orElseThrow();

            @SuppressWarnings("unchecked")
            Tool<Map<String, Object>, Object> typedTool = (Tool<Map<String, Object>, Object>) tool;

            assertThatThrownBy(() -> typedTool.execute(Map.of()))
                    .isInstanceOf(ToolExecutionException.class)
                    .hasMessageContaining("failing-tool")
                    .hasMessageContaining("failed");
        }

        @Test
        @DisplayName("应进行参数类型转换")
        void shouldConvertParameterTypes() {
            TestToolService bean = new TestToolService();
            registrar.scanAndRegister(bean);

            Tool<?, ?> tool = registry.getTool("calculate").orElseThrow();

            @SuppressWarnings("unchecked")
            Tool<Map<String, Object>, Object> typedTool = (Tool<Map<String, Object>, Object>) tool;
            // 传入 String 类型的数字参数，应转换为 Integer
            Object result = typedTool.execute(Map.of("a", "7", "b", "3"));

            assertThat(result).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("重复注册")
    class DuplicateRegistration {

        @Test
        @DisplayName("重复注册同名的 Tool 应替换")
        void shouldReplaceDuplicateTool() {
            TestToolService bean = new TestToolService();
            registrar.scanAndRegister(bean);

            assertThat(registry.exists("weather")).isTrue();
            assertThat(registry.size()).isEqualTo(3);

            // 再次扫描同一个 bean
            registrar.scanAndRegister(bean);

            assertThat(registry.exists("weather")).isTrue();
            assertThat(registry.size()).isEqualTo(3);
        }
    }
}
