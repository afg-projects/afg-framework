package io.github.afgprojects.framework.ai.core.workflow.dsl;

public interface VariableResolver {
    Object resolve(String expression, java.util.Map<String, ?> nodeOutputs);
    String renderTemplate(String template, java.util.Map<String, ?> nodeOutputs);
}
