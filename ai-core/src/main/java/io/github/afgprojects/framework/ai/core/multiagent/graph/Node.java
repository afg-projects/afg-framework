package io.github.afgprojects.framework.ai.core.multiagent.graph;

import io.github.afgprojects.framework.ai.core.multiagent.state.WorkflowState;
import org.jspecify.annotations.NonNull;

import java.util.Set;

/**
 * 节点接口
 *
 * <p>所有工作流节点的基接口。
 */
public interface Node {

    /**
     * 获取节点ID
     */
    @NonNull
    String getId();

    /**
     * 执行节点
     *
     * @param state 当前工作流状态
     * @return 执行结果
     */
    @NonNull
    NodeResult execute(@NonNull WorkflowState state);

    /**
     * 获取依赖节点列表
     */
    @NonNull
    default Set<String> getDependencies() {
        return Set.of();
    }

    /**
     * 获取节点类型
     */
    @NonNull
    NodeType getType();
}
