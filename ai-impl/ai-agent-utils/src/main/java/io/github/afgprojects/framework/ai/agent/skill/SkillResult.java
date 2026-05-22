package io.github.afgprojects.framework.ai.agent.skill;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Skill 执行结果
 *
 * @author afg-projects
 * @since 1.0.0
 */
public record SkillResult(
    /**
     * 是否成功
     */
    boolean success,

    /**
     * 输出内容
     */
    @Nullable String output,

    /**
     * 错误信息
     */
    @Nullable String error,

    /**
     * 执行的工具调用记录
     */
    @Nullable List<ToolCallRecord> toolCalls,

    /**
     * 执行的子 skills 记录
     */
    @Nullable List<SkillResult> subSkills,

    /**
     * 执行元数据
     */
    @Nullable Map<String, Object> metadata
) {

    /**
     * 工具调用记录
     */
    public record ToolCallRecord(
        @NonNull String toolName,
        @NonNull String arguments,
        @Nullable String result,
        boolean success
    ) {}

    /**
     * 创建成功结果
     */
    @NonNull
    public static SkillResult success(@Nullable String output) {
        return new SkillResult(true, output, null, null, null, null);
    }

    /**
     * 创建失败结果
     */
    @NonNull
    public static SkillResult failure(@NonNull String error) {
        return new SkillResult(false, null, error, null, null, null);
    }
}