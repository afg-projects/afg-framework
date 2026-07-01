package io.github.afgprojects.framework.ai.core.workflow.node.logic;

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
 * Loop node - iterates over a collection or a specified number of times.
 *
 * <p>Executes a sub-workflow or repeated logic for each item in a collection
 * or for a specified number of iterations. Collects results from each iteration.</p>
 *
 * <p>Parameters are declared on {@link Params}.</p>
 *
 * <p><strong>Alpha feature:</strong> The loop body execution requires integration with
 * the DAG engine's sub-workflow execution capability. Current implementation collects
 * iteration metadata; actual sub-node execution will be added in a future version.</p>
 */
@Slf4j
public class LoopNode extends AbstractWorkflowNode<LoopNode.Params> {

    public static final String TYPE = "loop";

    /** Strongly-typed parameters for {@link LoopNode}. */
    public record Params(
            @Param(displayName = "Items to iterate", description = "A list of items to iterate over")
            List<Object> items,
            @Param(displayName = "Iteration count", description = "Number of iterations (if items not provided)")
            Integer count,
            @Param(displayName = "Item variable name", description = "Variable name for current item", defaultValue = "item")
            String itemVariable,
            @Param(displayName = "Index variable name", description = "Variable name for current index", defaultValue = "index")
            String indexVariable
    ) {
        /** Effective item variable name. */
        public String effectiveItemVariable() {
            return itemVariable == null || itemVariable.isBlank() ? "item" : itemVariable;
        }

        /** Effective index variable name. */
        public String effectiveIndexVariable() {
            return indexVariable == null || indexVariable.isBlank() ? "index" : indexVariable;
        }
    }

    /** Output descriptor for {@link LoopNode}. */
    public record Output(
            @Out(description = "Number of iterations") int iterations,
            @Out(description = "Iteration results") List<Map<String, Object>> iterationResults,
            @Out(description = "Loop completed") boolean completed
    ) {}

    public LoopNode(String nodeId) {
        super(nodeId, TYPE, Params.class);
    }

    @Override
    protected Class<?> outputRecordType() {
        return Output.class;
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Params params) {
        List<Object> items = params.items();
        Integer count = params.count();
        String itemVariable = params.effectiveItemVariable();
        String indexVariable = params.effectiveIndexVariable();

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
}
