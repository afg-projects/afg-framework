package io.github.afgprojects.framework.data.core.relation;

import org.jspecify.annotations.NonNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 多对多关联注解
 * <p>
 * 标注在实体的关联字段上，表示与目标实体的多对多关系。
 * 通过中间表实现关联。
 * <p>
 * 使用示例：
 * <pre>{@code
 * public class User {
 *     @ManyToMany
 *     private Set<Role> roles;
 *
 *     @ManyToMany(mappedBy = "users")
 *     private Set<Group> groups;
 * }
 * }</pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ManyToMany {

    /**
     * 关联的目标实体类
     * <p>
     * 默认通过字段的泛型参数类型推断，显式指定可覆盖推断结果
     *
     * @return 目标实体类
     */
    Class<?> targetEntity() default void.class;

    /**
     * 映射字段
     * <p>
     * 表示关系由对方维护，值为对方实体中关联当前实体的字段名
     *
     * @return 对方实体中的关联字段名，默认空字符串表示当前方维护
     */
    String mappedBy() default "";

    /**
     * 中间表名
     * <p>
     * 默认使用两个表名的组合，如 user_role
     *
     * @return 中间表名
     */
    String joinTable() default "";

    /**
     * 中间表中当前实体的外键列名
     *
     * @return 当前实体的外键列名
     */
    String joinColumn() default "";

    /**
     * 中间表中目标实体的外键列名
     *
     * @return 目标实体的外键列名
     */
    String inverseJoinColumn() default "";

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
    FetchType fetch() default FetchType.LAZY;
}
