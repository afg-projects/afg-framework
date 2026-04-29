package io.github.afgprojects.framework.core.autoconfigure.condition;

import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * 功能开关条件判断实现
 *
 * <p>检查配置项 afg.feature.{feature}.enabled 的值是否符合预期
 *
 * @see ConditionalOnFeature
 */
public class OnFeatureCondition implements Condition {

    /**
     * 功能开关配置前缀
     */
    private static final String FEATURE_PREFIX = "afg.feature.";

    /**
     * 功能开关后缀
     */
    private static final String ENABLED_SUFFIX = ".enabled";

    @Override
    public boolean matches(@NonNull ConditionContext context, @NonNull AnnotatedTypeMetadata metadata) {
        // 获取注解属性
        var attributes = metadata.getAnnotationAttributes(ConditionalOnFeature.class.getName());
        if (attributes == null) {
            return false;
        }

        String feature = (String) attributes.get("feature");
        Boolean expectedEnabled = (Boolean) attributes.get("enabled");

        if (feature == null || feature.isEmpty()) {
            return false;
        }

        // 构建配置属性名: afg.feature.{feature}.enabled
        String property = FEATURE_PREFIX + feature + ENABLED_SUFFIX;

        // 获取配置值
        String enabledValue = context.getEnvironment().getProperty(property);

        // 配置不存在时默认不匹配
        if (enabledValue == null || enabledValue.isEmpty()) {
            return false;
        }

        // 解析布尔值并比较
        boolean actualEnabled = Boolean.parseBoolean(enabledValue);
        return actualEnabled == expectedEnabled;
    }
}