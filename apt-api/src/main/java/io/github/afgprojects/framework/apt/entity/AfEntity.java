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
 * 配合 JPA 注解使用，支持数据库场景。
 *
 * <p>生成的元数据类特性：
 * <ul>
 *   <li>实现 {@code DatabaseEntityMetadata<T>} 接口</li>
 *   <li>提供编译时确定的表名、列名映射</li>
 *   <li>支持关联关系元数据生成</li>
 *   <li>自动检测软删除、多租户、审计、版本化特性</li>
 * </ul>
 *
 * <pre>
 * 基础用法：
 * {@code
 * @AfEntity
 * @Table(name = "sys_user")
 * public class User {
 *     @Id
 *     private Long id;
 *
 *     @Column(name = "user_name")
 *     private String userName;
 *
 *     private Boolean deleted;  // 自动检测软删除
 *     private String tenantId;  // 自动检测多租户
 * }
 * }
 *
 * 带关联关系：
 * {@code
 * @AfEntity
 * @Table(name = "sys_order")
 * public class Order {
 *     @Id
 *     private Long id;
 *
 *     @ManyToOne
 *     private User user;
 *
 *     @OneToMany(mappedBy = "order")
 *     private List<OrderItem> items;
 * }
 * }
 *
 * 显式指定特性：
 * {@code
 * @AfEntity(tableName = "sys_log")
 * public class SystemLog {
 *     @Id
 *     private Long id;
 * }
 * }
 * </pre>
 **/
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AfEntity {

    /**
     * 表名（可选）
     * <p>
     * 如果指定，优先级高于 @Table 注解。
     * 如果不指定，按以下顺序解析：
     * <ol>
     *   <li>@Table 注解的 name 属性</li>
     *   <li>类名转换为 snake_case</li>
     * </ol>
     *
     * @return 表名，默认空字符串表示使用默认规则
     */
    String tableName() default "";

    /**
     * 是否生成关联关系元数据
     * <p>
     * 如果为 false，跳过关联关系解析，提升编译速度。
     * 适用于不需要关联加载的简单实体。
     *
     * @return 是否生成关联元数据，默认 true
     */
    boolean generateRelations() default true;
}
