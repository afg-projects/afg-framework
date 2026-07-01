package io.github.afgprojects.framework.ai.core.workflow.node.tool;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Out;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Param;
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
 * <p>Parameters are declared on {@link Params} so the node is self-describing.</p>
 *
 * <p><strong>Alpha feature:</strong> Requires DataManager integration for actual
 * database write execution. Current implementation stores the operation metadata.</p>
 */
@Slf4j
public class DatabaseWriteNode extends AbstractWorkflowNode<DatabaseWriteNode.Params> {

    public static final String TYPE = "database-write";

    /** Strongly-typed parameters for {@link DatabaseWriteNode}. */
    public record Params(
            @Param(displayName = "SQL", description = "SQL statement to execute (INSERT/UPDATE/DELETE)", required = true)
            String sql,
            @Param(displayName = "Parameters", description = "List of query parameters")
            List<Object> params
    ) {}

    /** Output descriptor for {@link DatabaseWriteNode}. */
    public record Output(
            @Out(description = "SQL statement") String sql,
            @Out(description = "Query parameters") List<Object> params,
            @Out(description = "Whether write operation") boolean isWrite,
            @Out(description = "Whether executed") boolean executed
    ) {}

    public DatabaseWriteNode(String nodeId) {
        super(nodeId, TYPE, Params.class);
    }

    @Override
    protected Class<?> outputRecordType() {
        return Output.class;
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Params params) {
        String sql = params.sql();

        log.debug("DatabaseWriteNode [{}] executing write: {}", getNodeId(), truncate(sql, 100));

        List<Object> queryParams = params.params();

        // Store write operation info for the DAG engine to execute via DataManager
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sql", sql);
        result.put("params", queryParams != null ? queryParams : List.of());
        result.put("isWrite", true);
        result.put("executed", false);
        result.put("message", "Database write node - requires DataManager integration for execution");
        return result;
    }

    private static String truncate(String str, int maxLen) {
        if (str == null) return null;
        return str.length() <= maxLen ? str : str.substring(0, maxLen) + "...";
    }
}
