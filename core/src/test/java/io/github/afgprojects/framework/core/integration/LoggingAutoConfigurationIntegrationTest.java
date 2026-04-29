package io.github.afgprojects.framework.core.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import io.github.afgprojects.framework.core.autoconfigure.LoggingAutoConfiguration;
import io.github.afgprojects.framework.core.support.BaseIntegrationTest;
import io.github.afgprojects.framework.core.web.context.AfgRequestContextHolder;
import io.github.afgprojects.framework.core.web.context.RequestContext;
import io.github.afgprojects.framework.core.web.logging.LoggingProperties;
import io.github.afgprojects.framework.core.web.logging.MdcFilter;

/**
 * LoggingAutoConfiguration 集成测试
 * 验证日志自动配置是否正确注入所有 Bean
 */
class LoggingAutoConfigurationIntegrationTest extends BaseIntegrationTest {

    @BeforeEach
    void setUp() {
        MDC.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("应该成功注入 MdcFilter Bean")
    void shouldInjectMdcFilter() {
        // when
        MdcFilter filter = getBean(MdcFilter.class);

        // then
        assertThat(filter).isNotNull();
    }

    @Test
    @DisplayName("应该成功注入 LoggingProperties Bean")
    void shouldInjectLoggingProperties() {
        // when
        LoggingProperties properties = getBean(LoggingProperties.class);

        // then
        assertThat(properties).isNotNull();
        assertThat(properties.getMdc()).isNotNull();
        assertThat(properties.getMdc().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("应该成功注入 LoggingAutoConfiguration Bean")
    void shouldInjectLoggingAutoConfiguration() {
        // when
        LoggingAutoConfiguration autoConfig = getBean(LoggingAutoConfiguration.class);

        // then
        assertThat(autoConfig).isNotNull();
    }

    @Test
    @DisplayName("MdcFilter 应该从 RequestContext 填充 MDC")
    void shouldPopulateMdcFromRequestContext() throws Exception {
        // given
        MdcFilter filter = getBean(MdcFilter.class);

        // 设置 Spring RequestContextHolder
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        RequestAttributes requestAttrs = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttrs);

        // 创建 RequestContext
        RequestContext context = RequestContext.builder()
                .traceId("test-trace-id")
                .requestId("test-request-id")
                .tenantId(100L)
                .userId(200L)
                .username("testuser")
                .clientIp("192.168.1.1")
                .requestPath("/api/test")
                .requestMethod("GET")
                .build();
        AfgRequestContextHolder.setContext(context);

        // 用于捕获 filter chain 执行时的 MDC 值
        Map<String, String> capturedMdc = new HashMap<>();
        FilterChain capturingFilterChain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response) {
                // 在 filter chain 执行期间捕获 MDC 值
                capturedMdc.put("tenantId", MDC.get("tenantId"));
                capturedMdc.put("userId", MDC.get("userId"));
                capturedMdc.put("requestPath", MDC.get("requestPath"));
            }
        };

        // when
        filter.doFilter(request, response, capturingFilterChain);

        // then - 默认启用的字段是 tenantId, userId, requestPath
        assertThat(capturedMdc.get("tenantId")).isEqualTo("100");
        assertThat(capturedMdc.get("userId")).isEqualTo("200");
        assertThat(capturedMdc.get("requestPath")).isEqualTo("/api/test");

        // cleanup
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("MdcFilter 应该在请求结束后清理 MDC")
    void shouldClearMdcAfterRequest() throws Exception {
        // given
        MdcFilter filter = getBean(MdcFilter.class);

        // 设置 Spring RequestContextHolder
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        RequestAttributes requestAttrs = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttrs);

        RequestContext context =
                RequestContext.builder().tenantId(100L).requestPath("/api/test").build();
        AfgRequestContextHolder.setContext(context);

        // when
        filter.doFilter(request, response, new MockFilterChain());

        // then - 在 finally 块之后 MDC 应该被清理
        assertThat(MDC.get("tenantId")).isNull();
        assertThat(MDC.get("userId")).isNull();
        assertThat(MDC.get("requestPath")).isNull();

        // cleanup
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("MdcFilter 应该处理 null RequestContext")
    void shouldHandleNullRequestContext() throws Exception {
        // given
        MdcFilter filter = getBean(MdcFilter.class);
        RequestContextHolder.resetRequestAttributes(); // 确保 RequestAttributes 为 null

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when & then - 不应该抛出异常
        filter.doFilter(request, response, new MockFilterChain());
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("LoggingProperties 默认配置应该正确")
    void shouldHaveCorrectDefaultConfiguration() {
        // when
        LoggingProperties properties = getBean(LoggingProperties.class);

        // then
        assertThat(properties.getMdc().isEnabled()).isTrue();
        assertThat(properties.getMdc().getFields()).containsExactly("traceId", "tenantId", "userId", "requestPath");
        assertThat(properties.getStructured().isEnabled()).isFalse();
        assertThat(properties.getStructured().isPrettyPrint()).isFalse();
    }

    @Test
    @DisplayName("所有日志相关 Bean 应该在 ApplicationContext 中可用")
    void allLoggingBeansShouldBeAvailable() {
        // when & then
        assertThat(applicationContext.containsBean("mdcFilter")).isTrue();
        // LoggingProperties bean name is auto-generated by Spring Boot
        String[] beanNames = applicationContext.getBeanNamesForType(LoggingProperties.class);
        assertThat(beanNames).isNotEmpty();
    }
}
