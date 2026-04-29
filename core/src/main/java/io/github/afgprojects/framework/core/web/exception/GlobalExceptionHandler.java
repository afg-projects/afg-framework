package io.github.afgprojects.framework.core.web.exception;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import io.github.afgprojects.framework.core.model.exception.BusinessException;
import io.github.afgprojects.framework.core.model.exception.CommonErrorCode;
import io.github.afgprojects.framework.core.model.result.Result;
import io.github.afgprojects.framework.core.model.result.Results;
import io.github.afgprojects.framework.core.web.context.AfgRequestContextHolder;
import io.github.afgprojects.framework.core.web.context.RequestContext;
import io.github.afgprojects.framework.core.web.security.util.SensitiveDataMasker;
import lombok.extern.slf4j.Slf4j;

/**
 * 全局异常处理器
 * 统一处理各类异常并返回规范错误响应
 *
 * <p>状态码策略：BusinessException 返回 HTTP 200 + 业务码，
 * 其他异常返回对应 HTTP 语义状态码（400/404/405/415/500）。
 *
 * <p>国际化支持：根据请求 Accept-Language 头自动选择语言
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常 — HTTP 200 + 业务码
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        Locale locale = resolveLocale(request);
        String localizedMessage = e.getMessage(locale);
        log.warn(
                "Business exception | traceId={} | path={} | code={} | message={}",
                getTraceId(),
                getRequestPath(),
                e.formatCode(),
                e.getMessage());
        return Results.fail(e.getCode(), localizedMessage);
    }

    /**
     * 参数校验异常（@Valid on request body）— 400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = extractFieldErrors(e.getBindingResult());
        log.info("Validation failed | traceId={} | path={} | errors={}", getTraceId(), getRequestPath(), message);
        return Results.fail(CommonErrorCode.PARAM_ERROR, message);
    }

    /**
     * 参数校验异常（@Valid on form data）— 400
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e, HttpServletRequest request) {
        String message = extractFieldErrors(e.getBindingResult());
        log.info("Validation failed | traceId={} | path={} | errors={}", getTraceId(), getRequestPath(), message);
        return Results.fail(CommonErrorCode.PARAM_ERROR, message);
    }

    /**
     * 约束校验异常（@Validated on method parameters）— 400
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolationException(
            ConstraintViolationException e, HttpServletRequest request) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.info("Constraint violation | traceId={} | path={} | message={}", getTraceId(), getRequestPath(), message);
        return Results.fail(CommonErrorCode.PARAM_ERROR, message);
    }

    /**
     * 缺少必需参数 — 400
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e, HttpServletRequest request) {
        Locale locale = resolveLocale(request);
        String message = CommonErrorCode.PARAM_MISSING.getMessage(locale) + ": " + e.getParameterName();
        log.info(
                "Missing parameter | traceId={} | path={} | param={}",
                getTraceId(),
                getRequestPath(),
                e.getParameterName());
        return Results.fail(CommonErrorCode.PARAM_ERROR, message);
    }

    /**
     * 参数类型不匹配 — 400
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        Locale locale = resolveLocale(request);
        String message = CommonErrorCode.PARAM_FORMAT_ERROR.getMessage(locale) + ": " + e.getName();
        String maskedValue = SensitiveDataMasker.mask(
                e.getName(), e.getValue() != null ? e.getValue().toString() : null);
        log.info(
                "Parameter type mismatch | traceId={} | path={} | param={} | value={}",
                getTraceId(),
                getRequestPath(),
                e.getName(),
                maskedValue);
        return Results.fail(CommonErrorCode.PARAM_ERROR, message);
    }

    /**
     * 请求体解析失败 — 400
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e, HttpServletRequest request) {
        Locale locale = resolveLocale(request);
        log.info(
                "Request body parse failed | traceId={} | path={} | error={}",
                getTraceId(),
                getRequestPath(),
                e.getMessage());
        return Results.fail(CommonErrorCode.PARAM_ERROR, CommonErrorCode.PARAM_FORMAT_ERROR.getMessage(locale));
    }

    /**
     * 请求方法不支持 — 405
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        Locale locale = resolveLocale(request);
        log.warn(
                "Method not supported | traceId={} | path={} | method={}",
                getTraceId(),
                getRequestPath(),
                e.getMethod());
        String message = CommonErrorCode.METHOD_NOT_ALLOWED.getMessage(locale) + ": " + e.getMethod();
        return Results.fail(CommonErrorCode.METHOD_NOT_ALLOWED, message);
    }

    /**
     * 媒体类型不支持 — 415
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public Result<Void> handleHttpMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        Locale locale = resolveLocale(request);
        log.warn(
                "Media type not supported | traceId={} | path={} | contentType={}",
                getTraceId(),
                getRequestPath(),
                e.getContentType());
        return Results.fail(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE, CommonErrorCode.UNSUPPORTED_MEDIA_TYPE.getMessage(locale));
    }

    /**
     * 资源不存在 — 404
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoHandlerFoundException(NoHandlerFoundException e, HttpServletRequest request) {
        Locale locale = resolveLocale(request);
        log.warn(
                "No handler found | traceId={} | method={} | path={}",
                getTraceId(),
                e.getHttpMethod(),
                e.getRequestURL());
        return Results.fail(CommonErrorCode.NOT_FOUND, CommonErrorCode.NOT_FOUND.getMessage(locale));
    }

    /**
     * 访问拒绝 — 403
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException e, HttpServletRequest request) {
        Locale locale = resolveLocale(request);
        log.warn(
                "Access denied | traceId={} | path={} | userId={} | message={}",
                getTraceId(),
                getRequestPath(),
                getUserId(),
                e.getMessage());
        return Results.fail(CommonErrorCode.FORBIDDEN.getCode(), CommonErrorCode.FORBIDDEN.getMessage(locale));
    }

    /**
     * 认证失败 — 401
     */
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleAuthenticationException(
            org.springframework.security.core.AuthenticationException e, HttpServletRequest request) {
        Locale locale = resolveLocale(request);
        log.warn(
                "Authentication failed | traceId={} | path={} | message={}",
                getTraceId(),
                getRequestPath(),
                e.getMessage());
        return Results.fail(CommonErrorCode.UNAUTHORIZED.getCode(), CommonErrorCode.UNAUTHORIZED.getMessage(locale));
    }

    /**
     * 未知异常 — 500
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        Locale locale = resolveLocale(request);
        log.error(
                "Unexpected exception | traceId={} | path={} | method={} | userId={} | exception={} | message={}",
                getTraceId(),
                request.getRequestURI(),
                request.getMethod(),
                getUserId(),
                e.getClass().getName(),
                e.getMessage(),
                e);
        return Results.fail(CommonErrorCode.SYSTEM_ERROR.getCode(), CommonErrorCode.SYSTEM_ERROR.getMessage(locale));
    }

    /**
     * 解析请求的语言
     * 使用 LocaleContextHolder 中的 Locale（由 LocaleFilter 设置）
     */
    private Locale resolveLocale(HttpServletRequest request) {
        Locale locale = LocaleContextHolder.getLocale();
        return locale != null ? locale : Locale.getDefault();
    }

    /**
     * 获取当前请求的 TraceId
     */
    private String getTraceId() {
        RequestContext context = AfgRequestContextHolder.getContext();
        return context != null ? context.getTraceId() : "N/A";
    }

    /**
     * 获取当前请求路径
     */
    private String getRequestPath() {
        RequestContext context = AfgRequestContextHolder.getContext();
        return context != null ? context.getRequestPath() : "N/A";
    }

    /**
     * 获取当前用户ID
     */
    private Long getUserId() {
        RequestContext context = AfgRequestContextHolder.getContext();
        return context != null ? context.getUserId() : null;
    }

    private String extractFieldErrors(BindingResult bindingResult) {
        List<FieldError> fieldErrors = bindingResult.getFieldErrors();
        return fieldErrors.stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
    }
}
