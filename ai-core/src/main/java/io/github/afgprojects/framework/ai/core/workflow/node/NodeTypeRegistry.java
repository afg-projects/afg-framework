package io.github.afgprojects.framework.ai.core.workflow.node;

import io.github.afgprojects.framework.ai.core.workflow.definition.NodeDefinition;
import java.util.Collection;
import java.util.Optional;

public interface NodeTypeRegistry {
    void register(NodeDefinition nodeDefinition);
    Optional<NodeDefinition> get(String type);
    Collection<NodeDefinition> getAll();
    Collection<NodeDefinition> getByCategory(String category);
}
