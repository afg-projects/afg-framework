package io.github.afgprojects.framework.core.autoconfigure.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * 基于功能开关的条件装配注解
 *
 * <p>根据配置中的功能开关状态决定是否装配 Bean
 * <p>功能开关配置格式: afg.feature.{featureName}.enabled={true|false}
 *
 * <h3>使用示例:</h3>
 * <pre>{@code
 * @Bean
 * @ConditionalOnFeature(feature = "cache", enabled = true)
 * public CacheService cacheService() {
 *     return new CacheService();
 * }
 * }</pre>
 *
 * <p>对应的配置:
 * <pre>
 * afg:
 *   feature:
 *     cache:
 *       enabled: true
 * </pre>
 *
 * @see OnFeatureCondition
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnFeatureCondition.class)
public @interface ConditionalOnFeature {

    /**
     * 功能名称
     * <p>
     * 对应配置项: afg.feature.{feature}.enabled
     *
     * @return 功能名称
     */
    String feature();

    /**
     * 期望的功能状态
     * <p>
     * 默认为 true，即功能启用时才装配
     *
     * @return 期望状态
     */
    boolean enabled() default true;
}
