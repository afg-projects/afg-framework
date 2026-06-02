package io.github.afgprojects.framework.ai.workflow.engine;

import io.github.afgprojects.framework.ai.core.api.workflow.definition.EdgeDefinition;
import io.github.afgprojects.framework.ai.core.api.workflow.definition.WorkflowDefinition;
import io.github.afgprojects.framework.ai.core.api.workflow.definition.WorkflowDefinition.NodeInstance;
import io.github.afgprojects.framework.ai.core.api.workflow.definition.WorkflowDefinition.Position;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TopologicalSorterTest {

    private final TopologicalSorter sorter = new TopologicalSorter();

    @Test
    void simpleLinearGraph() {
        // a -> b -> c
        WorkflowDefinition workflow = new WorkflowDefinition(
            "1.0",
            List.of(
                new NodeInstance("a", "start", "A", new Position(0, 0), Map.of()),
                new NodeInstance("b", "llm", "B", new Position(100, 0), Map.of()),
                new NodeInstance("c", "end", "C", new Position(200, 0), Map.of())
            ),
            List.of(
                new EdgeDefinition("e1", "a", "b", "output"),
                new EdgeDefinition("e2", "b", "c", "output")
            )
        );

        List<List<String>> layers = sorter.sort(workflow);

        assertEquals(3, layers.size());
        assertEquals(List.of("a"), layers.get(0));
        assertEquals(List.of("b"), layers.get(1));
        assertEquals(List.of("c"), layers.get(2));
    }

    @Test
    void parallelNodesInSameLayer() {
        // a -> b, a -> c, b -> d, c -> d
        WorkflowDefinition workflow = new WorkflowDefinition(
            "1.0",
            List.of(
                new NodeInstance("a", "start", "A", new Position(0, 0), Map.of()),
                new NodeInstance("b", "llm", "B", new Position(100, -50), Map.of()),
                new NodeInstance("c", "llm", "C", new Position(100, 50), Map.of()),
                new NodeInstance("d", "end", "D", new Position(200, 0), Map.of())
            ),
            List.of(
                new EdgeDefinition("e1", "a", "b", "output"),
                new EdgeDefinition("e2", "a", "c", "output"),
                new EdgeDefinition("e3", "b", "d", "output"),
                new EdgeDefinition("e4", "c", "d", "output")
            )
        );

        List<List<String>> layers = sorter.sort(workflow);

        assertEquals(3, layers.size());
        assertEquals(List.of("a"), layers.get(0));
        // b and c should be in the same layer (order may vary)
        assertEquals(2, layers.get(1).size());
        assertTrue(layers.get(1).contains("b"));
        assertTrue(layers.get(1).contains("c"));
        assertEquals(List.of("d"), layers.get(2));
    }

    @Test
    void cycleDetection() {
        // a -> b -> a (cycle)
        WorkflowDefinition workflow = new WorkflowDefinition(
            "1.0",
            List.of(
                new NodeInstance("a", "start", "A", new Position(0, 0), Map.of()),
                new NodeInstance("b", "llm", "B", new Position(100, 0), Map.of())
            ),
            List.of(
                new EdgeDefinition("e1", "a", "b", "output"),
                new EdgeDefinition("e2", "b", "a", "output")
            )
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> sorter.sort(workflow));
        assertTrue(exception.getMessage().contains("cycle") || exception.getMessage().contains("Cycle"),
            "Exception message should mention cycle, got: " + exception.getMessage());
    }

    @Test
    void isolatedNodeAppearsInFirstLayer() {
        // a (isolated, no edges)
        WorkflowDefinition workflow = new WorkflowDefinition(
            "1.0",
            List.of(
                new NodeInstance("a", "start", "A", new Position(0, 0), Map.of())
            ),
            List.of()
        );

        List<List<String>> layers = sorter.sort(workflow);

        assertEquals(1, layers.size());
        assertEquals(List.of("a"), layers.get(0));
    }
}
