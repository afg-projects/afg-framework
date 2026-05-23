package io.github.afgprojects.framework.ai.core.etl;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EtlResultTest {

    @Test
    void testIsSuccess() {
        EtlResult success = new EtlResult(10, 10, 0, List.of(), List.of(), Duration.ofSeconds(1), java.util.Map.of());
        assertTrue(success.isSuccess());

        EtlResult failure = new EtlResult(10, 8, 2, List.of(), List.of(), Duration.ofSeconds(1), java.util.Map.of());
        assertFalse(failure.isSuccess());
    }

    @Test
    void testGetSuccessRate() {
        EtlResult result = new EtlResult(10, 8, 2, List.of(), List.of(), Duration.ofSeconds(1), java.util.Map.of());
        assertEquals(0.8, result.getSuccessRate(), 0.001);

        EtlResult empty = EtlResult.empty(Duration.ZERO);
        assertEquals(0.0, empty.getSuccessRate(), 0.001);
    }

    @Test
    void testBuilder() {
        EtlResult result = EtlResult.builder()
            .totalDocuments(10)
            .successCount(8)
            .failureCount(2)
            .duration(Duration.ofSeconds(5))
            .build();

        assertEquals(10, result.totalDocuments());
        assertEquals(8, result.successCount());
        assertEquals(2, result.failureCount());
        assertEquals(Duration.ofSeconds(5), result.duration());
    }
}