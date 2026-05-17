package io.github.afgprojects.framework.ai.core.tool;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ToolPermissionChecker 测试。
 */
class ToolPermissionCheckerTest {

    @Test
    void testPermissionResultAllowed() {
        ToolPermissionChecker.PermissionResult result = ToolPermissionChecker.PermissionResult.allowed();

        assertTrue(result.isAllowed());
        assertFalse(result.isDenied());
        assertNull(result.getDenyReason());
        assertNull(result.getRequiredPermission());
    }

    @Test
    void testPermissionResultDenied() {
        ToolPermissionChecker.PermissionResult result = ToolPermissionChecker.PermissionResult.denied("Access denied");

        assertFalse(result.isAllowed());
        assertTrue(result.isDenied());
        assertEquals("Access denied", result.getDenyReason());
    }

    @Test
    void testPermissionResultDeniedWithPermission() {
        ToolPermissionChecker.PermissionResult result = ToolPermissionChecker.PermissionResult.denied(
            "Permission required",
            "user:read"
        );

        assertFalse(result.isAllowed());
        assertEquals("Permission required", result.getDenyReason());
        assertEquals("user:read", result.getRequiredPermission());
    }

    @Test
    void testDefaultPermissionResult() {
        DefaultPermissionResult allowed = DefaultPermissionResult.ALLOWED;
        assertTrue(allowed.isAllowed());

        DefaultPermissionResult denied = new DefaultPermissionResult(false, "Test reason", "test:permission");
        assertFalse(denied.isAllowed());
        assertEquals("Test reason", denied.getDenyReason());
        assertEquals("test:permission", denied.getRequiredPermission());
    }
}
