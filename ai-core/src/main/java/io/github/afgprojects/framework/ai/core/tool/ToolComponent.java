package io.github.afgprojects.framework.ai.core.tool;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Annotation to mark a class as a Tool component.
 * <p>
 * Classes annotated with @ToolComponent will be automatically
 * registered in the ToolRegistry by Spring's component scanning.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>
 * &#64;ToolComponent(name = "weather", description = "Get weather information")
 * public class WeatherTool implements Tool&lt;WeatherRequest, WeatherResponse&gt; {
 *     // implementation
 * }
 * </pre>
 *
 * @author AFG Projects
 * @since 1.0.0
 * @see Tool
 * @see ToolRegistry
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface ToolComponent {

    /**
     * The name of the tool.
     * <p>
     * If not specified, the bean name will be derived from the class name.
     * </p>
     *
     * @return the tool name
     */
    String name() default "";

    /**
     * A description of what the tool does.
     * <p>
     * This description is used by AI models to understand when
     * and how to use the tool.
     * </p>
     *
     * @return the tool description
     */
    String description() default "";

    /**
     * The value may indicate a suggestion for a logical component name,
     * to be turned into a Spring bean in case of an autodetected component.
     *
     * @return the suggested component name
     */
    String value() default "";
}
