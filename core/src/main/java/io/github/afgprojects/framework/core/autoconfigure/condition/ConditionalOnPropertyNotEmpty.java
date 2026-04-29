package io.github.afgprojects.framework.core.autoconfigure.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * 属性非空条件装配注解
 *
 * <p>当指定的配置属性非空（存在且不为空字符串）时才装配 Bean
 *
 * <h3>使用示例:</h3>
 * <pre>{@code
 * // 单个属性
 * @Bean
 * @ConditionalOnPropertyNotEmpty("afg.database.url")
 * public DatabaseClient databaseClient() {
 *     return new DatabaseClient();
 * }
 *
 * // 带前缀的属性
 * @Bean
 * @ConditionalOnPropertyNotEmpty(value = "url", prefix = "afg.database")
 * public DatabaseClient databaseClient() {
 *     return new DatabaseClient();
 * }
 * }</pre>
 *
 * @see OnPropertyNotEmptyCondition
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnPropertyNotEmptyCondition.class)
public @interface ConditionalOnPropertyNotEmpty {

    /**
     * 属性名
     * <p>
     * 如果指定了 prefix，则完整属性名为 prefix + "." + value
     *
     * @return 属性名
     */
    String value();

    /**
     * 属性前缀
     * <p>
     * 默认为空，不添加前缀
     *
     * @return 属性前缀
     */
    String prefix() default "";
}
