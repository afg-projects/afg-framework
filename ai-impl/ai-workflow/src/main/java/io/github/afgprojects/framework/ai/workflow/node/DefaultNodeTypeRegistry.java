package io.github.afgprojects.framework.ai.workflow.node;

import io.github.afgprojects.framework.ai.core.workflow.definition.NodeDefinition;
import io.github.afgprojects.framework.ai.core.workflow.node.NodeTypeRegistry;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Default implementation of NodeTypeRegistry using ConcurrentHashMap.
 */
public class DefaultNodeTypeRegistry implements NodeTypeRegistry {

    private final ConcurrentHashMap<String, NodeDefinition> registry = new ConcurrentHashMap<>();

    @Override
    public void register(NodeDefinition nodeDefinition) {
        registry.put(nodeDefinition.getType(), nodeDefinition);
    }

    @Override
    public Optional<NodeDefinition> get(String type) {
        return Optional.ofNullable(registry.get(type));
    }

    @Override
    public Collection<NodeDefinition> getAll() {
        return Collections.unmodifiableCollection(registry.values());
    }

    @Override
    public Collection<NodeDefinition> getByCategory(String category) {
        return registry.values().stream()
                .filter(def -> category.equals(def.getCategory()))
                .collect(Collectors.toUnmodifiableList());
    }
}
