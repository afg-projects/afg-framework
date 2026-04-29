package io.github.afgprojects.framework.core.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AFG 配置属性注解
 * 用于标记模块配置类
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConfigurationProperties
public @interface AfgConfigProperties {

    /**
     * 配置前缀
     */
    String prefix();

    /**
     * 是否忽略未知字段
     */
    boolean ignoreUnknownFields() default true;

    /**
     * 是否忽略无效字段
     */
    boolean ignoreInvalidFields() default true;
}
