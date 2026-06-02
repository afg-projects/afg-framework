package io.github.afgprojects.framework.ai.core.api.skill;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 意图分析结果
 *
 * @param matches Skill 匹配列表
 * @param rawResponse LLM 原始响应
 *
 * @author afg-projects
 * @since 1.0.0
 */
public record IntentResult(
        @NonNull List<SkillMatch> matches,
        @Nullable String rawResponse
) {

    /**
     * 获取最佳匹配
     */
    @Nullable
    public SkillMatch bestMatch() {
        if (matches.isEmpty()) {
            return null;
        }
        return matches.getFirst();
    }

    /**
     * 是否有匹配
     */
    public boolean hasMatch() {
        return !matches.isEmpty();
    }

    /**
     * 创建空结果
     */
    public static IntentResult empty() {
        return new IntentResult(List.of(), null);
    }

    /**
     * 从匹配列表创建结果
     */
    public static IntentResult of(@NonNull List<SkillMatch> matches) {
        return new IntentResult(matches, null);
    }

    /**
     * 从匹配列表和原始响应创建结果
     */
    public static IntentResult of(@NonNull List<SkillMatch> matches, @Nullable String rawResponse) {
        return new IntentResult(matches, rawResponse);
    }
}