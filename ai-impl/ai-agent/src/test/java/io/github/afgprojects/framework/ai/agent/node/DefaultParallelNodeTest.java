package io.github.afgprojects.framework.ai.agent.node;

import io.github.afgprojects.framework.ai.core.api.multiagent.graph.NodeResult;
import io.github.afgprojects.framework.ai.core.api.multiagent.graph.NodeType;
import io.github.afgprojects.framework.ai.core.api.multiagent.node.ParallelNode;
import io.github.afgprojects.framework.ai.core.api.multiagent.state.WorkflowState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultParallelNodeTest {

    private DefaultParallelNode parallelNode;

    @BeforeEach
    void setUp() {
        parallelNode = new DefaultParallelNode("parallel",
                List.of("node-1", "node-2", "node-3"),
                ParallelNode.ParallelStrategy.ALL,
                (results, state) -> state.with("aggregated", results.size()),
                ParallelNode.FailureHandling.CONTINUE
        );
    }

    @Test
    @DisplayName("获取节点ID")
    void getId_returnsId() {
        assertThat(parallelNode.getId()).isEqualTo("parallel");
    }

    @Test
    @DisplayName("获取节点类型")
    void getType_returnsParallel() {
        assertThat(parallelNode.getType()).isEqualTo(NodeType.PARALLEL);
    }

    @Test
    @DisplayName("获取并行节点列表")
    void getParallelNodes_returnsList() {
        assertThat(parallelNode.getParallelNodes())
                .containsExactly("node-1", "node-2", "node-3");
    }

    @Test
    @DisplayName("获取并行策略")
    void getStrategy_returnsStrategy() {
        assertThat(parallelNode.getStrategy()).isEqualTo(ParallelNode.ParallelStrategy.ALL);
    }

    @Test
    @DisplayName("执行返回并行节点列表")
    void execute_returnsParallelNodes() {
        WorkflowState state = WorkflowState.empty();

        NodeResult result = parallelNode.execute(state);

        assertThat(result.status().name()).isEqualTo("PARALLEL_WAIT");
        assertThat(result.parallelNodes()).containsExactly("node-1", "node-2", "node-3");
    }

    @Test
    @DisplayName("使用 ANY 策略")
    void withAnyStrategy() {
        DefaultParallelNode node = new DefaultParallelNode("parallel",
                List.of("node-1", "node-2"),
                ParallelNode.ParallelStrategy.ANY,
                (results, state) -> state,
                ParallelNode.FailureHandling.CONTINUE
        );

        assertThat(node.getStrategy()).isEqualTo(ParallelNode.ParallelStrategy.ANY);
    }

    @Test
    @DisplayName("使用 N_OF_M 策略")
    void withNOfMStrategy() {
        DefaultParallelNode node = new DefaultParallelNode("parallel",
                List.of("node-1", "node-2", "node-3"),
                ParallelNode.ParallelStrategy.N_OF_M,
                (results, state) -> state,
                ParallelNode.FailureHandling.ABORT
        );

        assertThat(node.getStrategy()).isEqualTo(ParallelNode.ParallelStrategy.N_OF_M);
        assertThat(node.getFailureHandling()).isEqualTo(ParallelNode.FailureHandling.ABORT);
    }

    @Test
    @DisplayName("使用 RETRY 失败处理")
    void withRetryFailureHandling() {
        DefaultParallelNode node = new DefaultParallelNode("parallel",
                List.of("node-1"),
                ParallelNode.ParallelStrategy.ALL,
                (results, state) -> state,
                ParallelNode.FailureHandling.RETRY
        );

        assertThat(node.getFailureHandling()).isEqualTo(ParallelNode.FailureHandling.RETRY);
    }

    @Test
    @DisplayName("结果聚合器可以聚合结果")
    void aggregator_canAggregateResults() {
        WorkflowState state = WorkflowState.empty();
        List<NodeResult> results = List.of(
                NodeResult.success(state.with("result1", "value1")),
                NodeResult.success(state.with("result2", "value2"))
        );

        WorkflowState aggregated = parallelNode.getAggregator().aggregate(results, state);

        Integer aggregatedValue = aggregated.get("aggregated");
        assertThat(aggregatedValue).isEqualTo(2);
    }

    @Test
    @DisplayName("并行节点列表不可变")
    void parallelNodes_isImmutable() {
        List<String> nodes = parallelNode.getParallelNodes();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> nodes.add("node-4"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("执行结果包含更新后的状态")
    void execute_preservesState() {
        WorkflowState state = WorkflowState.empty().with("key", "value");

        NodeResult result = parallelNode.execute(state);

        String keyValue = result.updatedState().get("key");
        assertThat(keyValue).isEqualTo("value");
    }
}
