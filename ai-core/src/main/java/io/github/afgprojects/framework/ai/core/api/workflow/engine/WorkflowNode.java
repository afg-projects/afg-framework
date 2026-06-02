package io.github.afgprojects.framework.ai.core.api.workflow.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import reactor.core.publisher.Flux;

public interface WorkflowNode {
    String getNodeId();
    String getType();
    NodeOutput execute(ExecutionContext context, Map<String, Object> params);
    Flux<NodeEvent> executeStream(ExecutionContext context, Map<String, Object> params);

    ObjectMapper NODE_EVENT_MAPPER = new ObjectMapper();

    record NodeEvent(String type, String content) {
        public static NodeEvent text(String content) {
            return new NodeEvent("TEXT", content);
        }
        public static NodeEvent complete(NodeOutput output) {
            if (output == null) {
                return new NodeEvent("COMPLETE", null);
            }
            try {
                return new NodeEvent("COMPLETE", NODE_EVENT_MAPPER.writeValueAsString(output));
            } catch (JsonProcessingException e) {
                // Fallback to simple string representation
                return new NodeEvent("COMPLETE", String.format(
                    "{\"data\":%s,\"anchor\":\"%s\",\"tokenInput\":%d,\"tokenOutput\":%d,\"durationMs\":%d}",
                    output.data() != null ? output.data().toString() : "null",
                    output.anchor() != null ? output.anchor() : "output",
                    output.tokenInput(),
                    output.tokenOutput(),
                    output.durationMs()
                ));
            }
        }
    }
}
