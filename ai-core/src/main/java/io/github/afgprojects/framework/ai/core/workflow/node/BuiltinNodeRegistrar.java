package io.github.afgprojects.framework.ai.core.workflow.node;

import io.github.afgprojects.framework.ai.core.api.workflow.definition.NodeDefinition;
import io.github.afgprojects.framework.ai.core.api.workflow.definition.OutputSchema;
import io.github.afgprojects.framework.ai.core.api.workflow.definition.ParamSchema;
import io.github.afgprojects.framework.ai.core.api.workflow.node.NodeCategory;
import io.github.afgprojects.framework.ai.core.api.workflow.node.NodeTypeRegistry;
import io.github.afgprojects.framework.ai.core.api.workflow.node.NodeTypes;
import io.github.afgprojects.framework.ai.core.workflow.node.ai.AiChatNode;
import io.github.afgprojects.framework.ai.core.workflow.node.ai.AiEmbeddingNode;
import io.github.afgprojects.framework.ai.core.workflow.node.checkpoint.CheckpointNode;
import io.github.afgprojects.framework.ai.core.workflow.node.checkpoint.RecoveryNode;
import io.github.afgprojects.framework.ai.core.workflow.node.human.HumanApprovalNode;
import io.github.afgprojects.framework.ai.core.workflow.node.human.HumanChoiceNode;
import io.github.afgprojects.framework.ai.core.workflow.node.human.HumanInputNode;
import io.github.afgprojects.framework.ai.core.workflow.node.input.DatabaseQueryNode;
import io.github.afgprojects.framework.ai.core.workflow.node.input.FileInputNode;
import io.github.afgprojects.framework.ai.core.workflow.node.input.HttpRequestNode;
import io.github.afgprojects.framework.ai.core.workflow.node.input.InputNode;
import io.github.afgprojects.framework.ai.core.workflow.node.logic.ConditionNode;
import io.github.afgprojects.framework.ai.core.workflow.node.logic.DelayNode;
import io.github.afgprojects.framework.ai.core.workflow.node.logic.LoopNode;
import io.github.afgprojects.framework.ai.core.workflow.node.logic.MergeNode;
import io.github.afgprojects.framework.ai.core.workflow.node.logic.SubWorkflowNode;
import io.github.afgprojects.framework.ai.core.workflow.node.logic.SwitchNode;
import io.github.afgprojects.framework.ai.core.workflow.node.output.FileOutputNode;
import io.github.afgprojects.framework.ai.core.workflow.node.output.LogOutputNode;
import io.github.afgprojects.framework.ai.core.workflow.node.output.NotificationNode;
import io.github.afgprojects.framework.ai.core.workflow.node.output.OutputNode;
import io.github.afgprojects.framework.ai.core.workflow.node.output.WebhookNode;
import io.github.afgprojects.framework.ai.core.workflow.node.rag.EmbeddingNode;
import io.github.afgprojects.framework.ai.core.workflow.node.rag.ReRankNode;
import io.github.afgprojects.framework.ai.core.workflow.node.rag.RetrievalNode;
import io.github.afgprojects.framework.ai.core.workflow.node.tool.CodeExecuteNode;
import io.github.afgprojects.framework.ai.core.workflow.node.tool.DatabaseWriteNode;
import io.github.afgprojects.framework.ai.core.workflow.node.tool.HttpCallNode;
import io.github.afgprojects.framework.ai.core.workflow.node.tool.McpToolNode;
import io.github.afgprojects.framework.ai.core.workflow.node.tool.ToolNode;
import io.github.afgprojects.framework.ai.core.workflow.node.transform.AggregateNode;
import io.github.afgprojects.framework.ai.core.workflow.node.transform.FilterNode;
import io.github.afgprojects.framework.ai.core.workflow.node.transform.JsonTransformNode;
import io.github.afgprojects.framework.ai.core.workflow.node.transform.MappingNode;
import io.github.afgprojects.framework.ai.core.workflow.node.transform.TextTransformNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Registers all built-in workflow node type definitions into the NodeTypeRegistry.
 *
 * <p><strong>Self-describing schema (R2):</strong> parameter and output schemas are
 * read from each node's own {@link AbstractWorkflowNode#getParamSchema()} /
 * {@link #getOutputSchema()} (reflected from its {@code @Param}/{@code @Out} record),
 * so the schema lives in exactly one place — the node class. This class no longer
 * duplicates parameter schemas as literals.</p>
 *
 * <p><strong>Editor metadata:</strong> the human-facing {@code displayName} and
 * {@code category} are not derivable from the node class, so they remain declared
 * here as a compact metadata table. Anchors come from the node's
 * {@link NodeDefinition#getSourceAnchors()} / {@link #getTargetAnchors()} (Condition /
 * Human nodes override the defaults).</p>
 */
public final class BuiltinNodeRegistrar {

    private BuiltinNodeRegistrar() {}

    /** Editor metadata for a node type: friendly name + category. */
    private record Meta(String type, String displayName, String category,
                        Function<String, AbstractWorkflowNode<?>> factory) {}

    /**
     * Metadata for every built-in node type. The factory builds a throwaway instance
     * used only to read the self-describing schema/anchors — it is never executed.
     */
    private static final List<Meta> METAS = List.of(
            // INPUT
            meta(NodeTypes.INPUT, "Input", NodeCategory.INPUT, InputNode::new),
            meta(NodeTypes.FILE_INPUT, "File Input", NodeCategory.INPUT, FileInputNode::new),
            meta(NodeTypes.HTTP_REQUEST, "HTTP Request", NodeCategory.INPUT, HttpRequestNode::new),
            meta(NodeTypes.DATABASE_QUERY, "Database Query", NodeCategory.INPUT, DatabaseQueryNode::new),
            // AI
            meta(NodeTypes.AI_CHAT, "AI Chat", NodeCategory.AI, AiChatNode::new),
            meta(NodeTypes.AI_EMBEDDING, "AI Embedding", NodeCategory.AI, AiEmbeddingNode::new),
            // LOGIC
            meta(NodeTypes.CONDITION, "Condition", NodeCategory.LOGIC, ConditionNode::new),
            meta(NodeTypes.LOOP, "Loop", NodeCategory.LOGIC, LoopNode::new),
            meta(NodeTypes.SWITCH, "Switch", NodeCategory.LOGIC, SwitchNode::new),
            meta(NodeTypes.MERGE, "Merge", NodeCategory.LOGIC, MergeNode::new),
            meta(NodeTypes.DELAY, "Delay", NodeCategory.LOGIC, DelayNode::new),
            meta(NodeTypes.SUB_WORKFLOW, "Sub-Workflow", NodeCategory.LOGIC, SubWorkflowNode::new),
            // TOOL
            meta(NodeTypes.TOOL, "Tool", NodeCategory.TOOL, ToolNode::new),
            meta(NodeTypes.HTTP_CALL, "HTTP Call", NodeCategory.TOOL, HttpCallNode::new),
            meta(NodeTypes.DATABASE_WRITE, "Database Write", NodeCategory.TOOL, DatabaseWriteNode::new),
            meta(NodeTypes.CODE_EXECUTE, "Code Execute", NodeCategory.TOOL, CodeExecuteNode::new),
            meta(NodeTypes.MCP_TOOL, "MCP Tool", NodeCategory.TOOL, McpToolNode::new),
            // OUTPUT
            meta(NodeTypes.OUTPUT, "Output", NodeCategory.OUTPUT, OutputNode::new),
            meta(NodeTypes.FILE_OUTPUT, "File Output", NodeCategory.OUTPUT, FileOutputNode::new),
            meta(NodeTypes.NOTIFICATION, "Notification", NodeCategory.OUTPUT, NotificationNode::new),
            meta(NodeTypes.WEBHOOK, "Webhook", NodeCategory.OUTPUT, WebhookNode::new),
            meta(NodeTypes.LOG_OUTPUT, "Log Output", NodeCategory.OUTPUT, LogOutputNode::new),
            // HUMAN
            meta(NodeTypes.HUMAN_APPROVAL, "Human Approval", NodeCategory.HUMAN, HumanApprovalNode::new),
            meta(NodeTypes.HUMAN_INPUT, "Human Input", NodeCategory.HUMAN, HumanInputNode::new),
            meta(NodeTypes.HUMAN_CHOICE, "Human Choice", NodeCategory.HUMAN, HumanChoiceNode::new),
            // TRANSFORM
            meta(NodeTypes.JSON_TRANSFORM, "JSON Transform", NodeCategory.TRANSFORM, JsonTransformNode::new),
            meta(NodeTypes.TEXT_TRANSFORM, "Text Transform", NodeCategory.TRANSFORM, TextTransformNode::new),
            meta(NodeTypes.MAPPING, "Mapping", NodeCategory.TRANSFORM, MappingNode::new),
            meta(NodeTypes.FILTER, "Filter", NodeCategory.TRANSFORM, FilterNode::new),
            meta(NodeTypes.AGGREGATE, "Aggregate", NodeCategory.TRANSFORM, AggregateNode::new),
            // RAG
            meta(NodeTypes.RETRIEVAL, "Retrieval", NodeCategory.RAG, RetrievalNode::new),
            meta(NodeTypes.EMBEDDING, "Embedding", NodeCategory.RAG, EmbeddingNode::new),
            meta(NodeTypes.RE_RANK, "Re-Rank", NodeCategory.RAG, ReRankNode::new),
            // CHECKPOINT
            meta(NodeTypes.CHECKPOINT, "Checkpoint", NodeCategory.CHECKPOINT, CheckpointNode::new),
            meta(NodeTypes.RECOVERY, "Recovery", NodeCategory.CHECKPOINT, RecoveryNode::new)
    );

    private static Meta meta(String type, String displayName, NodeCategory category,
                             Function<String, AbstractWorkflowNode<?>> factory) {
        return new Meta(type, displayName, category.name(), factory);
    }

    /**
     * Registers all built-in node type definitions into the given registry.
     *
     * <p>For each type, a throwaway node instance is built to read its self-describing
     * schema and anchors; displayName and category come from the metadata table.</p>
     *
     * @param registry the node type registry to populate
     */
    public static void registerAll(NodeTypeRegistry registry) {
        for (Meta m : METAS) {
            AbstractWorkflowNode<?> instance = m.factory().apply("__reg__");
            registry.register(new NodeDefinition() {
                @Override
                public String getType() { return m.type(); }
                @Override
                public String getDisplayName() { return m.displayName(); }
                @Override
                public String getCategory() { return m.category(); }
                @Override
                public Map<String, ParamSchema> getParamSchema() {
                    return instance.getParamSchema();
                }
                @Override
                public Map<String, OutputSchema> getOutputSchema() {
                    return instance.getOutputSchema();
                }
                @Override
                public Set<String> getSourceAnchors() {
                    return instance.getSourceAnchors();
                }
                @Override
                public Set<String> getTargetAnchors() {
                    return instance.getTargetAnchors();
                }
            });
        }
    }
}
