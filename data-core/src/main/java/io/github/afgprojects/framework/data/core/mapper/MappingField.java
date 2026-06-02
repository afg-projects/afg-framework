package io.github.afgprojects.framework.data.core.mapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 映射字段注解
 * <p>
 * 用于 DTO 映射时指定 ResultSet 列名与字段/Record 组件的映射关系。
 * 支持 POJO 字段和 Java Record 组件。
 *
 * <pre>{@code
 * // POJO 用法
 * public class UserDto {
 *     @MappingField(column = "user_name")
 *     private String name;
 * }
 *
 * // Record 用法
 * public record UserDto(
 *     @MappingField(column = "user_name") String name,
 *     Integer age
 * ) {}
 * }</pre>
 */
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface MappingField {

    /**
     * 源列名（与 column 互斥，优先使用 column）
     */
    String source() default "";

    /**
     * 目标列名
     */
    String column() default "";
}
