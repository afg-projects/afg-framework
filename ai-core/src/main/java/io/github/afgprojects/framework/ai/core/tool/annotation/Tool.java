package io.github.afgprojects.framework.ai.core.tool.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明式 Tool 注解。
 * <p>
 * 标注在方法上，自动注册为 Tool 到 {@link io.github.afgprojects.framework.ai.core.api.tool.ToolRegistry}，
 * 供 AI 模型在 function calling 时调用。
 *
 * <p>与 {@link ToolExecution} 的区别：
 * <ul>
 *   <li>{@code @Tool} — 声明式工具定义注解，标注在方法上自动注册到 ToolRegistry</li>
 *   <li>{@link ToolExecution} — 工具执行审计注解，记录工具调用的生命周期事件</li>
 * </ul>
 *
 * <pre>
 * {@code
 * @Tool(name = "weather", description = "查询天气")
 * public String getWeather(@ToolParam(name = "city", description = "城市") String city) {
 *     // 调用天气 API
 * }
 * }
 * </pre>
 *
 * @author afg-projects
 * @since 1.0.0
 * @see io.github.afgprojects.framework.ai.core.api.tool.ToolRegistry
 * @see io.github.afgprojects.framework.ai.core.tool.ToolRegistrar
 * @see ToolParam
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Tool {

    /**
     * Tool 名称，在注册表中唯一标识。
     * <p>
     * 建议使用动词+名词格式，如 "get_weather"、"send_email"。
     *
     * @return Tool 名称
     */
    String name();

    /**
     * Tool 描述，用于 AI 模型理解工具的用途和调用时机。
     *
     * @return Tool 描述
     */
    String description() default "";

    /**
     * 是否异步执行。
     * <p>
     * 异步工具的执行结果通过回调机制返回，适用于耗时操作。
     *
     * @return 是否异步，默认 false
     */
    boolean async() default false;
}
