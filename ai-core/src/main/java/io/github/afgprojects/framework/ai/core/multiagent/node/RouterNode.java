package io.github.afgprojects.framework.ai.core.multiagent.node;

import io.github.afgprojects.framework.ai.core.multiagent.graph.Node;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 路由节点接口
 *
 * <p>根据条件决定下一个执行的节点。
 */
public interface RouterNode extends Node {

    /**
     * 获取路由条件列表
     */
    @NonNull
    List<RouteCondition> getConditions();

    /**
     * 获取默认路由（无条件匹配时）
     */
    @Nullable
    String getDefaultTarget();
}
