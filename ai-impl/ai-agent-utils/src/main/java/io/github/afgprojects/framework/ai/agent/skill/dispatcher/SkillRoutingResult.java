package io.github.afgprojects.framework.ai.agent.skill.dispatcher;

import io.github.afgprojects.framework.ai.agent.skill.SkillResult;
import io.github.afgprojects.framework.ai.agent.skill.intent.IntentResult;
import io.github.afgprojects.framework.ai.agent.skill.intent.SkillMatch;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Skill 调度结果
 *
 * @param status            调度状态
 * @param executionResult   执行结果
 * @param selectedSkill     选中的 Skill 匹配信息
 * @param alternativeSkills 备选的 Skills
 * @param clarificationQuestion 澄清问题
 * @param message           附加消息
 *
 * @author afg-projects
 * @since 1.0.0
 */
public record SkillRoutingResult(
    @NonNull RoutingStatus status,
    @Nullable SkillResult executionResult,
    @Nullable SkillMatch selectedSkill,
    @NonNull List<SkillMatch> alternativeSkills,
    @Nullable String clarificationQuestion,
    @Nullable String message
) {

    /**
     * 调度状态
     */
    public enum RoutingStatus {
        /**
         * 成功执行
         */
        SUCCESS,

        /**
         * 需要澄清
         */
        NEEDS_CLARIFICATION,

        /**
         * 置信度过低
         */
        LOW_CONFIDENCE,

        /**
         * 无匹配 Skill
         */
        NO_MATCH,

        /**
         * 执行失败
         */
        EXECUTION_FAILED
    }

    /**
     * 创建成功结果
     */
    @NonNull
    public static SkillRoutingResult success(@NonNull SkillResult result, @NonNull SkillMatch selected) {
        return new SkillRoutingResult(RoutingStatus.SUCCESS, result, selected, List.of(), null, null);
    }

    /**
     * 创建需要澄清的结果
     */
    @NonNull
    public static SkillRoutingResult needsClarification(
        @NonNull String question,
        @NonNull List<SkillMatch> alternatives
    ) {
        return new SkillRoutingResult(
            RoutingStatus.NEEDS_CLARIFICATION,
            null,
            null,
            alternatives,
            question,
            null
        );
    }

    /**
     * 创建低置信度结果
     */
    @NonNull
    public static SkillRoutingResult lowConfidence(
        @NonNull List<SkillMatch> matches,
        @NonNull String message
    ) {
        return new SkillRoutingResult(RoutingStatus.LOW_CONFIDENCE, null, null, matches, null, message);
    }

    /**
     * 创建无匹配结果
     */
    @NonNull
    public static SkillRoutingResult noMatch(@NonNull String message) {
        return new SkillRoutingResult(RoutingStatus.NO_MATCH, null, null, List.of(), null, message);
    }

    /**
     * 创建执行失败结果
     */
    @NonNull
    public static SkillRoutingResult executionFailed(@NonNull String error) {
        return new SkillRoutingResult(RoutingStatus.EXECUTION_FAILED, null, null, List.of(), null, error);
    }

    /**
     * 检查是否成功
     */
    public boolean isSuccess() {
        return status == RoutingStatus.SUCCESS;
    }

    /**
     * 检查是否需要用户输入
     */
    public boolean needsUserInput() {
        return status == RoutingStatus.NEEDS_CLARIFICATION || status == RoutingStatus.LOW_CONFIDENCE;
    }
}