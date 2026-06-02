package io.github.afgprojects.framework.ai.core.chat.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明式 AI 对话注解。
 * <p>
 * 标注在方法上，自动调用 AfgChatClient 进行对话。
 * 方法参数和返回值将自动映射到对话请求和响应。
 *
 * <pre>
 * {@code
 * @AiChat(client = "myChatClient", systemPrompt = "你是一个助手")
 * public String ask(String question) {
 *     // 方法体不会执行，AOP 切面会拦截并调用 AI
 * }
 * }
 * </pre>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AiChat {
    /** 使用的 ChatClient 名称，默认使用 "default" */
    String client() default "default";
    /** 系统提示词 */
    String systemPrompt() default "";
    /** 对话记忆 key，为空则不使用记忆 */
    String memoryKey() default "";
    /** 温度参数，使用 -1 表示未设置 */
    double temperature() default -1;
    /** 最大 token 数，使用 -1 表示未设置 */
    int maxTokens() default -1;
}
