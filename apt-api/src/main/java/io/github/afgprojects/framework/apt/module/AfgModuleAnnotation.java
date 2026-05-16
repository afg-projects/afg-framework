package io.github.afgprojects.framework.apt.module;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AliasFor;

/**
 * AFG 模块注解
 *
 * 用于标识一个模块，自动注册到模块注册表，并自动扫描模块包下的组件
 *
 * 使用示例：
 * <pre>
 * {@literal @}AfgModuleAnnotation(
 *     name = "认证授权模块",
 *     dependencies = {"system"}
 * )
 * public class AuthModuleConfig {
 *     // 模块配置
 * }
 * </pre>
 *
 * 默认值：
 * - id: 包名最后一部分（如 io.github.afgprojects.auth -> auth）
 * - basePackage: 注解所在类的包名
 * - contextPath: /{id}-api（如 /auth-api）
 *
 * 注意：框架会自动扫描并导入所有带有此注解的配置类，无需在主应用中手动配置 @ComponentScan
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Configuration
@ComponentScan
public @interface AfgModuleAnnotation {

    /**
     * 模块唯一标识
     */
    @AliasFor(attribute = "value")
    String id() default "";

    /**
     * 模块唯一标识（别名）
     */
    @AliasFor(attribute = "id")
    String value() default "";

    /**
     * 模块显示名称
     */
    String name() default "";

    /**
     * 模块基础包名
     * 用于自动为该包下的 Controller 添加 contextPath 前缀
     * 同时也用于组件扫描
     * 默认使用注解所在类的包名
     */
    @AliasFor(annotation = ComponentScan.class, attribute = "basePackages")
    String basePackage() default "";

    /**
     * 模块上下文路径
     * 用于 Web MVC 路径映射前缀
     */
    String contextPath() default "";

    /**
     * 依赖的模块ID列表
     */
    String[] dependencies() default {};

    /**
     * 模块版本
     */
    String version() default "1.0.0";

    /**
     * 模块描述
     */
    String description() default "";

    /**
     * 模块配置文件名
     * 默认使用 module-{moduleId}.yml
     * 例如: "module-auth.yml"
     */
    String configFile() default "";
}