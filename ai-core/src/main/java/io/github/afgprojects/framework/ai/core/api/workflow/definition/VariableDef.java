package io.github.afgprojects.framework.ai.core.api.workflow.definition;

public record VariableDef(
    String name,
    ParamType type,
    boolean required
) {
    public static VariableDef of(String name, ParamType type, boolean required) {
        return new VariableDef(name, type, required);
    }
}
