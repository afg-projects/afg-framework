package io.github.afgprojects.framework.ai.core.workflow.node.transform;

import io.github.afgprojects.framework.ai.core.api.workflow.definition.ParamType;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Out;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Param;
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
 * <p>Parameters are declared on {@link Params} so the node is self-describing.</p>
 */
@Slf4j
public class FilterNode extends AbstractWorkflowNode<FilterNode.Params> {

    public static final String TYPE = "filter";

    /** Strongly-typed parameters for {@link FilterNode}. */
    public record Params(
            @Param(displayName = "Items", description = "List of items to filter", required = true)
            List<Object> items,
            @Param(displayName = "Field", description = "Field name to filter on (for item maps)")
            String field,
            @Param(displayName = "Operator", description = "Filter operator",
                    type = ParamType.ENUM,
                    enumValues = {"eq", "ne", "contains", "gt", "lt", "regex"},
                    defaultValue = "eq")
            String operator,
            @Param(displayName = "Value", description = "Value to compare against")
            Object value,
            @Param(displayName = "Condition", description = "Custom predicate expression")
            String condition
    ) {
        /** Effective operator, defaulting to "eq". */
        public String effectiveOperator() {
            return operator == null || operator.isBlank() ? "eq" : operator;
        }
    }

    /** Output descriptor for {@link FilterNode}. */
    public record Output(
            @Out(description = "Filtered items") List<Object> filteredItems,
            @Out(description = "Original count") int originalCount,
            @Out(description = "Filtered count") int filteredCount
    ) {}

    public FilterNode(String nodeId) {
        super(nodeId, TYPE, Params.class);
    }

    @Override
    protected Class<?> outputRecordType() {
        return Output.class;
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Params params) {
        List<Object> items = params.items();
        String field = params.field();
        String operator = params.effectiveOperator();
        Object value = params.value();

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
}
