package io.github.afgprojects.framework.core.feature;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 功能开关注解
 * <p>
 * 标注在方法上，通过功能开关控制方法的执行。
 * 当功能关闭时，可以执行回退方法或抛出异常。
 * </p>
 *
 * <pre>{@code
 * @FeatureToggle(feature = "new-feature", enabledByDefault = true)
 * public Result<String> newFeature() {
 *     // 新功能逻辑
 * }
 *
 * @FeatureToggle(feature = "new-feature", fallbackMethod = "fallbackMethod")
 * public Result<String> newFeatureWithFallback() {
 *     // 新功能逻辑
 * }
 *
 * public Result<String> fallbackMethod() {
 *     // 回退逻辑
 *     return Results.success("fallback");
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FeatureToggle {

    /**
     * 功能名称
     * <p>
     * 用于在配置中标识功能开关的唯一标识符
     * </p>
     *
     * @return 功能名称
     */
    String feature();

    /**
     * 默认是否启用
     * <p>
     * 当配置中心不可用或未配置时，使用此默认值
     * </p>
     *
     * @return 默认是否启用，默认为 true
     */
    boolean enabledByDefault() default true;

    /**
     * 回退方法名称
     * <p>
     * 当功能关闭时执行的回退方法。回退方法必须满足以下条件：
     * <ul>
     *     <li>与原方法在同一个类中</li>
     *     <li>具有相同的参数列表</li>
     *     <li>具有相同或兼容的返回类型</li>
     * </ul>
     * 如果为空，功能关闭时将抛出 {@link FeatureDisabledException}
     * </p>
     *
     * @return 回退方法名称，默认为空
     */
    String fallbackMethod() default "";
}