package io.github.afgprojects.framework.ai.workflow.engine;

import io.github.afgprojects.framework.ai.core.api.workflow.definition.EdgeDefinition;
import io.github.afgprojects.framework.ai.core.api.workflow.definition.WorkflowDefinition;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.*;
import io.github.afgprojects.framework.ai.workflow.dsl.DefaultVariableResolver;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Default implementation of DagEngine.
 * Executes workflow nodes in topological order with parallel execution within layers.
 */
@Slf4j
public class DefaultDagEngine implements DagEngine {

    private final Function<String, WorkflowNode> nodeResolver;
    private final TopologicalSorter sorter;
    private final DefaultVariableResolver variableResolver;

    public DefaultDagEngine(Function<String, WorkflowNode> nodeResolver) {
        this.nodeResolver = nodeResolver;
        this.sorter = new TopologicalSorter();
        this.variableResolver = new DefaultVariableResolver();
    }

    @Override
    public DagResult execute(WorkflowDefinition workflow, ExecutionContext context) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. Topological sort to get layers
            List<List<String>> layers = sorter.sort(workflow);

            // 2. Build edge index: source -> list of edges
            Map<String, List<EdgeDefinition>> edgesBySource = workflow.edges().stream()
                    .collect(Collectors.groupingBy(EdgeDefinition::source));

            // 3. Build nodeId -> type mapping
            Map<String, String> nodeTypes = workflow.nodes().stream()
                    .collect(Collectors.toMap(
                            WorkflowDefinition.NodeInstance::id,
                            WorkflowDefinition.NodeInstance::type
                    ));

            // 4. Build nodeId -> params mapping
            Map<String, Map<String, Object>> nodeParams = workflow.nodes().stream()
                    .collect(Collectors.toMap(
                            WorkflowDefinition.NodeInstance::id,
                            n -> n.params() != null ? n.params() : Map.of()
                    ));

            // 5. Build targetNodeId -> Set<sourceAnchor> mapping
            Map<String, Set<String>> targetAnchors = new HashMap<>();
            for (EdgeDefinition edge : workflow.edges()) {
                targetAnchors.computeIfAbsent(edge.target(), k -> new HashSet<>())
                        .add(edge.sourceAnchor());
            }

            // 6. Execute layer by layer
            String finalContent = null;

            for (List<String> layer : layers) {
                // Filter nodes that should execute
                List<String> executableNodes = layer.stream()
                        .filter(nodeId -> shouldExecute(nodeId, nodeTypes.get(nodeId),
                                edgesBySource, targetAnchors, context))
                        .toList();

                // Execute nodes in parallel within the layer
                executableNodes.parallelStream().forEach(nodeId -> {
                    String type = nodeTypes.get(nodeId);
                    Map<String, Object> params = renderParams(nodeParams.get(nodeId), context);

                    WorkflowNode node = nodeResolver.apply(nodeId);
                    if (node == null) {
                        log.warn("No node resolver found for nodeId: {}, skipping", nodeId);
                        return;
                    }

                    long nodeStart = System.currentTimeMillis();
                    try {
                        NodeOutput output = node.execute(context, params);
                        long duration = System.currentTimeMillis() - nodeStart;
                        if (output != null) {
                            output = output.withDuration(duration);
                        }
                        context.setNodeOutput(nodeId, output);
                        log.debug("Node {} executed successfully in {}ms", nodeId, duration);
                    } catch (Exception e) {
                        log.error("Node {} execution failed", nodeId, e);
                        context.setNodeOutput(nodeId, NodeOutput.of(
                                Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error")
                        ));
                    }
                });

                // Check for reply nodes in this layer to capture final content
                for (String nodeId : executableNodes) {
                    String type = nodeTypes.get(nodeId);
                    if ("reply".equals(type)) {
                        NodeOutput output = context.getNodeOutputs().get(nodeId);
                        if (output != null && output.data().containsKey("content")) {
                            finalContent = String.valueOf(output.data().get("content"));
                        }
                    }
                }
            }

            // 7. Aggregate token usage
            long totalTokenInput = 0;
            long totalTokenOutput = 0;
            for (NodeOutput output : context.getNodeOutputs().values()) {
                if (output != null) {
                    totalTokenInput += output.tokenInput();
                    totalTokenOutput += output.tokenOutput();
                }
            }

