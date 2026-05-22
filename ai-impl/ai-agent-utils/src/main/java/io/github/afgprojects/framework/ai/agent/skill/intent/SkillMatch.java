package io.github.afgprojects.framework.ai.agent.skill.intent;

import io.github.afgprojects.framework.ai.agent.skill.SkillDefinition;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Skill 匹配结果
 *
 * @param skill             匹配的 Skill
 * @param confidence        置信度（0.0 - 1.0）
 * @param matchReason       匹配原因
 * @param suggestedParameters 建议的参数
 *
 * @author afg-projects
 * @since 1.0.0
 */
public record SkillMatch(
    @NonNull SkillDefinition skill,
    double confidence,
    @Nullable String matchReason,
    @NonNull Map<String, Object> suggestedParameters
) {

    /**
     * 创建匹配结果
     */
    @NonNull
    public static SkillMatch of(
        @NonNull SkillDefinition skill,
        double confidence,
        @Nullable String matchReason
    ) {
        return new SkillMatch(skill, confidence, matchReason, Map.of());
    }

    /**
     * 创建带参数的匹配结果
     */
    @NonNull
    public static SkillMatch of(
        @NonNull SkillDefinition skill,
        double confidence,
        @Nullable String matchReason,
        @NonNull Map<String, Object> suggestedParameters
    ) {
        return new SkillMatch(skill, confidence, matchReason, suggestedParameters);
    }
}