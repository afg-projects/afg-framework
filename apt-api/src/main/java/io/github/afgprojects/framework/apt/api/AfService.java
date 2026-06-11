package io.github.afgprojects.framework.apt.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 动态可调用服务标记注解。
 * <p>
 * 标注在 Spring Bean 类上，APT 处理器会在编译时提取服务元数据，
 * 用于运行时动态服务发现和调用。配合 @AfOperation、@AfParam、@AfResult 注解使用。
 *
 * <h2>生成的元数据</h2>
 * <ul>
 *   <li>生成 {ClassName}ServiceMetadata 类，实现 ServiceMetadata 接口</li>
 *   <li>生成 META-INF/afg/service-metadata.index 索引文件</li>
 *   <li>提取所有 @AfOperation 方法的元数据</li>
 * </ul>
 *
 * <h2>基础用法</h2>
 * <pre>{@code
 * @AfService(name = "userService", description = "用户管理服务")
 * public class UserService {
 *
 *     @AfOperation(name = "createUser", description = "创建用户")
 *     public User createUser(@AfParam(name = "user") User user) {
 *         return userRepository.save(user);
 *     }
 *
 *     @AfOperation(name = "findUser", description = "查找用户")
 *     public Optional<User> findUser(@AfParam(name = "id") Long id) {
 *         return userRepository.findById(id);
 *     }
 * }
 * }</pre>
 *
 * <h2>带分类和标签</h2>
 * <pre>{@code
 * @AfService(
 *     name = "roleService",
 *     description = "角色管理服务",
 *     category = "system",
 *     tags = {"auth", "rbac"}
 * )
 * public class RoleService { ... }
 * }</pre>
 *
 * @see AfOperation
 * @see AfParam
 * @see AfResult
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface AfService {

    /**
     * 服务名称。
     * <p>
     * 用于运行时服务查找和调用。如果为空，默认使用类名首字母小写形式。
     *
     * <p>示例：UserService → userService
     *
     * @return 服务名称，默认空字符串表示使用默认规则
     */
    String name() default "";

    /**
     * 服务描述。
     * <p>
     * 用于文档生成和 AI 工具定义。支持 Markdown 格式。
     *
     * @return 服务描述，默认空字符串
     */
    String description() default "";

    /**
     * 服务分类。
     * <p>
     * 用于服务分组和过滤。常见分类：system、business、integration 等。
     *
     * @return 服务分类，默认空字符串
     */
    String category() default "";

    /**
     * 服务标签。
     * <p>
     * 用于服务过滤和搜索。支持多个标签。
     *
     * <p>示例：
     * <pre>{@code
     * @AfService(tags = {"auth", "rbac", "security"})
     * }</pre>
     *
     * @return 标签数组，默认空数组
     */
    String[] tags() default {};

    /**
     * 是否已废弃。
     * <p>
     * 标记为废弃的服务会在文档和 AI 工具定义中显示废弃警告。
     *
     * @return 是否废弃，默认 false
     */
    boolean deprecated() default false;

    /**
     * 服务图标（UI 展示支持）。
     * <p>
     * 用于前端 UI 展示的图标标识，可以是图标名称或 URL。
     *
     * <p>示例：
     * <pre>{@code
     * @AfService(icon = "user-icon")
     * }</pre>
     *
     * @return 图标标识，默认空字符串
     */
    String icon() default "";

    /**
     * 服务使用示例（UI 展示支持）。
     * <p>
     * 用于前端 UI 展示的服务使用示例，支持多个示例。
     *
     * <p>示例：
     * <pre>{@code
     * @AfService(examples = {
     *     "userService.createUser(user)",
     *     "userService.findUser(1L)"
     * })
     * }</pre>
     *
     * @return 示例数组，默认空数组
     */
    String[] examples() default {};
}
