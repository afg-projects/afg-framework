package io.github.afgprojects.framework.ai.core.tool;

import io.github.afgprojects.framework.ai.core.tool.remote.ToolContextHeaders;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ToolContextHeaders} 测试类。
 */
class ToolContextHeadersTest {

    @Test
    void testPropagateAuthHeaders() {
        // 创建原始请求头
        Map<String, String> originalHeaders = Map.of(
            "Authorization", "Bearer token-123",
            "Cookie", "SESSION=abc123",
            "X-Custom-Header", "custom-value"
        );

        // 透传认证请求头
        Map<String, String> authHeaders = ToolContextHeaders.propagateAuthHeaders(originalHeaders);

        // 验证：只透传认证相关请求头
        assertEquals("Bearer token-123", authHeaders.get("Authorization"));
        assertEquals("SESSION=abc123", authHeaders.get("Cookie"));
        assertNull(authHeaders.get("X-Custom-Header")); // 非认证请求头不透传
    }

    @Test
    void testWriteToHeaders() {
        // 创建上下文（仅包含辅助信息）
        ToolContext context = ToolContext.builder()
            .requestId("request-001")
            .sessionId("session-001")
            .clientIp("192.168.1.1")
            .build();

        // 写入请求头
        Map<String, String> headers = ToolContextHeaders.writeToHeaders(context);

        // 验证：只包含辅助信息
        assertEquals("request-001", headers.get(ToolContextHeaders.REQUEST_ID));
        assertEquals("session-001", headers.get(ToolContextHeaders.SESSION_ID));
        assertEquals("192.168.1.1", headers.get(ToolContextHeaders.CLIENT_IP));
        // 不包含用户身份信息
        assertNull(headers.get("X-AFG-User-Id"));
        assertNull(headers.get("X-AFG-Tenant-Id"));
    }

    @Test
    void testHasAuthHeaders() {
        // 有认证请求头
        Map<String, String> withAuth = Map.of("Authorization", "Bearer token");
        assertTrue(ToolContextHeaders.hasAuthHeaders(withAuth));

        // 无认证请求头
        Map<String, String> withoutAuth = Map.of("X-Custom", "value");
        assertFalse(ToolContextHeaders.hasAuthHeaders(withoutAuth));
    }

    @Test
    void testGetAuthType() {
        assertEquals("bearer", ToolContextHeaders.getAuthType(
            Map.of("Authorization", "Bearer token")));

        assertEquals("basic", ToolContextHeaders.getAuthType(
            Map.of("Authorization", "Basic credentials")));

        assertEquals("api-key", ToolContextHeaders.getAuthType(
            Map.of("X-Api-Key", "key123")));

        assertEquals("auth-token", ToolContextHeaders.getAuthType(
            Map.of("X-Auth-Token", "token123")));

        assertEquals("cookie", ToolContextHeaders.getAuthType(
            Map.of("Cookie", "SESSION=abc")));

        assertEquals("none", ToolContextHeaders.getAuthType(Map.of()));
    }

    @Test
    void testExtractAuxiliaryHeaders() {
        Map<String, String> headers = Map.of(
            ToolContextHeaders.REQUEST_ID, "req-001",
            ToolContextHeaders.SESSION_ID, "sess-001",
            ToolContextHeaders.CLIENT_IP, "10.0.0.1",
            "Authorization", "Bearer token"
        );

        Map<String, String> auxiliary = ToolContextHeaders.extractAuxiliaryHeaders(headers);

        assertEquals("req-001", auxiliary.get("requestId"));
        assertEquals("sess-001", auxiliary.get("sessionId"));
        assertEquals("10.0.0.1", auxiliary.get("clientIp"));
        assertNull(auxiliary.get("Authorization")); // 认证请求头不包含
    }

    @Test
    void testToolContextWithOriginalHeaders() {
        // 创建带原始请求头的上下文
        Map<String, String> originalHeaders = Map.of(
            "Authorization", "Bearer token-456",
            "X-Api-Key", "key-789"
        );

        ToolContext context = ToolContext.builder()
            .requestId("request-002")
            .originalHeaders(originalHeaders)
            .build();

        // 验证原始请求头被保存
        assertEquals(originalHeaders, context.getOriginalHeaders());
        assertEquals("Bearer token-456", context.getOriginalHeaders().get("Authorization"));
    }
}
