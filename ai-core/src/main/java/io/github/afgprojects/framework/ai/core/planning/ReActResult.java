package io.github.afgprojects.framework.ai.core.planning;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * ReAct 执行结果
 *
 * <p>封装 ReAct (Reasoning + Acting) 模式的执行结果，包含：
 * <ul>
 *   <li>答案 - 最终得出的答案</li>
 *   <li>步骤 - 执行过程中的推理和行动步骤</li>
 *   <li>成功标志 - 是否成功完成任务</li>
 * </ul>
 *
 * <p>ReAct 模式是一种交替进行推理 (Reasoning) 和行动 (Acting) 的问题解决方法：
 * <pre>
 * Thought: 我需要先查询用户信息
 * Action: queryUser(userId=123)
 * Observation: 用户名为张三
 * Thought: 现在我可以更新用户状态
 * Action: updateUserStatus(userId=123, status=active)
 * Observation: 更新成功
 * Answer: 用户状态已更新为活跃
 * </pre>
 *
 * @param answer  最终答案
 * @param steps   执行步骤列表
 * @param success 是否成功
 * @author afg-projects
 * @since 1.0.0
 */
public record ReActResult(
    @Nullable String answer,
    @NonNull List<Object> steps,
    boolean success
) {

    /**
     * 创建成功的结果
     *
     * @param answer 最终答案
     * @param steps  执行步骤
     * @return 成功结果
     */
    @NonNull
    public static ReActResult success(@Nullable String answer, @NonNull List<Object> steps) {
        return new ReActResult(answer, steps, true);
    }

    /**
     * 创建失败的结果
     *
     * @param answer 错误信息
     * @param steps  已执行的步骤
     * @return 失败结果
     */
    @NonNull
    public static ReActResult failure(@Nullable String answer, @NonNull List<Object> steps) {
        return new ReActResult(answer, steps, false);
    }

    /**
     * 创建空步骤的成功结果
     *
     * @param answer 最终答案
     * @return 成功结果
     */
    @NonNull
    public static ReActResult completed(@Nullable String answer) {
        return new ReActResult(answer, List.of(), true);
    }

    /**
     * 判断是否有步骤
     *
     * @return 是否有步骤
     */
    public boolean hasSteps() {
        return !steps.isEmpty();
    }

    /**
     * 获取步骤数量
     *
     * @return 步骤数量
     */
    public int stepCount() {
        return steps.size();
    }
}
