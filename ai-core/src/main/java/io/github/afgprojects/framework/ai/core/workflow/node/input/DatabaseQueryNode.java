package io.github.afgprojects.framework.ai.core.workflow.node.input;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Out;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Param;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Database query node - executes a database query and returns the result set.
 *
 * <p>Executes a SQL query via the workflow context's data access capabilities
 * and provides the query results as workflow output data.</p>
 *
 * <p>Parameters are declared on {@link Params} so the node is self-describing.</p>
 *
 * <p><strong>Alpha feature:</strong> This node requires a DataManager or equivalent
 * data access bean to be available in the execution context. The current implementation
 * stores the SQL and parameters for later execution by the DAG engine.</p>
 */
@Slf4j
public class DatabaseQueryNode extends AbstractWorkflowNode<DatabaseQueryNode.Params> {

    public static final String TYPE = "database-query";

    /** Strongly-typed parameters for {@link DatabaseQueryNode}. */
    public record Params(
            @Param(displayName = "SQL query", description = "SQL query to execute", required = true)
            String sql,
            @Param(displayName = "Query parameters", description = "List of query parameters")
            List<Object> params,
            @Param(displayName = "Max rows", description = "Maximum number of rows to return", defaultValue = "1000")
            Integer maxRows
    ) {
        /** Effective row cap, defaulting to 1000 when absent. */
        public int effectiveMaxRows() {
            return maxRows == null ? 1000 : maxRows;
        }
    }

    /** Output descriptor for {@link DatabaseQueryNode}. */
    public record Output(
            @Out(description = "SQL query") String sql,
            @Out(description = "Query parameters") List<Object> queryParams,
            @Out(description = "Max rows") int maxRows,
            @Out(description = "Whether executed") boolean executed,
            @Out(description = "Message") String message
    ) {}

    public DatabaseQueryNode(String nodeId) {
        super(nodeId, TYPE, Params.class);
    }

    @Override
    protected Class<?> outputRecordType() {
        return Output.class;
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Params params) {
        String sql = params.sql();
        int maxRows = params.effectiveMaxRows();

        log.debug("DatabaseQueryNode [{}] executing query: {}", getNodeId(), truncate(sql, 100));

        List<Object> queryParams = params.params();

        // Store query info for the DAG engine to execute via DataManager
        // The actual execution requires a DataManager bean which is resolved at engine level
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sql", sql);
        result.put("queryParams", queryParams != null ? queryParams : List.of());
        result.put("maxRows", maxRows);
        result.put("executed", false);
        result.put("message", "Database query node - requires DataManager integration for execution");

        return result;
    }

    private static String truncate(String str, int maxLen) {
        if (str == null) return null;
        return str.length() <= maxLen ? str : str.substring(0, maxLen) + "...";
    }
}
