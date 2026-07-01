package io.github.afgprojects.framework.ai.core.workflow.node;

import io.github.afgprojects.framework.ai.core.api.workflow.definition.EditorMeta;
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

    /** Editor metadata for a node type: friendly name + category + editor meta. */
    private record Meta(String type, String displayName, String displayNameZh,
                        String category, EditorMeta editorMeta,
                        Function<String, AbstractWorkflowNode<?>> factory) {}

    /**
     * Metadata for every built-in node type. The factory builds a throwaway instance
     * used only to read the self-describing schema/anchors — it is never executed.
     */
    private static final List<Meta> METAS = List.of(
            // INPUT
            meta(NodeTypes.INPUT, "Input", "输入", NodeCategory.INPUT, EditorMeta.of("LoginOutlined", "#52c41a"), InputNode::new),
            meta(NodeTypes.FILE_INPUT, "File Input", "文件输入", NodeCategory.INPUT, EditorMeta.of("FileOutlined", "#52c41a"), FileInputNode::new),
            meta(NodeTypes.HTTP_REQUEST, "HTTP Request", "HTTP 请求", NodeCategory.INPUT, EditorMeta.of("ApiOutlined", "#52c41a"), HttpRequestNode::new),
            meta(NodeTypes.DATABASE_QUERY, "Database Query", "数据库查询", NodeCategory.INPUT, EditorMeta.of("DatabaseOutlined", "#52c41a"), DatabaseQueryNode::new),
            // AI
            meta(NodeTypes.AI_CHAT, "AI Chat", "AI 对话", NodeCategory.AI, EditorMeta.of("RobotOutlined", "#1677ff"), AiChatNode::new),
            meta(NodeTypes.AI_EMBEDDING, "AI Embedding", "AI 向量化", NodeCategory.AI, EditorMeta.of("BlockOutlined", "#1677ff"), AiEmbeddingNode::new),
            // LOGIC
            meta(NodeTypes.CONDITION, "Condition", "条件判断", NodeCategory.LOGIC, EditorMeta.of("ForkOutlined", "#faad14"), ConditionNode::new),
            meta(NodeTypes.LOOP, "Loop", "循环", NodeCategory.LOGIC, EditorMeta.of("SyncOutlined", "#fa8c16"), LoopNode::new),
            meta(NodeTypes.SWITCH, "Switch", "分支", NodeCategory.LOGIC, EditorMeta.of("SwitcherOutlined", "#faad14"), SwitchNode::new),
            meta(NodeTypes.MERGE, "Merge", "合并", NodeCategory.LOGIC, EditorMeta.of("MergeCellsOutlined", "#faad14"), MergeNode::new),
            meta(NodeTypes.DELAY, "Delay", "延迟", NodeCategory.LOGIC, EditorMeta.of("ClockCircleOutlined", "#faad14"), DelayNode::new),
            meta(NodeTypes.SUB_WORKFLOW, "Sub-Workflow", "子工作流", NodeCategory.LOGIC, EditorMeta.of("ApartmentOutlined", "#faad14"), SubWorkflowNode::new),
            // TOOL
            meta(NodeTypes.TOOL, "Tool", "函数工具", NodeCategory.TOOL, EditorMeta.of("CodeOutlined", "#8c8c8c"), ToolNode::new),
            meta(NodeTypes.HTTP_CALL, "HTTP Call", "HTTP 调用", NodeCategory.TOOL, EditorMeta.of("ApiOutlined", "#8c8c8c"), HttpCallNode::new),
            meta(NodeTypes.DATABASE_WRITE, "Database Write", "数据库写入", NodeCategory.TOOL, EditorMeta.of("DatabaseOutlined", "#8c8c8c"), DatabaseWriteNode::new),
            meta(NodeTypes.CODE_EXECUTE, "Code Execute", "代码执行", NodeCategory.TOOL, EditorMeta.of("ConsoleSqlOutlined", "#8c8c8c"), CodeExecuteNode::new),
            meta(NodeTypes.MCP_TOOL, "MCP Tool", "MCP 工具", NodeCategory.TOOL, EditorMeta.of("ToolOutlined", "#8c8c8c"), McpToolNode::new),
            // OUTPUT
            meta(NodeTypes.OUTPUT, "Output", "输出", NodeCategory.OUTPUT, EditorMeta.of("ExportOutlined", "#722ed1"), OutputNode::new),
            meta(NodeTypes.FILE_OUTPUT, "File Output", "文件输出", NodeCategory.OUTPUT, EditorMeta.of("FileOutlined", "#722ed1"), FileOutputNode::new),
            meta(NodeTypes.NOTIFICATION, "Notification", "通知", NodeCategory.OUTPUT, EditorMeta.of("BellOutlined", "#722ed1"), NotificationNode::new),
            meta(NodeTypes.WEBHOOK, "Webhook", "Webhook", NodeCategory.OUTPUT, EditorMeta.of("WebhookOutlined", "#722ed1"), WebhookNode::new),
            meta(NodeTypes.LOG_OUTPUT, "Log Output", "日志输出", NodeCategory.OUTPUT, EditorMeta.of("FileTextOutlined", "#722ed1"), LogOutputNode::new),
            // HUMAN
            meta(NodeTypes.HUMAN_APPROVAL, "Human Approval", "人工审批", NodeCategory.HUMAN, EditorMeta.of("CheckCircleOutlined", "#eb2f96"), HumanApprovalNode::new),
            meta(NodeTypes.HUMAN_INPUT, "Human Input", "人工输入", NodeCategory.HUMAN, EditorMeta.of("EditOutlined", "#eb2f96"), HumanInputNode::new),
            meta(NodeTypes.HUMAN_CHOICE, "Human Choice", "人工选择", NodeCategory.HUMAN, EditorMeta.of("SelectOutlined", "#eb2f96"), HumanChoiceNode::new),
            // TRANSFORM
            meta(NodeTypes.JSON_TRANSFORM, "JSON Transform", "JSON 转换", NodeCategory.TRANSFORM, EditorMeta.of("BlockOutlined", "#13c2c2"), JsonTransformNode::new),
            meta(NodeTypes.TEXT_TRANSFORM, "Text Transform", "文本转换", NodeCategory.TRANSFORM, EditorMeta.of("FontSizeOutlined", "#13c2c2"), TextTransformNode::new),
            meta(NodeTypes.MAPPING, "Mapping", "字段映射", NodeCategory.TRANSFORM, EditorMeta.of("SwapOutlined", "#13c2c2"), MappingNode::new),
            meta(NodeTypes.FILTER, "Filter", "过滤", NodeCategory.TRANSFORM, EditorMeta.of("FilterOutlined", "#13c2c2"), FilterNode::new),
            meta(NodeTypes.AGGREGATE, "Aggregate", "聚合", NodeCategory.TRANSFORM, EditorMeta.of("GroupOutlined", "#13c2c2"), AggregateNode::new),
            // RAG
            meta(NodeTypes.RETRIEVAL, "Retrieval", "知识检索", NodeCategory.RAG, EditorMeta.of("SearchOutlined", "#2f54eb"), RetrievalNode::new),
            meta(NodeTypes.EMBEDDING, "Embedding", "向量化", NodeCategory.RAG, EditorMeta.of("BlockOutlined", "#2f54eb"), EmbeddingNode::new),
            meta(NodeTypes.RE_RANK, "Re-Rank", "重排序", NodeCategory.RAG, EditorMeta.of("OrderedListOutlined", "#2f54eb"), ReRankNode::new),
            // CHECKPOINT
            meta(NodeTypes.CHECKPOINT, "Checkpoint", "检查点", NodeCategory.CHECKPOINT, EditorMeta.of("SaveOutlined", "#a0d911"), CheckpointNode::new),
            meta(NodeTypes.RECOVERY, "Recovery", "恢复", NodeCategory.CHECKPOINT, EditorMeta.of("RollbackOutlined", "#a0d911"), RecoveryNode::new)
    );

    private static Meta meta(String type, String displayName, String displayNameZh,
                             NodeCategory category, EditorMeta editorMeta,
                             Function<String, AbstractWorkflowNode<?>> factory) {
        return new Meta(type, displayName, displayNameZh, category.name(), editorMeta, factory);
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
                public String getDisplayNameZh() { return m.displayNameZh(); }
                @Override
                public String getCategory() { return m.category(); }
                @Override
                public EditorMeta getEditorMeta() { return m.editorMeta(); }
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
