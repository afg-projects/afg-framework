package io.github.afgprojects.framework.ai.core.api.skill;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Skill 匹配结果
 *
 * @param skill        匹配的 Skill
 * @param confidence   置信度（0.0 - 1.0）
 * @param matchReason  匹配原因
 * @param parameters   提取的参数
 *
 * @author afg-projects
 * @since 1.0.0
 */
public record SkillMatch(
        @NonNull SkillDefinition skill,
        double confidence,
        @Nullable String matchReason,
        @NonNull Map<String, Object> parameters
) {

    /**
     * 创建匹配结果（无参数）
     */
    public static SkillMatch of(@NonNull SkillDefinition skill, double confidence, @Nullable String matchReason) {
        return new SkillMatch(skill, confidence, matchReason, Map.of());
    }

    /**
     * 创建匹配结果（带参数）
     */
    public static SkillMatch of(@NonNull SkillDefinition skill, double confidence, @Nullable String matchReason,
                                @NonNull Map<String, Object> parameters) {
        return new SkillMatch(skill, confidence, matchReason, parameters);
    }
}