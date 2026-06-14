package io.github.afgprojects.framework.ai.core.workflow.node.logic;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.NodeOutput;
import io.github.afgprojects.framework.ai.core.workflow.engine.DefaultExecutionContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * SwitchNode 纯单元测试
 *
 * <p>测试多路分支节点的值匹配逻辑：
 * 直接值切换、上下文变量切换、大小写匹配、默认分支。
 * 不需要 Spring 上下文或外部依赖。
 */
@DisplayName("SwitchNode")
class SwitchNodeTest {

    private ExecutionContext createContext(Map<String, Object> variables) {
        return new DefaultExecutionContext("wf-1", "conv-1", "user-1", variables);
    }

    @Nested
    @DisplayName("基础属性")
    class BasicProperties {

        @Test
        @DisplayName("应返回正确的节点 ID 和类型")
        void shouldReturnNodeIdAndType() {
            SwitchNode node = new SwitchNode("switch-1");

            assertThat(node.getNodeId()).isEqualTo("switch-1");
            assertThat(node.getType()).isEqualTo("switch");
        }
    }

    @Nested
    @DisplayName("直接值切换")
    class DirectValueSwitch {

        @Test
        @DisplayName("value 参数直接匹配 case 时应返回对应 anchor")
        void shouldReturnMatchedCase_whenDirectValueMatches() {
            SwitchNode node = new SwitchNode("switch-1");
            ExecutionContext context = createContext(Map.of());

            NodeOutput output = node.execute(context, Map.of(
                "value", "A",
                "cases", Map.of("A", "case-a", "B", "case-b")
            ));

            assertThat(output.anchor()).isEqualTo("case-a");
            assertThat(output.data())
                .containsEntry("switchValue", "A")
                .containsEntry("matchedCase", "case-a");
        }

        @Test
        @DisplayName("value 不匹配任何 case 时应返回 defaultCase")
        void shouldReturnDefaultCase_whenValueDoesNotMatch() {
            SwitchNode node = new SwitchNode("switch-1");
            ExecutionContext context = createContext(Map.of());

            NodeOutput output = node.execute(context, Map.of(
                "value", "C",
                "cases", Map.of("A", "case-a", "B", "case-b")
            ));

            assertThat(output.anchor()).isEqualTo("default");
            assertThat(output.data()).containsEntry("matchedCase", "default");
        }

        @Test
        @DisplayName("value 不匹配且自定义 defaultCase 时应返回自定义默认")
        void shouldReturnCustomDefaultCase_whenValueDoesNotMatch() {
            SwitchNode node = new SwitchNode("switch-1");
            ExecutionContext context = createContext(Map.of());

            NodeOutput output = node.execute(context, Map.of(
                "value", "C",
                "cases", Map.of("A", "case-a", "B", "case-b"),
                "defaultCase", "fallback"
            ));

            assertThat(output.anchor()).isEqualTo("fallback");
        }
    }

    @Nested
    @DisplayName("上下文变量切换")
    class VariableSwitch {

        @Test
        @DisplayName("从上下文变量读取值并匹配 case")
        void shouldMatchCaseFromContextVariable() {
            SwitchNode node = new SwitchNode("switch-1");
            ExecutionContext context = createContext(Map.of("type", "premium"));

            NodeOutput output = node.execute(context, Map.of(
                "variable", "type",
                "cases", Map.of("premium", "premium-flow", "standard", "standard-flow")
            ));

            assertThat(output.anchor()).isEqualTo("premium-flow");
        }

        @Test
        @DisplayName("上下文变量不存在时应返回 defaultCase")
        void shouldReturnDefaultCase_whenContextVariableMissing() {
            SwitchNode node = new SwitchNode("switch-1");
            ExecutionContext context = createContext(Map.of());

            NodeOutput output = node.execute(context, Map.of(
                "variable", "type",
                "cases", Map.of("premium", "premium-flow")
            ));

            assertThat(output.anchor()).isEqualTo("default");
        }

        @Test
        @DisplayName("value 参数优先于 variable 参数")
        void shouldPreferValueOverVariable() {
            SwitchNode node = new SwitchNode("switch-1");
            ExecutionContext context = createContext(Map.of("type", "premium"));

            NodeOutput output = node.execute(context, Map.of(
                "value", "standard",
                "variable", "type",
                "cases", Map.of("premium", "premium-flow", "standard", "standard-flow")
            ));

            assertThat(output.anchor()).isEqualTo("standard-flow");
        }
    }

    @Nested
    @DisplayName("无匹配场景")
    class NoMatchScenario {

        @Test
        @DisplayName("无 cases 参数时应返回 default")
        void shouldReturnDefault_whenNoCasesProvided() {
            SwitchNode node = new SwitchNode("switch-1");
            ExecutionContext context = createContext(Map.of());

            NodeOutput output = node.execute(context, Map.of("value", "A"));

            assertThat(output.anchor()).isEqualTo("default");
        }

        @Test
        @DisplayName("无 value 和 variable 时应返回 default")
        void shouldReturnDefault_whenNoSwitchValueProvided() {
            SwitchNode node = new SwitchNode("switch-1");
            ExecutionContext context = createContext(Map.of());

            NodeOutput output = node.execute(context, Map.of(
                "cases", Map.of("A", "case-a")
            ));

            assertThat(output.anchor()).isEqualTo("default");
        }

        @Test
        @DisplayName("null 参数时 resolveSwitchValue 应返回错误输出或默认结果")
        void shouldHandleNullParams() {
            SwitchNode node = new SwitchNode("switch-1");
            ExecutionContext context = createContext(Map.of());

            // SwitchNode does not extend AbstractWorkflowNode; null params causes NPE
            // which is caught in the try-catch and returns error output
            NodeOutput output = node.execute(context, null);

            // Either the node handles null gracefully (returns data with matchedCase)
            // or it catches the exception and returns error data
            assertThat(output.data()).isNotNull();
        }
    }

    @Nested
    @DisplayName("输出属性")
    class OutputProperties {

        @Test
        @DisplayName("输出应包含耗时信息")
        void shouldIncludeDuration() {
            SwitchNode node = new SwitchNode("switch-1");
            ExecutionContext context = createContext(Map.of());

            NodeOutput output = node.execute(context, Map.of("value", "A"));

            assertThat(output.durationMs()).isGreaterThanOrEqualTo(0);
        }
    }
}
