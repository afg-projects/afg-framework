package io.github.afgprojects.framework.ai.workflow.dsl;

import io.github.afgprojects.framework.ai.core.api.workflow.definition.EdgeDefinition;
import io.github.afgprojects.framework.ai.core.api.workflow.definition.ParamType;
import io.github.afgprojects.framework.ai.core.api.workflow.definition.WorkflowDefinition;
import io.github.afgprojects.framework.ai.core.api.workflow.definition.WorkflowDefinition.NodeInstance;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowBuilderTest {

    @Test
    void buildSimpleWorkflow_startAiChatReply() {
        // Start → AiChat → Reply
        WorkflowDefinition workflow = WorkflowBuilder.create()
                .start("start_1")
                    .variable("user_input", ParamType.STRING, true)
                    .done()
                .aiChat("ai_chat_1")
                    .param("prompt", "Hello")
                    .param("modelId", "qwen2.5:7b")
                    .done()
                .reply("reply_1")
                    .param("content", "${ai_chat_1.content}")
                    .done()
                .edge("start_1", "ai_chat_1")
                .edge("ai_chat_1", "reply_1")
                .build();

        // Verify version
        assertEquals("1.0", workflow.version());

        // Verify node count
        assertEquals(3, workflow.nodes().size());

        // Verify node types
        assertEquals("start", workflow.nodes().get(0).type());
        assertEquals("ai-chat", workflow.nodes().get(1).type());
        assertEquals("reply", workflow.nodes().get(2).type());

        // Verify node ids
        assertEquals("start_1", workflow.nodes().get(0).id());
        assertEquals("ai_chat_1", workflow.nodes().get(1).id());
        assertEquals("reply_1", workflow.nodes().get(2).id());

        // Verify start node has variables
        NodeInstance startNode = workflow.nodes().get(0);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> variables = (List<Map<String, Object>>) startNode.params().get("variables");
        assertNotNull(variables);
        assertEquals(1, variables.size());
        assertEquals("user_input", variables.get(0).get("name"));
        assertEquals("STRING", variables.get(0).get("type"));
        assertEquals(true, variables.get(0).get("required"));

        // Verify ai-chat node params
        NodeInstance aiChatNode = workflow.nodes().get(1);
        assertEquals("Hello", aiChatNode.params().get("prompt"));
        assertEquals("qwen2.5:7b", aiChatNode.params().get("modelId"));

        // Verify reply node params
        NodeInstance replyNode = workflow.nodes().get(2);
        assertEquals("${ai_chat_1.content}", replyNode.params().get("content"));

        // Verify edge count
        assertEquals(2, workflow.edges().size());

        // Verify edges
        EdgeDefinition edge1 = workflow.edges().get(0);
        assertEquals("start_1", edge1.source());
        assertEquals("ai_chat_1", edge1.target());
        assertEquals("output", edge1.sourceAnchor());

        EdgeDefinition edge2 = workflow.edges().get(1);
        assertEquals("ai_chat_1", edge2.source());
        assertEquals("reply_1", edge2.target());
        assertEquals("output", edge2.sourceAnchor());
    }

    @Test
    void buildConditionWorkflow_verifySourceAnchors() {
        // Start → AiChat → Condition → (branch_a → Reply_a, branch_b → Reply_b, else → Reply_c)
        WorkflowDefinition workflow = WorkflowBuilder.create()
                .start("start_1")
                    .done()
                .aiChat("ai_chat_1")
                    .param("prompt", "分析意图")
                    .done()
                .condition("cond_1")
                    .branch("branch_a")
                        .eq("${ai_chat_1.intent}", "search")
                        .done()
                    .branch("branch_b")
                        .notNull("${ai_chat_1.intent}")
                        .done()
                    .elseBranch("branch_c")
                        .done()
                    .done()
                .reply("reply_a")
                    .param("content", "搜索结果")
                    .done()
                .reply("reply_b")
                    .param("content", "意图分析")
                    .done()
                .reply("reply_c")
                    .param("content", "默认回复")
                    .done()
                .edge("start_1", "ai_chat_1")
                .edge("ai_chat_1", "cond_1")
                .edge("cond_1", "reply_a", "branch_a")
                .edge("cond_1", "reply_b", "branch_b")
                .edge("cond_1", "reply_c", "branch_c")
                .build();

        // Verify node count (start + aiChat + condition + 3 replies = 6)
        assertEquals(6, workflow.nodes().size());

        // Verify condition node has branches
        NodeInstance condNode = workflow.nodes().stream()
                .filter(n -> "cond_1".equals(n.id()))
                .findFirst()
                .orElseThrow();
        assertEquals("condition", condNode.type());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> branches = (List<Map<String, Object>>) condNode.params().get("branches");
        assertNotNull(branches);
        assertEquals(3, branches.size());

        // Verify branch_a has eq condition
        Map<String, Object> branchA = branches.get(0);
        assertEquals("branch_a", branchA.get("id"));
        assertEquals("condition", branchA.get("type"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> conditionsA = (List<Map<String, Object>>) branchA.get("conditions");
        assertEquals(1, conditionsA.size());
        assertEquals("eq", conditionsA.get(0).get("operator"));
        assertEquals("${ai_chat_1.intent}", conditionsA.get(0).get("left"));
        assertEquals("search", conditionsA.get(0).get("right"));

        // Verify branch_b has notNull condition
        Map<String, Object> branchB = branches.get(1);
        assertEquals("branch_b", branchB.get("id"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> conditionsB = (List<Map<String, Object>>) branchB.get("conditions");
        assertEquals(1, conditionsB.size());
        assertEquals("not_null", conditionsB.get(0).get("type"));

        // Verify else branch
        Map<String, Object> branchC = branches.get(2);
        assertEquals("branch_c", branchC.get("id"));
        assertEquals("else", branchC.get("type"));

        // Verify edge count
        assertEquals(5, workflow.edges().size());

        // Verify condition edges have correct sourceAnchor values
        EdgeDefinition edgeToA = workflow.edges().stream()
                .filter(e -> "reply_a".equals(e.target()))
                .findFirst()
                .orElseThrow();
        assertEquals("cond_1", edgeToA.source());
        assertEquals("branch_a", edgeToA.sourceAnchor());

        EdgeDefinition edgeToB = workflow.edges().stream()
                .filter(e -> "reply_b".equals(e.target()))
                .findFirst()
                .orElseThrow();
        assertEquals("cond_1", edgeToB.source());
        assertEquals("branch_b", edgeToB.sourceAnchor());

        EdgeDefinition edgeToC = workflow.edges().stream()
                .filter(e -> "reply_c".equals(e.target()))
                .findFirst()
                .orElseThrow();
        assertEquals("cond_1", edgeToC.source());
        assertEquals("branch_c", edgeToC.sourceAnchor());

        // Verify default edge has "output" sourceAnchor
        EdgeDefinition defaultEdge = workflow.edges().stream()
                .filter(e -> "ai_chat_1".equals(e.source()) && "cond_1".equals(e.target()))
                .findFirst()
                .orElseThrow();
        assertEquals("output", defaultEdge.sourceAnchor());
    }

    @Test
    void buildWorkflow_withFunctionToolAndLoop() {
        WorkflowDefinition workflow = WorkflowBuilder.create()
                .start("start_1")
                    .done()
                .functionTool("tool_1")
                    .param("toolName", "web_search")
                    .done()
                .loop("loop_1")
                    .param("maxIterations", 5)
                    .done()
                .reply("reply_1")
                    .done()
                .edge("start_1", "tool_1")
                .edge("tool_1", "loop_1")
                .edge("loop_1", "reply_1")
                .build();

        assertEquals(4, workflow.nodes().size());
        assertEquals("function-tool", workflow.nodes().get(1).type());
        assertEquals("loop", workflow.nodes().get(2).type());
        assertEquals(3, workflow.edges().size());
    }

    @Test
    void buildWorkflow_customVersion() {
        WorkflowDefinition workflow = WorkflowBuilder.create()
                .version("2.0")
                .start("start_1")
                    .done()
                .build();

        assertEquals("2.0", workflow.version());
    }
}
