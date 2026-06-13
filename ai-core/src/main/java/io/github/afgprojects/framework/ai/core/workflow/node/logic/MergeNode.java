package io.github.afgprojects.framework.ai.core.workflow.node.logic;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.NodeOutput;
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
 * <p>Parameters:</p>
 * <ul>
 *   <li>{@code strategy} (optional) - merge strategy: "merge_all" (default), "first", "last", "flatten"</li>
 *   <li>{@code sourceNodes} (optional) - list of node IDs to merge outputs from</li>
 *   <li>{@code keyMapping} (optional) - Map to rename keys during merge</li>
 * </ul>
 */
@Slf4j
public class MergeNode extends AbstractWorkflowNode {

    public static final String TYPE = "merge";

    public MergeNode(String nodeId) {
        super(nodeId, TYPE);
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Map<String, Object> params) {
        String strategy = getParam(params, "strategy", "merge_all");

        @SuppressWarnings("unchecked")
        List<String> sourceNodes = (List<String>) params.get("sourceNodes");

        log.debug("MergeNode [{}] merging with strategy={}, sources={}", getNodeId(), strategy, sourceNodes);

        Map<String, NodeOutput> nodeOutputs = context.getNodeOutputs();
        List<Map<String, Object>> collectedData = new ArrayList<>();

        if (sourceNodes != null) {
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
            case "merge_all" -> {
                for (Map<String, Object> data : collectedData) {
                    result.putAll(data);
                }
            }
            default -> {
                for (Map<String, Object> data : collectedData) {
                    result.putAll(data);
                }
            }
        }

        return result;
    }

    private String getParam(Map<String, Object> params, String key, String defaultValue) {
        Object value = params.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}
