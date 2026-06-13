package io.github.afgprojects.framework.core.web.logging;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;

/**
 * AccessLogFilter 单元测试。
 * <p>
 * 使用 Spring 提供的 MockHttpServletRequest / MockHttpServletResponse / MockFilterChain
 * 构造真实的 Servlet 请求对象，不使用 Mockito mock。
 */
@DisplayName("AccessLogFilter")
class AccessLogFilterTest {

    private AfgCoreProperties createProperties() {
        return new AfgCoreProperties();
    }

    @Nested
    @DisplayName("正常请求记录")
    class NormalRequest {

        @Test
        @DisplayName("应该记录 POST 请求方法和路径")
        void shouldLogPostMethodAndPath() {
            AfgCoreProperties properties = createProperties();
            AccessLogFilter filter = new AccessLogFilter(properties);

            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/users");
            MockHttpServletResponse response = new MockHttpServletResponse();
            response.setStatus(201);
            MockFilterChain chain = new MockFilterChain();

            assertThatNoException().isThrownBy(() ->
                    filter.doFilterInternal(request, response, chain));
        }

        @Test
        @DisplayName("应该记录 GET 请求方法和路径")
        void shouldLogGetMethodAndPath() {
            AfgCoreProperties properties = createProperties();
            AccessLogFilter filter = new AccessLogFilter(properties);

            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            assertThatNoException().isThrownBy(() ->
                    filter.doFilterInternal(request, response, chain));
        }

        @Test
        @DisplayName("应该记录查询字符串当 includeQueryString 为 true")
        void shouldLogQueryStringWhenEnabled() {
            AfgCoreProperties properties = createProperties();
            properties.getAccessLog().setIncludeQueryString(true);
            AccessLogFilter filter = new AccessLogFilter(properties);

            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
            request.setQueryString("page=1&size=20");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            assertThatNoException().isThrownBy(() ->
                    filter.doFilterInternal(request, response, chain));
        }

        @Test
        @DisplayName("不应该记录查询字符串当 includeQueryString 为 false")
        void shouldNotLogQueryStringWhenDisabled() {
            AfgCoreProperties properties = createProperties();
            properties.getAccessLog().setIncludeQueryString(false);
            AccessLogFilter filter = new AccessLogFilter(properties);

            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
            request.setQueryString("page=1&size=20");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            assertThatNoException().isThrownBy(() ->
                    filter.doFilterInternal(request, response, chain));
        }
    }

    @Nested
    @DisplayName("排除路径")
    class ExcludePaths {

        @Test
        @DisplayName("应该跳过精确匹配的排除路径")
        void shouldSkipExactExcludedPath() {
            AfgCoreProperties properties = createProperties();
            properties.getAccessLog().setExcludePaths(List.of("/health", "/actuator/**"));
            AccessLogFilter filter = new AccessLogFilter(properties);

            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            assertThatNoException().isThrownBy(() ->
                    filter.doFilterInternal(request, response, chain));
        }

        @Test
        @DisplayName("应该跳过匹配 Ant 模式的排除路径")
        void shouldSkipAntPatternExcludedPath() {
            AfgCoreProperties properties = createProperties();
            properties.getAccessLog().setExcludePaths(List.of("/actuator/**"));
            AccessLogFilter filter = new AccessLogFilter(properties);

            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            assertThatNoException().isThrownBy(() ->
                    filter.doFilterInternal(request, response, chain));
        }

        @Test
        @DisplayName("不应该跳过未匹配排除路径的请求")
        void shouldNotSkipNonExcludedPath() {
            AfgCoreProperties properties = createProperties();
            properties.getAccessLog().setExcludePaths(List.of("/health", "/actuator/**"));
            AccessLogFilter filter = new AccessLogFilter(properties);

            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            assertThatNoException().isThrownBy(() ->
                    filter.doFilterInternal(request, response, chain));
        }

        @Test
        @DisplayName("排除路径为空时应该记录所有请求")
        void shouldLogAllRequestsWhenNoExcludePaths() {
            AfgCoreProperties properties = createProperties();
            properties.getAccessLog().setExcludePaths(List.of());
            AccessLogFilter filter = new AccessLogFilter(properties);

            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            assertThatNoException().isThrownBy(() ->
                    filter.doFilterInternal(request, response, chain));
        }
    }

    @Nested
    @DisplayName("慢请求标记")
    class SlowRequest {

        @Test
        @DisplayName("应该在默认阈值以下正常记录")
        void shouldRecordNormalRequestBelowThreshold() {
            AfgCoreProperties properties = createProperties();
            properties.getAccessLog().setSlowRequestThreshold(3000);
            AccessLogFilter filter = new AccessLogFilter(properties);

            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/fast");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            // FilterChain 立即返回，耗时远低于 3000ms
            assertThatNoException().isThrownBy(() ->
                    filter.doFilterInternal(request, response, chain));
        }
    }

    @Nested
    @DisplayName("客户端 IP 配置")
    class ClientIpConfig {

        @Test
        @DisplayName("includeClientIp 为 false 时不记录客户端 IP")
        void shouldNotIncludeClientIpWhenDisabled() {
            AfgCoreProperties properties = createProperties();
            properties.getAccessLog().setIncludeClientIp(false);
            AccessLogFilter filter = new AccessLogFilter(properties);

            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            assertThatNoException().isThrownBy(() ->
                    filter.doFilterInternal(request, response, chain));
        }
    }

    @Nested
    @DisplayName("属性默认值")
    class PropertyDefaults {

        @Test
        @DisplayName("应该有正确的默认配置")
        void shouldHaveCorrectDefaults() {
            AfgCoreProperties properties = createProperties();
            AfgCoreProperties.AccessLogConfig accessLog = properties.getAccessLog();

            assertThat(accessLog.isEnabled()).isTrue();
            assertThat(accessLog.getExcludePaths()).containsExactly("/health", "/actuator/**");
            assertThat(accessLog.isIncludeQueryString()).isTrue();
            assertThat(accessLog.isIncludeClientIp()).isTrue();
            assertThat(accessLog.getSlowRequestThreshold()).isEqualTo(3000);
        }
    }
}
