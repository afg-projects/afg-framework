package io.github.afgprojects.framework.core.web.version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import jakarta.servlet.http.HttpServletRequest;

/**
 * ApiVersionResolver 测试
 */
@DisplayName("ApiVersionResolver 测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApiVersionResolverTest {

    @Mock
    private HttpServletRequest request;

    private ApiVersionProperties properties;
    private ApiVersionResolver resolver;

    @BeforeEach
    void setUp() {
        properties = new ApiVersionProperties();
        resolver = new ApiVersionResolver(properties);
    }

    @Nested
    @DisplayName("HEADER 策略测试")
    class HeaderStrategyTests {

        @Test
        @DisplayName("应该从请求头解析版本")
        void shouldResolveFromHeader() {
            // given
            when(request.getHeader("X-API-Version")).thenReturn("2.0");

            // when
            ApiVersionResolver.ResolvedVersion result = resolver.resolve(request);

            // then
            assertThat(result.getVersion()).isEqualTo("2.0");
            assertThat(result.getMajor()).isEqualTo(2);
            assertThat(result.getMinor()).isEqualTo(0);
            assertThat(result.source()).contains("header");
        }

        @Test
        @DisplayName("应该处理带空格的版本值")
        void shouldHandleVersionWithSpaces() {
            // given
            when(request.getHeader("X-API-Version")).thenReturn("  1.5  ");

            // when
            ApiVersionResolver.ResolvedVersion result = resolver.resolve(request);

            // then
            assertThat(result.getVersion()).isEqualTo("1.5");
        }

        @Test
        @DisplayName("空请求头应该返回 null")
        void shouldReturnNullForEmptyHeader() {
            // given
            when(request.getHeader("X-API-Version")).thenReturn("");
            when(request.getRequestURI()).thenReturn("/users");
            when(request.getParameter("version")).thenReturn(null);

            // when
            ApiVersionResolver.ResolvedVersion result = resolver.resolve(request);

            // then
            assertThat(result.getVersion()).isEqualTo(properties.getDefaultVersion());
        }
    }

    @Nested
    @DisplayName("URL 策略测试")
    class UrlStrategyTests {

        @BeforeEach
        void setUpUrl() {
            // 设置 URL 策略优先
            properties.setResolutionOrder(new String[] {"URL", "HEADER", "PARAMETER"});
            resolver = new ApiVersionResolver(properties);
        }

        @Test
        @DisplayName("应该从 URL 解析版本")
        void shouldResolveFromUrl() {
            // given
            when(request.getRequestURI()).thenReturn("/v2/users");

            // when
            ApiVersionResolver.ResolvedVersion result = resolver.resolve(request);

            // then
            assertThat(result.getVersion()).isEqualTo("2.0");
            assertThat(result.getMajor()).isEqualTo(2);
            assertThat(result.source()).contains("url");
        }

        @Test
        @DisplayName("应该解析带次版本号的 URL")
        void shouldResolveUrlWithMinorVersion() {
            // given
            when(request.getRequestURI()).thenReturn("/v1.5/users");

            // when
            ApiVersionResolver.ResolvedVersion result = resolver.resolve(request);

            // then
            assertThat(result.getVersion()).isEqualTo("1.5");
            assertThat(result.getMajor()).isEqualTo(1);
            assertThat(result.getMinor()).isEqualTo(5);
        }

        @Test
        @DisplayName("不包含版本的 URL 应该返回默认版本")
        void shouldReturnDefaultForUrlWithoutVersion() {
            // given
            when(request.getRequestURI()).thenReturn("/users");
            when(request.getHeader("X-API-Version")).thenReturn(null);
            when(request.getParameter("version")).thenReturn(null);

            // when
            ApiVersionResolver.ResolvedVersion result = resolver.resolve(request);

            // then
            assertThat(result.getVersion()).isEqualTo(properties.getDefaultVersion());
        }
    }

    @Nested
    @DisplayName("PARAMETER 策略测试")
    class ParameterStrategyTests {

        @BeforeEach
        void setUpParameter() {
            // 设置 PARAMETER 策略优先
            properties.setResolutionOrder(new String[] {"PARAMETER", "URL", "HEADER"});
            resolver = new ApiVersionResolver(properties);
        }

        @Test
        @DisplayName("应该从请求参数解析版本")
        void shouldResolveFromParameter() {
            // given
            when(request.getParameter("version")).thenReturn("3.0");

            // when
            ApiVersionResolver.ResolvedVersion result = resolver.resolve(request);

            // then
            assertThat(result.getVersion()).isEqualTo("3.0");
            assertThat(result.getMajor()).isEqualTo(3);
            assertThat(result.source()).contains("parameter");
        }

        @Test
        @DisplayName("空参数应该返回默认版本")
        void shouldReturnDefaultForEmptyParameter() {
            // given
            when(request.getParameter("version")).thenReturn("");
            when(request.getRequestURI()).thenReturn("/users");
            when(request.getHeader("X-API-Version")).thenReturn(null);

            // when
            ApiVersionResolver.ResolvedVersion result = resolver.resolve(request);

            // then
            assertThat(result.getVersion()).isEqualTo(properties.getDefaultVersion());
        }
    }

    @Nested
    @DisplayName("优先级测试")
    class PriorityTests {

        @Test
        @DisplayName("Header 应该优先于 URL")
        void headerShouldHaveHigherPriority() {
            // given
            when(request.getHeader("X-API-Version")).thenReturn("1.0");
            when(request.getRequestURI()).thenReturn("/v2/users");

            // when
            ApiVersionResolver.ResolvedVersion result = resolver.resolve(request);

            // then
            assertThat(result.getVersion()).isEqualTo("1.0");
            assertThat(result.source()).contains("header");
        }

        @Test
        @DisplayName("URL 应该优先于 Parameter（默认配置）")
        void urlShouldHaveHigherPriorityThanParameter() {
            // given
            properties.setResolutionOrder(new String[] {"URL", "PARAMETER", "HEADER"});
            resolver = new ApiVersionResolver(properties);

            when(request.getRequestURI()).thenReturn("/v2/users");
            when(request.getParameter("version")).thenReturn("3.0");
            when(request.getHeader("X-API-Version")).thenReturn(null);

            // when
            ApiVersionResolver.ResolvedVersion result = resolver.resolve(request);

            // then
            assertThat(result.getVersion()).isEqualTo("2.0");
            assertThat(result.source()).contains("url");
        }
    }

    @Nested
    @DisplayName("默认版本测试")
    class DefaultVersionTests {

        @Test
        @DisplayName("无法解析时应该返回默认版本")
        void shouldReturnDefaultWhenCannotResolve() {
            // given
            when(request.getHeader("X-API-Version")).thenReturn(null);
            when(request.getRequestURI()).thenReturn("/users");
            when(request.getParameter("version")).thenReturn(null);

            // when
            ApiVersionResolver.ResolvedVersion result = resolver.resolve(request);

            // then
            assertThat(result.getVersion()).isEqualTo("1.0.0");
            assertThat(result.source()).isEqualTo("default");
        }

        @Test
        @DisplayName("应该使用配置的默认版本")
        void shouldUseConfiguredDefaultVersion() {
            // given
            properties.setDefaultVersion("2.5.0");
            resolver = new ApiVersionResolver(properties);

            when(request.getHeader("X-API-Version")).thenReturn(null);
            when(request.getRequestURI()).thenReturn("/users");
            when(request.getParameter("version")).thenReturn(null);

            // when
            ApiVersionResolver.ResolvedVersion result = resolver.resolve(request);

            // then
            assertThat(result.getVersion()).isEqualTo("2.5.0");
        }
    }

    @Nested
    @DisplayName("自定义配置测试")
    class CustomConfigTests {

        @Test
        @DisplayName("应该使用自定义请求头名称")
        void shouldUseCustomHeaderName() {
            // given
            properties.setHeaderName("X-Custom-Version");
            resolver = new ApiVersionResolver(properties);

            when(request.getHeader("X-Custom-Version")).thenReturn("3.0");

            // when
            ApiVersionResolver.ResolvedVersion result = resolver.resolve(request);

            // then
            assertThat(result.getVersion()).isEqualTo("3.0");
        }

        @Test
        @DisplayName("应该使用自定义参数名称")
        void shouldUseCustomParameterName() {
            // given
            properties.setParameterName("apiVersion");
            properties.setResolutionOrder(new String[] {"PARAMETER", "URL", "HEADER"});
            resolver = new ApiVersionResolver(properties);

            when(request.getParameter("apiVersion")).thenReturn("4.0");

            // when
            ApiVersionResolver.ResolvedVersion result = resolver.resolve(request);

            // then
            assertThat(result.getVersion()).isEqualTo("4.0");
        }
    }

    @Nested
    @DisplayName("ResolvedVersion 测试")
    class ResolvedVersionTests {

        @Test
        @DisplayName("toString 应该包含版本和来源")
        void toStringShouldIncludeVersionAndSource() {
            // given
            when(request.getHeader("X-API-Version")).thenReturn("1.5");

            // when
            ApiVersionResolver.ResolvedVersion result = resolver.resolve(request);
            String str = result.toString();

            // then
            assertThat(str).contains("1.5").contains("from");
        }
    }
}