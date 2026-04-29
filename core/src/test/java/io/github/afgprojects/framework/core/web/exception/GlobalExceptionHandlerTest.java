package io.github.afgprojects.framework.core.web.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import io.github.afgprojects.framework.core.model.exception.BusinessException;
import io.github.afgprojects.framework.core.model.exception.CommonErrorCode;
import io.github.afgprojects.framework.core.model.exception.ErrorCodeMessageSource;
import io.github.afgprojects.framework.core.model.result.Result;
import io.github.afgprojects.framework.core.web.context.AfgRequestContextHolder;
import io.github.afgprojects.framework.core.web.context.RequestContext;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getRequestURI()).thenReturn("/api/test");
        when(mockRequest.getMethod()).thenReturn("GET");

        // Setup Spring RequestContextHolder
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestContext context = new RequestContext();
        context.setTraceId("test-trace");
        context.setRequestId("test-request");
        context.setRequestPath("/api/test");
        AfgRequestContextHolder.setContext(context);

        // Initialize MessageSource for i18n tests
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        ErrorCodeMessageSource.setMessageSource(messageSource);
    }

    @AfterEach
    void tearDown() {
        AfgRequestContextHolder.clear();
        RequestContextHolder.resetRequestAttributes();
        LocaleContextHolder.resetLocaleContext();
    }

    @Nested
    @DisplayName("BusinessException tests")
    class BusinessExceptionTests {

        @Test
        @DisplayName("Should return business result when BusinessException")
        void should_returnBusinessResult_when_businessException() {
            BusinessException ex = new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND);

            Result<Void> result = handler.handleBusinessException(ex, mockRequest);

            assertThat(result.code()).isEqualTo(CommonErrorCode.ENTITY_NOT_FOUND.getCode());
            assertThat(result.message()).isEqualTo(CommonErrorCode.ENTITY_NOT_FOUND.getMessage());
            assertThat(result.traceId()).isEqualTo("test-trace");
        }

        @Test
        @DisplayName("Should return param error when BusinessException with custom message")
        void should_returnParamError_when_businessExceptionWithCustomCode() {
            BusinessException ex = new BusinessException(CommonErrorCode.PARAM_ERROR, "用户名不能为空");

            Result<Void> result = handler.handleBusinessException(ex, mockRequest);

            assertThat(result.code()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode());
            assertThat(result.message()).isEqualTo("用户名不能为空");
        }
    }

    @Nested
    @DisplayName("Internationalization tests")
    class InternationalizationTests {

        @Test
        @DisplayName("Should return Chinese message when Accept-Language is zh-CN")
        void shouldReturnChineseMessage_whenAcceptLanguageIsZhCN() {
            LocaleContextHolder.setLocale(Locale.CHINA);

            BusinessException ex = new BusinessException(CommonErrorCode.PARAM_ERROR);
            Result<Void> result = handler.handleBusinessException(ex, mockRequest);

            assertThat(result.message()).isEqualTo("参数错误");
        }

        @Test
        @DisplayName("Should return English message when Accept-Language is en")
        void shouldReturnEnglishMessage_whenAcceptLanguageIsEn() {
            LocaleContextHolder.setLocale(Locale.ENGLISH);

            BusinessException ex = new BusinessException(CommonErrorCode.PARAM_ERROR);
            Result<Void> result = handler.handleBusinessException(ex, mockRequest);

            assertThat(result.message()).isEqualTo("Invalid parameter");
        }

        @Test
        @DisplayName("Should return English message when Accept-Language is en-US")
        void shouldReturnEnglishMessage_whenAcceptLanguageIsEnUS() {
            LocaleContextHolder.setLocale(Locale.US);

            BusinessException ex = new BusinessException(CommonErrorCode.UNAUTHORIZED);
            Result<Void> result = handler.handleBusinessException(ex, mockRequest);

            assertThat(result.message()).isEqualTo("Unauthorized or session expired");
        }

        @Test
        @DisplayName("Should return Chinese message when Accept-Language is zh")
        void shouldReturnChineseMessage_whenAcceptLanguageIsZh() {
            LocaleContextHolder.setLocale(Locale.CHINESE);

            BusinessException ex = new BusinessException(CommonErrorCode.NOT_FOUND);
            Result<Void> result = handler.handleBusinessException(ex, mockRequest);

            assertThat(result.message()).isEqualTo("资源不存在");
        }

        @Test
        @DisplayName("Should handle multiple Accept-Language values")
        void shouldHandleMultipleAcceptLanguageValues() {
            LocaleContextHolder.setLocale(Locale.US);

            BusinessException ex = new BusinessException(CommonErrorCode.FORBIDDEN);
            Result<Void> result = handler.handleBusinessException(ex, mockRequest);

            assertThat(result.message()).isEqualTo("Access denied");
        }
    }

    @Nested
    @DisplayName("Validation exception tests")
    class ValidationExceptionTests {

        @Test
        @DisplayName("Should handle MethodArgumentNotValidException")
        void should_handleMethodArgumentNotValidException() {
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            BindException bindException = new BindException(new Object(), "target");
            bindException.addError(new FieldError("target", "name", "不能为空"));
            when(ex.getBindingResult()).thenReturn(bindException.getBindingResult());

            Result<Void> result = handler.handleMethodArgumentNotValidException(ex, mockRequest);

            assertThat(result.code()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode());
            assertThat(result.message()).contains("name");
            assertThat(result.message()).contains("不能为空");
        }

        @Test
        @DisplayName("Should handle BindException")
        void should_handleBindException() {
            BindException ex = new BindException(new Object(), "target");
            ex.addError(new FieldError("target", "email", "邮箱格式不正确"));
            ex.addError(new FieldError("target", "phone", "手机号格式不正确"));

            Result<Void> result = handler.handleBindException(ex, mockRequest);

            assertThat(result.code()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode());
            assertThat(result.message()).contains("email", "phone");
        }

        @Test
        @DisplayName("Should handle ConstraintViolationException")
        void should_handleConstraintViolationException() {
            ConstraintViolation<?> violation = mock(ConstraintViolation.class);
            when(violation.getMessage()).thenReturn("必须是正数");
            ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

            Result<Void> result = handler.handleConstraintViolationException(ex, mockRequest);

            assertThat(result.code()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode());
            assertThat(result.message()).contains("必须是正数");
        }

        @Test
        @DisplayName("Should handle MissingServletRequestParameterException")
        void should_handleMissingServletRequestParameterException() {
            MissingServletRequestParameterException ex = new MissingServletRequestParameterException("userId", "Long");

            Result<Void> result = handler.handleMissingServletRequestParameterException(ex, mockRequest);

            assertThat(result.code()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode());
            assertThat(result.message()).contains("userId");
        }

        @Test
        @DisplayName("Should handle MissingServletRequestParameterException with English locale")
        void should_handleMissingServletRequestParameterException_withEnglishLocale() {
            LocaleContextHolder.setLocale(Locale.ENGLISH);

            MissingServletRequestParameterException ex = new MissingServletRequestParameterException("userId", "Long");

            Result<Void> result = handler.handleMissingServletRequestParameterException(ex, mockRequest);

            assertThat(result.code()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode());
            assertThat(result.message()).contains("userId");
            assertThat(result.message()).contains("Missing parameter");
        }

        @Test
        @DisplayName("Should handle MethodArgumentTypeMismatchException")
        void should_handleMethodArgumentTypeMismatchException() {
            MethodArgumentTypeMismatchException ex =
                    new MethodArgumentTypeMismatchException("abc", Long.class, "id", null, new NumberFormatException());

            Result<Void> result = handler.handleMethodArgumentTypeMismatchException(ex, mockRequest);

            assertThat(result.code()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode());
            assertThat(result.message()).contains("id");
        }

        @Test
        @DisplayName("Should handle HttpMessageNotReadableException")
        void should_handleHttpMessageNotReadableException() {
            HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON parse error", null);

            Result<Void> result = handler.handleHttpMessageNotReadableException(ex, mockRequest);

            assertThat(result.code()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode());
            assertThat(result.message()).contains("参数格式错误");
        }

        @Test
        @DisplayName("Should handle HttpMessageNotReadableException with English locale")
        void should_handleHttpMessageNotReadableException_withEnglishLocale() {
            LocaleContextHolder.setLocale(Locale.ENGLISH);

            HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON parse error", null);

            Result<Void> result = handler.handleHttpMessageNotReadableException(ex, mockRequest);

            assertThat(result.code()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode());
            assertThat(result.message()).isEqualTo("Invalid parameter format");
        }
    }

    @Nested
    @DisplayName("HTTP exception tests")
    class HttpExceptionTests {

        @Test
        @DisplayName("Should handle HttpRequestMethodNotSupportedException")
        void should_handleHttpRequestMethodNotSupportedException() {
            HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("DELETE");

            Result<Void> result = handler.handleHttpRequestMethodNotSupportedException(ex, mockRequest);

            assertThat(result.code()).isEqualTo(CommonErrorCode.METHOD_NOT_ALLOWED.getCode());
            assertThat(result.message()).contains("DELETE");
        }

        @Test
        @DisplayName("Should handle HttpRequestMethodNotSupportedException with English locale")
        void should_handleHttpRequestMethodNotSupportedException_withEnglishLocale() {
            LocaleContextHolder.setLocale(Locale.ENGLISH);

            HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("DELETE");

            Result<Void> result = handler.handleHttpRequestMethodNotSupportedException(ex, mockRequest);

            assertThat(result.code()).isEqualTo(CommonErrorCode.METHOD_NOT_ALLOWED.getCode());
            assertThat(result.message()).contains("DELETE");
            assertThat(result.message()).contains("Method not allowed");
        }

        @Test
        @DisplayName("Should handle HttpMediaTypeNotSupportedException")
        void should_handleHttpMediaTypeNotSupportedException() {
            HttpMediaTypeNotSupportedException ex = new HttpMediaTypeNotSupportedException("application/xml");

            Result<Void> result = handler.handleHttpMediaTypeNotSupportedException(ex, mockRequest);

            assertThat(result.code()).isEqualTo(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE.getCode());
            assertThat(result.message()).contains("不支持的媒体类型");
        }

        @Test
        @DisplayName("Should handle HttpMediaTypeNotSupportedException with English locale")
        void should_handleHttpMediaTypeNotSupportedException_withEnglishLocale() {
            LocaleContextHolder.setLocale(Locale.ENGLISH);

            HttpMediaTypeNotSupportedException ex = new HttpMediaTypeNotSupportedException("application/xml");

            Result<Void> result = handler.handleHttpMediaTypeNotSupportedException(ex, mockRequest);

            assertThat(result.code()).isEqualTo(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE.getCode());
            assertThat(result.message()).isEqualTo("Unsupported media type");
        }

        @Test
        @DisplayName("Should handle NoHandlerFoundException")
        void should_handleNoHandlerFoundException() {
            NoHandlerFoundException ex = new NoHandlerFoundException("GET", "/not-found", null);

            Result<Void> result = handler.handleNoHandlerFoundException(ex, mockRequest);

            assertThat(result.code()).isEqualTo(CommonErrorCode.NOT_FOUND.getCode());
            assertThat(result.message()).contains("不存在");
        }

        @Test
        @DisplayName("Should handle NoHandlerFoundException with English locale")
        void should_handleNoHandlerFoundException_withEnglishLocale() {
            LocaleContextHolder.setLocale(Locale.ENGLISH);

            NoHandlerFoundException ex = new NoHandlerFoundException("GET", "/not-found", null);

            Result<Void> result = handler.handleNoHandlerFoundException(ex, mockRequest);

            assertThat(result.code()).isEqualTo(CommonErrorCode.NOT_FOUND.getCode());
            assertThat(result.message()).isEqualTo("Resource not found");
        }
    }

    @Nested
    @DisplayName("Security exception tests")
    class SecurityExceptionTests {

        @Test
        @DisplayName("Should return forbidden when AccessDeniedException")
        void should_returnForbidden_when_accessDeniedException() {
            org.springframework.security.access.AccessDeniedException ex =
                    new org.springframework.security.access.AccessDeniedException("Access denied");

            Result<Void> result = handler.handleAccessDenied(ex, mockRequest);

            assertThat(result.code()).isEqualTo(CommonErrorCode.FORBIDDEN.getCode());
            assertThat(result.message()).isEqualTo(CommonErrorCode.FORBIDDEN.getMessage());
        }

        @Test
        @DisplayName("Should return forbidden with English locale")
        void should_returnForbidden_withEnglishLocale() {
            LocaleContextHolder.setLocale(Locale.ENGLISH);

            org.springframework.security.access.AccessDeniedException ex =
                    new org.springframework.security.access.AccessDeniedException("Access denied");

            Result<Void> result = handler.handleAccessDenied(ex, mockRequest);

            assertThat(result.code()).isEqualTo(CommonErrorCode.FORBIDDEN.getCode());
            assertThat(result.message()).isEqualTo("Access denied");
        }

        @Test
        @DisplayName("Should return unauthorized when AuthenticationException")
        void should_returnUnauthorized_when_authenticationException() {
            org.springframework.security.core.AuthenticationException ex =
                    new org.springframework.security.authentication.BadCredentialsException("Bad credentials");

            Result<Void> result = handler.handleAuthenticationException(ex, mockRequest);

            assertThat(result.code()).isEqualTo(CommonErrorCode.UNAUTHORIZED.getCode());
            assertThat(result.message()).isEqualTo(CommonErrorCode.UNAUTHORIZED.getMessage());
        }

        @Test
        @DisplayName("Should return unauthorized with English locale")
        void should_returnUnauthorized_withEnglishLocale() {
            LocaleContextHolder.setLocale(Locale.ENGLISH);

            org.springframework.security.core.AuthenticationException ex =
                    new org.springframework.security.authentication.BadCredentialsException("Bad credentials");

            Result<Void> result = handler.handleAuthenticationException(ex, mockRequest);

            assertThat(result.code()).isEqualTo(CommonErrorCode.UNAUTHORIZED.getCode());
            assertThat(result.message()).isEqualTo("Unauthorized or session expired");
        }
    }

    @Nested
    @DisplayName("Generic exception tests")
    class GenericExceptionTests {

        @Test
        @DisplayName("Should return system error when unknown exception")
        void should_returnSystemError_when_unknownException() {
            Exception ex = new RuntimeException("Unknown error");

            Result<Void> result = handler.handleException(ex, mockRequest);

            assertThat(result.code()).isEqualTo(CommonErrorCode.SYSTEM_ERROR.getCode());
            assertThat(result.message()).isEqualTo("系统异常");
        }

        @Test
        @DisplayName("Should return system error when NullPointerException")
        void should_returnSystemError_when_nullPointerException() {
            Exception ex = new NullPointerException("Null reference");

            Result<Void> result = handler.handleException(ex, mockRequest);

            assertThat(result.code()).isEqualTo(CommonErrorCode.SYSTEM_ERROR.getCode());
        }

        @Test
        @DisplayName("Should return system error with English locale")
        void should_returnSystemError_withEnglishLocale() {
            LocaleContextHolder.setLocale(Locale.ENGLISH);

            Exception ex = new RuntimeException("Unknown error");

            Result<Void> result = handler.handleException(ex, mockRequest);

            assertThat(result.code()).isEqualTo(CommonErrorCode.SYSTEM_ERROR.getCode());
            assertThat(result.message()).isEqualTo("System error");
        }
    }
}
