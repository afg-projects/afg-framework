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
 * ConditionNode 纯单元测试
 *
 * <p>测试条件分支节点的各种条件评估策略：
 * Predicate 条件、变量比较、表达式求值、默认行为。
 * 不需要 Spring 上下文或外部依赖。
 */
@DisplayName("ConditionNode")
class ConditionNodeTest {

    private ExecutionContext createContext(Map<String, Object> variables) {
        return new DefaultExecutionContext("wf-1", "conv-1", "user-1", variables);
    }

    @Nested
    @DisplayName("基础属性")
    class BasicProperties {

        @Test
        @DisplayName("应返回正确的节点 ID 和类型")
        void shouldReturnNodeIdAndType() {
            ConditionNode node = new ConditionNode("cond-1");

            assertThat(node.getNodeId()).isEqualTo("cond-1");
            assertThat(node.getType()).isEqualTo("condition");
        }
    }

    @Nested
    @DisplayName("Predicate 条件评估")
    class PredicateCondition {

        @Test
        @DisplayName("Predicate 返回 true 时 anchor 应为 true")
        void shouldReturnTrueAnchor_whenPredicateReturnsTrue() {
            ConditionNode node = new ConditionNode("cond-1", ctx -> true);
            ExecutionContext context = createContext(Map.of());

            NodeOutput output = node.execute(context, Map.of());

            assertThat(output.anchor()).isEqualTo("true");
            assertThat(output.data()).containsEntry("result", true);
        }

        @Test
        @DisplayName("Predicate 返回 false 时 anchor 应为 false")
        void shouldReturnFalseAnchor_whenPredicateReturnsFalse() {
            ConditionNode node = new ConditionNode("cond-1", ctx -> false);
            ExecutionContext context = createContext(Map.of());

            NodeOutput output = node.execute(context, Map.of());

            assertThat(output.anchor()).isEqualTo("false");
            assertThat(output.data()).containsEntry("result", false);
        }

        @Test
        @DisplayName("Predicate 应能访问上下文变量")
        void shouldAccessContextVariables_whenUsingPredicate() {
            ConditionNode node = new ConditionNode("cond-1",
                ctx -> "admin".equals(ctx.getVariables().get("role")));
            ExecutionContext context = createContext(Map.of("role", "admin"));

            NodeOutput output = node.execute(context, Map.of());

            assertThat(output.anchor()).isEqualTo("true");
        }

        @Test
        @DisplayName("Predicate 返回 false 当变量不匹配")
        void shouldReturnFalse_whenVariableDoesNotMatch() {
            ConditionNode node = new ConditionNode("cond-1",
                ctx -> "admin".equals(ctx.getVariables().get("role")));
            ExecutionContext context = createContext(Map.of("role", "user"));

            NodeOutput output = node.execute(context, Map.of());

            assertThat(output.anchor()).isEqualTo("false");
        }
    }

    @Nested
    @DisplayName("变量比较评估")
    class VariableComparison {

        @Test
        @DisplayName("variable + expectedValue 匹配时应返回 true")
        void shouldReturnTrue_whenVariableMatchesExpectedValue() {
            ConditionNode node = new ConditionNode("cond-1");
            ExecutionContext context = createContext(Map.of("status", "active"));

            NodeOutput output = node.execute(context, Map.of(
                "variable", "status",
                "expectedValue", "active"
            ));

            assertThat(output.anchor()).isEqualTo("true");
        }

        @Test
        @DisplayName("variable + expectedValue 不匹配时应返回 false")
        void shouldReturnFalse_whenVariableDoesNotMatchExpectedValue() {
            ConditionNode node = new ConditionNode("cond-1");
            ExecutionContext context = createContext(Map.of("status", "inactive"));

            NodeOutput output = node.execute(context, Map.of(
                "variable", "status",
                "expectedValue", "active"
            ));

            assertThat(output.anchor()).isEqualTo("false");
        }

        @Test
        @DisplayName("variable 存在且 expectedValue 为 null 时应返回 true")
        void shouldReturnTrue_whenVariableExistsAndNoExpectedValue() {
            ConditionNode node = new ConditionNode("cond-1");
            ExecutionContext context = createContext(Map.of("name", "test"));

            NodeOutput output = node.execute(context, Map.of("variable", "name"));

            assertThat(output.anchor()).isEqualTo("true");
        }

