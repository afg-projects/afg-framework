package io.github.afgprojects.framework.core.autoconfigure.condition;

import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * 属性非空条件判断实现
 *
 * <p>检查指定属性是否存在且值不为空
 *
 * @see ConditionalOnPropertyNotEmpty
 */
public class OnPropertyNotEmptyCondition implements Condition {

    @Override
    public boolean matches(@NonNull ConditionContext context, @NonNull AnnotatedTypeMetadata metadata) {
        // 获取注解属性
        var attributes = metadata.getAnnotationAttributes(ConditionalOnPropertyNotEmpty.class.getName());
        if (attributes == null) {
            return false;
        }

        String value = (String) attributes.get("value");
        String prefix = (String) attributes.get("prefix");

        if (value == null || value.isEmpty()) {
            return false;
        }

        // 构建完整的属性名
        String property = buildPropertyName(prefix, value);

        // 获取属性值
        String propertyValue = context.getEnvironment().getProperty(property);

        // 检查属性非空
        return propertyValue != null && !propertyValue.isEmpty();
    }

    /**
     * 构建属性名
     *
     * @param prefix 属性前缀
     * @param value  属性名
     * @return 完整属性名
     */
    private @NonNull String buildPropertyName(String prefix, String value) {
        if (prefix == null || prefix.isEmpty()) {
            return value;
        }

        // 确保前缀和值之间正确连接
        if (prefix.endsWith(".")) {
            return prefix + value;
        }
        return prefix + "." + value;
    }
}