package io.github.afgprojects.framework.ai.core.tool.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Tool 参数注解。
 * <p>
 * 标注在 {@link Tool} 方法的参数上，描述参数元数据，
 * 用于生成 Tool 的 inputSchema 供 AI 模型理解调用方式。
 *
 * <pre>
 * {@code
 * @Tool(name = "weather", description = "查询天气")
 * public String getWeather(
 *     @ToolParam(name = "city", description = "城市名称", required = true) String city,
 *     @ToolParam(name = "unit", description = "温度单位", defaultValue = "celsius") String unit
 * ) {
 *     // ...
 * }
 * }
 * </pre>
 *
 * @author afg-projects
 * @since 1.0.0
 * @see Tool
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ToolParam {

    /**
     * 参数名称。
     *
     * @return 参数名称
     */
    String name();

    /**
     * 参数描述，供 AI 模型理解参数含义。
     *
     * @return 参数描述
     */
    String description() default "";

    /**
     * 是否必填参数。
     *
     * @return 是否必填，默认 true
     */
    boolean required() default true;

    /**
     * 参数默认值。
     * <p>
     * 仅在参数非必填时有效。
     *
     * @return 默认值，空字符串表示无默认值
     */
    String defaultValue() default "";
}
