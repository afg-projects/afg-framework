package io.github.afgprojects.framework.ai.core.api.workflow.node;

/**
 * Central registry of all workflow node type identifiers.
 *
 * <p>Each node type corresponds to a concrete {@link io.github.afgprojects.framework.ai.core.api.workflow.engine.WorkflowNode}
 * implementation. The type string is used in workflow DSL definitions to reference nodes.</p>
 *
 * <p>Node types are organized by category:</p>
 * <ul>
 *   <li>{@link #INPUT} category: nodes that provide data to the workflow</li>
 *   <li>{@link #AI} category: nodes that invoke AI models</li>
 *   <li>{@link #LOGIC} category: nodes that control workflow flow</li>
 *   <li>{@link #TOOL} category: nodes that invoke external tools/services</li>
 *   <li>{@link #OUTPUT} category: nodes that produce workflow output</li>
 *   <li>{@link #HUMAN} category: nodes that require human interaction</li>
 *   <li>{@link #TRANSFORM} category: nodes that transform data</li>
 *   <li>{@link #RAG} category: nodes for retrieval-augmented generation</li>
 *   <li>{@link #CHECKPOINT} category: nodes for workflow persistence</li>
 * </ul>
 */
public final class NodeTypes {

    private NodeTypes() {}

    // ---- INPUT ----
    public static final String INPUT = "input";
    public static final String FILE_INPUT = "file-input";
    public static final String HTTP_REQUEST = "http-request";
    public static final String DATABASE_QUERY = "database-query";

    // ---- AI ----
    public static final String AI_CHAT = "ai-chat";
    public static final String AI_EMBEDDING = "ai-embedding";

    // ---- LOGIC ----
    public static final String CONDITION = "condition";
    public static final String LOOP = "loop";
    public static final String SWITCH = "switch";
    public static final String MERGE = "merge";
    public static final String DELAY = "delay";
    public static final String SUB_WORKFLOW = "sub-workflow";

    // ---- TOOL ----
    public static final String TOOL = "tool";
    public static final String HTTP_CALL = "http-call";
    public static final String DATABASE_WRITE = "database-write";
    public static final String CODE_EXECUTE = "code-execute";
    public static final String MCP_TOOL = "mcp-tool";

    // ---- OUTPUT ----
    public static final String OUTPUT = "output";
    public static final String FILE_OUTPUT = "file-output";
    public static final String NOTIFICATION = "notification";
    public static final String WEBHOOK = "webhook";
    public static final String LOG_OUTPUT = "log-output";

    // ---- HUMAN ----
    public static final String HUMAN_APPROVAL = "human-approval";
    public static final String HUMAN_INPUT = "human-input";
    public static final String HUMAN_CHOICE = "human-choice";

    // ---- TRANSFORM ----
    public static final String JSON_TRANSFORM = "json-transform";
    public static final String TEXT_TRANSFORM = "text-transform";
    public static final String MAPPING = "mapping";
    public static final String FILTER = "filter";
    public static final String AGGREGATE = "aggregate";

    // ---- RAG ----
    public static final String RETRIEVAL = "retrieval";
    public static final String EMBEDDING = "embedding";
    public static final String RE_RANK = "re-rank";

    // ---- CHECKPOINT ----
    public static final String CHECKPOINT = "checkpoint";
    public static final String RECOVERY = "recovery";
}
