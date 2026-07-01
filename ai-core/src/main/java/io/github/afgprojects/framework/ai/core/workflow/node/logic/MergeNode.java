package io.github.afgprojects.framework.ai.core.workflow.node.logic;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.NodeOutput;
import io.github.afgprojects.framework.ai.core.api.workflow.definition.ParamType;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Out;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Param;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Merge node - combines outputs from multiple parallel branches.
 *
 * <p>Collects outputs from multiple upstream nodes (typically after a parallel
 * execution) and merges them into a single output. Supports different merge
 * strategies for combining data.</p>
 *
 * <p>Parameters are declared on {@link Params}.</p>
 */
@Slf4j
public class MergeNode extends AbstractWorkflowNode<MergeNode.Params> {

    public static final String TYPE = "merge";

    /** Strongly-typed parameters for {@link MergeNode}. */
    public record Params(
            @Param(displayName = "Merge strategy", description = "Merge strategy",
                    type = ParamType.ENUM, enumValues = {"merge_all", "first", "last", "flatten"},
                    defaultValue = "merge_all")
            String strategy,
            @Param(displayName = "Source nodes", description = "List of node IDs to merge outputs from")
            List<String> sourceNodes,
            @Param(displayName = "Key mapping", description = "Map to rename keys during merge")
            Map<String, String> keyMapping
    ) {
        /** Effective strategy, defaulting to merge_all. */
        public String effectiveStrategy() {
            return strategy == null || strategy.isBlank() ? "merge_all" : strategy;
        }
    }

    /** Output descriptor for {@link MergeNode}. */
    public record Output(
            @Out(description = "Number of merged sources") int mergedCount
    ) {}

    public MergeNode(String nodeId) {
        super(nodeId, TYPE, Params.class);
    }

    @Override
    protected Class<?> outputRecordType() {
        return Output.class;
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Params params) {
        String strategy = params.effectiveStrategy();
        List<String> sourceNodes = params.sourceNodes();

        log.debug("MergeNode [{}] merging with strategy={}, sources={}", getNodeId(), strategy, sourceNodes);

        Map<String, NodeOutput> nodeOutputs = context.getNodeOutputs();
        List<Map<String, Object>> collectedData = new ArrayList<>();

        if (sourceNodes != null && nodeOutputs != null) {
            for (String sourceNodeId : sourceNodes) {
                NodeOutput output = nodeOutputs.get(sourceNodeId);
                if (output != null && output.data() != null) {
                    collectedData.add(output.data());
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mergedCount", collectedData.size());

        switch (strategy) {
            case "first" -> {
                if (!collectedData.isEmpty()) {
                    result.putAll(collectedData.get(0));
                }
            }
            case "last" -> {
                if (!collectedData.isEmpty()) {
                    result.putAll(collectedData.get(collectedData.size() - 1));
                }
            }
            case "flatten" -> {
                result.put("items", collectedData);
            }
            default -> {
                // merge_all and any unknown strategy
                for (Map<String, Object> data : collectedData) {
                    result.putAll(data);
                }
            }
        }

        return result;
    }
}
