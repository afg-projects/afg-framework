package io.github.afgprojects.framework.apt.entity;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 框架实体标记注解
 * <p>
 * 标记在实体类上，触发 APT 生成元数据类。
 * 配合 @Table 注解使用，支持数据库场景。
 *
 * <pre>
 * &#64;AfEntity
 * &#64;Table(name = "sys_user")
 * public class User {
 *     &#64;Id
 *     private Long id;
 *
 *     &#64;Column(name = "is_deleted")
 *     private Boolean deleted;
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AfEntity {
}
