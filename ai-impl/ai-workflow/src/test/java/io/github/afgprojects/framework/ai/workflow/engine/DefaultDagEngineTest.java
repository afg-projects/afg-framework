package io.github.afgprojects.framework.ai.workflow.engine;

import io.github.afgprojects.framework.ai.core.api.workflow.definition.EdgeDefinition;
import io.github.afgprojects.framework.ai.core.api.workflow.definition.WorkflowDefinition;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DefaultDagEngine.
 */
class DefaultDagEngineTest {

    private DefaultDagEngine engine;

    @BeforeEach
    void setUp() {
        // Node resolver will be set per test
    }

    @Test
    @DisplayName("Simple linear workflow: Start -> Reply")
    void testSimpleLinearWorkflow() {
        // Create test nodes
        TestStartNode startNode = new TestStartNode("start-1");
        TestReplyNode replyNode = new TestReplyNode("reply-1", "Hello, World!");

        Function<String, WorkflowNode> nodeResolver = nodeId -> switch (nodeId) {
            case "start-1" -> startNode;
            case "reply-1" -> replyNode;
            default -> null;
        };

        engine = new DefaultDagEngine(nodeResolver);

        // Create workflow definition
        WorkflowDefinition workflow = new WorkflowDefinition(
                "1.0",
                List.of(
                        new WorkflowDefinition.NodeInstance("start-1", "start", "Start",
                                new WorkflowDefinition.Position(0, 0), Map.of()),
                        new WorkflowDefinition.NodeInstance("reply-1", "reply", "Reply",
                                new WorkflowDefinition.Position(100, 0), Map.of("content", "Hello, World!"))
                ),
                List.of(
                        new EdgeDefinition("e1", "start-1", "reply-1", "output")
                )
        );

        // Create execution context
        ExecutionContext context = new DefaultExecutionContext("wf-1", "conv-1", "user-1");

        // Execute
        DagResult result = engine.execute(workflow, context);

        // Verify
        assertNotNull(result);
        assertEquals(DagStatus.SUCCESS, result.status());
        assertEquals("Hello, World!", result.content());
        assertEquals(2, result.nodeOutputs().size());

        // Verify start node output
        NodeOutput startOutput = result.nodeOutputs().get("start-1");
        assertNotNull(startOutput);
        assertEquals("test input", startOutput.data().get("user_input"));

        // Verify reply node output
        NodeOutput replyOutput = result.nodeOutputs().get("reply-1");
        assertNotNull(replyOutput);
        assertEquals("Hello, World!", replyOutput.data().get("content"));
    }

    @Test
    @DisplayName("Conditional branch workflow: Start -> Condition -> Reply_A / Reply_B")
    void testConditionalBranchWorkflow() {
        // Create test nodes
        TestStartNode startNode = new TestStartNode("start-1");
        TestConditionNode conditionNode = new TestConditionNode("condition-1", "branch_a");
        TestReplyNode replyNodeA = new TestReplyNode("reply-a", "Branch A output");
        TestReplyNode replyNodeB = new TestReplyNode("reply-b", "Branch B output");

        Function<String, WorkflowNode> nodeResolver = nodeId -> switch (nodeId) {
            case "start-1" -> startNode;
            case "condition-1" -> conditionNode;
            case "reply-a" -> replyNodeA;
            case "reply-b" -> replyNodeB;
            default -> null;
        };

        engine = new DefaultDagEngine(nodeResolver);

        // Create workflow definition
        WorkflowDefinition workflow = new WorkflowDefinition(
                "1.0",
                List.of(
                        new WorkflowDefinition.NodeInstance("start-1", "start", "Start",
                                new WorkflowDefinition.Position(0, 0), Map.of()),
                        new WorkflowDefinition.NodeInstance("condition-1", "condition", "Condition",
                                new WorkflowDefinition.Position(100, 0), Map.of()),
                        new WorkflowDefinition.NodeInstance("reply-a", "reply", "Reply A",
                                new WorkflowDefinition.Position(200, -50), Map.of("content", "Branch A output")),
                        new WorkflowDefinition.NodeInstance("reply-b", "reply", "Reply B",
                                new WorkflowDefinition.Position(200, 50), Map.of("content", "Branch B output"))
                ),
                List.of(
                        new EdgeDefinition("e1", "start-1", "condition-1", "output"),
                        new EdgeDefinition("e2", "condition-1", "reply-a", "branch_a"),
                        new EdgeDefinition("e3", "condition-1", "reply-b", "branch_b")
                )
        );

        // Create execution context
        ExecutionContext context = new DefaultExecutionContext("wf-2", "conv-2", "user-1");

        // Execute
        DagResult result = engine.execute(workflow, context);

        // Verify
        assertNotNull(result);
        assertEquals(DagStatus.SUCCESS, result.status());
        assertEquals("Branch A output", result.content());

        // Verify only branch A reply was executed
        assertTrue(result.nodeOutputs().containsKey("start-1"));
        assertTrue(result.nodeOutputs().containsKey("condition-1"));
        assertTrue(result.nodeOutputs().containsKey("reply-a"));
        assertFalse(result.nodeOutputs().containsKey("reply-b"));

        // Verify condition node output anchor
        NodeOutput conditionOutput = result.nodeOutputs().get("condition-1");
        assertNotNull(conditionOutput);
        assertEquals("branch_a", conditionOutput.anchor());
    }

