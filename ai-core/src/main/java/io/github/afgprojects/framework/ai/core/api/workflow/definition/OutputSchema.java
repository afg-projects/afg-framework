package io.github.afgprojects.framework.ai.core.api.workflow.definition;

public record OutputSchema(
    String name,
    ParamType type,
    String description,
    boolean optional
) {
    public static OutputSchema of(ParamType type, String description) {
        return new OutputSchema(null, type, description, false);
    }

    public static OutputSchema of(ParamType type, String description, boolean optional) {
        return new OutputSchema(null, type, description, optional);
    }
}
