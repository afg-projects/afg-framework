package io.github.afgprojects.framework.ai.core.workflow.node.transform;

import io.github.afgprojects.framework.ai.core.api.workflow.definition.ParamType;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Out;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Param;
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
 * <p>Parameters are declared on {@link Params} so the node is self-describing.</p>
 */
@Slf4j
public class TextTransformNode extends AbstractWorkflowNode<TextTransformNode.Params> {

    public static final String TYPE = "text-transform";

    /** Strongly-typed parameters for {@link TextTransformNode}. */
    public record Params(
            @Param(displayName = "Text", description = "Input text to transform", required = true)
            String text,
            @Param(displayName = "Operation", description = "Transformation operation",
                    type = ParamType.ENUM,
                    enumValues = {"replace", "trim", "uppercase", "lowercase", "capitalize", "template"},
                    required = true)
            String operation,
            @Param(displayName = "Pattern", description = "Regex pattern for \"replace\" operation")
            String pattern,
            @Param(displayName = "Replacement", description = "Replacement string for \"replace\" operation",
                    defaultValue = "")
            String replacement,
            @Param(displayName = "Template", description = "Template string for \"template\" operation")
            String template
    ) {
        /** Effective replacement, defaulting to empty string. */
        public String effectiveReplacement() {
            return replacement == null ? "" : replacement;
        }
    }

    /** Output descriptor for {@link TextTransformNode}. */
    public record Output(
            @Out(description = "Transformed text") String text,
            @Out(description = "Text length") int length
    ) {}

    public TextTransformNode(String nodeId) {
        super(nodeId, TYPE, Params.class);
    }

    @Override
    protected Class<?> outputRecordType() {
        return Output.class;
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Params params) {
        String text = params.text();
        String operation = params.operation();

        log.debug("TextTransformNode [{}] applying operation: {}", getNodeId(), operation);

        String result = switch (operation.toLowerCase()) {
            case "trim" -> text.trim();
            case "uppercase" -> text.toUpperCase();
            case "lowercase" -> text.toLowerCase();
            case "capitalize" -> text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
            case "replace" -> {
                String pattern = params.pattern();
                String replacement = params.effectiveReplacement();
                yield text.replaceAll(pattern, replacement);
            }
            case "template" -> {
                String template = params.template();
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
}
