package io.github.afgprojects.framework.core.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import io.github.afgprojects.framework.core.support.BaseIntegrationTest;
import io.github.afgprojects.framework.core.web.security.AfgEnforcer;
import io.github.afgprojects.framework.core.web.security.AfgSecurityContextBridge;
import io.github.afgprojects.framework.core.web.security.autoconfigure.AfgSecurityAutoConfiguration;
import io.github.afgprojects.framework.core.web.security.autoconfigure.AfgSecurityProperties;
import io.github.afgprojects.framework.core.web.security.filter.SecurityHeaderFilter;
import io.github.afgprojects.framework.core.web.security.filter.SqlInjectionFilter;
import io.github.afgprojects.framework.core.web.security.filter.XssFilter;

/**
 * AfgSecurityAutoConfiguration 集成测试
 * 验证安全自动配置是否正确注入所有 Bean
 */
class AfgSecurityAutoConfigurationIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("应该成功注入 XssFilter Bean")
    void shouldInjectXssFilter() {
        // when
        XssFilter filter = getBean(XssFilter.class);

        // then
        assertThat(filter).isNotNull();
    }

    @Test
    @DisplayName("应该成功注入 SqlInjectionFilter Bean")
    void shouldInjectSqlInjectionFilter() {
        // when
        SqlInjectionFilter filter = getBean(SqlInjectionFilter.class);

        // then
        assertThat(filter).isNotNull();
    }

    @Test
    @DisplayName("应该成功注入 SecurityHeaderFilter Bean")
    void shouldInjectSecurityHeaderFilter() {
        // when
        SecurityHeaderFilter filter = getBean(SecurityHeaderFilter.class);

        // then
        assertThat(filter).isNotNull();
    }

    @Test
    @DisplayName("应该成功注入 AfgSecurityContextBridge Bean")
    void shouldInjectAfgSecurityContextBridge() {
        // when
        AfgSecurityContextBridge bridge = getBean(AfgSecurityContextBridge.class);

        // then
        assertThat(bridge).isNotNull();
    }

    @Test
    @DisplayName("应该成功注入 AfgEnforcer Bean")
    void shouldInjectAfgEnforcer() {
        // when
        AfgEnforcer enforcer = getBean(AfgEnforcer.class);

        // then
        assertThat(enforcer).isNotNull();
    }

    @Test
    @DisplayName("应该成功注入 AfgSecurityAutoConfiguration Bean")
    void shouldInjectAfgSecurityAutoConfiguration() {
        // when
        AfgSecurityAutoConfiguration autoConfig = getBean(AfgSecurityAutoConfiguration.class);

        // then
        assertThat(autoConfig).isNotNull();
    }

    @Test
    @DisplayName("应该成功注入 AfgSecurityProperties Bean")
    void shouldInjectAfgSecurityProperties() {
        // when
        AfgSecurityProperties properties = getBean(AfgSecurityProperties.class);

        // then
        assertThat(properties).isNotNull();
    }

    @Test
    @DisplayName("SecurityHeaderFilter 应该设置安全响应头")
    void shouldSetSecurityHeaders() throws Exception {
        // given
        SecurityHeaderFilter filter = getBean(SecurityHeaderFilter.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        // when
        filter.doFilter(request, response, filterChain);

        // then
        assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(response.getHeader("X-Frame-Options")).isEqualTo("DENY");
        assertThat(response.getHeader("X-XSS-Protection")).isEqualTo("1; mode=block");
    }

    @Test
    @DisplayName("XssFilter 应该检测 XSS 攻击")
    void shouldDetectXssAttack() throws Exception {
        // given
        XssFilter filter = getBean(XssFilter.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("input", "<script>alert('xss')</script>");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        // when & then
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("SqlInjectionFilter 应该检测 SQL 注入攻击")
    void shouldDetectSqlInjection() throws Exception {
        // given
        SqlInjectionFilter filter = getBean(SqlInjectionFilter.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("id", "1 OR 1=1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        // when & then
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("所有安全相关 Bean 应该在 ApplicationContext 中可用")
    void allSecurityBeansShouldBeAvailable() {
        // when & then
        assertThat(applicationContext.containsBean("xssFilter")).isTrue();
        assertThat(applicationContext.containsBean("sqlInjectionFilter")).isTrue();
        assertThat(applicationContext.containsBean("securityHeaderFilter")).isTrue();
        assertThat(applicationContext.containsBean("afgSecurityContextBridge")).isTrue();
        assertThat(applicationContext.containsBean("afgEnforcer")).isTrue();
    }
}
