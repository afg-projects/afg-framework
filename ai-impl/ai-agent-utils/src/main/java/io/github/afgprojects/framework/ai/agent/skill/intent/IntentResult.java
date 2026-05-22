package io.github.afgprojects.framework.ai.agent.skill.intent;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * 意图分析结果
 *
 * @param intentDescription 用户意图描述
 * @param matchedSkills     匹配的 Skills（按置信度排序）
 * @param recommendedSkill  推荐执行的 Skill（置信度最高的）
 * @param needsClarification 是否需要澄清（多个 Skills 置信度相近）
 * @param clarificationQuestion 澄清问题（如果需要）
 * @param extractedParameters 提取的参数
 *
 * @author afg-projects
 * @since 1.0.0
 */
public record IntentResult(
    @NonNull String intentDescription,
    @NonNull List<SkillMatch> matchedSkills,
    @Nullable SkillMatch recommendedSkill,
    boolean needsClarification,
    @Nullable String clarificationQuestion,
    @NonNull Map<String, Object> extractedParameters
) {

    /**
     * 创建无匹配的结果
     */
    @NonNull
    public static IntentResult noMatch(@NonNull String intentDescription) {
        return new IntentResult(
            intentDescription,
            List.of(),
            null,
            false,
            null,
            Map.of()
        );
    }

    /**
     * 创建需要澄清的结果
     */
    @NonNull
    public static IntentResult needsClarification(
        @NonNull String intentDescription,
        @NonNull List<SkillMatch> matchedSkills,
        @NonNull String clarificationQuestion
    ) {
        return new IntentResult(
            intentDescription,
            matchedSkills,
            null,
            true,
            clarificationQuestion,
            Map.of()
        );
    }

    /**
     * 创建成功匹配的结果
     */
    @NonNull
    public static IntentResult matched(
        @NonNull String intentDescription,
        @NonNull SkillMatch recommendedSkill,
        @NonNull List<SkillMatch> matchedSkills,
        @NonNull Map<String, Object> extractedParameters
    ) {
        return new IntentResult(
            intentDescription,
            matchedSkills,
            recommendedSkill,
            false,
            null,
            extractedParameters
        );
    }
}