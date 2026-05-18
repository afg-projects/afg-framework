package io.github.afgprojects.framework.security.resource.permission;

import org.jspecify.annotations.NonNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色校验注解。
 *
 * <p>标注在方法或类上，表示需要指定角色才能访问。
 * 从当前 JWT Token 中解析角色进行校验。
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * @RequireRole("ADMIN")
 * public void deleteUser(Long id) { ... }
 *
 * @RequireRole(value = {"ADMIN", "MANAGER"}, logical = Logical.OR)
 * public void approveRequest(Long id) { ... }
 * }</pre>
 *
 * @since 1.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /**
     * 角色标识。
     */
    @NonNull
    String[] value();

    /**
     * 逻辑关系，默认 AND。
     */
    Logical logical() default Logical.AND;
}