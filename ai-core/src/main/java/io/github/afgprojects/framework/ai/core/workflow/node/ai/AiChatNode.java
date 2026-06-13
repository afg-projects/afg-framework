package io.github.afgprojects.framework.ai.core.workflow.node.ai;

import io.github.afgprojects.framework.ai.core.api.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.api.chat.AiChatMetadata;
import io.github.afgprojects.framework.ai.core.api.chat.AiChatResponse;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.NodeOutput;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI Chat node - invokes an AI chat model and returns the response.
 *
 * <p>Sends a user message (with optional system prompt) to an {@link AfgChatClient}
 * and provides the AI response as workflow output data.</p>
 *
 * <p>Parameters:</p>
 * <ul>
 *   <li>{@code message} (required) - the user message to send</li>
 *   <li>{@code systemPrompt} (optional) - system prompt for the AI</li>
 *   <li>{@code clientName} (optional) - name of the chat client to use</li>
 *   <li>{@code streaming} (optional) - whether to use streaming, defaults to false</li>
 * </ul>
 */
@Slf4j
public class AiChatNode implements WorkflowNode {

    public static final String TYPE = "ai-chat";

    private final String nodeId;
    private final AfgChatClient chatClient;

    public AiChatNode(String nodeId, AfgChatClient chatClient) {
        this.nodeId = nodeId;
        this.chatClient = chatClient;
    }

    public AiChatNode(String nodeId) {
        this.nodeId = nodeId;
        this.chatClient = null;
    }

    @Override
    public String getNodeId() {
        return nodeId;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public NodeOutput execute(ExecutionContext context, Map<String, Object> params) {
        long startTime = System.currentTimeMillis();
        try {
            String message = getRequiredParam(params, "message");
            String systemPrompt = getParam(params, "systemPrompt", null);

            log.debug("AiChatNode [{}] sending message: {}", nodeId, truncate(message, 200));

            if (chatClient == null) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("message", message);
                result.put("error", "No AfgChatClient available - AI chat not configured");
                long duration = System.currentTimeMillis() - startTime;
                return NodeOutput.of(result).withDuration(duration);
            }

            AfgChatClient client = chatClient;
            if (systemPrompt != null) {
                client = client.withSystemPrompt(systemPrompt);
            }

            AiChatResponse response = client.chat(message);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("content", response.content());
            AiChatMetadata metadata = response.metadata();
            if (metadata != null) {
                result.put("model", metadata.model());
                result.put("finishReason", metadata.finishReason());
            }
            long duration = System.currentTimeMillis() - startTime;
            long tokenInput = metadata != null && metadata.promptTokens() != null ? metadata.promptTokens() : 0;
            long tokenOutput = metadata != null && metadata.completionTokens() != null ? metadata.completionTokens() : 0;
            return NodeOutput.of(result).withDuration(duration)
                    .withTokenUsage(tokenInput, tokenOutput);

        } catch (Exception e) {
            log.error("AiChatNode [{}] execution failed", nodeId, e);
            long duration = System.currentTimeMillis() - startTime;
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("error", e.getMessage() != null ? e.getMessage() : "Unknown error");
            return NodeOutput.of(errorData).withDuration(duration);
        }
    }

    @Override
    public Flux<NodeEvent> executeStream(ExecutionContext context, Map<String, Object> params) {
        if (chatClient == null) {
            return Flux.just(NodeEvent.complete(execute(context, params)));
        }

        String message = getRequiredParam(params, "message");
        String systemPrompt = getParam(params, "systemPrompt", null);
        Boolean streaming = (Boolean) params.get("streaming");

        if (Boolean.TRUE.equals(streaming)) {
            AfgChatClient client = chatClient;
            if (systemPrompt != null) {
                client = client.withSystemPrompt(systemPrompt);
            }
            return client.chatStream(message)
                    .map(NodeEvent::text)
                    .concatWith(Flux.defer(() -> {
                        Map<String, Object> data = Map.of("streamingComplete", true);
                        return Flux.just(NodeEvent.complete(NodeOutput.of(data)));
                    }));
        }

        return Flux.just(NodeEvent.complete(execute(context, params)));
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

    private static String truncate(String str, int maxLen) {
        if (str == null) return null;
        return str.length() <= maxLen ? str : str.substring(0, maxLen) + "...";
    }
}
