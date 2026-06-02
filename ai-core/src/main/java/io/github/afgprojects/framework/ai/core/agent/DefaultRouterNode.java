package io.github.afgprojects.framework.ai.core.agent;

import io.github.afgprojects.framework.ai.core.api.multiagent.node.RouteCondition;
import io.github.afgprojects.framework.ai.core.api.multiagent.node.RouterNode;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.*;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 默认路由节点实现
 *
 * <p>根据条件将请求路由到不同的子节点。
 * 支持基于条件和默认路由两种策略。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
public class DefaultRouterNode implements RouterNode {

    private final String nodeId;
    private final Map<String, WorkflowNode> routes;
    private final Function<ExecutionContext, String> routingFunction;
    private final String defaultRoute;

    /**
     * 创建路由节点
     *
     * @param nodeId          节点 ID
     * @param routingFunction 路由函数，根据上下文决定路由目标
     * @param routes          路由映射（key 为路由名，value 为目标节点）
     * @param defaultRoute    默认路由（找不到匹配时使用）
     */
    public DefaultRouterNode(
            @NonNull String nodeId,
            @NonNull Function<ExecutionContext, String> routingFunction,
            @NonNull Map<String, WorkflowNode> routes,
            @Nullable String defaultRoute
    ) {
        this.nodeId = nodeId;
        this.routingFunction = routingFunction;
        this.routes = new LinkedHashMap<>(routes);
        this.defaultRoute = defaultRoute;
    }

    /**
     * 创建路由节点（无默认路由）
     *
     * @param nodeId          节点 ID
     * @param routingFunction 路由函数
     * @param routes          路由映射
     */
    public DefaultRouterNode(
            @NonNull String nodeId,
            @NonNull Function<ExecutionContext, String> routingFunction,
            @NonNull Map<String, WorkflowNode> routes
    ) {
        this(nodeId, routingFunction, routes, null);
    }

    @Override
    @NonNull
    public String getNodeId() {
        return nodeId;
    }

    @Override
    @NonNull
    public String getType() {
        return "router";
    }

    @Override
    @NonNull
    public NodeOutput execute(@NonNull ExecutionContext context, @NonNull Map<String, Object> params) {
        // 调用路由函数确定目标
        String targetRoute = routingFunction.apply(context);

        if (targetRoute == null && defaultRoute != null) {
            targetRoute = defaultRoute;
            log.debug("Using default route: {}", defaultRoute);
        }

        if (targetRoute == null) {
            log.warn("No route matched and no default route defined: nodeId={}", nodeId);
            return NodeOutput.of(Map.of("error", "No matching route"), nodeId);
        }

        WorkflowNode targetNode = routes.get(targetRoute);
        if (targetNode == null) {
            log.warn("Route target not found: route={}, nodeId={}", targetRoute, nodeId);
            return NodeOutput.of(Map.of("error", "Route target not found: " + targetRoute), nodeId);
        }

        log.debug("Routing to: route={}, nodeId={}", targetRoute, targetNode.getNodeId());
        return targetNode.execute(context, params);
    }

    @Override
    @NonNull
    public Flux<WorkflowNode.NodeEvent> executeStream(@NonNull ExecutionContext context, @NonNull Map<String, Object> params) {
        // 调用路由函数确定目标
        String targetRoute = routingFunction.apply(context);

        if (targetRoute == null && defaultRoute != null) {
            targetRoute = defaultRoute;
        }

        if (targetRoute == null) {
            return Flux.empty();
        }

        WorkflowNode targetNode = routes.get(targetRoute);
        if (targetNode == null) {
            return Flux.empty();
        }

        return targetNode.executeStream(context, params);
    }

    @Override
    public List<RouteCondition> getConditions() {
        return List.of();
    }

    @Override
    @Nullable
    public String getDefaultTarget() {
        return defaultRoute;
    }
}