package io.github.afgprojects.framework.ai.core.workflow.node;

import io.github.afgprojects.framework.ai.core.api.workflow.definition.NodeDefinition;
import io.github.afgprojects.framework.ai.core.api.workflow.definition.OutputSchema;
import io.github.afgprojects.framework.ai.core.api.workflow.definition.ParamSchema;
import io.github.afgprojects.framework.ai.core.api.workflow.definition.ParamType;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.NodeOutput;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.WorkflowNode;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Out;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Param;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.lang.reflect.RecordComponent;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Abstract base class for all workflow nodes, unifying the execution lifecycle
 * (timing, error handling, logging, default streaming) with schema-driven
 * parameter binding.
 *
 * <p>Each node declares a strongly-typed params record {@code P}. The base
 * class reflects over {@code P}'s record components and {@link Param} /
 * {@link Out} annotations to build the node's {@link NodeDefinition} — so the
 * parameter declaration, validation schema, and execution entry point all read
 * from the same record. At runtime {@code execute} binds the raw parameter map
 * to {@code P} via {@link ParamBinder} before delegating to
 * {@link #doExecute(ExecutionContext, Object)}; subclasses never touch a raw
 * {@code Map.get(String)}.</p>
 *
 * <p>Streaming nodes override {@link #doExecuteStream}; the default streams a
 * single COMPLETE event built from the synchronous result.</p>
 *
 * <h2>Migration bridge</h2>
 * Nodes not yet migrated to a typed params record may extend
 * {@code AbstractWorkflowNode<Map<String, Object>>} and override
 * {@link #paramSchema()} / {@link #outputSchema()} directly, keeping their
 * existing {@code doExecute(ctx, Map)} body unchanged. This keeps the codebase
 * compiling while nodes are migrated incrementally.
 *
 * @param <P> the strongly-typed params record (or {@code Map<String,Object>}
 *           for unmigrated nodes)
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
public abstract class AbstractWorkflowNode<P> implements WorkflowNode, NodeDefinition {

    private final String nodeId;
    private final String type;
    private final Class<P> paramType;

    private final ParamBinder paramBinder;

    /**
     * @param paramType the params record class (or {@code Map.class} for the
     *                  migration bridge). Declared as a raw {@link Class} so
     *                  call sites can pass {@code Map.class} for
     *                  {@code P = Map<String, Object>} without an unchecked cast.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected AbstractWorkflowNode(String nodeId, String type, Class paramType) {
        this(nodeId, type, (Class<P>) paramType, defaultBinder());
    }

    protected AbstractWorkflowNode(String nodeId, String type, Class<P> paramType, ParamBinder binder) {
        this.nodeId = nodeId;
        this.type = type;
        this.paramType = paramType;
        this.paramBinder = binder;
    }

    // ------------------------------------------------------------------
    // WorkflowNode
    // ------------------------------------------------------------------

    @Override
    public final String getNodeId() {
        return nodeId;
    }

    @Override
    public final String getType() {
        return type;
    }

    @Override
    public NodeOutput execute(ExecutionContext context, Map<String, Object> params) {
        long startTime = System.currentTimeMillis();
        try {
            log.debug("Node [{}] (type={}) executing", nodeId, type);
            P bound = paramBinder.bind(params != null ? params : Map.of(),
                    getParamSchema(), paramType, nodeId);
            NodeResult result = doExecuteRich(context, bound);
            long duration = System.currentTimeMillis() - startTime;
            String anchor = result.anchor() != null ? result.anchor() : "output";
            return NodeOutput.of(result.data(), anchor).withDuration(duration)
                    .withTokenUsage(result.tokenInput(), result.tokenOutput());
        } catch (ParamBindingException e) {
            log.error("Node [{}] (type={}) parameter binding failed: {}", nodeId, type, e.getMessage());
            long duration = System.currentTimeMillis() - startTime;
            return errorOutput(e.getMessage(), duration);
        } catch (Exception e) {
            log.error("Node [{}] (type={}) execution failed", nodeId, type, e);
            long duration = System.currentTimeMillis() - startTime;
            return errorOutput(e.getMessage() != null ? e.getMessage() : "Unknown error", duration);
        }
    }

    @Override
    public Flux<NodeEvent> executeStream(ExecutionContext context, Map<String, Object> params) {
        try {
            P bound = paramBinder.bind(params != null ? params : Map.of(),
                    getParamSchema(), paramType, nodeId);
            return doExecuteStream(context, bound);
        } catch (ParamBindingException e) {
            log.error("Node [{}] (type={}) parameter binding failed: {}", nodeId, type, e.getMessage());
            return Flux.just(NodeEvent.complete(
                    NodeOutput.of(Map.of("error", e.getMessage()))));
        }
    }

    /**
     * Execute the node-specific logic against the strongly-typed params.
     *
     * <p>Return a plain {@code Map} for the common case; nodes that need to
     * surface token usage override {@link #doExecuteRich} instead.</p>
     *
     * @param context the workflow execution context
     * @param params  the bound, validated, type-safe parameters
     * @return a map of output data
     */
    protected abstract Map<String, Object> doExecute(ExecutionContext context, P params);

    /**
     * Execute and return a {@link NodeResult} carrying data plus optional token
     * usage. Default delegates to {@link #doExecute}. Override when the node
     * needs to report token consumption (e.g. AI nodes).
     */
    protected NodeResult doExecuteRich(ExecutionContext context, P params) {
        return NodeResult.of(doExecute(context, params));
    }

    /**
     * Streaming hook. Default delegates to {@link #execute} and wraps the
     * result in a single COMPLETE event. Override for true streaming.
     */
    protected Flux<NodeEvent> doExecuteStream(ExecutionContext context, P params) {
        Map<String, Object> result = doExecute(context, params);
        return Flux.just(NodeEvent.complete(NodeOutput.of(result)));
    }

    /**
     * Result of node execution: output data, optional routing anchor, and
     * optional token usage. Built via {@link #of(Map)}; anchor defaults to null
     * (meaning "output") and token fields default to zero.
     */
    public record NodeResult(Map<String, Object> data, String anchor, long tokenInput, long tokenOutput) {
        public static NodeResult of(Map<String, Object> data) {
            return new NodeResult(data, null, 0, 0);
        }

        public static NodeResult of(Map<String, Object> data, String anchor) {
            return new NodeResult(data, anchor, 0, 0);
        }

        public static NodeResult of(Map<String, Object> data, long tokenInput, long tokenOutput) {
            return new NodeResult(data, null, tokenInput, tokenOutput);
        }

        public static NodeResult of(Map<String, Object> data, String anchor, long tokenInput, long tokenOutput) {
            return new NodeResult(data, anchor, tokenInput, tokenOutput);
        }
    }

    // ------------------------------------------------------------------
    // NodeDefinition (self-describing, reflected from P)
    // ------------------------------------------------------------------

    @Override
    public String getDisplayName() {
        return type;
    }

    @Override
    public String getCategory() {
        return category();
    }

    /**
     * Node category id (e.g. a {@link io.github.afgprojects.framework.ai.core.api.workflow.node.NodeCategory}
     * name). Migrated nodes override this; the migration bridge leaves it empty
     * since category is still declared in {@code BuiltinNodeRegistrar} until
     * batch 1 switches to self-describing registration.
     */
    protected String category() {
        return "";
    }

    @Override
    public Map<String, ParamSchema> getParamSchema() {
        return isRawMapParam() ? paramSchema() : reflectParamSchema();
    }

    @Override
    public Map<String, OutputSchema> getOutputSchema() {
        return isRawMapParam() ? outputSchema() : reflectOutputSchema();
    }

    @Override
    public Set<String> getSourceAnchors() {
        return Set.of("output");
    }

    @Override
    public Set<String> getTargetAnchors() {
        return Set.of("input");
    }

    // ------------------------------------------------------------------
    // Migration-bridge hooks: overridden by unmigrated nodes only.
    // ------------------------------------------------------------------

    /**
     * Override only when extending with {@code P = Map<String,Object>} (unmigrated).
     * Typed nodes get schema from record reflection instead.
     */
    protected Map<String, ParamSchema> paramSchema() {
        return Map.of();
    }

    /**
     * Override only when extending with {@code P = Map<String,Object>} (unmigrated).
     */
    protected Map<String, OutputSchema> outputSchema() {
        return Map.of();
    }

    // ------------------------------------------------------------------
    // Reflection of the params record into ParamSchema / OutputSchema.
    // ------------------------------------------------------------------

    private boolean isRawMapParam() {
        return paramType == Map.class;
    }

    private Map<String, ParamSchema> reflectParamSchema() {
        Map<String, ParamSchema> result = new LinkedHashMap<>();
        for (RecordComponent rc : paramType.getRecordComponents()) {
            Param p = rc.getAnnotation(Param.class);
            if (p == null) {
                continue;
            }
            ParamType inferred = p.type() != ParamType.OBJECT || p.enumValues().length > 0
                    ? (p.enumValues().length > 0 ? ParamType.ENUM : p.type())
                    : inferType(rc.getType());
            result.put(rc.getName(), new ParamSchema(
                    rc.getName(),
                    inferred,
                    p.displayName().isEmpty() ? rc.getName() : p.displayName(),
                    p.description().isEmpty() ? null : p.description(),
                    p.required(),
                    p.defaultValue().isEmpty() ? null : p.defaultValue(),
                    p.enumValues().length == 0 ? null : String.join(",", p.enumValues())
            ));
        }
        return result;
    }

    private Map<String, OutputSchema> reflectOutputSchema() {
        Map<String, OutputSchema> result = new LinkedHashMap<>();
        Class<?> outRecord = outputRecordType();
        if (outRecord == null) {
            return result;
        }
        for (RecordComponent rc : outRecord.getRecordComponents()) {
            Out o = rc.getAnnotation(Out.class);
            if (o == null) {
                continue;
            }
            ParamType inferred = o.type() != ParamType.OBJECT ? o.type() : inferType(rc.getType());
            result.put(rc.getName(), new OutputSchema(
                    rc.getName(),
                    inferred,
                    o.description().isEmpty() ? null : o.description(),
                    o.optional()
            ));
        }
        return result;
    }

    /**
     * Override to declare an output-descriptor record carrying {@link Out}
     * annotations. Default {@code null} → no reflected output schema.
     */
    protected Class<?> outputRecordType() {
        return null;
    }

    private ParamType inferType(Class<?> javaType) {
        if (javaType == String.class || CharSequence.class.isAssignableFrom(javaType)) {
            return ParamType.STRING;
        }
        if (javaType == Boolean.class || javaType == boolean.class) {
            return ParamType.BOOLEAN;
        }
        if (Number.class.isAssignableFrom(javaType) || javaType.isPrimitive()) {
            return ParamType.NUMBER;
        }
        if (javaType.isArray() || Iterable.class.isAssignableFrom(javaType)) {
            return ParamType.ARRAY;
        }
        return ParamType.OBJECT;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private NodeOutput errorOutput(String message, long duration) {
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("error", message);
        errorData.put("nodeType", type);
        return NodeOutput.of(errorData).withDuration(duration);
    }

    private static ParamBinder defaultBinder() {
        return DefaultNodeFactory.SHARED_BINDER;
    }
}
