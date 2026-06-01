package io.github.afgprojects.framework.ai.workflow.engine;

import io.github.afgprojects.framework.ai.core.workflow.definition.EdgeDefinition;
import io.github.afgprojects.framework.ai.core.workflow.definition.WorkflowDefinition;

import java.util.*;

/**
 * Kahn's algorithm topological sorter that outputs nodes by layer.
 * Nodes in the same layer can be executed in parallel.
 */
public class TopologicalSorter {

    /**
     * Sort the workflow nodes into layers using Kahn's algorithm.
     *
     * @param workflow the workflow definition
     * @return list of layers, each layer is a list of node IDs that can execute in parallel
     * @throws IllegalStateException if a cycle is detected
     */
    public List<List<String>> sort(WorkflowDefinition workflow) {
        // Build adjacency list and in-degree map
        Map<String, Set<String>> adjacency = new LinkedHashMap<>();
        Map<String, Integer> inDegree = new LinkedHashMap<>();

        // Initialize all nodes with 0 in-degree
        for (var node : workflow.nodes()) {
            adjacency.put(node.id(), new LinkedHashSet<>());
            inDegree.put(node.id(), 0);
        }

        // Build edges
        for (EdgeDefinition edge : workflow.edges()) {
            adjacency.get(edge.source()).add(edge.target());
            inDegree.merge(edge.target(), 1, Integer::sum);
        }

        // Kahn's algorithm - process by layers
        List<List<String>> layers = new ArrayList<>();
        Queue<String> queue = new ArrayDeque<>();

        // Start with all nodes that have 0 in-degree
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        int processedCount = 0;

        while (!queue.isEmpty()) {
            // All nodes currently in the queue form one layer
            List<String> layer = new ArrayList<>();
            int layerSize = queue.size();
            for (int i = 0; i < layerSize; i++) {
                String node = queue.poll();
                layer.add(node);
                processedCount++;

                // Reduce in-degree of neighbors
                for (String neighbor : adjacency.get(node)) {
                    int newDegree = inDegree.get(neighbor) - 1;
                    inDegree.put(neighbor, newDegree);
                    if (newDegree == 0) {
                        queue.add(neighbor);
                    }
                }
            }
            layers.add(layer);
        }

        // If not all nodes were processed, there is a cycle
        if (processedCount != workflow.nodes().size()) {
            throw new IllegalStateException("Workflow contains a cycle, topological sort is not possible");
        }

        return layers;
    }
}