        @Test
        @DisplayName("variable 不存在且 expectedValue 为 null 时应返回 false")
        void shouldReturnFalse_whenVariableMissingAndNoExpectedValue() {
            ConditionNode node = new ConditionNode("cond-1");
            ExecutionContext context = createContext(Map.of());

            NodeOutput output = node.execute(context, Map.of("variable", "missing"));

            assertThat(output.anchor()).isEqualTo("false");
        }
    }

    @Nested
    @DisplayName("表达式评估")
    class ExpressionEvaluation {

        @Test
        @DisplayName("等值表达式匹配时应返回 true")
        void shouldReturnTrue_whenExpressionMatches() {
            ConditionNode node = new ConditionNode("cond-1");
            ExecutionContext context = createContext(Map.of("level", "high"));

            NodeOutput output = node.execute(context, Map.of(
                "expression", "level == high"
            ));

            assertThat(output.anchor()).isEqualTo("true");
        }

        @Test
        @DisplayName("等值表达式不匹配时应返回 false")
        void shouldReturnFalse_whenExpressionDoesNotMatch() {
            ConditionNode node = new ConditionNode("cond-1");
            ExecutionContext context = createContext(Map.of("level", "low"));

            NodeOutput output = node.execute(context, Map.of(
                "expression", "level == high"
            ));

            assertThat(output.anchor()).isEqualTo("false");
        }

        @Test
        @DisplayName("不等表达式匹配时应返回 true")
        void shouldReturnTrue_whenNotEqualsExpressionMatches() {
            ConditionNode node = new ConditionNode("cond-1");
            ExecutionContext context = createContext(Map.of("level", "low"));

            NodeOutput output = node.execute(context, Map.of(
                "expression", "level != high"
            ));

            assertThat(output.anchor()).isEqualTo("true");
        }

        @Test
        @DisplayName("布尔变量真值检查应返回 true")
        void shouldReturnTrue_whenBooleanVariableIsTrue() {
            ConditionNode node = new ConditionNode("cond-1");
            ExecutionContext context = createContext(Map.of("enabled", true));

            NodeOutput output = node.execute(context, Map.of(
                "expression", "enabled"
            ));

            assertThat(output.anchor()).isEqualTo("true");
        }

        @Test
        @DisplayName("布尔变量假值检查应返回 false")
        void shouldReturnFalse_whenBooleanVariableIsFalse() {
            ConditionNode node = new ConditionNode("cond-1");
            ExecutionContext context = createContext(Map.of("enabled", false));

            NodeOutput output = node.execute(context, Map.of(
                "expression", "enabled"
            ));

            assertThat(output.anchor()).isEqualTo("false");
        }
    }

    @Nested
    @DisplayName("默认行为")
    class DefaultBehavior {

        @Test
        @DisplayName("无 Predicate 且无参数时默认返回 true")
        void shouldReturnTrueByDefault_whenNoConditionSpecified() {
            ConditionNode node = new ConditionNode("cond-1");
            ExecutionContext context = createContext(Map.of());

            NodeOutput output = node.execute(context, Map.of());

            assertThat(output.anchor()).isEqualTo("true");
            assertThat(output.data()).containsEntry("result", true);
        }

        @Test
        @DisplayName("null 参数时 evaluateCondition 应返回错误输出或默认结果")
        void shouldHandleNullParams() {
            ConditionNode node = new ConditionNode("cond-1");
            ExecutionContext context = createContext(Map.of());

            // ConditionNode does not extend AbstractWorkflowNode; null params causes NPE
            // which is caught in the try-catch and returns error output
            NodeOutput output = node.execute(context, null);

            // Either the node handles null gracefully (returns data with result)
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
            ConditionNode node = new ConditionNode("cond-1", ctx -> true);
            ExecutionContext context = createContext(Map.of());

            NodeOutput output = node.execute(context, Map.of());

            assertThat(output.durationMs()).isGreaterThanOrEqualTo(0);
        }
    }
}
