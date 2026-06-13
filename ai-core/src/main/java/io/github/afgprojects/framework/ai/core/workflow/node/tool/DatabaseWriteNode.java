package io.github.afgprojects.framework.ai.core.workflow.node.tool;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Database write node - writes data to the database.
 *
 * <p>Executes INSERT, UPDATE, or DELETE SQL statements via the workflow context's
 * data access capabilities. Unlike DatabaseQueryNode which is read-only, this
 * node performs write operations.</p>
 *
 * <p>Parameters:</p>
 * <ul>
 *   <li>{@code sql} (required) - SQL statement to execute (INSERT/UPDATE/DELETE)</li>
 *   <li>{@code params} (optional) - List of query parameters</li>
 * </ul>
 *
 * <p><strong>Alpha feature:</strong> Requires DataManager integration for actual
 * database write execution. Current implementation stores the operation metadata.</p>
 */
@Slf4j
public class DatabaseWriteNode extends AbstractWorkflowNode {

    public static final String TYPE = "database-write";

    public DatabaseWriteNode(String nodeId) {
        super(nodeId, TYPE);
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Map<String, Object> params) {
        String sql = getRequiredParam(params, "sql");

        log.debug("DatabaseWriteNode [{}] executing write: {}", getNodeId(), truncate(sql, 100));

        @SuppressWarnings("unchecked")
        List<Object> queryParams = (List<Object>) params.get("params");

        // Store write operation info for the DAG engine to execute via DataManager
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sql", sql);
        result.put("params", queryParams != null ? queryParams : List.of());
        result.put("isWrite", true);
        result.put("executed", false);
        result.put("message", "Database write node - requires DataManager integration for execution");
        return result;
    }

    private String getRequiredParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Required parameter '" + key + "' is missing");
        }
        return value.toString();
    }

    private static String truncate(String str, int maxLen) {
        if (str == null) return null;
        return str.length() <= maxLen ? str : str.substring(0, maxLen) + "...";
    }
}
