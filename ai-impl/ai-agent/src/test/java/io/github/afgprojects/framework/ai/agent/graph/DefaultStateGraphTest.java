package io.github.afgprojects.framework.ai.agent.graph;

import io.github.afgprojects.framework.ai.core.multiagent.graph.*;
import io.github.afgprojects.framework.ai.core.multiagent.state.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultStateGraphTest {

    private DefaultStateGraph graph;

    @BeforeEach
    void setUp() {
        graph = new DefaultStateGraph("test-graph");
    }

    @Test
    @DisplayName("添加节点")
    void addNode_shouldAddNode() {
        Node node = createTestNode("node-1");

        StateGraph result = graph.addNode("node-1", node);

        assertThat(result).isSameAs(graph);
    }

    @Test
    @DisplayName("添加边")
    void addEdge_shouldAddEdge() {
        graph.addNode("node-1", createTestNode("node-1"));
        graph.addNode("node-2", createTestNode("node-2"));

        StateGraph result = graph.addEdge("node-1", "node-2");

        assertThat(result).isSameAs(graph);
    }

    @Test
    @DisplayName("添加条件边")
    void addConditionalEdge_shouldAddConditionalEdge() {
        graph.addNode("node-1", createTestNode("node-1"));
        graph.addNode("node-2a", createTestNode("node-2a"));
        graph.addNode("node-2b", createTestNode("node-2b"));

        StateGraph result = graph.addConditionalEdge("node-1",
                state -> "a",
                Map.of("a", "node-2a", "b", "node-2b"));

        assertThat(result).isSameAs(graph);
    }

    @Test
    @DisplayName("设置入口节点")
    void setEntryPoint_shouldSetEntry() {
        graph.addNode("node-1", createTestNode("node-1"));

        StateGraph result = graph.setEntryPoint("node-1");

        assertThat(result).isSameAs(graph);
    }

    @Test
    @DisplayName("执行简单工作流")
    void execute_simpleWorkflow_completes() {
        Node node1 = createTestNodeWithOutput("node-1", "output-1");
        Node node2 = createTestNodeWithOutput("node-2", "output-2");

        graph.addNode("node-1", node1);
        graph.addNode("node-2", node2);
        graph.addEdge("node-1", "node-2");
        graph.setEntryPoint("node-1");
        graph.setFinishPoint("node-2");

        WorkflowState result = graph.execute(WorkflowInput.empty());

        String status = result.get("_status");
        assertThat(status).isEqualTo(WorkflowStatus.COMPLETED.name());
        assertThat(result.getCurrentNodeId()).isEqualTo("node-2");

        // 验证节点输出
        Map<String, Object> nodeOutputs = result.get("_nodeOutputs");
        assertThat(nodeOutputs).isNotNull();
        assertThat(nodeOutputs).containsKeys("node-1", "node-2");
    }

    @Test
    @DisplayName("执行条件分支工作流")
    void execute_conditionalWorkflow_routesCorrectly() {
        Node router = createRouterNode("router", "branch-a");
        Node nodeA = createTestNodeWithOutput("node-a", "output-a");
        Node nodeB = createTestNodeWithOutput("node-b", "output-b");

        graph.addNode("router", router);
        graph.addNode("node-a", nodeA);
        graph.addNode("node-b", nodeB);
        graph.addConditionalEdge("router",
                state -> state.get("branch"),
                Map.of("branch-a", "node-a", "branch-b", "node-b"));
        graph.setEntryPoint("router");
        graph.setFinishPoint("node-a");
        graph.setFinishPoint("node-b");

        WorkflowState result = graph.execute(WorkflowInput.empty());

        String status = result.get("_status");
        assertThat(status).isEqualTo(WorkflowStatus.COMPLETED.name());
        assertThat(result.getCurrentNodeId()).isEqualTo("node-a");

        // 验证只有 node-a 被执行
        Map<String, Object> nodeOutputs = result.get("_nodeOutputs");
        assertThat(nodeOutputs).containsKeys("router", "node-a");
        assertThat(nodeOutputs).doesNotContainKey("node-b");
    }

    @Test
    @DisplayName("未设置入口节点时抛出异常")
    void execute_noEntryPoint_throwsException() {
        graph.addNode("node-1", createTestNode("node-1"));

        assertThatThrownBy(() -> graph.execute(WorkflowInput.empty()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Entry point not set");
    }

    @Test
    @DisplayName("节点不存在时抛出异常")
    void execute_nodeNotFound_throwsException() {
        graph.addNode("node-1", createTestNode("node-1"));
        graph.addEdge("node-1", "node-2"); // node-2 不存在
        graph.setEntryPoint("node-1");

        assertThatThrownBy(() -> graph.execute(WorkflowInput.empty()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Node not found");
    }

    @Test
    @DisplayName("获取图名称")
    void getName_shouldReturnName() {
        assertThat(graph.getName()).isEqualTo("test-graph");
    }

    @Test
    @DisplayName("多个结束节点")
    void execute_multipleFinishPoints_completesAtFirstFinish() {
        Node node1 = createTestNodeWithOutput("node-1", "output-1");
        Node node2 = createTestNodeWithOutput("node-2", "output-2");
        Node node3 = createTestNodeWithOutput("node-3", "output-3");

        graph.addNode("node-1", node1);
        graph.addNode("node-2", node2);
        graph.addNode("node-3", node3);
        graph.addEdge("node-1", "node-2");
        graph.addEdge("node-2", "node-3");
        graph.setEntryPoint("node-1");
        graph.setFinishPoint("node-2");
        graph.setFinishPoint("node-3");

        WorkflowState result = graph.execute(WorkflowInput.empty());

        // 应该在 node-2 完成，不会执行 node-3
        String status = result.get("_status");
        assertThat(status).isEqualTo(WorkflowStatus.COMPLETED.name());
        assertThat(result.getCurrentNodeId()).isEqualTo("node-2");
    }

    @Test
    @DisplayName("使用 NodeResult.routeTo 指定下一个节点")
    void execute_routeToNextNode_routesCorrectly() {
        Node node1 = new Node() {
            @Override
            public String getId() {
                return "node-1";
            }

            @Override
            public NodeResult execute(WorkflowState state) {
                return NodeResult.routeTo(state.with("routed", true), "node-3");
            }

            @Override
            public NodeType getType() {
                return NodeType.ROUTER;
            }
        };
        Node node2 = createTestNodeWithOutput("node-2", "output-2");
        Node node3 = createTestNodeWithOutput("node-3", "output-3");

        graph.addNode("node-1", node1);
        graph.addNode("node-2", node2);
        graph.addNode("node-3", node3);
        graph.addEdge("node-1", "node-2"); // 普通边，但会被 routeTo 覆盖
        graph.setEntryPoint("node-1");
        graph.setFinishPoint("node-3");

        WorkflowState result = graph.execute(WorkflowInput.empty());

        // 应该跳过 node-2，直接到 node-3
        String status = result.get("_status");
        assertThat(status).isEqualTo(WorkflowStatus.COMPLETED.name());
        assertThat(result.getCurrentNodeId()).isEqualTo("node-3");

        Map<String, Object> nodeOutputs = result.get("_nodeOutputs");
        assertThat(nodeOutputs).containsKeys("node-1", "node-3");
        assertThat(nodeOutputs).doesNotContainKey("node-2");
    }

    private Node createTestNode(String id) {
        return createTestNodeWithOutput(id, "default-output");
    }

    private Node createTestNodeWithOutput(String id, String output) {
        return new Node() {
            @Override
            public String getId() {
                return id;
            }

            @Override
            public NodeResult execute(WorkflowState state) {
                return NodeResult.success(state.with("output_" + id, output));
            }

            @Override
            public NodeType getType() {
                return NodeType.AGENT;
            }
        };
    }

    private Node createRouterNode(String id, String branch) {
        return new Node() {
            @Override
            public String getId() {
                return id;
            }

            @Override
            public NodeResult execute(WorkflowState state) {
                return NodeResult.success(state.with("branch", branch));
            }

            @Override
            public NodeType getType() {
                return NodeType.ROUTER;
            }
        };
    }
}
