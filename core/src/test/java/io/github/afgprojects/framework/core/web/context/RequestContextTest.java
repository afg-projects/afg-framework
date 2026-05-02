package io.github.afgprojects.framework.core.web.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * RequestContext 测试
 */
@DisplayName("RequestContext 测试")
class RequestContextTest {

    @Nested
    @DisplayName("Builder 测试")
    class BuilderTests {

        @Test
        @DisplayName("应该使用 Builder 构建上下文")
        void shouldBuildWithContext() {
            LocalDateTime now = LocalDateTime.now();
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("key", "value");

            RequestContext context = RequestContext.builder()
                    .traceId("trace-123")
                    .requestId("req-456")
                    .userId(1L)
                    .username("testuser")
                    .tenantId(100L)
                    .clientIp("127.0.0.1")
                    .source("web")
                    .requestTime(now)
                    .requestPath("/api/test")
                    .requestMethod("GET")
                    .attributes(attrs)
                    .build();

            assertThat(context.getTraceId()).isEqualTo("trace-123");
            assertThat(context.getRequestId()).isEqualTo("req-456");
            assertThat(context.getUserId()).isEqualTo(1L);
            assertThat(context.getUsername()).isEqualTo("testuser");
            assertThat(context.getTenantId()).isEqualTo(100L);
            assertThat(context.getClientIp()).isEqualTo("127.0.0.1");
            assertThat(context.getSource()).isEqualTo("web");
            assertThat(context.getRequestTime()).isEqualTo(now);
            assertThat(context.getRequestPath()).isEqualTo("/api/test");
            assertThat(context.getRequestMethod()).isEqualTo("GET");
            assertThat(context.getAttributes()).containsEntry("key", "value");
        }
    }

    @Nested
    @DisplayName("属性操作测试")
    class AttributeTests {

        @Test
        @DisplayName("应该正确获取和设置属性")
        void shouldGetAndSetAttribute() {
            RequestContext context = new RequestContext();

            context.setAttribute("key1", "value1");
            context.setAttribute("key2", 123);

            assertThat(context.getAttribute("key1")).isEqualTo("value1");
            assertThat(context.getAttribute("key2")).isEqualTo(123);
        }

        @Test
        @DisplayName("getAttributes 应该返回非空 Map")
        void shouldReturnNonNullAttributes() {
            RequestContext context = new RequestContext();

            assertThat(context.getAttributes()).isNotNull();
            assertThat(context.getAttributes()).isEmpty();
        }

        @Test
        @DisplayName("setAttributes 应该处理 null")
        void shouldHandleNullAttributes() {
            RequestContext context = new RequestContext();

            context.setAttributes(null);

            assertThat(context.getAttributes()).isNotNull();
            assertThat(context.getAttributes()).isEmpty();
        }

        @Test
        @DisplayName("setAttributes 应该正确设置属性")
        void shouldSetAttributes() {
            RequestContext context = new RequestContext();
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("key", "value");

            context.setAttributes(attrs);

            assertThat(context.getAttributes()).containsEntry("key", "value");
        }
    }

    @Nested
    @DisplayName("构造方法测试")
    class ConstructorTests {

        @Test
        @DisplayName("无参构造方法应该创建空上下文")
        void shouldCreateEmptyContext() {
            RequestContext context = new RequestContext();

            assertThat(context.getTraceId()).isNull();
            assertThat(context.getRequestId()).isNull();
            assertThat(context.getUserId()).isNull();
            assertThat(context.getAttributes()).isNotNull();
        }

        @Test
        @DisplayName("全参构造方法应该正确设置所有字段")
        void shouldCreateFullContext() {
            LocalDateTime now = LocalDateTime.now();
            Map<String, Object> attrs = new HashMap<>();

            RequestContext context = new RequestContext(
                    "trace-1", "req-1", 1L, "user", 100L,
                    "127.0.0.1", "web", now, "/api/test", "GET", attrs
            );

            assertThat(context.getTraceId()).isEqualTo("trace-1");
            assertThat(context.getRequestId()).isEqualTo("req-1");
            assertThat(context.getUserId()).isEqualTo(1L);
            assertThat(context.getUsername()).isEqualTo("user");
            assertThat(context.getTenantId()).isEqualTo(100L);
            assertThat(context.getClientIp()).isEqualTo("127.0.0.1");
            assertThat(context.getSource()).isEqualTo("web");
            assertThat(context.getRequestTime()).isEqualTo(now);
            assertThat(context.getRequestPath()).isEqualTo("/api/test");
            assertThat(context.getRequestMethod()).isEqualTo("GET");
            assertThat(context.getAttributes()).isSameAs(attrs);
        }
    }
}