            long totalDuration = System.currentTimeMillis() - startTime;

            return new DagResult(
                    finalContent,
                    new HashMap<>(context.getNodeOutputs()),
                    totalTokenInput,
                    totalTokenOutput,
                    totalDuration,
                    DagStatus.SUCCESS
            );

        } catch (Exception e) {
            log.error("Workflow execution failed", e);
            long totalDuration = System.currentTimeMillis() - startTime;
            return new DagResult(
                    null,
                    new HashMap<>(context.getNodeOutputs()),
                    0, 0,
                    totalDuration,
                    DagStatus.FAILED
            );
        }
    }

    @Override
    public Flux<DagEvent> executeStream(WorkflowDefinition workflow, ExecutionContext context) {
        return Flux.create(sink -> {
            Thread.startVirtualThread(() -> {
                long startTime = System.currentTimeMillis();

                try {
                    // 1. Topological sort
                    List<List<String>> layers = sorter.sort(workflow);

                    // 2. Build edge index
                    Map<String, List<EdgeDefinition>> edgesBySource = workflow.edges().stream()
                            .collect(Collectors.groupingBy(EdgeDefinition::source));

                    // 3. Build nodeId -> type mapping
                    Map<String, String> nodeTypes = workflow.nodes().stream()
                            .collect(Collectors.toMap(
                                    WorkflowDefinition.NodeInstance::id,
                                    WorkflowDefinition.NodeInstance::type
                            ));

                    // 4. Build nodeId -> params mapping
                    Map<String, Map<String, Object>> nodeParams = workflow.nodes().stream()
                            .collect(Collectors.toMap(
                                    WorkflowDefinition.NodeInstance::id,
                                    n -> n.params() != null ? n.params() : Map.of()
                            ));

                    // 5. Build targetNodeId -> Set<sourceAnchor> mapping
                    Map<String, Set<String>> targetAnchors = new HashMap<>();
                    for (EdgeDefinition edge : workflow.edges()) {
                        targetAnchors.computeIfAbsent(edge.target(), k -> new HashSet<>())
                                .add(edge.sourceAnchor());
                    }

                    // 6. Execute layer by layer
                    String finalContent = null;

                    for (List<String> layer : layers) {
                        List<String> executableNodes = layer.stream()
                                .filter(nodeId -> shouldExecute(nodeId, nodeTypes.get(nodeId),
                                        edgesBySource, targetAnchors, context))
                                .toList();

                        for (String nodeId : executableNodes) {
                            String type = nodeTypes.get(nodeId);
                            Map<String, Object> params = renderParams(nodeParams.get(nodeId), context);

                            WorkflowNode node = nodeResolver.apply(nodeId);
                            if (node == null) {
                                log.warn("No node resolver found for nodeId: {}, skipping", nodeId);
                                continue;
                            }

                            // Emit NODE_START event
                            sink.next(DagEvent.nodeStart(nodeId));

                            long nodeStart = System.currentTimeMillis();

                            try {
                                // Use executeStream for streaming nodes, execute for non-streaming
                                if (isStreamingNode(type)) {
                                    node.executeStream(context, params)
                                            .doOnNext(nodeEvent -> {
                                                if ("TEXT".equals(nodeEvent.type())) {
                                                    sink.next(new DagEvent("NODE_STREAM", nodeId, nodeEvent.content()));
                                                } else if ("COMPLETE".equals(nodeEvent.type())) {
                                                    // Stream complete
                                                }
                                            })
                                            .blockLast();

                                    // After streaming, get the output from context (node should have set it)
                                    NodeOutput output = context.getNodeOutputs().get(nodeId);
                                    long duration = System.currentTimeMillis() - nodeStart;
                                    if (output != null) {
                                        output = output.withDuration(duration);
                                        context.setNodeOutput(nodeId, output);
                                    }
                                    sink.next(DagEvent.nodeComplete(nodeId, output));
                                } else {
                                    NodeOutput output = node.execute(context, params);
                                    long duration = System.currentTimeMillis() - nodeStart;
                                    if (output != null) {
                                        output = output.withDuration(duration);
                                    }
                                    context.setNodeOutput(nodeId, output);
                                    sink.next(DagEvent.nodeComplete(nodeId, output));
                                }

                                // Check for reply nodes
                                if ("reply".equals(type)) {
                                    NodeOutput output = context.getNodeOutputs().get(nodeId);
                                    if (output != null && output.data().containsKey("content")) {
                                        finalContent = String.valueOf(output.data().get("content"));
                                    }
                                }

                            } catch (Exception e) {
                                log.error("Node {} execution failed", nodeId, e);
                                sink.next(DagEvent.nodeError(nodeId,
                                        e.getMessage() != null ? e.getMessage() : "Unknown error"));
                            }
                        }
                    }

                    // 7. Aggregate token usage
                    long totalTokenInput = 0;
                    long totalTokenOutput = 0;
                    for (NodeOutput output : context.getNodeOutputs().values()) {
                        if (output != null) {
                            totalTokenInput += output.tokenInput();
                            totalTokenOutput += output.tokenOutput();
                        }
                    }

                    long totalDuration = System.currentTimeMillis() - startTime;

                    DagResult result = new DagResult(
                            finalContent,
                            new HashMap<>(context.getNodeOutputs()),
                            totalTokenInput,
                            totalTokenOutput,
                            totalDuration,
                            DagStatus.SUCCESS
                    );

                    sink.next(DagEvent.workflowComplete(result));
                    sink.complete();

                } catch (Exception e) {
                    log.error("Workflow streaming execution failed", e);
                    long totalDuration = System.currentTimeMillis() - startTime;
                    DagResult result = new DagResult(
                            null,
                            new HashMap<>(context.getNodeOutputs()),
                            0, 0,
                            totalDuration,
                            DagStatus.FAILED
                    );
                    sink.next(DagEvent.workflowComplete(result));
                    sink.complete();
                }
            });
        });
    }

    /**
     * Determine if a node should be executed based on upstream condition routing.
     *
     * - start nodes always execute
     * - If all incoming edges have sourceAnchor "output", the node always executes
     * - Otherwise, check if any upstream condition node's output anchor matches the edge's sourceAnchor
     */
    private boolean shouldExecute(String nodeId, String type,
                                  Map<String, List<EdgeDefinition>> edgesBySource,
                                  Map<String, Set<String>> targetAnchors,
                                  ExecutionContext context) {
        // Start nodes always execute
        if ("start".equals(type)) {
            return true;
        }

        // If all incoming edges have sourceAnchor "output", always execute
        Set<String> incomingAnchors = targetAnchors.get(nodeId);
        if (incomingAnchors == null || incomingAnchors.isEmpty()) {
            // No incoming edges (shouldn't happen for non-start nodes in a valid DAG)
            return true;
        }

        if (incomingAnchors.size() == 1 && incomingAnchors.contains("output")) {
            return true;
        }

        // Check if any upstream condition node's output anchor matches an incoming edge's sourceAnchor
        for (EdgeDefinition edge : edgesBySource.values().stream()
                .flatMap(List::stream)
                .filter(e -> nodeId.equals(e.target()))
                .toList()) {

            NodeOutput sourceOutput = context.getNodeOutputs().get(edge.source());
            if (sourceOutput == null) {
                // Source hasn't executed yet, skip this edge
                continue;
            }

            // If the source node's output anchor matches this edge's sourceAnchor, execute
            if (edge.sourceAnchor().equals(sourceOutput.anchor())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Render parameter templates by resolving variable references.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> renderParams(Map<String, Object> params, ExecutionContext context) {
        if (params == null || params.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> rendered = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String strValue) {
                rendered.put(entry.getKey(), variableResolver.renderTemplate(strValue, context.getNodeOutputs()));
            } else if (value instanceof Map<?, ?> mapValue) {
                rendered.put(entry.getKey(), renderParams((Map<String, Object>) mapValue, context));
            } else {
                rendered.put(entry.getKey(), value);
            }
        }
        return rendered;
    }

    /**
     * Determine if a node type supports streaming output.
     */
    private boolean isStreamingNode(String type) {
        return "ai-chat".equals(type) || "llm".equals(type) || "chat".equals(type) || "reply".equals(type);
    }
}
