package io.github.afgprojects.framework.core.web.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class RequestContextTest {

    @Test
    void should_createContext_when_builderUsed() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        // When
        RequestContext context = RequestContext.builder()
                .traceId("trace-123")
                .requestId("request-456")
                .userId(1L)
                .username("testuser")
                .tenantId(100L)
                .clientIp("192.168.1.1")
                .source("web")
                .requestTime(now)
                .requestPath("/api/users")
                .requestMethod("GET")
                .build();

        // Then
        assertThat(context.getTraceId()).isEqualTo("trace-123");
        assertThat(context.getRequestId()).isEqualTo("request-456");
        assertThat(context.getUserId()).isEqualTo(1L);
        assertThat(context.getUsername()).isEqualTo("testuser");
        assertThat(context.getTenantId()).isEqualTo(100L);
        assertThat(context.getClientIp()).isEqualTo("192.168.1.1");
        assertThat(context.getSource()).isEqualTo("web");
        assertThat(context.getRequestTime()).isEqualTo(now);
        assertThat(context.getRequestPath()).isEqualTo("/api/users");
        assertThat(context.getRequestMethod()).isEqualTo("GET");
    }

    @Test
    void should_setAndGetAttribute_when_attributeMethodsCalled() {
        // Given
        RequestContext context = new RequestContext();
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("key1", "value1");

        // When
        context.setAttributes(attrs);
        context.setAttribute("key2", "value2");

        // Then
        assertThat(context.getAttribute("key1")).isEqualTo("value1");
        assertThat(context.getAttribute("key2")).isEqualTo("value2");
        assertThat(context.getAttribute("nonexistent")).isNull();
    }
}
