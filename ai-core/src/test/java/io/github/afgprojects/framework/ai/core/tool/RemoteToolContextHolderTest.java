package io.github.afgprojects.framework.ai.core.api.tool.

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link RemoteToolContextHolder} 测试类。
 */
class RemoteToolContextHolderTest {

    @AfterEach
    void tearDown() {
        RemoteToolContextHolder.clear();
    }

    @Test
    void testSetAndGetContext() {
        ToolContext context = ToolContext.builder()
            .userId("user-001")
            .tenantId("tenant-001")
            .build();

        RemoteToolContextHolder.setContext(context);

        assertTrue(RemoteToolContextHolder.hasContext());
        assertEquals(context, RemoteToolContextHolder.getContext());
    }

    @Test
    void testGetRequiredContext() {
        ToolContext context = ToolContext.builder()
            .userId("user-002")
            .build();

        RemoteToolContextHolder.setContext(context);

        assertEquals(context, RemoteToolContextHolder.getRequiredContext());
    }

    @Test
    void testGetRequiredContextThrowsWhenNotSet() {
        assertThrows(IllegalStateException.class, RemoteToolContextHolder::getRequiredContext);
    }

    @Test
    void testClear() {
        ToolContext context = ToolContext.builder()
            .userId("user-003")
            .build();

        RemoteToolContextHolder.setContext(context);
        assertTrue(RemoteToolContextHolder.hasContext());

        RemoteToolContextHolder.clear();
        assertFalse(RemoteToolContextHolder.hasContext());
        assertNull(RemoteToolContextHolder.getContext());
    }

    @Test
    void testHasContext() {
        assertFalse(RemoteToolContextHolder.hasContext());

        RemoteToolContextHolder.setContext(ToolContext.of("user-004"));
        assertTrue(RemoteToolContextHolder.hasContext());

        RemoteToolContextHolder.clear();
        assertFalse(RemoteToolContextHolder.hasContext());
    }

    @Test
    void testGetContextReturnsNullWhenNotSet() {
        assertNull(RemoteToolContextHolder.getContext());
    }
}
