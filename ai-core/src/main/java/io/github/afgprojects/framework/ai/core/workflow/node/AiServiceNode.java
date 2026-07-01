package io.github.afgprojects.framework.ai.core.workflow.node;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Out;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Param;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Abstract base class for workflow nodes that invoke an AI service.
 *
 * <p>Subclasses provide the AI service instance, prompt template, and the actual LLM
 * invocation logic via {@link #doExecuteService}. The base class renders the prompt
 * template from workflow parameters and delegates execution to the subclass, while
 * {@link AbstractWorkflowNode} supplies timing, error handling, logging, and
 * parameter binding.</p>
 *
 * <p>This node uses the migration bridge ({@code P = Map<String, Object>}) because its
 * parameters are the dynamic template variables consumed by
 * {@link #renderPrompt(Map)}; subclasses that want a typed params record should extend
 * {@link AbstractWorkflowNode} directly instead.</p>
 *
 * <p>Usage:
 * <pre>
 * public class MyLlmNode extends AiServiceNode {
 *     private final MyAiService aiService;
 *
 *     public MyLlmNode(String nodeId, MyAiService aiService) {
 *         super(nodeId, "llm");
 *         this.aiService = aiService;
 *     }
 *
 *     protected Object getAiService() { return aiService; }
 *
 *     protected String getPromptTemplate() {
 *         return "Analyze the following: {{input}}";
 *     }
 *
 *     protected Map&lt;String, Object&gt; doExecuteService(ExecutionContext ctx,
 *             Map&lt;String, Object&gt; params, String prompt, String systemPrompt) {
 *         String result = aiService.chat(systemPrompt, prompt);
 *         return Map.of("content", result);
 *     }
 * }
 * </pre>
 */
@Slf4j
public abstract class AiServiceNode extends AbstractWorkflowNode<Map<String, Object>> {

    /** Output descriptor for AI service nodes. */
    public record Output(
            @Out(description = "AI response content") String content
    ) {}

    protected AiServiceNode(String nodeId, String type) {
        super(nodeId, type, Map.class);
    }

    @Override
    protected Class<?> outputRecordType() {
        return Output.class;
    }

    /**
     * Returns the AI service instance used by this node.
     * The return type is Object to allow different AI service interface types.
     */
    protected abstract Object getAiService();

    /**
     * Returns the prompt template string with {@code {{variable}}} placeholders.
     * Variables are resolved from the workflow execution params.
     */
    protected abstract String getPromptTemplate();

    /**
     * Returns an optional system prompt. Defaults to null (no system prompt).
     * Subclasses can override to provide a system-level instruction.
     */
    protected String getSystemPrompt() {
        return null;
    }

    /**
     * Executes the actual LLM call with the rendered prompt.
     *
     * @param context      the workflow execution context
     * @param params       the original (pre-rendered) parameters
     * @param prompt       the rendered user prompt
     * @param systemPrompt the system prompt, or null if none
     * @return a map of output data
     */
    protected abstract Map<String, Object> doExecuteService(ExecutionContext context,
                                                             Map<String, Object> params,
                                                             String prompt,
                                                             String systemPrompt);

    @Override
    protected final Map<String, Object> doExecute(ExecutionContext context, Map<String, Object> params) {
        // Render prompt template with params as variables
        String prompt = renderPrompt(params);
        String systemPrompt = getSystemPrompt();

        log.debug("Node [{}] executing with prompt: {}", getNodeId(), truncate(prompt, 200));

        return doExecuteService(context, params, prompt, systemPrompt);
    }

    /**
     * Renders the prompt template by substituting {@code {{variable}}} placeholders
     * with values from the params map.
     */
    protected String renderPrompt(Map<String, Object> params) {
        String template = getPromptTemplate();
        if (template == null || template.isEmpty()) {
            return "";
        }
        if (params == null || params.isEmpty()) {
            return template;
        }

        // Simple template substitution for {{variable}} placeholders
        String result = template;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }

    private static String truncate(String str, int maxLen) {
        if (str == null) return null;
        return str.length() <= maxLen ? str : str.substring(0, maxLen) + "...";
    }
}
