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
 * MergeNode 纯单元测试
 *
 * <p>测试合并节点的各种合并策略：
 * merge_all、first、last、flatten 策略。
 * 不需要 Spring 上下文或外部依赖。
 */
@DisplayName("MergeNode")
class MergeNodeTest {

    private ExecutionContext createContextWithOutputs(Map<String, NodeOutput> nodeOutputs) {
        DefaultExecutionContext context = new DefaultExecutionContext("wf-1", "conv-1", "user-1");
        nodeOutputs.forEach(context::setNodeOutput);
        return context;
    }

    private NodeOutput createOutput(Map<String, Object> data) {
        return NodeOutput.of(data);
    }

    @Nested
    @DisplayName("基础属性")
    class BasicProperties {

        @Test
        @DisplayName("应返回正确的节点 ID 和类型")
        void shouldReturnNodeIdAndType() {
            MergeNode node = new MergeNode("merge-1");

            assertThat(node.getNodeId()).isEqualTo("merge-1");
            assertThat(node.getType()).isEqualTo("merge");
        }
    }

    @Nested
    @DisplayName("merge_all 策略")
    class MergeAllStrategy {

        @Test
        @DisplayName("应合并所有源节点输出到一个 map")
        void shouldMergeAllSourceOutputs() {
            MergeNode node = new MergeNode("merge-1");
            ExecutionContext context = createContextWithOutputs(Map.of(
                "node-a", createOutput(Map.of("key1", "val1")),
                "node-b", createOutput(Map.of("key2", "val2"))
            ));

            NodeOutput output = node.execute(context, Map.of(
                "strategy", "merge_all",
                "sourceNodes", java.util.List.of("node-a", "node-b")
            ));

            assertThat(output.data())
                .containsEntry("key1", "val1")
                .containsEntry("key2", "val2")
                .containsEntry("mergedCount", 2);
        }

        @Test
        @DisplayName("后合并的节点应覆盖前节点的同名字段")
        void shouldOverrideOverlappingKeys() {
            MergeNode node = new MergeNode("merge-1");
            ExecutionContext context = createContextWithOutputs(Map.of(
                "node-a", createOutput(Map.of("key", "from-a")),
                "node-b", createOutput(Map.of("key", "from-b"))
            ));

            NodeOutput output = node.execute(context, Map.of(
                "strategy", "merge_all",
                "sourceNodes", java.util.List.of("node-a", "node-b")
            ));

            // merge_all iterates and putAll, so last one wins
            assertThat(output.data()).containsEntry("key", "from-b");
        }
    }

    @Nested
    @DisplayName("first 策略")
    class FirstStrategy {

        @Test
        @DisplayName("应只取第一个源节点的输出")
        void shouldReturnFirstSourceOutput() {
            MergeNode node = new MergeNode("merge-1");
            ExecutionContext context = createContextWithOutputs(Map.of(
                "node-a", createOutput(Map.of("key1", "val1", "exclusive", true)),
                "node-b", createOutput(Map.of("key2", "val2"))
            ));

            NodeOutput output = node.execute(context, Map.of(
                "strategy", "first",
                "sourceNodes", java.util.List.of("node-a", "node-b")
            ));

            assertThat(output.data())
                .containsEntry("key1", "val1")
                .containsEntry("exclusive", true)
                .doesNotContainKey("key2");
        }

        @Test
        @DisplayName("无源节点输出时应返回 mergedCount=0")
        void shouldReturnZeroMergedCount_whenNoSourceOutputs() {
            MergeNode node = new MergeNode("merge-1");
            ExecutionContext context = createContextWithOutputs(Map.of());

            NodeOutput output = node.execute(context, Map.of(
                "strategy", "first",
                "sourceNodes", java.util.List.of("node-a")
            ));

            assertThat(output.data()).containsEntry("mergedCount", 0);
        }
    }

    @Nested
    @DisplayName("last 策略")
    class LastStrategy {

        @Test
        @DisplayName("应只取最后一个源节点的输出")
        void shouldReturnLastSourceOutput() {
            MergeNode node = new MergeNode("merge-1");
            ExecutionContext context = createContextWithOutputs(Map.of(
                "node-a", createOutput(Map.of("key1", "val1")),
                "node-b", createOutput(Map.of("key2", "val2", "exclusive", true))
            ));

            NodeOutput output = node.execute(context, Map.of(
                "strategy", "last",
                "sourceNodes", java.util.List.of("node-a", "node-b")
            ));

            assertThat(output.data())
                .containsEntry("key2", "val2")
                .containsEntry("exclusive", true)
                .doesNotContainKey("key1");
        }
    }

    @Nested
    @DisplayName("flatten 策略")
    class FlattenStrategy {

        @Test
        @DisplayName("应将所有源输出收集为 items 列表")
        void shouldCollectAllOutputsAsItems() {
            MergeNode node = new MergeNode("merge-1");
            ExecutionContext context = createContextWithOutputs(Map.of(
                "node-a", createOutput(Map.of("key1", "val1")),
                "node-b", createOutput(Map.of("key2", "val2"))
            ));

            NodeOutput output = node.execute(context, Map.of(
                "strategy", "flatten",
                "sourceNodes", java.util.List.of("node-a", "node-b")
            ));

            assertThat(output.data()).containsEntry("mergedCount", 2);
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> items =
                (java.util.List<Map<String, Object>>) output.data().get("items");
            assertThat(items).hasSize(2);
        }
    }

    @Nested
    @DisplayName("边界情况")
    class EdgeCases {

        @Test
        @DisplayName("sourceNodes 为 null 时应返回 mergedCount=0")
        void shouldReturnZeroMergedCount_whenSourceNodesNull() {
            MergeNode node = new MergeNode("merge-1");
            ExecutionContext context = createContextWithOutputs(Map.of());

            NodeOutput output = node.execute(context, Map.of());

            assertThat(output.data()).containsEntry("mergedCount", 0);
        }

        @Test
        @DisplayName("null 参数应视为空 map 处理")
        void shouldHandleNullParams() {
            MergeNode node = new MergeNode("merge-1");
            ExecutionContext context = createContextWithOutputs(Map.of());

            NodeOutput output = node.execute(context, null);

            assertThat(output.data()).containsEntry("mergedCount", 0);
        }
    }
}
