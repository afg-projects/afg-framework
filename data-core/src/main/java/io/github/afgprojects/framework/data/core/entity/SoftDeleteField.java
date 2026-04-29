package io.github.afgprojects.framework.data.core.entity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 软删除字段注解
 * <p>
 * 标注在实体的软删除字段上，用于自定义配置
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SoftDeleteField {
    /**
     * 字段名（默认 "deleted"）
     */
    String value() default "deleted";

    /**
     * 已删除值（默认 "1"）
     */
    String deletedValue() default "1";

    /**
     * 未删除值（默认 "0"）
     */
    String notDeletedValue() default "0";
}
