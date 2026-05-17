package io.github.afgprojects.framework.apt.entity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明通用字段定义
 * <p>
 * 用于注册可复用的字段元数据，避免 APT 为每个实体重复生成相同的内部类。
 * <p>
 * 使用方式：
 * <ul>
 *   <li>实体字段上：声明该字段为通用字段</li>
 *   <li>专门的定义类上：集中声明多个通用字段</li>
 * </ul>
 *
 * <p>示例：
 * <pre>{@code
 * // 方式一：在实体字段上声明
 * @AfEntity
 * public class Order {
 *     @CommonFieldDefinition(name = "ORDER_NO", propertyName = "orderNo", fieldType = String.class)
 *     private String orderNo;
 * }
 *
 * // 方式二：在专门的定义类上声明
 * @CommonFieldDefinitions({
 *     @CommonFieldDefinition(name = "ORG_ID", propertyName = "orgId", fieldType = String.class),
 *     @CommonFieldDefinition(name = "COMPANY_ID", propertyName = "companyId", fieldType = Long.class)
 * })
 * public class MyCommonFields {}
 * }</pre>
 *
 * @see CommonFieldDefinitions
 */
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
@Repeatable(CommonFieldDefinitions.class)
public @interface CommonFieldDefinition {

    /**
     * 注册名称，用于生成常量引用
     * <p>
     * 例如：name = "ORG_ID" → CommonFieldMetadata.ORG_ID
     *
     * @return 注册名称（建议大写下划线格式）
     */
    String name();

    /**
     * 属性名
     * <p>
     * 用于匹配实体字段，如 "orgId"
     *
     * @return 属性名
     */
    String propertyName();

    /**
     * 列名
     * <p>
     * 如果为空，则使用 snake_case 转换属性名。
     * 例如：propertyName = "orgId" → columnName = "org_id"
     *
     * @return 列名，默认空表示自动转换
     */
    String columnName() default "";

    /**
     * 字段类型
     * <p>
     * 用于类型匹配，只有类型完全匹配时才使用通用字段元数据。
     *
     * @return 字段类型
     */
    Class<?> fieldType();

    /**
     * 是否主键
     *
     * @return 是否主键，默认 false
     */
    boolean isId() default false;

    /**
     * 是否自动生成
     *
     * @return 是否自动生成，默认 false
     */
    boolean isGenerated() default false;
}
