package io.github.afgprojects.framework.core.security.datascope;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jspecify.annotations.NonNull;

/**
 * 数据权限注解
 * <p>
 * 用于声明方法或类级别的数据权限范围。
 * 可标注在 Controller 方法、Service 方法或 Mapper 接口上。
 * <p>
 * 支持多表关联场景，可通过在同一个方法上标注多个 @DataScope 注解来声明不同表的权限控制。
 * <p>
 * 示例：
 * <pre>
 * // 单表权限控制
 * {@code @DataScope(table = "sys_user", column = "dept_id", scopeType = DataScopeType.DEPT)}
 * public List<User> listUsers() { ... }
 *
 * // 多表关联权限控制
 * {@code @DataScope(table = "sys_user", column = "dept_id", scopeType = DataScopeType.DEPT_AND_CHILD)}
 * {@code @DataScope(table = "sys_order", column = "user_id", scopeType = DataScopeType.SELF)}
 * public List<Order> listOrders() { ... }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(DataScope.List.class)
public @interface DataScope {

    /**
     * 表名
     * <p>
     * 需要进行数据权限过滤的表名，支持表别名，如 "u" 或 "sys_user u"
     */
    @NonNull
    String table();

    /**
     * 权限过滤列名
     * <p>
     * 用于数据权限过滤的字段，如 "dept_id"、"create_by" 等
     */
    @NonNull
    String column();

    /**
     * 数据范围类型
     * <p>
     * 默认为本部门数据
     */
    DataScopeType scopeType() default DataScopeType.DEPT;

    /**
     * 自定义 SQL 条件
     * <p>
     * 当 scopeType 为 CUSTOM 时使用，支持 SpEL 表达式
     * <p>
     * 示例：
     * <ul>
     *   <li>{@code "dept_id IN (1, 2, 3)"}</li>
     *   <li>{@code "status = #{status} AND dept_id = #{deptId}"}</li>
     * </ul>
     */
    String customCondition() default "";

    /**
     * 部门表名
     * <p>
     * 用于 DEPT_AND_CHILD 类型，指定部门表的名称，默认为 "sys_dept"
     */
    String deptTable() default "sys_dept";

    /**
     * 部门 ID 字段名
     * <p>
     * 用于 DEPT 和 DEPT_AND_CHILD 类型，指定部门表的主键字段名
     */
    String deptIdColumn() default "id";

    /**
     * 部门父级 ID 字段名
     * <p>
     * 用于 DEPT_AND_CHILD 类型，指定部门表的父级字段名
     */
    String deptParentColumn() default "parent_id";

    /**
     * 用户 ID 字段名
     * <p>
     * 用于 SELF 类型，指定当前用户 ID 在目标表中的字段名
     */
    String userIdColumn() default "create_by";

    /**
     * 是否忽略租户隔离
     * <p>
     * 设置为 true 时，数据权限过滤不受租户隔离限制
     */
    boolean ignoreTenant() default false;

    /**
     * 别名前缀
     * <p>
     * 用于表别名场景，当 SQL 中使用了表别名时，需要设置此属性
     */
    String aliasPrefix() default "";

    /**
     * 允许同名的多个 @DataScope 注解
     */
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        DataScope[] value();
    }
}