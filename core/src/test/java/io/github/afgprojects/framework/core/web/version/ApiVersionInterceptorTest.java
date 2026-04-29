package io.github.afgprojects.framework.core.web.version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.method.HandlerMethod;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * ApiVersionInterceptor 测试
 */
@DisplayName("ApiVersionInterceptor 测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApiVersionInterceptorTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HandlerMethod handlerMethod;

    private ApiVersionProperties properties;
    private ApiVersionResolver resolver;
    private ApiVersionInterceptor interceptor;

    @BeforeEach
    void setUp() {
        properties = new ApiVersionProperties();
        resolver = new ApiVersionResolver(properties);
        interceptor = new ApiVersionInterceptor(resolver, properties);
    }

    @Nested
    @DisplayName("preHandle 测试")
    class PreHandleTests {

        @Test
        @DisplayName("非 HandlerMethod 应该直接通过")
        void shouldPassForNonHandlerMethod() throws Exception {
            // when
            boolean result = interceptor.preHandle(request, response, new Object());

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("无 @ApiVersion 注解应该通过")
        void shouldPassWithoutApiVersionAnnotation() throws Exception {
            // given
            when(request.getHeader("X-API-Version")).thenReturn(null);
            when(request.getRequestURI()).thenReturn("/users");
            when(request.getParameter("version")).thenReturn(null);
            when(handlerMethod.getBeanType()).thenReturn((Class) Object.class);
            when(handlerMethod.getMethodAnnotation(ApiVersion.class)).thenReturn(null);

            // when
            boolean result = interceptor.preHandle(request, response, handlerMethod);

            // then
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("版本兼容性测试")
    class VersionCompatibilityTests {

        @Test
        @DisplayName("主版本号相同应该通过")
        void shouldPassWithSameMajorVersion() throws Exception {
            // given
            ApiVersion annotation = createApiVersion("1.0", false, "", "", "", "");
            when(handlerMethod.getMethodAnnotation(ApiVersion.class)).thenReturn(annotation);
            when(handlerMethod.getBeanType()).thenReturn((Class) Object.class);
            when(request.getHeader("X-API-Version")).thenReturn("1.5");
            when(request.getAttribute(ApiVersionInterceptor.VERSION_ATTRIBUTE)).thenReturn(null);

            // when
            boolean result = interceptor.preHandle(request, response, handlerMethod);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("主版本号不同应该拒绝")
        void shouldRejectWithDifferentMajorVersion() throws Exception {
            // given
            ApiVersion annotation = createApiVersion("1.0", false, "", "", "", "");
            when(handlerMethod.getMethodAnnotation(ApiVersion.class)).thenReturn(annotation);
            when(handlerMethod.getBeanType()).thenReturn((Class) Object.class);
            when(request.getHeader("X-API-Version")).thenReturn("2.0");
            when(request.getAttribute(ApiVersionInterceptor.VERSION_ATTRIBUTE)).thenReturn(null);

            // when
            boolean result = interceptor.preHandle(request, response, handlerMethod);

            // then
            assertThat(result).isFalse();
            verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("废弃版本处理测试")
    class DeprecationTests {

        @Test
        @DisplayName("废弃版本应该添加警告头")
        void shouldAddWarningHeaderForDeprecatedVersion() throws Exception {
            // given
            ApiVersion annotation = createApiVersion("1.0", true, "", "2.0", "use v2", "");
            when(handlerMethod.getMethodAnnotation(ApiVersion.class)).thenReturn(annotation);
            when(handlerMethod.getBeanType()).thenReturn((Class) Object.class);
            when(request.getHeader("X-API-Version")).thenReturn("1.0");
            when(request.getAttribute(ApiVersionInterceptor.VERSION_ATTRIBUTE)).thenReturn(null);

            // when
            interceptor.preHandle(request, response, handlerMethod);

            // then
            ArgumentCaptor<String> headerNameCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> headerValueCaptor = ArgumentCaptor.forClass(String.class);
            verify(response, org.mockito.Mockito.atLeastOnce())
                    .setHeader(headerNameCaptor.capture(), headerValueCaptor.capture());
            assertThat(headerNameCaptor.getAllValues()).contains("X-API-Deprecated");
            assertThat(headerValueCaptor.getAllValues().stream()
                    .anyMatch(v -> v.contains("deprecated"))).isTrue();
        }

        @Test
        @DisplayName("禁用废弃检查不应该添加警告头")
        void shouldNotAddWarningWhenDeprecationDisabled() throws Exception {
            // given
            properties.getDeprecation().setEnabled(false);
            interceptor = new ApiVersionInterceptor(resolver, properties);

            ApiVersion annotation = createApiVersion("1.0", true, "", "", "", "");
            when(handlerMethod.getMethodAnnotation(ApiVersion.class)).thenReturn(annotation);
            when(handlerMethod.getBeanType()).thenReturn((Class) Object.class);
            when(request.getHeader("X-API-Version")).thenReturn("1.0");
            when(request.getAttribute(ApiVersionInterceptor.VERSION_ATTRIBUTE)).thenReturn(null);

            // when
            interceptor.preHandle(request, response, handlerMethod);

            // then
            verify(response).setHeader("X-API-Version", "1.0");
        }
    }

    @Nested
    @DisplayName("响应头设置测试")
    class ResponseHeaderTests {

        @Test
        @DisplayName("应该设置 API 版本响应头")
        void shouldSetApiVersionHeader() throws Exception {
            // given
            ApiVersion annotation = createApiVersion("2.5", false, "", "", "", "");
            when(handlerMethod.getMethodAnnotation(ApiVersion.class)).thenReturn(annotation);
            when(handlerMethod.getBeanType()).thenReturn((Class) Object.class);
            when(request.getHeader("X-API-Version")).thenReturn("2.0");
            when(request.getAttribute(ApiVersionInterceptor.VERSION_ATTRIBUTE)).thenReturn(null);

            // when
            interceptor.preHandle(request, response, handlerMethod);

            // then
            verify(response).setHeader("X-API-Version", "2.5");
        }
    }

    @Nested
    @DisplayName("静态方法测试")
    class StaticMethodTests {

        @Test
        @DisplayName("getResolvedVersion 应该从请求属性获取版本")
        void shouldGetResolvedVersionFromRequest() {
            // given
            ApiVersionResolver.ResolvedVersion version =
                    new ApiVersionResolver.ResolvedVersion(ApiVersionInfo.of("1.0"), "1.0", "test");
            when(request.getAttribute(ApiVersionInterceptor.VERSION_ATTRIBUTE)).thenReturn(version);

            // when
            Object result = ApiVersionInterceptor.getResolvedVersion(request);

            // then
            assertThat(result).isInstanceOf(ApiVersionResolver.ResolvedVersion.class);
            assertThat(((ApiVersionResolver.ResolvedVersion) result).getVersion()).isEqualTo("1.0");
        }

        @Test
        @DisplayName("getApiVersionInfo 应该从请求属性获取版本信息")
        void shouldGetApiVersionInfoFromRequest() {
            // given
            ApiVersionInfo info = ApiVersionInfo.of("2.0");
            when(request.getAttribute(ApiVersionInterceptor.API_VERSION_ATTRIBUTE)).thenReturn(info);

            // when
            ApiVersionInfo result = ApiVersionInterceptor.getApiVersionInfo(request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.value()).isEqualTo("2.0");
        }
    }

    /**
     * 创建 ApiVersion 注解实例
     */
    private ApiVersion createApiVersion(
            String value, boolean deprecated, String since, String until, String replacement, String reason) {
        return new ApiVersion() {
            @Override
            public String value() {
                return value;
            }

            @Override
            public boolean deprecated() {
                return deprecated;
            }

            @Override
            public String since() {
                return since;
            }

            @Override
            public String until() {
                return until;
            }

            @Override
            public String replacement() {
                return replacement;
            }

            @Override
            public String reason() {
                return reason;
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return ApiVersion.class;
            }
        };
    }
}