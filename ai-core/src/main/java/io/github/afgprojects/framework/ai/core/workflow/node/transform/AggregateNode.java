package io.github.afgprojects.framework.ai.core.workflow.node.transform;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregate node - aggregates data from a collection.
 *
 * <p>Performs aggregation operations on a list of items, such as
 * counting, summing, averaging, grouping, etc.</p>
 *
 * <p>Parameters:</p>
 * <ul>
 *   <li>{@code items} (required) - list of items to aggregate</li>
 *   <li>{@code operation} (required) - aggregation operation: "count", "sum", "avg", "min", "max", "group"</li>
 *   <li>{@code field} (optional) - field name to aggregate on (for item maps)</li>
 *   <li>{@code groupBy} (optional) - field name to group by (for "group" operation)</li>
 * </ul>
 */
@Slf4j
public class AggregateNode extends AbstractWorkflowNode {

    public static final String TYPE = "aggregate";

    public AggregateNode(String nodeId) {
        super(nodeId, TYPE);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Map<String, Object> doExecute(ExecutionContext context, Map<String, Object> params) {
        List<Object> items = (List<Object>) params.get("items");
        if (items == null) {
            throw new IllegalArgumentException("Required parameter 'items' is missing");
        }

        String operation = getRequiredParam(params, "operation");
        String field = getParam(params, "field", null);

        log.debug("AggregateNode [{}] applying {} on {} items", getNodeId(), operation, items.size());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("operation", operation);
        result.put("itemCount", items.size());

        switch (operation.toLowerCase()) {
            case "count" -> result.put("result", items.size());
            case "sum" -> result.put("result", sumField(items, field));
            case "avg" -> result.put("result", avgField(items, field));
            case "min" -> result.put("result", minField(items, field));
            case "max" -> result.put("result", maxField(items, field));
            case "group" -> {
                String groupBy = getRequiredParam(params, "groupBy");
                result.put("result", groupBy(items, groupBy));
            }
            default -> throw new IllegalArgumentException("Unknown aggregation operation: " + operation);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private double sumField(List<Object> items, String field) {
        double sum = 0;
        for (Object item : items) {
            Object value = extractFieldValue(item, field);
            if (value instanceof Number num) {
                sum += num.doubleValue();
            }
        }
        return sum;
    }

    @SuppressWarnings("unchecked")
    private double avgField(List<Object> items, String field) {
        if (items.isEmpty()) return 0;
        return sumField(items, field) / items.size();
    }

    @SuppressWarnings("unchecked")
    private double minField(List<Object> items, String field) {
        double min = Double.MAX_VALUE;
        for (Object item : items) {
            Object value = extractFieldValue(item, field);
            if (value instanceof Number num) {
                min = Math.min(min, num.doubleValue());
            }
        }
        return min == Double.MAX_VALUE ? 0 : min;
    }

    @SuppressWarnings("unchecked")
    private double maxField(List<Object> items, String field) {
        double max = Double.MIN_VALUE;
        for (Object item : items) {
            Object value = extractFieldValue(item, field);
            if (value instanceof Number num) {
                max = Math.max(max, num.doubleValue());
            }
        }
        return max == Double.MIN_VALUE ? 0 : max;
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<Object>> groupBy(List<Object> items, String groupByField) {
        Map<String, List<Object>> groups = new LinkedHashMap<>();
        for (Object item : items) {
            Object groupValue = extractFieldValue(item, groupByField);
            String groupKey = groupValue != null ? groupValue.toString() : "null";
            groups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(item);
        }
        return groups;
    }

    @SuppressWarnings("unchecked")
    private Object extractFieldValue(Object item, String field) {
        if (field == null) return item;
        if (item instanceof Map<?, ?> map) {
            return ((Map<String, Object>) map).get(field);
        }
        return item;
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
}
