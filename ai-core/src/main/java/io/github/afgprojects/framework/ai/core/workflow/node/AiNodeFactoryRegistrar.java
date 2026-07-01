package io.github.afgprojects.framework.ai.core.workflow.node;

import io.github.afgprojects.framework.ai.core.api.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.api.chat.AfgEmbeddingClient;
import io.github.afgprojects.framework.ai.core.workflow.node.ai.AiChatNode;
import io.github.afgprojects.framework.ai.core.workflow.node.ai.AiEmbeddingNode;

import java.util.function.Supplier;

/**
 * Registers {@link NodeFactory} instances for AI node types that need an
 * external client dependency, into a {@link DefaultNodeFactory}.
 *
 * <p>Clients are supplied as {@link Supplier}s so they can be resolved lazily
 * (they may be optional beans absent in some deployments). When a client is
 * absent the factory still registers the node type — the node degrades at
 * execution time with a clear "not configured" message, matching the original
 * behavior.</p>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public final class AiNodeFactoryRegistrar {

    private AiNodeFactoryRegistrar() {}

    /**
     * Register AI node factories for the given (possibly null) client suppliers.
     */
    public static void registerAll(DefaultNodeFactory factory,
                                   Supplier<AfgChatClient> chatClientSupplier,
                                   Supplier<AfgEmbeddingClient> embeddingClientSupplier) {
        factory.register(simpleFactory(AiChatNode.TYPE, nodeId ->
                new AiChatNode(nodeId, chatClientSupplier != null ? chatClientSupplier.get() : null)));
        factory.register(simpleFactory(AiEmbeddingNode.TYPE, nodeId ->
                new AiEmbeddingNode(nodeId, embeddingClientSupplier != null ? embeddingClientSupplier.get() : null)));
    }

    private static NodeFactory simpleFactory(String type,
                                             java.util.function.Function<String,
                                                     io.github.afgprojects.framework.ai.core.api.workflow.engine.WorkflowNode> ctor) {
        return new NodeFactory() {
            @Override
            public String type() {
                return type;
            }

            @Override
            public io.github.afgprojects.framework.ai.core.api.workflow.engine.WorkflowNode create(String nodeId) {
                return ctor.apply(nodeId);
            }
        };
    }
}
