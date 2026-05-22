package io.github.afgprojects.framework.ai.agent.skill;

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
    /**
     * Skill 唯一标识
     */
    @NonNull String name,

    /**
     * Skill 描述
     */
    @NonNull String description,

    /**
     * 提示词模板
     */
    @NonNull String prompt,

    /**
     * 输入参数定义
     */
    @Nullable List<InputParameter> inputs,

    /**
     * 可用的工具列表
     */
    @Nullable List<String> tools,

    /**
     * 依赖的其他 skills
     */
    @Nullable List<String> dependsOn,

    /**
     * 扩展元数据
     */
    @Nullable Map<String, Object> metadata
) {

    /**
     * 输入参数定义
     */
    public record InputParameter(
        /**
         * 参数名
         */
        @NonNull String name,

        /**
         * 参数描述
         */
        @Nullable String description,

        /**
         * 参数类型
         */
        @NonNull ParameterType type,

        /**
         * 是否必填
         */
        boolean required,

        /**
         * 默认值
         */
        @Nullable String defaultValue,

        /**
         * 枚举值（用于 type=ENUM 时）
         */
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
