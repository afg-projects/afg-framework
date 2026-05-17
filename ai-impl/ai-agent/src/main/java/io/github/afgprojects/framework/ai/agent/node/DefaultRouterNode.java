package io.github.afgprojects.framework.ai.agent.node;

import io.github.afgprojects.framework.ai.core.multiagent.graph.NodeResult;
import io.github.afgprojects.framework.ai.core.multiagent.graph.NodeType;
import io.github.afgprojects.framework.ai.core.multiagent.node.RouteCondition;
import io.github.afgprojects.framework.ai.core.multiagent.node.RouterNode;
import io.github.afgprojects.framework.ai.core.multiagent.state.WorkflowState;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 默认路由节点实现
 */
public class DefaultRouterNode implements RouterNode {

    private final String id;
    private final List<RouteCondition> conditions;
    private final String defaultTarget;

    /**
     * 创建路由节点
     */
    public DefaultRouterNode(String id, List<RouteCondition> conditions, @Nullable String defaultTarget) {
        this.id = id;
        this.conditions = List.copyOf(conditions);
        this.defaultTarget = defaultTarget;
    }

    @Override
    @NonNull
    public String getId() {
        return id;
    }

    @Override
    @NonNull
    public NodeResult execute(@NonNull WorkflowState state) {
        for (RouteCondition condition : conditions) {
            if (condition.condition().test(state)) {
                return NodeResult.routeTo(state, condition.targetNodeId());
            }
        }
        return NodeResult.routeTo(state, defaultTarget);
    }

    @Override
    @NonNull
    public NodeType getType() {
        return NodeType.ROUTER;
    }

    @Override
    @NonNull
    public List<RouteCondition> getConditions() {
        return conditions;
    }

    @Override
    @Nullable
    public String getDefaultTarget() {
        return defaultTarget;
    }
}
