package io.github.afgprojects.framework.ai.core.api.skill;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Skill 定义
 *
 * <p>一个 Skill 代表一个可复用的能力，通过 YAML/Markdown 文件定义。
 * 包含名称、描述、提示词模板、输入参数定义等。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public record SkillDefinition(
    @NonNull String name,
    @NonNull String description,
    @NonNull String prompt,
    @Nullable List<InputParameter> inputs,
    @Nullable List<String> tools,
    @Nullable List<String> dependsOn,
    @Nullable Map<String, Object> metadata
) {

    /**
     * 输入参数定义
     */
    public record InputParameter(
        @NonNull String name,
        @Nullable String description,
        @NonNull ParameterType type,
        boolean required,
        @Nullable String defaultValue,
        @Nullable List<String> enumValues
    ) {}

    /**
     * 参数类型
     */
    public enum ParameterType {
        STRING,
        INTEGER,
        NUMBER,
        BOOLEAN,
        ENUM,
        ARRAY,
        OBJECT
    }
}