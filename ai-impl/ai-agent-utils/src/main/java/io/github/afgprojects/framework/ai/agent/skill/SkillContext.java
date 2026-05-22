package io.github.afgprojects.framework.ai.agent.skill;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Skill 执行上下文
 *
 * <p>包含执行 Skill 所需的所有信息：输入参数、工具注册表、执行状态等。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class SkillContext {

    private final String skillName;
    private final Map<String, Object> inputs;
    private final Map<String, Object> variables;
    private final SkillContext parent;

    public SkillContext(@NonNull String skillName, @NonNull Map<String, Object> inputs) {
        this(skillName, inputs, Map.of(), null);
    }

    public SkillContext(
        @NonNull String skillName,
        @NonNull Map<String, Object> inputs,
        @NonNull Map<String, Object> variables,
        @Nullable SkillContext parent
    ) {
        this.skillName = skillName;
        this.inputs = inputs;
        this.variables = variables;
        this.parent = parent;
    }

    /**
     * 获取 Skill 名称
     */
    @NonNull
    public String getSkillName() {
        return skillName;
    }

    /**
     * 获取输入参数
     */
    @NonNull
    public Map<String, Object> getInputs() {
        return inputs;
    }

    /**
     * 获取指定输入参数
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getInput(@NonNull String name) {
        return (T) inputs.get(name);
    }

    /**
     * 获取指定输入参数，带默认值
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public <T> T getInput(@NonNull String name, @NonNull T defaultValue) {
        Object value = inputs.get(name);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * 获取变量
     */
    @NonNull
    public Map<String, Object> getVariables() {
        return variables;
    }

    /**
     * 设置变量
     */
    public void setVariable(@NonNull String name, @Nullable Object value) {
        variables.put(name, value);
    }

    /**
     * 获取变量
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getVariable(@NonNull String name) {
        return (T) variables.get(name);
    }

    /**
     * 获取父上下文
     */
    @Nullable
    public SkillContext getParent() {
        return parent;
    }

    /**
     * 创建子上下文
     */
    @NonNull
    public SkillContext createChild(@NonNull String skillName, @NonNull Map<String, Object> inputs) {
        return new SkillContext(skillName, inputs, variables, this);
    }
}
