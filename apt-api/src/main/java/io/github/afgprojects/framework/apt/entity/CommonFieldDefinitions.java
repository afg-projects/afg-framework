package io.github.afgprojects.framework.apt.entity;

import java.lang.annotation.*;

/**
 * CommonFieldDefinition 的容器注解
 * <p>
 * 支持在同一个元素上使用多个 @CommonFieldDefinition 注解。
 *
 * <p>示例：
 * <pre>{@code
 * @CommonFieldDefinitions({
 *     @CommonFieldDefinition(name = "ORG_ID", propertyName = "orgId", fieldType = String.class),
 *     @CommonFieldDefinition(name = "COMPANY_ID", propertyName = "companyId", fieldType = Long.class)
 * })
 * public class MyCommonFields {}
 * }</pre>
 *
 * @see CommonFieldDefinition
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface CommonFieldDefinitions {
    /**
     * 通用字段定义数组
     *
     * @return 字段定义数组
     */
    CommonFieldDefinition[] value();
}
