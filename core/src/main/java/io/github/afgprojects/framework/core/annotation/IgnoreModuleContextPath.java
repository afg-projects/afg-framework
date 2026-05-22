package io.github.afgprojects.framework.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 忽略模块 Context-Path 前缀
 *
 * <p>在 Controller 类上添加此注解，表示该 Controller 不添加模块的 context-path 前缀。
 * 适用于需要直接访问的端点，如健康检查、监控端点等。
 *
 * <p>示例：
 * <pre>
 * &#64;RestController
 * &#64;IgnoreModuleContextPath
 * &#64;RequestMapping("/health")
 * public class HealthController {
 *     // 路径为 /health，不会添加模块前缀
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IgnoreModuleContextPath {
}
