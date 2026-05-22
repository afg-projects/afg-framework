package io.github.afgprojects.framework.ai.agent.skill.dispatcher;

import io.github.afgprojects.framework.ai.agent.skill.SkillContext;
import io.github.afgprojects.framework.ai.agent.skill.SkillDefinition;
import io.github.afgprojects.framework.ai.agent.skill.SkillResult;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Skill 调度器接口
 *
 * <p>负责根据意图分析结果调度执行 Skill。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface SkillDispatcher {

    /**
     * 调度执行用户请求
     *
     * <p>工作流程：
     * <ol>
     *   <li>分析用户意图</li>
     *   <li>根据置信度决定执行或请求澄清</li>
     *   <li>执行推荐的 Skill</li>
     *   <li>返回执行结果</li>
     * </ol>
     *
     * @param userInput 用户输入
     * @param context   执行上下文
     * @return 调度结果
     */
    @NonNull
    SkillRoutingResult dispatch(@NonNull String userInput, @NonNull SkillContext context);

    /**
     * 调度执行多个 Skills
     *
     * @param userInput 用户输入
     * @param skills    要执行的 Skills
     * @param context   执行上下文
     * @param strategy  执行策略
     * @return 执行结果列表
     */
    @NonNull
    List<SkillResult> dispatchMultiple(
        @NonNull String userInput,
        @NonNull List<SkillDefinition> skills,
        @NonNull SkillContext context,
        @NonNull DispatchStrategy strategy
    );

    /**
     * 执行策略
     */
    enum DispatchStrategy {
        /**
         * 并行执行
         */
        PARALLEL,

        /**
         * 串行执行
         */
        SEQUENTIAL,

        /**
         * 条件执行（根据前一个结果决定下一个）
         */
        CONDITIONAL
    }
}