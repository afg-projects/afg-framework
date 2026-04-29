package io.github.afgprojects.framework.data.core.relation;

import org.jspecify.annotations.NonNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 一对一关联注解
 * <p>
 * 标注在实体的关联字段上，表示与目标实体的一对一关系。
 * <p>
 * 使用示例：
 * <pre>{@code
 * public class User {
 *     @OneToOne
 *     private UserProfile profile;
 *
 *     @OneToOne(mappedBy = "user")
 *     private UserConfig config;
 * }
 * }</pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OneToOne {

    /**
     * 关联的目标实体类
     * <p>
     * 默认通过字段类型推断，显式指定可覆盖推断结果
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
     * 外键列名
     * <p>
     * 当前实体表中的外键列名，仅在当前方维护关系时有效
     *
     * @return 外键列名
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
    FetchType fetch() default FetchType.LAZY;
}
