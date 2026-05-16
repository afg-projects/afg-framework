package io.github.afgprojects.framework.ai.core.planning;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Plan-Execute 执行结果
 *
 * <p>封装 Plan-Execute 模式的执行结果，包含：
 * <ul>
 *   <li>最终结果 - 执行计划后得出的最终结果</li>
 *   <li>步骤结果 - 每个步骤的执行结果</li>
 *   <li>成功标志 - 是否成功完成计划</li>
 * </ul>
 *
 * <p>Plan-Execute 模式是一种两阶段问题解决方法：
 * <pre>
 * Plan 阶段:
 *   1. 分析目标
 *   2. 制定执行计划（步骤列表）
 *   3. 确定步骤依赖关系
 *
 * Execute 阶段:
 *   1. 按计划顺序执行步骤
 *   2. 收集每个步骤的结果
 *   3. 处理执行中的异常
 *   4. 汇总最终结果
 * </pre>
 *
 * @param finalResult 最终结果
 * @param stepResults 步骤执行结果列表
 * @param success     是否成功
 * @author afg-projects
 * @since 1.0.0
 */
public record PlanExecuteResult(
    @Nullable String finalResult,
    @NonNull List<Object> stepResults,
    boolean success
) {

    /**
     * 创建成功的结果
     *
     * @param finalResult 最终结果
     * @param stepResults 步骤结果
     * @return 成功结果
     */
    @NonNull
    public static PlanExecuteResult success(@Nullable String finalResult, @NonNull List<Object> stepResults) {
        return new PlanExecuteResult(finalResult, stepResults, true);
    }

    /**
     * 创建失败的结果
     *
     * @param finalResult 错误信息
     * @param stepResults 已执行的步骤结果
     * @return 失败结果
     */
    @NonNull
    public static PlanExecuteResult failure(@Nullable String finalResult, @NonNull List<Object> stepResults) {
        return new PlanExecuteResult(finalResult, stepResults, false);
    }

    /**
     * 创建空步骤的成功结果
     *
     * @param finalResult 最终结果
     * @return 成功结果
     */
    @NonNull
    public static PlanExecuteResult completed(@Nullable String finalResult) {
        return new PlanExecuteResult(finalResult, List.of(), true);
    }

    /**
     * 判断是否有步骤结果
     *
     * @return 是否有步骤结果
     */
    public boolean hasStepResults() {
        return !stepResults.isEmpty();
    }

    /**
     * 获取步骤数量
     *
     * @return 步骤数量
     */
    public int stepCount() {
        return stepResults.size();
    }

    /**
     * 获取第一个步骤结果
     *
     * @return 第一个步骤结果，如果为空则返回 null
     */
    @Nullable
    public Object firstStepResult() {
        return stepResults.isEmpty() ? null : stepResults.get(0);
    }

    /**
     * 获取最后一个步骤结果
     *
     * @return 最后一个步骤结果，如果为空则返回 null
     */
    @Nullable
    public Object lastStepResult() {
        return stepResults.isEmpty() ? null : stepResults.get(stepResults.size() - 1);
    }
}
