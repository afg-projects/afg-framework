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
 * Aggregate node - aggregates data from a collection.
 *
 * <p>Performs aggregation operations on a list of items, such as
 * counting, summing, averaging, grouping, etc.</p>
 *
 * <p>Parameters are declared on {@link Params} so the node is self-describing.</p>
 */
@Slf4j
public class AggregateNode extends AbstractWorkflowNode<AggregateNode.Params> {

    public static final String TYPE = "aggregate";

    /** Strongly-typed parameters for {@link AggregateNode}. */
    public record Params(
            @Param(displayName = "Items", description = "List of items to aggregate", required = true)
            List<Object> items,
            @Param(displayName = "Operation", description = "Aggregation operation",
                    type = ParamType.ENUM,
                    enumValues = {"count", "sum", "avg", "min", "max", "group"},
                    required = true)
            String operation,
            @Param(displayName = "Field", description = "Field name to aggregate on (for item maps)")
            String field,
            @Param(displayName = "Group by", description = "Field name to group by (for \"group\" operation)")
            String groupBy
    ) {}

    /** Output descriptor for {@link AggregateNode}. */
    public record Output(
            @Out(description = "Aggregation result") Object result,
            @Out(description = "Operation name") String operation,
            @Out(description = "Item count") int itemCount
    ) {}

    public AggregateNode(String nodeId) {
        super(nodeId, TYPE, Params.class);
    }

    @Override
    protected Class<?> outputRecordType() {
        return Output.class;
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Params params) {
        List<Object> items = params.items();
        String operation = params.operation();

        log.debug("AggregateNode [{}] applying {} on {} items", getNodeId(), operation, items.size());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("operation", operation);
        result.put("itemCount", items.size());

        switch (operation.toLowerCase()) {
            case "count" -> result.put("result", items.size());
            case "sum" -> result.put("result", sumField(items, params.field()));
            case "avg" -> result.put("result", avgField(items, params.field()));
            case "min" -> result.put("result", minField(items, params.field()));
            case "max" -> result.put("result", maxField(items, params.field()));
            case "group" -> {
                String groupBy = params.groupBy();
                result.put("result", groupBy(items, groupBy));
            }
            default -> throw new IllegalArgumentException("Unknown aggregation operation: " + operation);
        }

        return result;
    }

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

    private double avgField(List<Object> items, String field) {
        if (items.isEmpty()) return 0;
        return sumField(items, field) / items.size();
    }

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
}
