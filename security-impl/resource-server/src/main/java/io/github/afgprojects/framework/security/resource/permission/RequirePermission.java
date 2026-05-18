package io.github.afgprojects.framework.security.resource.permission;

import org.jspecify.annotations.NonNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限校验注解。
 *
 * <p>标注在方法或类上，表示需要指定权限才能访问。
 * 从当前 JWT Token 中解析权限进行校验。
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * @RequirePermission("user:read")
 * public User getUser(Long id) { ... }
 *
 * @RequirePermission(value = {"user:read", "user:write"}, logical = Logical.OR)
 * public void updateUser(User user) { ... }
 * }</pre>
 *
 * @since 1.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /**
     * 权限标识。
     */
    @NonNull
    String[] value();

    /**
     * 逻辑关系，默认 AND。
     */
    Logical logical() default Logical.AND;
}