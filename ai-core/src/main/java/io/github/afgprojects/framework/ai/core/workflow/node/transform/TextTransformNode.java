package io.github.afgprojects.framework.ai.core.workflow.node.transform;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Text transform node - applies text transformations.
 *
 * <p>Applies text transformations such as regex replacement, trimming,
 * case conversion, template rendering, and custom transformations.</p>
 *
 * <p>Parameters:</p>
 * <ul>
 *   <li>{@code text} (required) - input text to transform</li>
 *   <li>{@code operation} (required) - transformation operation: "replace", "trim", "uppercase", "lowercase", "template"</li>
 *   <li>{@code pattern} (optional) - regex pattern for "replace" operation</li>
 *   <li>{@code replacement} (optional) - replacement string for "replace" operation</li>
 *   <li>{@code template} (optional) - template string for "template" operation</li>
 * </ul>
 */
@Slf4j
public class TextTransformNode extends AbstractWorkflowNode {

    public static final String TYPE = "text-transform";

    public TextTransformNode(String nodeId) {
        super(nodeId, TYPE);
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Map<String, Object> params) {
        String text = getRequiredParam(params, "text");
        String operation = getRequiredParam(params, "operation");

        log.debug("TextTransformNode [{}] applying operation: {}", getNodeId(), operation);

        String result = switch (operation.toLowerCase()) {
            case "trim" -> text.trim();
            case "uppercase" -> text.toUpperCase();
            case "lowercase" -> text.toLowerCase();
            case "capitalize" -> text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
            case "replace" -> {
                String pattern = getRequiredParam(params, "pattern");
                String replacement = getParam(params, "replacement", "");
                yield text.replaceAll(pattern, replacement);
            }
            case "template" -> {
                String template = getRequiredParam(params, "template");
                yield renderTemplate(template, context);
            }
            default -> throw new IllegalArgumentException("Unknown text transform operation: " + operation);
        };

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("text", result);
        output.put("length", result.length());
        return output;
    }

    private String renderTemplate(String template, ExecutionContext context) {
        // Simple {{variable}} substitution from context variables
        String result = template;
        Map<String, Object> variables = context.getVariables();
        if (variables != null) {
            Pattern pattern = Pattern.compile("\\{\\{(\\w+)}}");
            Matcher matcher = pattern.matcher(template);
            StringBuilder sb = new StringBuilder();
            while (matcher.find()) {
                String varName = matcher.group(1);
                Object value = variables.get(varName);
                matcher.appendReplacement(sb, value != null ? value.toString() : "");
            }
            matcher.appendTail(sb);
            result = sb.toString();
        }
        return result;
    }

    private String getRequiredParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Required parameter '" + key + "' is missing");
        }
        return value.toString();
    }

    private String getParam(Map<String, Object> params, String key, String defaultValue) {
        Object value = params.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}
