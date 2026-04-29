package io.github.afgprojects.framework.core.module;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;

/**
 * AFG 模块标记注解
 * 用于声明一个类为 AFG 模块
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface AfgModuleAnnotation {

    /**
     * 模块唯一标识
     */
    String id();

    /**
     * 模块显示名称
     */
    String name();

    /**
     * 依赖的模块ID列表
     */
    String[] dependencies() default {};

    /**
     * 模块配置类
     */
    Class<?> configClass() default Void.class;
}
