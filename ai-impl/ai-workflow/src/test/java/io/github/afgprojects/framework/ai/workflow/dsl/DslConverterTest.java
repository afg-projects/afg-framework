package io.github.afgprojects.framework.ai.workflow.dsl;

import io.github.afgprojects.framework.ai.core.workflow.definition.EdgeDefinition;
import io.github.afgprojects.framework.ai.core.workflow.definition.WorkflowDefinition;
import io.github.afgprojects.framework.ai.core.workflow.definition.WorkflowDefinition.NodeInstance;
import io.github.afgprojects.framework.ai.core.workflow.definition.WorkflowDefinition.Position;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DslConverterTest {

    private final DefaultDslConverter converter = new DefaultDslConverter();

    @Test
    void roundTripJsonToWorkflowToJson() {
        WorkflowDefinition original = new WorkflowDefinition(
            "1.0",
            List.of(
                new NodeInstance("start_1", "start", "Start", new Position(100, 200), Map.of("prompt", "Hello")),
                new NodeInstance("llm_1", "llm", "LLM", new Position(300, 200), Map.of("model", "gpt-4"))
            ),
            List.of(
                new EdgeDefinition("e1", "start_1", "llm_1", "output")
            )
        );

        String json = converter.toJson(original);
        WorkflowDefinition roundTripped = converter.fromJson(json);

        // Verify version
        assertEquals(original.version(), roundTripped.version());

        // Verify nodes
        assertEquals(original.nodes().size(), roundTripped.nodes().size());
        for (int i = 0; i < original.nodes().size(); i++) {
            NodeInstance orig = original.nodes().get(i);
            NodeInstance rt = roundTripped.nodes().get(i);
            assertEquals(orig.id(), rt.id());
            assertEquals(orig.type(), rt.type());
            assertEquals(orig.name(), rt.name());
            assertEquals(orig.position().x(), rt.position().x());
            assertEquals(orig.position().y(), rt.position().y());
        }

        // Verify edges
        assertEquals(original.edges().size(), roundTripped.edges().size());
        for (int i = 0; i < original.edges().size(); i++) {
            EdgeDefinition orig = original.edges().get(i);
            EdgeDefinition rt = roundTripped.edges().get(i);
            assertEquals(orig.id(), rt.id());
            assertEquals(orig.source(), rt.source());
            assertEquals(orig.target(), rt.target());
            assertEquals(orig.sourceAnchor(), rt.sourceAnchor());
        }
    }

    @Test
    void fromJsonWithDefaultSourceAnchor() {
        String json = """
            {
              "version": "1.0",
              "nodes": [
                {
                  "id": "start_1",
                  "type": "start",
                  "name": "Start",
                  "position": {"x": 0, "y": 0},
                  "params": {}
                },
                {
                  "id": "end_1",
                  "type": "end",
                  "name": "End",
                  "position": {"x": 200, "y": 0},
                  "params": {}
                }
              ],
              "edges": [
                {
                  "id": "e1",
                  "source": "start_1",
                  "target": "end_1"
                }
              ]
            }
            """;

        WorkflowDefinition workflow = converter.fromJson(json);

        assertEquals(1, workflow.edges().size());
        // EdgeDefinition compact constructor defaults sourceAnchor to "output"
        assertEquals("output", workflow.edges().get(0).sourceAnchor());
    }

    @Test
    void toJsonProducesValidJson() {
        WorkflowDefinition workflow = new WorkflowDefinition(
            "1.0",
            List.of(
                new NodeInstance("a", "start", "A", new Position(0, 0), Map.of())
            ),
            List.of()
        );

        String json = converter.toJson(workflow);

        assertNotNull(json);
        assertTrue(json.contains("\"version\""));
        assertTrue(json.contains("\"nodes\""));
        assertTrue(json.contains("\"a\""));
    }
}
