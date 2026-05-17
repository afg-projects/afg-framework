package io.github.afgprojects.framework.ai.core.tool;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ToolContext 测试。
 */
class ToolContextTest {

    @Test
    void testEmptyContext() {
        ToolContext context = ToolContext.empty();

        assertNull(context.getUserId());
        assertNull(context.getUserDetails());
        assertNull(context.getTenantId());
        assertFalse(context.isAuthenticated());
        assertFalse(context.isAdmin());
        assertTrue(context.getAttributes().isEmpty());
    }

    @Test
    void testOfUserId() {
        ToolContext context = ToolContext.of("user-001");

        assertEquals("user-001", context.getUserId());
        assertNull(context.getTenantId());
        assertTrue(context.isAuthenticated());
    }

    @Test
    void testOfUserIdAndTenantId() {
        ToolContext context = ToolContext.of("user-001", "tenant-001");

        assertEquals("user-001", context.getUserId());
        assertEquals("tenant-001", context.getTenantId());
        assertTrue(context.isAuthenticated());
    }

    @Test
    void testBuilder() {
        ToolContext context = ToolContext.builder()
            .userId("user-001")
            .tenantId("tenant-001")
            .organizationId("org-001")
            .sessionId("session-001")
            .requestId("request-001")
            .clientIp("192.168.1.1")
            .attribute("customKey", "customValue")
            .build();

        assertEquals("user-001", context.getUserId());
        assertEquals("tenant-001", context.getTenantId());
        assertEquals("org-001", context.getOrganizationId());
        assertEquals("session-001", context.getSessionId());
        assertEquals("request-001", context.getRequestId());
        assertEquals("192.168.1.1", context.getClientIp());
        assertEquals("customValue", context.getAttribute("customKey"));
    }

    @Test
    void testGetAttributeWithType() {
        ToolContext context = ToolContext.builder()
            .attribute("intValue", 123)
            .attribute("stringValue", "test")
            .build();

        Integer intValue = context.getAttribute("intValue");
        String stringValue = context.getAttribute("stringValue");

        assertEquals(123, intValue);
        assertEquals("test", stringValue);
    }

    @Test
    void testIsAuthenticated() {
        assertFalse(ToolContext.empty().isAuthenticated());
        assertTrue(ToolContext.of("user-001").isAuthenticated());
    }

    @Test
    void testDefaultMethods() {
        ToolContext context = ToolContext.of("user-001");

        assertNull(context.getOrganizationId());
        assertFalse(context.isAdmin());
        assertTrue(context.getRoles().isEmpty());
    }
}
