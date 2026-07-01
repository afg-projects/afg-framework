package io.github.afgprojects.framework.ai.core.workflow.node.ai;

import io.github.afgprojects.framework.ai.core.api.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.api.chat.AiChatMetadata;
import io.github.afgprojects.framework.ai.core.api.chat.AiChatResponse;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.NodeOutput;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.WorkflowNode;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Out;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Param;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI Chat node - invokes an AI chat model and returns the response.
 *
 * <p>Sends a user message (with optional system prompt) to an {@link AfgChatClient}
 * and provides the AI response as workflow output data. When {@code streaming} is
 * true and a chat client is configured, {@link #doExecuteStream} emits TEXT events
 * for each token chunk followed by a COMPLETE event.</p>
 *
 * <p>Parameters are declared on {@link Params} so the node is self-describing. The
 * chat client is a construction-time dependency (not a parameter) supplied by the
 * node factory.</p>
 */
@Slf4j
public class AiChatNode extends AbstractWorkflowNode<AiChatNode.Params> {

    public static final String TYPE = "ai-chat";

    /** Strongly-typed parameters for {@link AiChatNode}. */
    public record Params(
            @Param(displayName = "User message", description = "The user message to send", required = true)
            String message,
            @Param(displayName = "System prompt", description = "System prompt for the AI")
            String systemPrompt,
            @Param(displayName = "Client name", description = "Name of the chat client to use")
            String clientName,
            @Param(displayName = "Streaming", description = "Whether to use streaming")
            Boolean streaming
    ) {
        /** Whether streaming output is requested. */
        public boolean isStreaming() {
            return Boolean.TRUE.equals(streaming);
        }
    }

    /** Output descriptor for {@link AiChatNode}. */
    public record Output(
            @Out(description = "AI response content") String content,
            @Out(description = "Model used") String model,
            @Out(description = "Finish reason") String finishReason
    ) {}

    private final AfgChatClient chatClient;

    public AiChatNode(String nodeId, AfgChatClient chatClient) {
        super(nodeId, TYPE, Params.class);
        this.chatClient = chatClient;
    }

    public AiChatNode(String nodeId) {
        this(nodeId, null);
    }

    @Override
    protected Class<?> outputRecordType() {
        return Output.class;
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Params params) {
        return doExecuteRich(context, params).data();
    }

    @Override
    protected NodeResult doExecuteRich(ExecutionContext context, Params params) {
        String message = params.message();
        String systemPrompt = params.systemPrompt();

        log.debug("AiChatNode [{}] sending message: {}", getNodeId(), truncate(message, 200));

        if (chatClient == null) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("message", message);
            result.put("error", "No AfgChatClient available - AI chat not configured");
            return NodeResult.of(result);
        }

        AfgChatClient client = chatClient;
        if (systemPrompt != null) {
            client = client.withSystemPrompt(systemPrompt);
        }

        AiChatResponse response = client.chat(message);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", response.content());
        AiChatMetadata metadata = response.metadata();
        long tokenInput = 0;
        long tokenOutput = 0;
        if (metadata != null) {
            result.put("model", metadata.model());
            result.put("finishReason", metadata.finishReason());
            tokenInput = metadata.promptTokens() != null ? metadata.promptTokens() : 0;
            tokenOutput = metadata.completionTokens() != null ? metadata.completionTokens() : 0;
        }
        return NodeResult.of(result, tokenInput, tokenOutput);
    }

    @Override
    protected Flux<NodeEvent> doExecuteStream(ExecutionContext context, Params params) {
        if (chatClient == null) {
            return Flux.just(NodeEvent.complete(
                    NodeOutput.of(doExecuteRich(context, params).data())));
        }

        String message = params.message();
        String systemPrompt = params.systemPrompt();

        if (params.isStreaming()) {
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

        return Flux.just(NodeEvent.complete(
                NodeOutput.of(doExecuteRich(context, params).data())));
    }

    private static String truncate(String str, int maxLen) {
        if (str == null) return null;
        return str.length() <= maxLen ? str : str.substring(0, maxLen) + "...";
    }
}
