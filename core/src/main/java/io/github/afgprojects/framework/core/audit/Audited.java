package io.github.afgprojects.framework.core.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 审计日志注解
 * <p>
 * 标注在需要审计的方法上，自动记录操作日志
 * </p>
 *
 * <pre>{@code
 * @Audited(operation = "创建用户", module = "用户管理")
 * public User createUser(UserRequest request) { ... }
 *
 * @Audited(operation = "更新密码", module = "用户管理", sensitiveFields = {"password", "oldPassword"})
 * public void updatePassword(String userId, String password, String oldPassword) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    /**
     * 操作名称
     * <p>
     * 如果为空，自动使用方法名
     * </p>
     *
     * @return 操作名称
     */
    String operation() default "";

    /**
     * 模块名称
     * <p>
     * 如果为空，自动使用类名
     * </p>
     *
     * @return 模块名称
     */
    String module() default "";

    /**
     * 敏感字段列表
     * <p>
     * 这些字段的值将被脱敏处理
     * </p>
     *
     * @return 敏感字段名称数组
     */
    String[] sensitiveFields() default {};

    /**
     * 是否记录方法参数
     * <p>
     * 默认记录，敏感字段会被脱敏
     * </p>
     *
     * @return 是否记录参数
     */
    boolean recordArgs() default true;

    /**
     * 是否记录返回值
     * <p>
     * 默认记录，敏感字段会被脱敏
     * </p>
     *
     * @return 是否记录返回值
     */
    boolean recordResult() default true;

    /**
     * 目标对象表达式
     * <p>
     * SpEL 表达式，用于提取操作目标对象
     * 例如: "#userId" 或 "#request.id"
     * </p>
     *
     * @return SpEL 表达式
     */
    String target() default "";
}
