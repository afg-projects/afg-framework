package io.github.afgprojects.framework.ai.core.workflow.node;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.WorkflowNode;
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
import io.github.afgprojects.framework.ai.core.workflow.node.logic.SwitchNode;
import io.github.afgprojects.framework.ai.core.workflow.node.logic.SubWorkflowNode;
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

import java.util.List;

/**
 * Registers {@link NodeFactory} instances for built-in node types whose
 * constructors take only a nodeId (no external dependency) into a
 * {@link DefaultNodeFactory}.
 *
 * <p>Nodes with external dependencies (AI clients, tool registries, etc.) are
 * also registered here when their no-arg constructor degrades gracefully
 * (returns an error result instead of NPE). Nodes whose no-arg constructor
 * would fail (hard dependency on an external client with no fallback) are
 * wired by their own batch's configurer instead.</p>
 *
 * <p>Each entry is a tiny lambda factory; the node class still owns its
 * {@code TYPE} constant, so the type id stays declared in exactly one place.</p>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public final class BuiltinNodeFactoryRegistrar {

    private BuiltinNodeFactoryRegistrar() {}

    /**
     * Register all dependency-free built-in node factories.
     */
    public static void registerAll(DefaultNodeFactory factory) {
        for (NodeFactory f : dependencyFreeFactories()) {
            factory.register(f);
        }
    }

    /**
     * The dependency-free built-in factories. Nodes requiring injected
     * collaborators are intentionally absent here — added per-batch as they
     * migrate to the typed base class.
     */
    static List<NodeFactory> dependencyFreeFactories() {
        return List.of(
                factory(InputNode.TYPE, InputNode::new),
                factory(FileInputNode.TYPE, FileInputNode::new),
                factory(HttpRequestNode.TYPE, HttpRequestNode::new),
                factory(DatabaseQueryNode.TYPE, DatabaseQueryNode::new),
                factory(ConditionNode.TYPE, ConditionNode::new),
                factory(LoopNode.TYPE, LoopNode::new),
                factory(SwitchNode.TYPE, SwitchNode::new),
                factory(MergeNode.TYPE, MergeNode::new),
                factory(DelayNode.TYPE, DelayNode::new),
                factory(SubWorkflowNode.TYPE, SubWorkflowNode::new),
                factory(DatabaseWriteNode.TYPE, DatabaseWriteNode::new),
                factory(CodeExecuteNode.TYPE, CodeExecuteNode::new),
                factory(OutputNode.TYPE, OutputNode::new),
                factory(FileOutputNode.TYPE, FileOutputNode::new),
                factory(NotificationNode.TYPE, NotificationNode::new),
                factory(LogOutputNode.TYPE, LogOutputNode::new),
                factory(MappingNode.TYPE, MappingNode::new),
                factory(TextTransformNode.TYPE, TextTransformNode::new),
                factory(FilterNode.TYPE, FilterNode::new),
                factory(AggregateNode.TYPE, AggregateNode::new),
                factory(JsonTransformNode.TYPE, JsonTransformNode::new),
                factory(ToolNode.TYPE, ToolNode::new),
                factory(HttpCallNode.TYPE, HttpCallNode::new),
                factory(McpToolNode.TYPE, McpToolNode::new),
                factory(WebhookNode.TYPE, WebhookNode::new),
                factory(HumanApprovalNode.TYPE, HumanApprovalNode::new),
                factory(HumanInputNode.TYPE, HumanInputNode::new),
                factory(HumanChoiceNode.TYPE, HumanChoiceNode::new),
                factory(RetrievalNode.TYPE, RetrievalNode::new),
                factory(EmbeddingNode.TYPE, EmbeddingNode::new),
                factory(ReRankNode.TYPE, ReRankNode::new),
                factory(CheckpointNode.TYPE, CheckpointNode::new),
                factory(RecoveryNode.TYPE, RecoveryNode::new)
        );
    }

    /**
     * Build a {@link NodeFactory} from a type id and a single-arg nodeId
     * constructor reference.
     */
    private static NodeFactory factory(String type,
                                       java.util.function.Function<String, WorkflowNode> ctor) {
        return new NodeFactory() {
            @Override
            public String type() {
                return type;
            }

            @Override
            public WorkflowNode create(String nodeId) {
                return ctor.apply(nodeId);
            }
        };
    }
}
