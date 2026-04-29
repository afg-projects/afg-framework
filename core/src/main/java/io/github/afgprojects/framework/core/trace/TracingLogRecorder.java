package io.github.afgprojects.framework.core.trace;

import java.util.Arrays;
import java.util.StringJoiner;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import io.micrometer.tracing.Span;

/**
 * 追踪日志记录器
 * <p>
 * 负责记录 Span 的参数、返回值和异常信息
 * </p>
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
class TracingLogRecorder {

    private static final int MAX_PARAM_LENGTH = 1000;
    private static final int MAX_STACKTRACE_LENGTH = 2000;

    /**
     * 记录参数
     *
     * @param span      Span 实例
     * @param joinPoint 切点
     */
    void logParameters(@NonNull Span span, @NonNull ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return;
        }

        MethodSignature signature =
                (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();

        for (int i = 0; i < args.length; i++) {
            String paramName = (paramNames != null && i < paramNames.length) ? paramNames[i] : "arg" + i;
            String paramValue = safeToString(args[i]);

            // 限制长度
            if (paramValue != null && paramValue.length() > MAX_PARAM_LENGTH) {
                paramValue = paramValue.substring(0, MAX_PARAM_LENGTH) + "...[truncated]";
            }

            span.tag("param." + paramName, paramValue != null ? paramValue : "null");
        }
    }

    /**
     * 记录返回值
     *
     * @param span   Span 实例
     * @param result 返回值
     */
    void logResult(@NonNull Span span, @Nullable Object result) {
        String resultStr = safeToString(result);

        // 限制长度
        if (resultStr != null && resultStr.length() > MAX_PARAM_LENGTH) {
            resultStr = resultStr.substring(0, MAX_PARAM_LENGTH) + "...[truncated]";
        }

        span.tag("result", resultStr != null ? resultStr : "null");
    }

    /**
     * 记录异常
     *
     * @param span  Span 实例
     * @param ex    异常
     * @param level 日志级别
     */
    void logException(@NonNull Span span, @NonNull Throwable ex, @NonNull ExceptionLogLevel level) {
        span.tag("exception.message", ex.getMessage());

        if (level == ExceptionLogLevel.STACK_TRACE) {
            StringJoiner joiner = new StringJoiner("\n");
            joiner.add(ex.toString());

            Arrays.stream(ex.getStackTrace())
                    .limit(10)
                    .forEach(frame -> joiner.add("    at " + frame.toString()));

            String stackTrace = joiner.toString();
            if (stackTrace.length() > MAX_STACKTRACE_LENGTH) {
                stackTrace = stackTrace.substring(0, MAX_STACKTRACE_LENGTH) + "...[truncated]";
            }

            span.tag("exception.stacktrace", stackTrace);
        }
    }

    /**
     * 安全转换为字符串
     *
     * @param obj 对象
     * @return 字符串表示
     */
    @Nullable String safeToString(@Nullable Object obj) {
        if (obj == null) {
            return null;
        }

        try {
            return obj.toString();
        } catch (Exception e) {
            return "[error calling toString: " + e.getMessage() + "]";
        }
    }
}
