package io.github.afgprojects.framework.apt.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 动态可调用操作标记注解。
 * <p>
 * 标注在 @AfService 类的方法上，声明该方法为可动态调用的操作。
 * APT 处理器会在编译时提取操作元数据，用于运行时动态调用。
 *
 * <h2>使用规则</h2>
 * <ul>
 *   <li>必须用在 @AfService 标注的类的方法上</li>
 *   <li>同一 @AfService 类内操作名（name）必须唯一</li>
 *   <li>如果 name 为空，默认使用方法名</li>
 * </ul>
 *
 * <h2>基础用法</h2>
 * <pre>{@code
 * @AfService(name = "userService")
 * public class UserService {
 *
 *     @AfOperation(name = "createUser", description = "创建用户")
 *     public User createUser(@AfParam(name = "user") User user) {
 *         return userRepository.save(user);
 *     }
 * }
 * }</pre>
 *
 * <h2>带权限和审计</h2>
 * <pre>{@code
 * @AfOperation(
 *     name = "deleteUser",
 *     description = "删除用户",
 *     permission = "user:delete",
 *     requiredRoles = {"ADMIN"},
 *     audit = true
 * )
 * public void deleteUser(Long userId) { ... }
 * }</pre>
 *
 * @see AfService
 * @see AfParam
 * @see AfResult
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface AfOperation {

    /**
     * 操作名称。
     * <p>
     * 在同一 @AfService 类内必须唯一。如果为空，默认使用方法名。
     *
     * @return 操作名称，默认空字符串表示使用方法名
     */
    String name() default "";

    /**
     * 操作描述。
     * <p>
     * 用于文档生成和 AI 工具定义。支持 Markdown 格式。
     *
     * @return 操作描述，默认空字符串
     */
    String description() default "";

    /**
     * 是否为异步操作。
     * <p>
     * 标记为异步的操作在调用时不会立即返回结果，而是返回异步任务标识。
     *
     * @return 是否异步，默认 false
     */
    boolean async() default false;

    /**
     * 是否已废弃。
     * <p>
     * 标记为废弃的操作会在文档和 AI 工具定义中显示废弃警告。
     *
     * @return 是否废弃，默认 false
     */
    boolean deprecated() default false;

    /**
     * 权限标识。
     * <p>
     * 调用此操作需要持有的权限标识。为空表示不需要权限检查。
     *
     * <p>示例：
     * <pre>{@code
     * @AfOperation(permission = "user:delete")
     * }</pre>
     *
     * @return 权限标识，默认空字符串表示无权限要求
     */
    String permission() default "";

    /**
     * 需要的角色列表。
     * <p>
     * 调用此操作需要持有的角色。为空表示不需要角色检查。
     *
     * <p>示例：
     * <pre>{@code
     * @AfOperation(requiredRoles = {"ADMIN", "SUPERADMIN"})
     * }</pre>
     *
     * @return 角色列表，默认空数组
     */
    String[] requiredRoles() default {};

    /**
     * 是否审计此操作的调用。
     * <p>
     * 启用后，每次调用此操作都会记录审计日志，包括调用者、参数、返回值和耗时。
     *
     * @return 是否审计，默认 true
     */
    boolean audit() default true;

    /**
     * 是否限制在当前租户范围内。
     * <p>
     * 启用后，操作的数据访问会自动注入租户过滤条件。
     *
     * @return 是否租户范围，默认 true
     */
    boolean tenantScope() default true;

    /**
     * 是否启用数据权限过滤。
     * <p>
     * 启用后，操作的数据访问会根据当前用户的数据权限自动过滤。
     *
     * @return 是否启用数据权限，默认 false
     */
    boolean dataScope() default false;

    /**
     * 手动指定输入 JSON Schema。
     * <p>
     * 为空时 APT 会从方法签名自动推导输入参数的 JSON Schema。
     * 仅在自动推导不满足需求时使用。
     *
     * @return JSON Schema 字符串，默认空字符串表示自动推导
     */
    String inputSchema() default "";
}
