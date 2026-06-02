package io.github.afgprojects.framework.ai.workflow.dsl;

import io.github.afgprojects.framework.ai.core.api.workflow.definition.EdgeDefinition;
import io.github.afgprojects.framework.ai.core.api.workflow.definition.ParamType;
import io.github.afgprojects.framework.ai.core.api.workflow.definition.WorkflowDefinition;
import io.github.afgprojects.framework.ai.core.api.workflow.definition.WorkflowDefinition.NodeInstance;
import io.github.afgprojects.framework.ai.core.api.workflow.definition.WorkflowDefinition.Position;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Chain-style builder for constructing {@link WorkflowDefinition} instances using a fluent Java DSL.
 *
 * <p>Example usage:
 * <pre>{@code
 * var workflow = WorkflowBuilder.create()
 *     .start("start_1")
 *         .variable("user_input", ParamType.STRING, true)
 *         .done()
 *     .aiChat("ai_chat_1")
 *         .param("prompt", "回答：${start_1.user_input}")
 *         .param("modelId", "qwen2.5:7b")
 *         .done()
 *     .edge("start_1", "ai_chat_1")
 *     .build();
 * }</pre>
 */
public class WorkflowBuilder {

    private static final String DEFAULT_VERSION = "1.0";

    private final List<NodeInstance> nodes = new ArrayList<>();
    private final List<EdgeDefinition> edges = new ArrayList<>();
    private String version = DEFAULT_VERSION;

    private WorkflowBuilder() {}

    /**
     * Create a new WorkflowBuilder instance.
     */
    public static WorkflowBuilder create() {
        return new WorkflowBuilder();
    }

    // ── Node entry points ──────────────────────────────────────────────

    /**
     * Start building a "start" node.
     */
    public NodeBuilder start(String id) {
        return new NodeBuilder(this, id, "start", id);
    }

    /**
     * Start building an "ai-chat" node.
     */
    public NodeBuilder aiChat(String id) {
        return new NodeBuilder(this, id, "ai-chat", id);
    }

    /**
     * Start building a "function-tool" node.
     */
    public NodeBuilder functionTool(String id) {
        return new NodeBuilder(this, id, "function-tool", id);
    }

    /**
     * Start building a "condition" node.
     */
    public NodeBuilder condition(String id) {
        return new NodeBuilder(this, id, "condition", id);
    }

    /**
     * Start building a "loop" node.
     */
    public NodeBuilder loop(String id) {
        return new NodeBuilder(this, id, "loop", id);
    }

    /**
     * Start building a "reply" node.
     */
    public NodeBuilder reply(String id) {
        return new NodeBuilder(this, id, "reply", id);
    }

    // ── Edge shortcuts ─────────────────────────────────────────────────

    /**
     * Add an edge with the default sourceAnchor ("output").
     */
    public WorkflowBuilder edge(String source, String target) {
        return edge(source, target, "output");
    }

    /**
     * Add an edge with a specified sourceAnchor.
     */
    public WorkflowBuilder edge(String source, String target, String sourceAnchor) {
        String edgeId = "e_" + source + "_" + target + "_" + sourceAnchor;
        edges.add(new EdgeDefinition(edgeId, source, target, sourceAnchor));
        return this;
    }

    // ── Version ────────────────────────────────────────────────────────

    /**
     * Set the workflow version. Defaults to "1.0".
     */
    public WorkflowBuilder version(String version) {
        this.version = version;
        return this;
    }

    // ── Build ──────────────────────────────────────────────────────────

    /**
     * Build the immutable {@link WorkflowDefinition}.
     */
    public WorkflowDefinition build() {
        return new WorkflowDefinition(version, List.copyOf(nodes), List.copyOf(edges));
    }

    // ── Internal helpers ───────────────────────────────────────────────

    private int nodeCount() {
        return nodes.size();
    }

    private void addNode(NodeInstance node) {
        nodes.add(node);
    }

    // ════════════════════════════════════════════════════════════════════
    // NodeBuilder
    // ════════════════════════════════════════════════════════════════════

    /**
     * Builder for a single workflow node. Supports adding params, variables,
     * and condition branches depending on the node type.
     */
    public static class NodeBuilder {

