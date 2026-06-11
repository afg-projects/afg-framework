package io.github.afgprojects.framework.apt.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法参数元数据注解。
 * <p>
 * 标注在 @AfOperation 方法的参数上，提供参数的元数据信息。
 * 用于覆盖编译后的参数名（当 -parameters 未启用时），
 * 以及为文档生成和 AI 工具定义提供参数描述。
 *
 * <h2>基础用法</h2>
 * <pre>{@code
 * @AfOperation(name = "findUser")
 * public User findUser(
 *     @AfParam(name = "userId", description = "用户ID", required = true) Long userId
 * ) { ... }
 * }</pre>
 *
 * <h2>带默认值和枚举提示</h2>
 * <pre>{@code
 * @AfOperation(name = "setStatus")
 * public void setStatus(
 *     @AfParam(name = "status", description = "用户状态",
 *              defaultValue = "ACTIVE", enumValues = {"ACTIVE", "INACTIVE", "DELETED"}) String status
 * ) { ... }
 * }</pre>
 *
 * @see AfOperation
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.SOURCE)
public @interface AfParam {

    /**
     * 参数名称。
     * <p>
     * 覆盖编译后的参数名。当编译时未启用 -parameters 选项时，
     * 可以通过此属性指定参数名。
     *
     * @return 参数名称，默认空字符串表示使用编译后参数名
     */
    String name() default "";

    /**
     * 参数描述。
     * <p>
     * 用于文档生成和 AI 工具定义。
     *
     * @return 参数描述，默认空字符串
     */
    String description() default "";

    /**
     * 是否必填。
     * <p>
     * 标记此参数是否为必填参数。用于调用时的参数校验。
     *
     * @return 是否必填，默认 true
     */
    boolean required() default true;

    /**
     * 默认值。
     * <p>
     * 字符串形式的默认值。为空表示无默认值。
     *
     * <p>示例：
     * <pre>{@code
     * @AfParam(defaultValue = "10")
     * }</pre>
     *
     * @return 默认值字符串，默认空字符串表示无默认值
     */
    String defaultValue() default "";

    /**
     * 枚举值提示。
     * <p>
     * 用于 AI 工具定义，提示此参数可接受的枚举值列表。
     *
     * <p>示例：
     * <pre>{@code
     * @AfParam(enumValues = {"ACTIVE", "INACTIVE"})
     * }</pre>
     *
     * @return 枚举值数组，默认空数组
     */
    String[] enumValues() default {};
}
