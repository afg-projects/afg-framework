package io.github.afgprojects.framework.ai.core.workflow.node;

import io.github.afgprojects.framework.ai.core.api.workflow.definition.NodeDefinition;
import io.github.afgprojects.framework.ai.core.api.workflow.definition.OutputSchema;
import io.github.afgprojects.framework.ai.core.api.workflow.definition.ParamSchema;
import io.github.afgprojects.framework.ai.core.api.workflow.definition.ParamType;
import io.github.afgprojects.framework.ai.core.api.workflow.node.NodeCategory;
import io.github.afgprojects.framework.ai.core.api.workflow.node.NodeTypeRegistry;
import io.github.afgprojects.framework.ai.core.api.workflow.node.NodeTypes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registers all built-in workflow node type definitions into the NodeTypeRegistry.
 *
 * <p>Each node type is registered with its parameter schema, output schema,
 * source/target anchors, and category. This enables the workflow editor and
 * DAG engine to discover and validate available node types.</p>
 */
public final class BuiltinNodeRegistrar {

    private BuiltinNodeRegistrar() {}

    /**
     * Registers all built-in node type definitions into the given registry.
     *
     * @param registry the node type registry to populate
     */
    public static void registerAll(NodeTypeRegistry registry) {
        // INPUT nodes
        register(registry, NodeTypes.INPUT, "Input", NodeCategory.INPUT.name(),
                Map.of("data", ParamSchema.of("data", ParamType.OBJECT, "Input data", false)),
                Map.of("data", OutputSchema.of(ParamType.OBJECT, "Input data")));

        register(registry, NodeTypes.FILE_INPUT, "File Input", NodeCategory.INPUT.name(),
                Map.of(
                    "filePath", ParamSchema.of("filePath", ParamType.STRING, "File path", true),
                    "encoding", ParamSchema.of("encoding", ParamType.STRING, "File encoding", false, "UTF-8")),
                Map.of(
                    "content", OutputSchema.of(ParamType.STRING, "File content"),
                    "fileName", OutputSchema.of(ParamType.STRING, "File name"),
                    "fileSize", OutputSchema.of(ParamType.NUMBER, "File size")));

        register(registry, NodeTypes.HTTP_REQUEST, "HTTP Request", NodeCategory.INPUT.name(),
                Map.of(
                    "url", ParamSchema.of("url", ParamType.STRING, "URL", true),
                    "method", ParamSchema.of("method", ParamType.ENUM, "HTTP method", false, "GET")),
                Map.of(
                    "statusCode", OutputSchema.of(ParamType.NUMBER, "Status code"),
                    "body", OutputSchema.of(ParamType.STRING, "Response body")));

        register(registry, NodeTypes.DATABASE_QUERY, "Database Query", NodeCategory.INPUT.name(),
                Map.of(
                    "sql", ParamSchema.of("sql", ParamType.STRING, "SQL query", true),
                    "maxRows", ParamSchema.of("maxRows", ParamType.NUMBER, "Max rows", false, 1000)),
                Map.of("results", OutputSchema.of(ParamType.ARRAY, "Query results")));

        // AI nodes
        register(registry, NodeTypes.AI_CHAT, "AI Chat", NodeCategory.AI.name(),
                Map.of(
                    "message", ParamSchema.of("message", ParamType.STRING, "User message", true),
                    "systemPrompt", ParamSchema.of("systemPrompt", ParamType.STRING, "System prompt", false)),
                Map.of(
                    "content", OutputSchema.of(ParamType.STRING, "AI response content"),
                    "model", OutputSchema.of(ParamType.STRING, "Model used")));

        register(registry, NodeTypes.AI_EMBEDDING, "AI Embedding", NodeCategory.AI.name(),
                Map.of(
                    "texts", ParamSchema.of("texts", ParamType.ARRAY, "Texts to embed", true),
                    "modelName", ParamSchema.of("modelName", ParamType.STRING, "Embedding model", false)),
                Map.of(
                    "embeddings", OutputSchema.of(ParamType.ARRAY, "Embedding vectors"),
                    "dimensions", OutputSchema.of(ParamType.NUMBER, "Vector dimensions")));

        // LOGIC nodes
        register(registry, NodeTypes.CONDITION, "Condition", NodeCategory.LOGIC.name(),
                Map.of(
                    "variable", ParamSchema.of("variable", ParamType.STRING, "Variable name", false),
                    "expectedValue", ParamSchema.of("expectedValue", ParamType.STRING, "Expected value", false),
                    "expression", ParamSchema.of("expression", ParamType.STRING, "Condition expression", false)),
                Map.of("result", OutputSchema.of(ParamType.BOOLEAN, "Condition result")),
                Set.of("true", "false"), Set.of("input"));

        register(registry, NodeTypes.LOOP, "Loop", NodeCategory.LOGIC.name(),
                Map.of(
                    "items", ParamSchema.of("items", ParamType.ARRAY, "Items to iterate", false),
                    "count", ParamSchema.of("count", ParamType.NUMBER, "Iteration count", false),
                    "itemVariable", ParamSchema.of("itemVariable", ParamType.STRING, "Item variable name", false, "item")),
                Map.of("iterations", OutputSchema.of(ParamType.NUMBER, "Number of iterations")));

        register(registry, NodeTypes.SWITCH, "Switch", NodeCategory.LOGIC.name(),
                Map.of(
                    "variable", ParamSchema.of("variable", ParamType.STRING, "Switch variable", false),
                    "cases", ParamSchema.of("cases", ParamType.OBJECT, "Case mapping", false)),
                Map.of("matchedCase", OutputSchema.of(ParamType.STRING, "Matched case name")));

        register(registry, NodeTypes.MERGE, "Merge", NodeCategory.LOGIC.name(),
                Map.of(
                    "strategy", ParamSchema.of("strategy", ParamType.ENUM, "Merge strategy", false, "merge_all"),
                    "sourceNodes", ParamSchema.of("sourceNodes", ParamType.ARRAY, "Source node IDs", false)),
                Map.of("mergedCount", OutputSchema.of(ParamType.NUMBER, "Number of merged sources")));

        register(registry, NodeTypes.DELAY, "Delay", NodeCategory.LOGIC.name(),
                Map.of(
                    "delayMs", ParamSchema.of("delayMs", ParamType.NUMBER, "Delay (ms)", true),
                    "reason", ParamSchema.of("reason", ParamType.STRING, "Delay reason", false)),
                Map.of("completed", OutputSchema.of(ParamType.BOOLEAN, "Delay completed")));

        register(registry, NodeTypes.SUB_WORKFLOW, "Sub-Workflow", NodeCategory.LOGIC.name(),
                Map.of(
                    "workflowId", ParamSchema.of("workflowId", ParamType.STRING, "Sub-workflow ID", true),
                    "inputMapping", ParamSchema.of("inputMapping", ParamType.OBJECT, "Input mapping", false)),
                Map.of("executed", OutputSchema.of(ParamType.BOOLEAN, "Sub-workflow executed")));

        // TOOL nodes
        register(registry, NodeTypes.TOOL, "Tool", NodeCategory.TOOL.name(),
                Map.of(
                    "toolName", ParamSchema.of("toolName", ParamType.STRING, "Tool name", true),
                    "toolInput", ParamSchema.of("toolInput", ParamType.OBJECT, "Tool input", false)),
                Map.of("result", OutputSchema.of(ParamType.OBJECT, "Tool output")));

        register(registry, NodeTypes.HTTP_CALL, "HTTP Call", NodeCategory.TOOL.name(),
                Map.of(
                    "url", ParamSchema.of("url", ParamType.STRING, "URL", true),
                    "method", ParamSchema.of("method", ParamType.ENUM, "HTTP method", false, "POST"),
                    "body", ParamSchema.of("body", ParamType.STRING, "Request body", false)),
                Map.of(
                    "statusCode", OutputSchema.of(ParamType.NUMBER, "Status code"),
                    "body", OutputSchema.of(ParamType.STRING, "Response body")));

        register(registry, NodeTypes.DATABASE_WRITE, "Database Write", NodeCategory.TOOL.name(),
                Map.of("sql", ParamSchema.of("sql", ParamType.STRING, "SQL statement", true)),
                Map.of("rowsAffected", OutputSchema.of(ParamType.NUMBER, "Rows affected")));

        register(registry, NodeTypes.CODE_EXECUTE, "Code Execute", NodeCategory.TOOL.name(),
                Map.of(
                    "code", ParamSchema.of("code", ParamType.STRING, "Code to execute", true),
                    "language", ParamSchema.of("language", ParamType.STRING, "Language", false, "javascript")),
                Map.of("result", OutputSchema.of(ParamType.OBJECT, "Execution result")));

        register(registry, NodeTypes.MCP_TOOL, "MCP Tool", NodeCategory.TOOL.name(),
                Map.of(
                    "toolName", ParamSchema.of("toolName", ParamType.STRING, "MCP tool name", true),
                    "serverName", ParamSchema.of("serverName", ParamType.STRING, "MCP server name", false)),
                Map.of("result", OutputSchema.of(ParamType.OBJECT, "Tool output")));

        // OUTPUT nodes
        register(registry, NodeTypes.OUTPUT, "Output", NodeCategory.OUTPUT.name(),
                Map.of("format", ParamSchema.of("format", ParamType.STRING, "Output format", false, "raw")),
                Map.of("data", OutputSchema.of(ParamType.OBJECT, "Output data")));

        register(registry, NodeTypes.FILE_OUTPUT, "File Output", NodeCategory.OUTPUT.name(),
                Map.of(
                    "filePath", ParamSchema.of("filePath", ParamType.STRING, "Output file path", true),
                    "content", ParamSchema.of("content", ParamType.STRING, "Content to write", true),
                    "append", ParamSchema.of("append", ParamType.BOOLEAN, "Append mode", false, false)),
                Map.of("bytesWritten", OutputSchema.of(ParamType.NUMBER, "Bytes written")));

        register(registry, NodeTypes.NOTIFICATION, "Notification", NodeCategory.OUTPUT.name(),
                Map.of(
                    "message", ParamSchema.of("message", ParamType.STRING, "Notification message", true),
                    "channel", ParamSchema.of("channel", ParamType.ENUM, "Channel", false, "log")),
                Map.of("sent", OutputSchema.of(ParamType.BOOLEAN, "Notification sent")));

        register(registry, NodeTypes.WEBHOOK, "Webhook", NodeCategory.OUTPUT.name(),
                Map.of(
                    "url", ParamSchema.of("url", ParamType.STRING, "Webhook URL", true),
                    "payload", ParamSchema.of("payload", ParamType.STRING, "JSON payload", false)),
                Map.of("success", OutputSchema.of(ParamType.BOOLEAN, "Webhook delivered")));

        register(registry, NodeTypes.LOG_OUTPUT, "Log Output", NodeCategory.OUTPUT.name(),
                Map.of(
                    "message", ParamSchema.of("message", ParamType.STRING, "Log message", true),
                    "level", ParamSchema.of("level", ParamType.ENUM, "Log level", false, "INFO")),
                Map.of("logged", OutputSchema.of(ParamType.BOOLEAN, "Logged")));

        // HUMAN nodes
        register(registry, NodeTypes.HUMAN_APPROVAL, "Human Approval", NodeCategory.HUMAN.name(),
                Map.of(
                    "title", ParamSchema.of("title", ParamType.STRING, "Approval title", false),
                    "description", ParamSchema.of("description", ParamType.STRING, "Description", false)),
                Map.of("approved", OutputSchema.of(ParamType.BOOLEAN, "Approved")),
                Set.of("approved", "rejected"), Set.of("input"));

        register(registry, NodeTypes.HUMAN_INPUT, "Human Input", NodeCategory.HUMAN.name(),
                Map.of(
                    "prompt", ParamSchema.of("prompt", ParamType.STRING, "Input prompt", true),
                    "schema", ParamSchema.of("schema", ParamType.OBJECT, "Input schema", false)),
                Map.of("input", OutputSchema.of(ParamType.OBJECT, "Human input data")));

        register(registry, NodeTypes.HUMAN_CHOICE, "Human Choice", NodeCategory.HUMAN.name(),
                Map.of(
                    "prompt", ParamSchema.of("prompt", ParamType.STRING, "Choice prompt", true),
                    "choices", ParamSchema.of("choices", ParamType.ARRAY, "Available choices", true)),
                Map.of("selectedChoice", OutputSchema.of(ParamType.STRING, "Selected choice value")));

        // TRANSFORM nodes
        register(registry, NodeTypes.JSON_TRANSFORM, "JSON Transform", NodeCategory.TRANSFORM.name(),
                Map.of(
                    "input", ParamSchema.of("input", ParamType.OBJECT, "Input data", true),
                    "removeFields", ParamSchema.of("removeFields", ParamType.ARRAY, "Fields to remove", false),
                    "addFields", ParamSchema.of("addFields", ParamType.OBJECT, "Fields to add", false)),
                Map.of("data", OutputSchema.of(ParamType.OBJECT, "Transformed data")));

        register(registry, NodeTypes.TEXT_TRANSFORM, "Text Transform", NodeCategory.TRANSFORM.name(),
                Map.of(
                    "text", ParamSchema.of("text", ParamType.STRING, "Input text", true),
                    "operation", ParamSchema.of("operation", ParamType.ENUM, "Transform operation", true)),
                Map.of(
                    "text", OutputSchema.of(ParamType.STRING, "Transformed text"),
                    "length", OutputSchema.of(ParamType.NUMBER, "Result length")));

        register(registry, NodeTypes.MAPPING, "Mapping", NodeCategory.TRANSFORM.name(),
                Map.of(
                    "input", ParamSchema.of("input", ParamType.OBJECT, "Input data", true),
                    "mapping", ParamSchema.of("mapping", ParamType.OBJECT, "Field mapping", true),
                    "defaults", ParamSchema.of("defaults", ParamType.OBJECT, "Default values", false)),
                Map.of("data", OutputSchema.of(ParamType.OBJECT, "Mapped data")));

        register(registry, NodeTypes.FILTER, "Filter", NodeCategory.TRANSFORM.name(),
                Map.of(
                    "items", ParamSchema.of("items", ParamType.ARRAY, "Items to filter", true),
                    "field", ParamSchema.of("field", ParamType.STRING, "Filter field", false),
                    "operator", ParamSchema.of("operator", ParamType.ENUM, "Filter operator", false, "eq"),
                    "value", ParamSchema.of("value", ParamType.STRING, "Filter value", false)),
                Map.of(
                    "filteredItems", OutputSchema.of(ParamType.ARRAY, "Filtered items"),
                    "filteredCount", OutputSchema.of(ParamType.NUMBER, "Filtered count")));

        register(registry, NodeTypes.AGGREGATE, "Aggregate", NodeCategory.TRANSFORM.name(),
                Map.of(
                    "items", ParamSchema.of("items", ParamType.ARRAY, "Items to aggregate", true),
                    "operation", ParamSchema.of("operation", ParamType.ENUM, "Aggregation operation", true),
                    "field", ParamSchema.of("field", ParamType.STRING, "Aggregate field", false)),
                Map.of("result", OutputSchema.of(ParamType.OBJECT, "Aggregation result")));

        // RAG nodes
        register(registry, NodeTypes.RETRIEVAL, "Retrieval", NodeCategory.RAG.name(),
                Map.of(
                    "query", ParamSchema.of("query", ParamType.STRING, "Search query", true),
                    "knowledgeBaseId", ParamSchema.of("knowledgeBaseId", ParamType.STRING, "Knowledge base ID", false),
                    "topK", ParamSchema.of("topK", ParamType.NUMBER, "Top K results", false, 5)),
                Map.of("results", OutputSchema.of(ParamType.ARRAY, "Search results")));

        register(registry, NodeTypes.EMBEDDING, "Embedding", NodeCategory.RAG.name(),
                Map.of(
                    "texts", ParamSchema.of("texts", ParamType.ARRAY, "Texts to embed", true),
                    "store", ParamSchema.of("store", ParamType.BOOLEAN, "Store embeddings", false, false)),
                Map.of(
                    "count", OutputSchema.of(ParamType.NUMBER, "Embedding count"),
                    "dimensions", OutputSchema.of(ParamType.NUMBER, "Vector dimensions")));

        register(registry, NodeTypes.RE_RANK, "Re-Rank", NodeCategory.RAG.name(),
                Map.of(
                    "query", ParamSchema.of("query", ParamType.STRING, "Original query", true),
                    "results", ParamSchema.of("results", ParamType.ARRAY, "Results to re-rank", true),
                    "topK", ParamSchema.of("topK", ParamType.NUMBER, "Top K results", false, 5)),
                Map.of("results", OutputSchema.of(ParamType.ARRAY, "Re-ranked results")));

        // CHECKPOINT nodes
        register(registry, NodeTypes.CHECKPOINT, "Checkpoint", NodeCategory.CHECKPOINT.name(),
                Map.of("label", ParamSchema.of("label", ParamType.STRING, "Checkpoint label", false)),
                Map.of("checkpointSaved", OutputSchema.of(ParamType.BOOLEAN, "Checkpoint saved")));

        register(registry, NodeTypes.RECOVERY, "Recovery", NodeCategory.CHECKPOINT.name(),
                Map.of(
                    "executionId", ParamSchema.of("executionId", ParamType.STRING, "Execution ID to recover", true),
                    "failIfNotFound", ParamSchema.of("failIfNotFound", ParamType.BOOLEAN, "Fail if not found", false, false)),
                Map.of("recovered", OutputSchema.of(ParamType.BOOLEAN, "Recovery successful")));
    }

    private static void register(NodeTypeRegistry registry, String type, String displayName,
                                  String category, Map<String, ParamSchema> paramSchema,
                                  Map<String, OutputSchema> outputSchema) {
        register(registry, type, displayName, category, paramSchema, outputSchema,
                Set.of("output"), Set.of("input"));
    }

    private static void register(NodeTypeRegistry registry, String type, String displayName,
                                  String category, Map<String, ParamSchema> paramSchema,
                                  Map<String, OutputSchema> outputSchema,
                                  Set<String> sourceAnchors, Set<String> targetAnchors) {
        registry.register(new NodeDefinition() {
            @Override
            public String getType() { return type; }
            @Override
            public String getDisplayName() { return displayName; }
            @Override
            public String getCategory() { return category; }
            @Override
            public Map<String, ParamSchema> getParamSchema() { return paramSchema; }
            @Override
            public Map<String, OutputSchema> getOutputSchema() { return outputSchema; }
            @Override
            public Set<String> getSourceAnchors() { return sourceAnchors; }
            @Override
            public Set<String> getTargetAnchors() { return targetAnchors; }
        });
    }
}
