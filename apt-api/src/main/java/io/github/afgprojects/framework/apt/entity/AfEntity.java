package io.github.afgprojects.framework.apt.entity;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 框架实体标记注解。
 * <p>
 * 标记在实体类上，触发 APT 生成元数据类。配合 @Table、@Column 等注解使用，
 * 支持数据库场景的 ORM 映射。
 *
 * <h2>生成的元数据类特性</h2>
 * <ul>
 *   <li>实现 {@code DatabaseEntityMetadata<T>} 接口</li>
 *   <li>提供编译时确定的表名、列名映射</li>
 *   <li>支持关联关系元数据生成（@ManyToOne、@OneToMany 等）</li>
 *   <li>自动检测软删除、多租户、审计、版本化特性</li>
 * </ul>
 *
 * <h2>基础用法</h2>
 * <pre>{@code
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
 * }</pre>
 *
 * <h2>带关联关系</h2>
 * <pre>{@code
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
 * }</pre>
 *
 * <h2>显式指定表名</h2>
 * <pre>{@code
 * @AfEntity(tableName = "sys_log")
 * public class SystemLog {
 *     @Id
 *     private Long id;
 * }
 * }</pre>
 *
 * @see io.github.afgprojects.framework.data.core.annotation.Table
 * @see io.github.afgprojects.framework.data.core.annotation.Column
 **/
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AfEntity {

    /**
     * 表名（可选）。
     * <p>
     * 如果指定，优先级高于 @Table 注解。如果不指定，按以下顺序解析：
     * <ol>
     *   <li>@Table 注解的 name 属性</li>
     *   <li>类名转换为 snake_case（如 UserEntity → user_entity）</li>
     * </ol>
     *
     * <p>示例：
     * <pre>{@code
     * @AfEntity(tableName = "sys_log")
     * public class SystemLog { ... }
     * }</pre>
     *
     * @return 表名，默认空字符串表示使用默认规则
     */
    String tableName() default "";

    /**
     * 是否生成关联关系元数据。
     * <p>
     * 如果为 false，跳过关联关系解析，提升编译速度。适用于不需要关联加载的简单实体。
     *
     * <p>示例：
     * <pre>{@code
     * @AfEntity(generateRelations = false)
     * public class SimpleLog { ... }
     * }</pre>
     *
     * @return 是否生成关联元数据，默认 true
     */
    boolean generateRelations() default true;

    /**
     * 是否自动填充时间戳字段（createdAt/updatedAt）。
     * <p>
     * 如果为 true（默认），DataManager 在保存实体时会自动填充 createdAt（新建时）
     * 和 updatedAt（新建和更新时）字段。如果实体继承自 BaseEntity，此属性默认生效。
     *
     * <p>如果为 false，需要手动设置时间戳字段。
     *
     * <p>示例：
     * <pre>{@code
     * @AfEntity(autoFillTimestamps = false)
     * public class ManualTimestampEntity {
     *     private Long id;
     *     private Instant createdAt;
     *     private Instant updatedAt;
     * }
     * }</pre>
     *
     * @return 是否自动填充时间戳，默认 true
     */
    boolean autoFillTimestamps() default true;
}
