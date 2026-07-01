package io.github.afgprojects.framework.ai.core.workflow.node.example;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Out;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Param;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Example custom node - echoes its input with an optional prefix.
 *
 * <p><strong>R5 extensibility demo:</strong> this node exists to prove that a new
 * node type can be added by declaring a typed {@link Params} record with
 * {@link Param} annotations and extending {@link AbstractWorkflowNode} — no
 * hand-written param-getter boilerplate, no separate schema literal. The schema,
 * validation, default-value fill, and execution entry point all derive from the
 * record. Registering it with a {@link io.github.afgprojects.framework.ai.core.workflow.node.NodeFactory}
 * is the only additional step.</p>
 */
@Slf4j
public class EchoNode extends AbstractWorkflowNode<EchoNode.Params> {

    public static final String TYPE = "echo";

    /** Strongly-typed parameters for {@link EchoNode}. */
    public record Params(
            @Param(displayName = "Message", description = "Text to echo back", required = true)
            String message,
            @Param(displayName = "Prefix", description = "Optional prefix prepended to the message",
                    defaultValue = "echo: ")
            String prefix
    ) {
        /** Effective prefix, defaulting to "echo: ". */
        public String effectivePrefix() {
            return prefix == null || prefix.isBlank() ? "echo: " : prefix;
        }
    }

    /** Output descriptor for {@link EchoNode}. */
    public record Output(
            @Out(description = "Echoed text") String text,
            @Out(description = "Original message") String original
    ) {}

    public EchoNode(String nodeId) {
        super(nodeId, TYPE, Params.class);
    }

    @Override
    protected Class<?> outputRecordType() {
        return Output.class;
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Params params) {
        String text = params.effectivePrefix() + params.message();
        log.debug("EchoNode [{}] echoing: {}", getNodeId(), text);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("text", text);
        result.put("original", params.message());
        return result;
    }
}
