package io.github.afgprojects.framework.core.audit;

import java.util.Set;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.afgprojects.framework.core.util.JsonProcessingException;
import io.github.afgprojects.framework.core.util.JacksonUtils;

/**
 * 审计日志参数序列化器
 * <p>
 * 负责序列化方法参数和返回值，支持敏感字段脱敏
 * </p>
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
class AuditLogSerializer {

    private static final Logger log = LoggerFactory.getLogger(AuditLogSerializer.class);

    private final SensitiveFieldProcessor sensitiveFieldProcessor;

    /**
     * 构造函数
     *
     * @param sensitiveFieldProcessor 敏感字段处理器
     */
    AuditLogSerializer(@NonNull SensitiveFieldProcessor sensitiveFieldProcessor) {
        this.sensitiveFieldProcessor = sensitiveFieldProcessor;
    }

    /**
     * 序列化参数并脱敏
     *
     * @param args            参数值数组
     * @param paramNames      参数名数组
     * @param sensitiveFields 敏感字段集合
     * @return 序列化后的参数字符串
     */
    @Nullable String serializeAndMask(
            @Nullable Object[] args,
            @Nullable String[] paramNames,
            @NonNull Set<String> sensitiveFields) {
        if (args == null || args.length == 0) {
            return null;
        }

        try {
            StringBuilder sb = new StringBuilder("{");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                String name = paramNames != null && i < paramNames.length ? paramNames[i] : "arg" + i;
                Object value = args[i];

                // 脱敏处理
                if (sensitiveFieldProcessor.isSensitive(name, sensitiveFields)) {
                    sb.append(name).append("=***");
                } else {
                    sb.append(name).append("=").append(serializeValue(value));
                }
            }
            sb.append("}");
            return sb.toString();
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize args: {}", e.getMessage());
            return "[serialization error]";
        } catch (RuntimeException e) {
            log.warn("Unexpected error serializing args: {}", e.getMessage());
            return "[serialization error]";
        }
    }

    /**
     * 序列化结果值
     *
     * @param result          方法返回值
     * @param annotation      审计注解
     * @param sensitiveFields 敏感字段集合
     * @return 序列化后的结果字符串
     */
    @Nullable String serializeResult(
            @Nullable Object result,
            @NonNull Audited annotation,
            @NonNull Set<String> sensitiveFields) {
        if (result == null || !annotation.recordResult()) {
            return null;
        }

        try {
            return serializeValue(result);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize result: {}", e.getMessage());
            return "[serialization error]";
        } catch (RuntimeException e) {
            log.warn("Unexpected error serializing result: {}", e.getMessage());
            return "[serialization error]";
        }
    }

    /**
     * 序列化单个值
     *
     * @param value 要序列化的值
     * @return 序列化后的字符串
     */
    @NonNull String serializeValue(@Nullable Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String str) {
            return "\"" + str + "\"";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        // 复杂对象转 JSON
        return JacksonUtils.toJson(value);
    }
}
