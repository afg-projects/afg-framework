package io.github.afgprojects.framework.ai.core.workflow.node.transform;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Filter node - filters items in a collection based on conditions.
 *
 * <p>Applies filter conditions to a list of items, keeping only those
 * that match the specified criteria.</p>
 *
 * <p>Parameters:</p>
 * <ul>
 *   <li>{@code items} (required) - list of items to filter</li>
 *   <li>{@code field} (optional) - field name to filter on (for item maps)</li>
 *   <li>{@code operator} (optional) - filter operator: "eq", "ne", "contains", "gt", "lt", "regex"</li>
 *   <li>{@code value} (optional) - value to compare against</li>
 *   <li>{@code condition} (optional) - custom predicate expression</li>
 * </ul>
 */
@Slf4j
public class FilterNode extends AbstractWorkflowNode {

    public static final String TYPE = "filter";

    public FilterNode(String nodeId) {
        super(nodeId, TYPE);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Map<String, Object> doExecute(ExecutionContext context, Map<String, Object> params) {
        List<Object> items = (List<Object>) params.get("items");
        if (items == null) {
            throw new IllegalArgumentException("Required parameter 'items' is missing");
        }

        String field = getParam(params, "field", null);
        String operator = getParam(params, "operator", "eq");
        Object value = params.get("value");

        log.debug("FilterNode [{}] filtering {} items with {} on {}", getNodeId(), items.size(), operator, field);

        List<Object> filtered = new ArrayList<>();

        for (Object item : items) {
            boolean matches = evaluateCondition(item, field, operator, value);
            if (matches) {
                filtered.add(item);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("filteredItems", filtered);
        result.put("originalCount", items.size());
        result.put("filteredCount", filtered.size());
        return result;
    }

    @SuppressWarnings("unchecked")
    private boolean evaluateCondition(Object item, String field, String operator, Object value) {
        Object fieldValue = null;
        if (item instanceof Map<?, ?> map && field != null) {
            fieldValue = ((Map<String, Object>) map).get(field);
        } else if (field == null) {
            fieldValue = item;
        } else {
            return true; // Can't filter on field of non-map item
        }

        if (value == null) {
            return "eq".equals(operator) ? fieldValue == null : fieldValue != null;
        }

        String fieldStr = fieldValue != null ? fieldValue.toString() : null;
        String valueStr = value.toString();

        return switch (operator.toLowerCase()) {
            case "eq" -> valueStr.equals(fieldStr);
            case "ne" -> !valueStr.equals(fieldStr);
            case "contains" -> fieldStr != null && fieldStr.contains(valueStr);
            case "gt" -> fieldStr != null && fieldStr.compareTo(valueStr) > 0;
            case "lt" -> fieldStr != null && fieldStr.compareTo(valueStr) < 0;
            case "regex" -> fieldStr != null && fieldStr.matches(valueStr);
            default -> true;
        };
    }

    private String getParam(Map<String, Object> params, String key, String defaultValue) {
        Object value = params.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}
