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
 * AFG 模块注册注解。
 * <p>
 * 标注在配置类上，自动注册到模块注册表，并自动扫描模块包下的组件。
 * 同时也是 {@code @Configuration} + {@code @ComponentScan} 的组合注解，
 * Spring 运行时会自动识别。
 *
 * <p>APT 处理器会在编译时生成模块索引文件 META-INF/afg-modules.index，
 * 运行时直接读取索引文件，无需扫描整个 classpath，大幅提升启动性能。
 *
 * <h2>索引文件格式</h2>
 * <pre>
 * moduleId:configFile:className:contextPath:basePackage
 * </pre>
 *
 * <h2>基础用法</h2>
 * <pre>{@code
 * @AfgModuleAnnotation(
 *     id = "auth",
 *     name = "认证授权模块",
 *     contextPath = "/auth-api",
 *     dependencies = {"system"}
 * )
 * public class AuthModuleConfig {
 *     // 模块配置
 * }
 * }</pre>
 *
 * <h2>默认值规则</h2>
 * <ul>
 *   <li>id: 包名最后一部分（如 io.github.afgprojects.auth → auth）</li>
 *   <li>basePackage: 注解所在类的包名</li>
 *   <li>contextPath: /{id}-api（如 /auth-api）</li>
 *   <li>configFile: module-{id}.yml（如 module-auth.yml）</li>
 * </ul>
 *
 * <p>注意：框架会自动扫描并导入所有带有此注解的配置类，无需在主应用中手动配置 @ComponentScan。
 * <p>注意：这是唯一 RUNTIME 级别的 APT 注解，因为 @Configuration + @ComponentScan 需要 Spring 运行时识别。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Configuration
@ComponentScan
public @interface AfgModuleAnnotation {

    /**
     * 模块唯一标识。
     * <p>
     * 用于模块注册和依赖引用。如果为空，默认使用包名最后一部分。
     *
     * <p>示例：id = "auth"
     */
    @AliasFor(attribute = "value")
    String id() default "";

    /**
     * 模块唯一标识（别名）。
     * <p>
     * 与 id 属性互为别名，使用 value 或 id 效果相同。
     *
     * <p>示例：
     * <pre>{@code
     * @AfgModuleAnnotation("auth")  // 等同于 id = "auth"
     * }</pre>
     */
    @AliasFor(attribute = "id")
    String value() default "";

    /**
     * 模块显示名称。
     * <p>
     * 用于管理界面和文档展示。
     *
     * <p>示例：name = "认证授权模块"
     */
    String name() default "";

    /**
     * 模块基础包名。
     * <p>
     * 用于自动为该包下的 Controller 添加 contextPath 前缀，同时也用于组件扫描。
     * 默认使用注解所在类的包名。
     *
     * <p>示例：basePackage = "io.github.afgprojects.auth"
     */
    @AliasFor(annotation = ComponentScan.class, attribute = "basePackages")
    String basePackage() default "";

    /**
     * 模块上下文路径。
     * <p>
     * 用于 Web MVC 路径映射前缀。如果为空，默认为 /{id}-api。
     *
     * <p>示例：contextPath = "/auth-api"
     *
     * <p>完整路径 = 应用 Context Path + 模块 Context Path + Controller 路径
     */
    String contextPath() default "";

    /**
     * 依赖的模块 ID 列表。
     * <p>
     * 声明此模块依赖的其他模块，框架会按依赖顺序初始化模块。
     *
     * <p>示例：dependencies = {"system", "data"}
     */
    String[] dependencies() default {};

    /**
     * 模块版本。
     * <p>
     * 用于模块兼容性检查和文档展示。
     *
     * @return 版本号，默认 "1.0.0"
     */
    String version() default "1.0.0";

    /**
     * 模块描述。
     * <p>
     * 用于管理界面和文档展示。
     *
     * <p>示例：description = "提供用户认证、授权、Token 管理等功能"
     */
    String description() default "";

    /**
     * 模块配置文件名。
     * <p>
     * 默认使用 module-{moduleId}.yml。例如：configFile = "module-auth.yml"
     *
     * @return 配置文件名，默认空字符串表示使用默认规则
     */
    String configFile() default "";
}
