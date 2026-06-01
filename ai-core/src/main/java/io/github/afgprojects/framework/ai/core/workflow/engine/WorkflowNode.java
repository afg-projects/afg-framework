package io.github.afgprojects.framework.ai.core.workflow.engine;

import java.util.Map;
import reactor.core.publisher.Flux;

public interface WorkflowNode {
    String getNodeId();
    String getType();
    NodeOutput execute(ExecutionContext context, Map<String, Object> params);
    Flux<NodeEvent> executeStream(ExecutionContext context, Map<String, Object> params);

    record NodeEvent(String type, String content) {
        public static NodeEvent text(String content) {
            return new NodeEvent("TEXT", content);
        }
        public static NodeEvent complete(NodeOutput output) {
            return new NodeEvent("COMPLETE", null);
        }
    }
}
