package io.github.afgprojects.framework.ai.core.workflow.node.input;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
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
 * <p>Parameters:</p>
 * <ul>
 *   <li>{@code sql} (required) - SQL query to execute</li>
 *   <li>{@code params} (optional) - List of query parameters</li>
 *   <li>{@code maxRows} (optional) - Maximum number of rows to return, defaults to 1000</li>
 * </ul>
 *
 * <p><strong>Alpha feature:</strong> This node requires a DataManager or equivalent
 * data access bean to be available in the execution context. The current implementation
 * stores the SQL and parameters for later execution by the DAG engine.</p>
 */
@Slf4j
public class DatabaseQueryNode extends AbstractWorkflowNode {

    public static final String TYPE = "database-query";

    public DatabaseQueryNode(String nodeId) {
        super(nodeId, TYPE);
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Map<String, Object> params) {
        String sql = getRequiredParam(params, "sql");
        int maxRows = getIntParam(params, "maxRows", 1000);

        log.debug("DatabaseQueryNode [{}] executing query: {}", getNodeId(), truncate(sql, 100));

        @SuppressWarnings("unchecked")
        List<Object> queryParams = (List<Object>) params.get("params");

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

    private String getRequiredParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Required parameter '" + key + "' is missing");
        }
        return value.toString();
    }

    private int getIntParam(Map<String, Object> params, String key, int defaultValue) {
        Object value = params.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number num) return num.intValue();
        return Integer.parseInt(value.toString());
    }

    private static String truncate(String str, int maxLen) {
        if (str == null) return null;
        return str.length() <= maxLen ? str : str.substring(0, maxLen) + "...";
    }
}
