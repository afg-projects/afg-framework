package io.github.afgprojects.framework.ai.core.workflow.node.logic;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loop node - iterates over a collection or a specified number of times.
 *
 * <p>Executes a sub-workflow or repeated logic for each item in a collection
 * or for a specified number of iterations. Collects results from each iteration.</p>
 *
 * <p>Parameters:</p>
 * <ul>
 *   <li>{@code items} (optional) - a list of items to iterate over</li>
 *   <li>{@code count} (optional) - number of iterations (if items not provided)</li>
 *   <li>{@code itemVariable} (optional) - variable name for current item, defaults to "item"</li>
 *   <li>{@code indexVariable} (optional) - variable name for current index, defaults to "index"</li>
 * </ul>
 *
 * <p><strong>Alpha feature:</strong> The loop body execution requires integration with
 * the DAG engine's sub-workflow execution capability. Current implementation collects
 * iteration metadata; actual sub-node execution will be added in a future version.</p>
 */
@Slf4j
public class LoopNode extends AbstractWorkflowNode {

    public static final String TYPE = "loop";

    public LoopNode(String nodeId) {
        super(nodeId, TYPE);
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Object> items = (List<Object>) params.get("items");
        Integer count = getIntParam(params, "count", null);
        String itemVariable = getParam(params, "itemVariable", "item");
        String indexVariable = getParam(params, "indexVariable", "index");

        int iterations;
        if (items != null) {
            iterations = items.size();
            log.debug("LoopNode [{}] iterating over {} items", getNodeId(), iterations);
        } else if (count != null) {
            iterations = count;
            items = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                items.add(i);
            }
            log.debug("LoopNode [{}] iterating {} times", getNodeId(), iterations);
        } else {
            throw new IllegalArgumentException("Either 'items' or 'count' parameter is required");
        }

        List<Map<String, Object>> iterationResults = new ArrayList<>();
        for (int i = 0; i < iterations; i++) {
            Object item = items.get(i);
            Map<String, Object> iterationData = new LinkedHashMap<>();
            iterationData.put(indexVariable, i);
            iterationData.put(itemVariable, item);
            iterationResults.add(iterationData);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("iterations", iterations);
        result.put("iterationResults", iterationResults);
        result.put("completed", true);

        return result;
    }

    private Integer getIntParam(Map<String, Object> params, String key, Integer defaultValue) {
        Object value = params.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number num) return num.intValue();
        return Integer.parseInt(value.toString());
    }

    private String getParam(Map<String, Object> params, String key, String defaultValue) {
        Object value = params.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}
