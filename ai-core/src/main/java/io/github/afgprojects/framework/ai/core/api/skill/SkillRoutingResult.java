package io.github.afgprojects.framework.ai.core.api.skill;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Skill 路由结果
 *
 * @param matched   是否匹配到 Skill
 * @param skill     匹配的 Skill 定义（可能为空）
 * @param confidence 匹配置信度
 * @param alternatives 备选 Skill 列表
 * @param reason    路由原因
 *
 * @author afg-projects
 * @since 1.0.0
 */
public record SkillRoutingResult(
        boolean matched,
        @Nullable SkillDefinition skill,
        double confidence,
        @NonNull List<SkillDefinition> alternatives,
        @Nullable String reason
) {

    /**
     * 创建成功匹配结果
     */
    public static SkillRoutingResult matched(@NonNull SkillDefinition skill, double confidence) {
        return new SkillRoutingResult(true, skill, confidence, List.of(), null);
    }

    /**
     * 创建成功匹配结果（带备选项）
     */
    public static SkillRoutingResult matched(@NonNull SkillDefinition skill, double confidence,
                                             @NonNull List<SkillDefinition> alternatives) {
        return new SkillRoutingResult(true, skill, confidence, alternatives, null);
    }

    /**
     * 创建未匹配结果
     */
    public static SkillRoutingResult notMatched(@Nullable String reason) {
        return new SkillRoutingResult(false, null, 0.0, List.of(), reason);
    }
}