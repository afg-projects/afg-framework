package io.github.afgprojects.framework.ai.core.workflow.node.logic;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.NodeOutput;
import io.github.afgprojects.framework.ai.core.workflow.engine.DefaultExecutionContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * LoopNode 纯单元测试
 *
 * <p>测试循环节点的迭代逻辑：
 * 列表迭代、计数迭代、自定义变量名、参数校验。
 * 不需要 Spring 上下文或外部依赖。
 */
@DisplayName("LoopNode")
class LoopNodeTest {

    private ExecutionContext createContext() {
        return new DefaultExecutionContext("wf-1", "conv-1", "user-1");
    }

    @Nested
    @DisplayName("基础属性")
    class BasicProperties {

        @Test
        @DisplayName("应返回正确的节点 ID 和类型")
        void shouldReturnNodeIdAndType() {
            LoopNode node = new LoopNode("loop-1");

            assertThat(node.getNodeId()).isEqualTo("loop-1");
            assertThat(node.getType()).isEqualTo("loop");
        }
    }

    @Nested
    @DisplayName("列表迭代")
    class ItemsIteration {

        @Test
        @DisplayName("应遍历列表中的所有元素")
        void shouldIterateOverAllItems() {
            LoopNode node = new LoopNode("loop-1");
            List<String> items = List.of("alpha", "beta", "gamma");

            NodeOutput output = node.execute(createContext(), Map.of("items", items));

            assertThat(output.data()).containsEntry("iterations", 3);
            assertThat(output.data()).containsEntry("completed", true);
        }

        @Test
        @DisplayName("空列表应产生零次迭代")
        void shouldProduceZeroIterations_whenEmptyList() {
            LoopNode node = new LoopNode("loop-1");

            NodeOutput output = node.execute(createContext(), Map.of("items", List.of()));

            assertThat(output.data()).containsEntry("iterations", 0);
            assertThat(output.data()).containsEntry("completed", true);
        }

        @Test
        @DisplayName("每次迭代应包含默认变量名 item 和 index")
        @SuppressWarnings("unchecked")
        void shouldIncludeDefaultVariableNames() {
            LoopNode node = new LoopNode("loop-1");

            NodeOutput output = node.execute(createContext(), Map.of("items", List.of("a", "b")));

            List<Map<String, Object>> results = (List<Map<String, Object>>) output.data().get("iterationResults");
            assertThat(results).hasSize(2);
            assertThat(results.get(0)).containsEntry("index", 0).containsEntry("item", "a");
            assertThat(results.get(1)).containsEntry("index", 1).containsEntry("item", "b");
        }
    }

    @Nested
    @DisplayName("计数迭代")
    class CountIteration {

        @Test
        @DisplayName("应按指定次数迭代")
        void shouldIterateSpecifiedCount() {
            LoopNode node = new LoopNode("loop-1");

            NodeOutput output = node.execute(createContext(), Map.of("count", 5));

            assertThat(output.data()).containsEntry("iterations", 5);
            assertThat(output.data()).containsEntry("completed", true);
        }

        @Test
        @DisplayName("count=0 应产生零次迭代")
        void shouldProduceZeroIterations_whenCountIsZero() {
            LoopNode node = new LoopNode("loop-1");

            NodeOutput output = node.execute(createContext(), Map.of("count", 0));

            assertThat(output.data()).containsEntry("iterations", 0);
        }

        @Test
        @DisplayName("count 迭代时 item 应为索引值")
        @SuppressWarnings("unchecked")
        void shouldUseIndexAsItem_whenCountIteration() {
            LoopNode node = new LoopNode("loop-1");

            NodeOutput output = node.execute(createContext(), Map.of("count", 3));

            List<Map<String, Object>> results = (List<Map<String, Object>>) output.data().get("iterationResults");
            assertThat(results.get(0)).containsEntry("item", 0);
            assertThat(results.get(1)).containsEntry("item", 1);
            assertThat(results.get(2)).containsEntry("item", 2);
        }
    }

    @Nested
    @DisplayName("自定义变量名")
    class CustomVariableNames {

        @Test
        @DisplayName("应使用自定义 itemVariable 名")
        @SuppressWarnings("unchecked")
        void shouldUseCustomItemVariableName() {
            LoopNode node = new LoopNode("loop-1");

            NodeOutput output = node.execute(createContext(), Map.of(
                "items", List.of("x"),
                "itemVariable", "element"
            ));

            List<Map<String, Object>> results = (List<Map<String, Object>>) output.data().get("iterationResults");
            assertThat(results.get(0)).containsKey("element");
            assertThat(results.get(0)).doesNotContainKey("item");
        }

        @Test
        @DisplayName("应使用自定义 indexVariable 名")
        @SuppressWarnings("unchecked")
        void shouldUseCustomIndexVariableName() {
            LoopNode node = new LoopNode("loop-1");

            NodeOutput output = node.execute(createContext(), Map.of(
                "items", List.of("x"),
                "indexVariable", "idx"
            ));

            List<Map<String, Object>> results = (List<Map<String, Object>>) output.data().get("iterationResults");
            assertThat(results.get(0)).containsKey("idx");
            assertThat(results.get(0)).doesNotContainKey("index");
        }
    }

    @Nested
    @DisplayName("参数校验")
    class ParameterValidation {

        @Test
        @DisplayName("缺少 items 和 count 时应返回错误输出")
        void shouldReturnError_whenMissingItemsAndCount() {
            LoopNode node = new LoopNode("loop-1");

            NodeOutput output = node.execute(createContext(), Map.of());

            // AbstractWorkflowNode catches exception and returns error data
            assertThat(output.data()).containsKey("error");
        }
    }

    @Nested
    @DisplayName("优先级")
    class Priority {

        @Test
        @DisplayName("items 优先于 count 参数")
        void shouldPreferItemsOverCount() {
            LoopNode node = new LoopNode("loop-1");

            NodeOutput output = node.execute(createContext(), Map.of(
                "items", List.of("a", "b"),
                "count", 100
            ));

            assertThat(output.data()).containsEntry("iterations", 2);
        }
    }
}