        private final WorkflowBuilder parent;
        private final String id;
        private final String type;
        private final String name;
        private final Map<String, Object> params = new LinkedHashMap<>();

        // Condition-specific
        private final List<Map<String, Object>> branches = new ArrayList<>();

        NodeBuilder(WorkflowBuilder parent, String id, String type, String name) {
            this.parent = parent;
            this.id = id;
            this.type = type;
            this.name = name;
        }

        /**
         * Add a generic parameter to this node.
         */
        public NodeBuilder param(String key, Object value) {
            params.put(key, value);
            return this;
        }

        /**
         * Add a variable definition (typically used on "start" nodes).
         *
         * @param name     variable name
         * @param type     variable type
         * @param required whether the variable is required
         */
        @SuppressWarnings("unchecked")
        public NodeBuilder variable(String name, ParamType type, boolean required) {
            List<Map<String, Object>> variables = (List<Map<String, Object>>) params.get("variables");
            if (variables == null) {
                variables = new ArrayList<>();
                params.put("variables", variables);
            }
            Map<String, Object> var = new LinkedHashMap<>();
            var.put("name", name);
            var.put("type", type.name());
            var.put("required", required);
            variables.add(var);
            return this;
        }

        /**
         * Start defining a condition branch.
         *
         * @param branchId the branch identifier (used as sourceAnchor on edges)
         */
        public ConditionBranchBuilder branch(String branchId) {
            return new ConditionBranchBuilder(this, branchId, false);
        }

        /**
         * Define the else (default) branch of a condition node.
         *
         * @param branchId the branch identifier
         */
        public ConditionBranchBuilder elseBranch(String branchId) {
            return new ConditionBranchBuilder(this, branchId, true);
        }

        /**
         * Finish building this node and return to the parent WorkflowBuilder.
         */
        public WorkflowBuilder done() {
            Map<String, Object> finalParams = new LinkedHashMap<>(params);
            if (!branches.isEmpty()) {
                finalParams.put("branches", List.copyOf(branches));
            }
            double x = parent.nodeCount() * 250.0;
            parent.addNode(new NodeInstance(id, type, name, new Position(x, 100), Map.copyOf(finalParams)));
            return parent;
        }

        void addBranch(Map<String, Object> branch) {
            branches.add(branch);
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // ConditionBranchBuilder
    // ════════════════════════════════════════════════════════════════════

    /**
     * Builder for a single branch within a condition node.
     * Supports adding comparison conditions like eq, notNull, etc.
     */
    public static class ConditionBranchBuilder {

        private final NodeBuilder parent;
        private final String branchId;
        private final boolean elseBranch;
        private final List<Map<String, Object>> conditions = new ArrayList<>();

        ConditionBranchBuilder(NodeBuilder parent, String branchId, boolean elseBranch) {
            this.parent = parent;
            this.branchId = branchId;
            this.elseBranch = elseBranch;
        }

        /**
         * Add an equality condition: left == right.
         */
        public ConditionBranchBuilder eq(String left, Object right) {
            return addCompare("eq", left, right);
        }

        /**
         * Add a not-null condition: the value is not null/empty.
         */
        public ConditionBranchBuilder notNull(String valueRef) {
            Map<String, Object> cond = new LinkedHashMap<>();
            cond.put("type", "not_null");
            cond.put("value", valueRef);
            conditions.add(cond);
            return this;
        }

        /**
         * Add a generic comparison condition.
         *
         * @param operator comparison operator (e.g. "eq", "ne", "gt", "lt", "contains")
         * @param left     left-hand side expression
         * @param right    right-hand side value
         */
        public ConditionBranchBuilder addCompare(String operator, String left, Object right) {
            Map<String, Object> cond = new LinkedHashMap<>();
            cond.put("type", "compare");
            cond.put("operator", operator);
            cond.put("left", left);
            cond.put("right", right);
            conditions.add(cond);
            return this;
        }

        /**
         * Finish building this branch and return to the NodeBuilder.
         */
        public NodeBuilder done() {
            Map<String, Object> branch = new LinkedHashMap<>();
            branch.put("id", branchId);
            if (elseBranch) {
                branch.put("type", "else");
            } else {
                branch.put("type", "condition");
                branch.put("conditions", List.copyOf(conditions));
            }
            parent.addBranch(branch);
            return parent;
        }
    }
}
