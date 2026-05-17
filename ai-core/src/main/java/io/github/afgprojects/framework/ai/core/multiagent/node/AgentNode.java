package io.github.afgprojects.framework.ai.core.multiagent.node;

import io.github.afgprojects.framework.ai.core.agent.Agent;
import io.github.afgprojects.framework.ai.core.agent.AgentRequest;
import io.github.afgprojects.framework.ai.core.agent.AgentResponse;
import io.github.afgprojects.framework.ai.core.multiagent.graph.Node;
import io.github.afgprojects.framework.ai.core.multiagent.state.WorkflowState;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Agent 执行节点接口
 *
 * <p>AgentNode 是工作流中执行 Agent 的节点类型。
 * 它将 Agent 集成到工作流图中，负责：
 * <ul>
 *   <li>从 WorkflowState 构建 AgentRequest</li>
 *   <li>调用 Agent 执行</li>
 *   <li>将 AgentResponse 写回 WorkflowState</li>
 * </ul>
 *
 * <p>实现示例：
 * <pre>{@code
 * AgentNode node = new DefaultAgentNode("my-agent", myAgent)
 *     .withInputKey("user_input")
 *     .withOutputKey("agent_output");
 *
 * // 在工作流中使用
 * graph.addNode("agent-step", node);
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface AgentNode extends Node {

    /**
     * 获取绑定的 Agent
     *
     * @return 绑定的 Agent 实例
     */
    @NonNull
    Agent getAgent();

    /**
     * 获取系统提示
     *
     * <p>系统提示会覆盖 Agent 的默认系统提示。
     *
     * @return 系统提示，如果使用 Agent 默认提示则返回 null
     */
    @Nullable
    String getSystemPrompt();

    /**
     * 获取工具列表
     *
     * <p>返回此节点可使用的工具名称列表。
     * 如果为空，则使用 Agent 的默认工具集。
     *
     * @return 工具名称列表
     */
    @NonNull
    List<String> getToolNames();

    /**
     * 获取执行模式
     *
     * <p>默认为同步执行。
     *
     * @return 执行模式
     */
    @NonNull
    default ExecutionMode getExecutionMode() {
        return ExecutionMode.SYNC;
    }

    /**
     * 构建请求：从 WorkflowState 提取 AgentRequest
     *
     * <p>子类可以覆盖此方法以自定义请求构建逻辑。
     *
     * @param state 当前工作流状态
     * @return 构建的 Agent 请求
     */
    @NonNull
    AgentRequest buildRequest(@NonNull WorkflowState state);

    /**
     * 处理响应：将 AgentResponse 写入 WorkflowState
     *
     * <p>子类可以覆盖此方法以自定义响应处理逻辑。
     *
     * @param response Agent 响应
     * @param state    当前工作流状态
     * @return 更新后的工作流状态
     */
    @NonNull
    WorkflowState processResponse(@NonNull AgentResponse response, @NonNull WorkflowState state);

    /**
     * 执行模式枚举
     */
    enum ExecutionMode {
        /**
         * 同步执行
         *
         * <p>阻塞等待 Agent 执行完成。
         */
        SYNC,

        /**
         * 异步执行
         *
         * <p>异步执行 Agent，立即返回 Future。
         */
        ASYNC,

        /**
         * 流式执行
         *
         * <p>流式返回 Agent 输出。
         */
        STREAMING
    }
}
