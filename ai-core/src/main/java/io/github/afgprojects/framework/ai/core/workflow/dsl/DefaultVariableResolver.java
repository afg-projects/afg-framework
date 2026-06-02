package io.github.afgprojects.framework.ai.core.workflow.dsl;

import io.github.afgprojects.framework.ai.core.api.workflow.dsl.VariableResolver;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.NodeOutput;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Default implementation of VariableResolver.
 * Variable reference syntax: ${nodeId.key}
 */
public class DefaultVariableResolver implements VariableResolver {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    @Override
    public Object resolve(String expression, Map<String, ?> nodeOutputs) {
        Matcher matcher = VARIABLE_PATTERN.matcher(expression);
        if (!matcher.matches()) {
            return null;
        }

        String ref = matcher.group(1);
        int dotIndex = ref.indexOf('.');
        if (dotIndex < 0) {
            return null;
        }

        String nodeId = ref.substring(0, dotIndex);
        String key = ref.substring(dotIndex + 1);

        Object outputObj = nodeOutputs.get(nodeId);
        if (!(outputObj instanceof NodeOutput output)) {
            return null;
        }

        return output.data().get(key);
    }

    @Override
    public String renderTemplate(String template, Map<String, ?> nodeOutputs) {
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String ref = matcher.group(1);
            int dotIndex = ref.indexOf('.');
            if (dotIndex < 0) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }

            String nodeId = ref.substring(0, dotIndex);
            String key = ref.substring(dotIndex + 1);

            Object outputObj = nodeOutputs.get(nodeId);
            if (!(outputObj instanceof NodeOutput output)) {
                // Leave unresolved variable as-is
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }

            Object value = output.data().get(key);
            if (value == null) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            } else {
                matcher.appendReplacement(result, Matcher.quoteReplacement(value.toString()));
            }
        }

        matcher.appendTail(result);
        return result.toString();
    }
}
