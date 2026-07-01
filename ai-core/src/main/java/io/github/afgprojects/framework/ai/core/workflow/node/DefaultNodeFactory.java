package io.github.afgprojects.framework.ai.core.workflow.node;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.WorkflowNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Aggregates per-type {@link NodeFactory} instances into a single resolver
 * keyed by node type id — the production implementation of the DAG engine's
 * {@code Function<String, WorkflowNode>} nodeResolver.
 *
 * <p>Built-in factories are registered here as nodes are migrated to the typed
 * base class; the {@link #SHARED_BINDER} is the single {@link ParamBinder}
 * reused by every node so that ObjectMapper configuration stays consistent.</p>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class DefaultNodeFactory implements java.util.function.Function<String, WorkflowNode> {

    /** Shared binder for all nodes built by this factory. */
    public static final ParamBinder SHARED_BINDER = new ParamBinder(new ObjectMapper());

    private final Map<String, NodeFactory> factories = new LinkedHashMap<>();

    public DefaultNodeFactory() {
        this(SHARED_BINDER);
    }

    public DefaultNodeFactory(ParamBinder binder) {
        // Built-in factories are registered here as nodes are migrated.
        // See BuiltinNodeFactoryRegistrar / per-node factory wiring in later batches.
    }

    /**
     * Register a factory for its type id.
     */
    public void register(NodeFactory factory) {
        factories.put(factory.type(), factory);
    }

    /**
     * Register several factories at once.
     */
    public void registerAll(List<NodeFactory> toRegister) {
        for (NodeFactory f : toRegister) {
            register(f);
        }
    }

    /**
     * Resolve and instantiate a node by type id, using a derived nodeId.
     *
     * <p>This is the {@code Function<String, WorkflowNode>} view expected by the
     * DAG engine, keyed by <em>type</em> (not nodeId — the engine passes the
     * node's type so the right implementation is instantiated).</p>
     *
     * @param type the node type id
     * @return a new node instance, or {@code null} if the type is unknown
     */
    @Override
    public WorkflowNode apply(String type) {
        return create(type, "node-" + type);
    }

    /**
     * Resolve and instantiate a node by type id with an explicit nodeId.
     */
    public WorkflowNode create(String type, String nodeId) {
        NodeFactory factory = factories.get(type);
        return factory == null ? null : factory.create(nodeId);
    }

    public Optional<NodeFactory> get(String type) {
        return Optional.ofNullable(factories.get(type));
    }
}