    @Test
    @DisplayName("Conditional branch workflow: Branch B selected")
    void testConditionalBranchWorkflow_BranchB() {
        // Create test nodes
        TestStartNode startNode = new TestStartNode("start-1");
        TestConditionNode conditionNode = new TestConditionNode("condition-1", "branch_b");
        TestReplyNode replyNodeA = new TestReplyNode("reply-a", "Branch A output");
        TestReplyNode replyNodeB = new TestReplyNode("reply-b", "Branch B output");

        Function<String, WorkflowNode> nodeResolver = nodeId -> switch (nodeId) {
            case "start-1" -> startNode;
            case "condition-1" -> conditionNode;
            case "reply-a" -> replyNodeA;
            case "reply-b" -> replyNodeB;
            default -> null;
        };

        engine = new DefaultDagEngine(nodeResolver);

        // Create workflow definition
        WorkflowDefinition workflow = new WorkflowDefinition(
                "1.0",
                List.of(
                        new WorkflowDefinition.NodeInstance("start-1", "start", "Start",
                                new WorkflowDefinition.Position(0, 0), Map.of()),
                        new WorkflowDefinition.NodeInstance("condition-1", "condition", "Condition",
                                new WorkflowDefinition.Position(100, 0), Map.of()),
                        new WorkflowDefinition.NodeInstance("reply-a", "reply", "Reply A",
                                new WorkflowDefinition.Position(200, -50), Map.of("content", "Branch A output")),
                        new WorkflowDefinition.NodeInstance("reply-b", "reply", "Reply B",
                                new WorkflowDefinition.Position(200, 50), Map.of("content", "Branch B output"))
                ),
                List.of(
                        new EdgeDefinition("e1", "start-1", "condition-1", "output"),
                        new EdgeDefinition("e2", "condition-1", "reply-a", "branch_a"),
                        new EdgeDefinition("e3", "condition-1", "reply-b", "branch_b")
                )
        );

        // Create execution context
        ExecutionContext context = new DefaultExecutionContext("wf-3", "conv-3", "user-1");

        // Execute
        DagResult result = engine.execute(workflow, context);

        // Verify
        assertNotNull(result);
        assertEquals(DagStatus.SUCCESS, result.status());
        assertEquals("Branch B output", result.content());

        // Verify only branch B reply was executed
        assertTrue(result.nodeOutputs().containsKey("start-1"));
        assertTrue(result.nodeOutputs().containsKey("condition-1"));
        assertFalse(result.nodeOutputs().containsKey("reply-a"));
        assertTrue(result.nodeOutputs().containsKey("reply-b"));
    }

