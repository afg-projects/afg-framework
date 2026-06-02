package io.github.afgprojects.framework.ai.core.agent.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明式 AI Agent 注解。
 * <p>
 * 标注在方法上，自动创建并执行 AI Agent。
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AiAgent {
    /** Agent 名称 */
    String value() default "";
    /** 最大迭代次数 */
    int maxIterations() default 10;
    /** 超时时间（毫秒） */
    long timeoutMs() default 30000;
    /** 使用的 ChatClient */
    String chatClient() default "default";
}
