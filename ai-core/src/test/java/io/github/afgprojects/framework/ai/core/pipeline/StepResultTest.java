package io.github.afgprojects.framework.ai.core.api.pipeline.

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class StepResultTest {
    @Test
    void ok_shouldNotBeSkipped() {
        StepResult result = StepResult.ok(Map.of("key", "value"));
        assertFalse(result.skipped());
        assertNull(result.skipReason());
        assertEquals("value", result.outputVariables().get("key"));
    }

    @Test
    void skip_shouldBeSkipped() {
        StepResult result = StepResult.skip("reason");
        assertTrue(result.skipped());
        assertEquals("reason", result.skipReason());
        assertTrue(result.outputVariables().isEmpty());
    }
}