    @Test
    @DisplayName("Variable reference in params: ${start-1.user_input}")
    void testVariableReferenceInParams() {
        // Create test nodes
        TestStartNode startNode = new TestStartNode("start-1");
        TestReplyNode replyNode = new TestReplyNode("reply-1", null) {
            @Override
            public NodeOutput execute(ExecutionContext context, Map<String, Object> params) {
                // The content should be rendered from variable reference
                String content = (String) params.get("content");
                return NodeOutput.of(Map.of("content", content));
            }
        };

        Function<String, WorkflowNode> nodeResolver = nodeId -> switch (nodeId) {
            case "start-1" -> startNode;
            case "reply-1" -> replyNode;
            default -> null;
        };

        engine = new DefaultDagEngine(nodeResolver);

        // Create workflow definition with variable reference
        WorkflowDefinition workflow = new WorkflowDefinition(
                "1.0",
                List.of(
                        new WorkflowDefinition.NodeInstance("start-1", "start", "Start",
                                new WorkflowDefinition.Position(0, 0), Map.of()),
                        new WorkflowDefinition.NodeInstance("reply-1", "reply", "Reply",
                                new WorkflowDefinition.Position(100, 0),
                                Map.of("content", "${start-1.user_input}"))
                ),
                List.of(
                        new EdgeDefinition("e1", "start-1", "reply-1", "output")
                )
        );

        // Create execution context
        ExecutionContext context = new DefaultExecutionContext("wf-4", "conv-4", "user-1");

        // Execute
        DagResult result = engine.execute(workflow, context);

        // Verify
        assertNotNull(result);
        assertEquals(DagStatus.SUCCESS, result.status());
        // The content should be resolved from start node's output
        assertEquals("test input", result.content());
    }

    // --- Test Node Implementations ---

    /**
     * Test Start Node - outputs a fixed user_input value.
     */
    static class TestStartNode implements WorkflowNode {
        private final String nodeId;

        TestStartNode(String nodeId) {
            this.nodeId = nodeId;
        }

        @Override
        public String getNodeId() {
            return nodeId;
        }

        @Override
        public String getType() {
            return "start";
        }

        @Override
        public NodeOutput execute(ExecutionContext context, Map<String, Object> params) {
            return NodeOutput.of(Map.of("user_input", "test input"));
        }

        @Override
        public reactor.core.publisher.Flux<NodeEvent> executeStream(ExecutionContext context, Map<String, Object> params) {
            return reactor.core.publisher.Flux.just(NodeEvent.complete(execute(context, params)));
        }
    }

    /**
     * Test Reply Node - outputs the content from params.
     */
    static class TestReplyNode implements WorkflowNode {
        private final String nodeId;
        private final String fixedContent;

        TestReplyNode(String nodeId, String fixedContent) {
            this.nodeId = nodeId;
            this.fixedContent = fixedContent;
        }

        @Override
        public String getNodeId() {
            return nodeId;
        }

        @Override
        public String getType() {
            return "reply";
        }

        @Override
        public NodeOutput execute(ExecutionContext context, Map<String, Object> params) {
            String content = fixedContent;
            if (content == null) {
                content = (String) params.get("content");
            }
            return NodeOutput.of(Map.of("content", content));
        }

        @Override
        public reactor.core.publisher.Flux<NodeEvent> executeStream(ExecutionContext context, Map<String, Object> params) {
            return reactor.core.publisher.Flux.just(NodeEvent.complete(execute(context, params)));
        }
    }

    /**
     * Test Condition Node - outputs anchor based on matchedBranch.
     */
    static class TestConditionNode implements WorkflowNode {
        private final String nodeId;
        private final String matchedBranch;

        TestConditionNode(String nodeId, String matchedBranch) {
            this.nodeId = nodeId;
            this.matchedBranch = matchedBranch;
        }

        @Override
        public String getNodeId() {
            return nodeId;
        }

        @Override
        public String getType() {
            return "condition";
        }

        @Override
        public NodeOutput execute(ExecutionContext context, Map<String, Object> params) {
            return NodeOutput.of(Map.of("branch", matchedBranch), matchedBranch);
        }

        @Override
        public reactor.core.publisher.Flux<NodeEvent> executeStream(ExecutionContext context, Map<String, Object> params) {
            return reactor.core.publisher.Flux.just(NodeEvent.complete(execute(context, params)));
        }
    }
}
