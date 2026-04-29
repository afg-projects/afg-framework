package io.github.afgprojects.framework.data.core.relation;

import org.jspecify.annotations.NonNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 多对一关联注解
 * <p>
 * 标注在实体的关联字段上，表示与目标实体的多对一关系。
 * <p>
 * 使用示例：
 * <pre>{@code
 * public class Order {
 *     @ManyToOne
 *     private User user;
 *
 *     @ManyToOne(fetch = FetchType.EAGER)
 *     private Department department;
 * }
 * }</pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ManyToOne {

    /**
     * 关联的目标实体类
     * <p>
     * 默认通过字段类型推断，显式指定可覆盖推断结果
     *
     * @return 目标实体类
     */
    Class<?> targetEntity() default void.class;

    /**
     * 外键列名
     * <p>
     * 当前实体表中的外键列名
     *
     * @return 外键列名，默认使用 字段名_id
     */
    String foreignKey() default "";

    /**
     * 级联操作类型
     *
     * @return 级联类型数组
     */
    CascadeType[] cascade() default {};

    /**
     * 抓取策略
     *
     * @return 抓取策略
     */
    FetchType fetch() default FetchType.EAGER;

    /**
     * 关联是否可选
     * <p>
     * 设置为 false 时，外键不允许为 null
     *
     * @return 关联是否可选
     */
    boolean optional() default true;
}
