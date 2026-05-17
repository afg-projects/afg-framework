package io.github.afgprojects.framework.ai.agent.node;

import io.github.afgprojects.framework.ai.core.multiagent.graph.NodeResult;
import io.github.afgprojects.framework.ai.core.multiagent.graph.NodeType;
import io.github.afgprojects.framework.ai.core.multiagent.node.RouteCondition;
import io.github.afgprojects.framework.ai.core.multiagent.state.WorkflowState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultRouterNodeTest {

    private DefaultRouterNode routerNode;

    @BeforeEach
    void setUp() {
        routerNode = new DefaultRouterNode("router",
                List.of(
                        RouteCondition.of("node-a", state -> "a".equals(state.get("branch"))),
                        RouteCondition.of("node-b", state -> "b".equals(state.get("branch")))
                ),
                "node-default"
        );
    }

    @Test
    @DisplayName("获取节点ID")
    void getId_returnsId() {
        assertThat(routerNode.getId()).isEqualTo("router");
    }

    @Test
    @DisplayName("获取节点类型")
    void getType_returnsRouter() {
        assertThat(routerNode.getType()).isEqualTo(NodeType.ROUTER);
    }

    @Test
    @DisplayName("匹配第一个条件")
    void execute_firstConditionMatches_routesToFirst() {
        WorkflowState state = WorkflowState.empty().with("branch", "a");

        NodeResult result = routerNode.execute(state);

        assertThat(result.nextNodeId()).isEqualTo("node-a");
    }

    @Test
    @DisplayName("匹配第二个条件")
    void execute_secondConditionMatches_routesToSecond() {
        WorkflowState state = WorkflowState.empty().with("branch", "b");

        NodeResult result = routerNode.execute(state);

        assertThat(result.nextNodeId()).isEqualTo("node-b");
    }

    @Test
    @DisplayName("无匹配条件使用默认路由")
    void execute_noMatch_usesDefault() {
        WorkflowState state = WorkflowState.empty().with("branch", "c");

        NodeResult result = routerNode.execute(state);

        assertThat(result.nextNodeId()).isEqualTo("node-default");
    }

    @Test
    @DisplayName("无默认路由返回null")
    void execute_noMatchNoDefault_returnsNull() {
        DefaultRouterNode node = new DefaultRouterNode("router",
                List.of(RouteCondition.of("node-a", state -> false)),
                null
        );

        WorkflowState state = WorkflowState.empty();
        NodeResult result = node.execute(state);

        assertThat(result.nextNodeId()).isNull();
    }
}
