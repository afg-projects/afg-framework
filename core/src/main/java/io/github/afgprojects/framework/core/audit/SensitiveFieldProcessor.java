package io.github.afgprojects.framework.core.audit;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.jspecify.annotations.NonNull;

/**
 * 审计日志敏感字段处理器
 * <p>
 * 负责构建和管理敏感字段集合，合并注解配置和全局配置
 * </p>
 */
class SensitiveFieldProcessor {

    private final AuditLogProperties properties;

    /**
     * 构造函数
     *
     * @param properties 审计日志配置
     */
    SensitiveFieldProcessor(@NonNull AuditLogProperties properties) {
        this.properties = properties;
    }

    /**
     * 构建敏感字段集合
     * <p>
     * 合并注解配置和全局配置的敏感字段
     * </p>
     *
     * @param annotation 审计注解
     * @return 敏感字段集合（已标准化）
     */
    @NonNull Set<String> buildSensitiveFields(@NonNull Audited annotation) {
        Set<String> fields = new HashSet<>();

        // 添加注解配置的敏感字段
        for (String field : annotation.sensitiveFields()) {
            fields.add(normalizeFieldName(field));
        }

        // 添加全局配置的敏感字段
        for (String field : properties.getSensitiveFields()) {
            fields.add(normalizeFieldName(field));
        }

        return fields;
    }

    /**
     * 检查字段名是否为敏感字段
     *
     * @param fieldName       原始字段名
     * @param sensitiveFields 敏感字段集合
     * @return 是否为敏感字段
     */
    boolean isSensitive(@NonNull String fieldName, @NonNull Set<String> sensitiveFields) {
        return sensitiveFields.contains(normalizeFieldName(fieldName));
    }

    /**
     * 标准化字段名（小写、去下划线）
     *
     * @param fieldName 原始字段名
     * @return 标准化后的字段名
     */
    @NonNull String normalizeFieldName(@NonNull String fieldName) {
        return fieldName.toLowerCase(Locale.ROOT).replace("_", "");
    }
}
