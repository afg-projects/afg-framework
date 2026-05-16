package io.github.afgprojects.framework.ai.core.multiagent;

import io.github.afgprojects.framework.ai.core.agent.AgentRequest;
import io.github.afgprojects.framework.ai.core.agent.AgentResponse;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Agent 工作流接口
 *
 * <p>定义 Multi-Agent 系统中的工作流，包括步骤定义、执行顺序和状态管理。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface AgentWorkflow {

    /**
     * 获取工作流 ID
     *
     * @return 工作流 ID
     */
    @NonNull String getWorkflowId();

    /**
     * 获取工作流名称
     *
     * @return 工作流名称
     */
    @NonNull String getName();

    /**
     * 获取工作流步骤
     *
     * @return 步骤列表
     */
    @NonNull List<WorkflowStep> getSteps();

    /**
     * 执行工作流
     *
     * @param request 初始请求
     * @return 执行结果
     */
    @NonNull CompletableFuture<WorkflowResult> execute(@NonNull AgentRequest request);

    /**
     * 获取工作流状态
     *
     * @return 当前状态
     */
    @NonNull WorkflowState getState();

    /**
     * 暂停工作流
     */
    void pause();

    /**
     * 恢复工作流
     */
    void resume();

    /**
     * 取消工作流
     */
    void cancel();

    /**
     * 工作流步骤
     *
     * @param stepId      步骤 ID
     * @param name        步骤名称
     * @param agentId     执行 Agent ID
     * @param dependencies 依赖的步骤 ID 列表
     * @param inputMapping 输入映射
     * @param outputMapping 输出映射
     * @param condition   执行条件（可选）
     */
    record WorkflowStep(
            @NonNull String stepId,
            @NonNull String name,
            @NonNull String agentId,
            @NonNull List<String> dependencies,
            @Nullable Map<String, String> inputMapping,
            @Nullable Map<String, String> outputMapping,
            @Nullable String condition
    ) {
        /**
         * 创建简单步骤
         */
        public static @NonNull WorkflowStep simple(
                @NonNull String stepId,
                @NonNull String name,
                @NonNull String agentId
        ) {
            return new WorkflowStep(stepId, name, agentId, List.of(), null, null, null);
        }

        /**
         * 创建带依赖的步骤
         */
        public static @NonNull WorkflowStep withDependencies(
                @NonNull String stepId,
                @NonNull String name,
                @NonNull String agentId,
                @NonNull List<String> dependencies
        ) {
            return new WorkflowStep(stepId, name, agentId, dependencies, null, null, null);
        }
    }

    /**
     * 工作流状态
     */
    enum WorkflowState {
        /**
         * 待执行
         */
        PENDING,
        /**
         * 执行中
         */
        RUNNING,
        /**
         * 已暂停
         */
        PAUSED,
        /**
         * 已完成
         */
        COMPLETED,
        /**
         * 已失败
         */
        FAILED,
        /**
         * 已取消
         */
        CANCELLED
    }

    /**
     * 工作流执行结果
     *
     * @param success    是否成功
     * @param result     最终结果
     * @param stepResults 各步骤结果
     * @param errors     错误信息
     */
    record WorkflowResult(
            boolean success,
            @Nullable AgentResponse result,
            @Nullable Map<String, Object> stepResults,
            @Nullable String errors
    ) {
        /**
         * 创建成功结果
         */
        public static @NonNull WorkflowResult success(@Nullable AgentResponse result) {
            return new WorkflowResult(true, result, null, null);
        }

        /**
         * 创建失败结果
         */
        public static @NonNull WorkflowResult failure(@NonNull String errors) {
            return new WorkflowResult(false, null, null, errors);
        }

        /**
         * 创建带步骤结果的成功结果
         */
        public static @NonNull WorkflowResult successWithSteps(
                @Nullable AgentResponse result,
                @NonNull Map<String, Object> stepResults
        ) {
            return new WorkflowResult(true, result, stepResults, null);
        }
    }
}
