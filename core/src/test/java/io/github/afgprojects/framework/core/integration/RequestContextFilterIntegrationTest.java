package io.github.afgprojects.framework.core.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import io.github.afgprojects.framework.core.support.BaseIntegrationTest;
import io.github.afgprojects.framework.core.web.context.RequestContextFilter;

/**
 * RequestContextFilter 集成测试
 * 验证请求上下文过滤器是否正确工作
 */
class RequestContextFilterIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("应该成功注入 RequestContextFilter Bean")
    void shouldInjectRequestContextFilter() {
        // when
        RequestContextFilter filter = getBean(RequestContextFilter.class);

        // then
        assertThat(filter).isNotNull();
    }

    @Test
    @DisplayName("过滤器应该从请求头获取 traceId")
    void shouldGetTraceIdFromHeader() throws Exception {
        // given
        RequestContextFilter filter = getBean(RequestContextFilter.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Trace-Id", "test-trace-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        // when
        filter.doFilter(request, response, filterChain);

        // then
        assertThat(response.getHeader("X-Trace-Id")).isEqualTo("test-trace-123");
    }

    @Test
    @DisplayName("过滤器应该从请求参数获取 traceId")
    void shouldGetTraceIdFromParameter() throws Exception {
        // given
        RequestContextFilter filter = getBean(RequestContextFilter.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("traceId", "param-trace-456");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        // when
        filter.doFilter(request, response, filterChain);

        // then
        assertThat(response.getHeader("X-Trace-Id")).isEqualTo("param-trace-456");
    }

    @Test
    @DisplayName("过滤器应该在请求头和参数都没有 traceId 时生成新的")
    void shouldGenerateTraceIdWhenMissing() throws Exception {
        // given
        RequestContextFilter filter = getBean(RequestContextFilter.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        // when
        filter.doFilter(request, response, filterChain);

        // then
        String traceId = response.getHeader("X-Trace-Id");
        assertThat(traceId).isNotNull();
        assertThat(traceId).isNotEmpty();
    }

    @Test
    @DisplayName("请求头优先级应该高于请求参数")
    void headerShouldHaveHigherPriorityThanParameter() throws Exception {
        // given
        RequestContextFilter filter = getBean(RequestContextFilter.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Trace-Id", "header-trace");
        request.setParameter("traceId", "param-trace");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        // when
        filter.doFilter(request, response, filterChain);

        // then
        assertThat(response.getHeader("X-Trace-Id")).isEqualTo("header-trace");
    }
}
