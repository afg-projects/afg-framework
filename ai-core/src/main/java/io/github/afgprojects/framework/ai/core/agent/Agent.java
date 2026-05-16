package io.github.afgprojects.framework.ai.core.agent;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Agent 核心接口
 *
 * <p>Agent 是 AI 智能体的核心抽象，定义了智能体的基本行为：
 * <ul>
 *   <li>名称和描述 - 标识 Agent</li>
 *   <li>执行能力 - 处理请求并返回响应</li>
 *   <li>工具集 - Agent 可使用的工具</li>
 * </ul>
 *
 * <p>实现示例：
 * <pre>{@code
 * public class MyAgent implements Agent {
 *     @Override
 *     public String getName() {
 *         return "MyAgent";
 *     }
 *
 *     @Override
 *     public String getDescription() {
 *         return "A custom agent";
 *     }
 *
 *     @Override
 *     public AgentResponse execute(AgentRequest request) {
 *         // 处理请求
 *         return AgentResponse.completed("Done");
 *     }
 *
 *     @Override
 *     public List<Tool> getTools() {
 *         return List.of(new MyTool());
 *     }
 * }
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface Agent {

    /**
     * 获取 Agent 名称
     *
     * <p>名称用于标识 Agent，应具有唯一性。
     *
     * @return Agent 名称
     */
    @NonNull
    String getName();

    /**
     * 获取 Agent 描述
     *
     * <p>描述用于说明 Agent 的功能和用途。
     *
     * @return Agent 描述
     */
    @NonNull
    String getDescription();

    /**
     * 执行 Agent 任务
     *
     * <p>处理用户请求并返回响应。实现类应：
     * <ul>
     *   <li>解析用户输入</li>
     *   <li>调用必要的工具</li>
     *   <li>生成响应</li>
     * </ul>
     *
     * @param request 请求对象
     * @return 响应对象
     */
    @NonNull
    AgentResponse execute(@NonNull AgentRequest request);

    /**
     * 获取 Agent 可用的工具列表
     *
     * <p>工具是 Agent 可以调用的外部能力，如：
     * <ul>
     *   <li>数据库查询</li>
     *   <li>API 调用</li>
     *   <li>文件操作</li>
     * </ul>
     *
     * @return 工具列表，如果没有工具则返回空列表
     */
    @NonNull
    List<?> getTools();

    /**
     * 判断 Agent 是否支持指定的工具
     *
     * @param toolName 工具名称
     * @return 是否支持
     */
    default boolean supportsTool(@NonNull String toolName) {
        return getTools().stream()
            .anyMatch(tool -> tool.getClass().getSimpleName().equals(toolName));
    }
}
