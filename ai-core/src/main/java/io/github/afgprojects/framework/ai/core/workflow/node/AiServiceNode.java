package io.github.afgprojects.framework.ai.core.workflow.node;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.NodeOutput;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract base class for workflow nodes that invoke an AI service.
 * <p>
 * Subclasses provide the AI service instance, prompt template, and the actual LLM
 * invocation logic via {@link #doExecute}. The base class handles prompt template
 * rendering from workflow parameters and delegates execution to the subclass.
 * <p>
 * Usage:
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
 *     protected Map&lt;String, Object&gt; doExecute(ExecutionContext ctx,
 *             Map&lt;String, Object&gt; params, String prompt, String systemPrompt) {
 *         String result = aiService.chat(systemPrompt, prompt);
 *         return Map.of("content", result);
 *     }
 * }
 * </pre>
 */
@Slf4j
public abstract class AiServiceNode implements WorkflowNode {

    private final String nodeId;
    private final String type;

    protected AiServiceNode(String nodeId, String type) {
        this.nodeId = nodeId;
        this.type = type;
    }

    /**
     * Returns the AI service instance used by this node.
     * The return type is Object to allow different AI service interface types.
     */
    protected abstract Object getAiService();

    /**
     * Returns the prompt template string with {{variable}} placeholders.
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
    protected abstract Map<String, Object> doExecute(ExecutionContext context,
                                                      Map<String, Object> params,
                                                      String prompt,
                                                      String systemPrompt);

    @Override
    public String getNodeId() {
        return nodeId;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public NodeOutput execute(ExecutionContext context, Map<String, Object> params) {
        long startTime = System.currentTimeMillis();
        try {
            // Render prompt template with params as variables
            String prompt = renderPrompt(params);
            String systemPrompt = getSystemPrompt();

            log.debug("Node [{}] executing with prompt: {}", nodeId, truncate(prompt, 200));

            Map<String, Object> result = doExecute(context, params, prompt, systemPrompt);

            long duration = System.currentTimeMillis() - startTime;
            return NodeOutput.of(result).withDuration(duration);

        } catch (Exception e) {
            log.error("Node [{}] execution failed", nodeId, e);
            long duration = System.currentTimeMillis() - startTime;
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("error", e.getMessage() != null ? e.getMessage() : "Unknown error");
            return NodeOutput.of(errorData).withDuration(duration);
        }
    }

    @Override
    public Flux<NodeEvent> executeStream(ExecutionContext context, Map<String, Object> params) {
        // Default: delegate to synchronous execute()
        // Subclasses can override for true streaming support
        NodeOutput output = execute(context, params);
        return Flux.just(NodeEvent.complete(output));
    }

    /**
     * Renders the prompt template by substituting {{variable}} placeholders
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
