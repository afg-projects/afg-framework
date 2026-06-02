package io.github.afgprojects.framework.ai.core.api.workflow.definition;

public record ParamSchema(
    String name,
    ParamType type,
    String displayName,
    String description,
    boolean required,
    Object defaultValue,
    String enumValues
) {
    public static ParamSchema of(String name, ParamType type, String displayName, boolean required) {
        return new ParamSchema(name, type, displayName, null, required, null, null);
    }

    public static ParamSchema of(String name, ParamType type, String displayName, boolean required, Object defaultValue) {
        return new ParamSchema(name, type, displayName, null, required, defaultValue, null);
    }
}
