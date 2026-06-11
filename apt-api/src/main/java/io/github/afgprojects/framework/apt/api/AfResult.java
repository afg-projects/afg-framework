package io.github.afgprojects.framework.apt.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法返回值元数据注解。
 * <p>
 * 标注在 @AfOperation 方法上，提供返回值的元数据信息。
 * 用于文档生成和 AI 工具定义。
 *
 * <h2>基础用法</h2>
 * <pre>{@code
 * @AfOperation(name = "listUsers")
 * @AfResult(description = "用户列表", paged = true)
 * public PageData<User> listUsers() { ... }
 * }</pre>
 *
 * <h2>流式返回</h2>
 * <pre>{@code
 * @AfOperation(name = "streamChat")
 * @AfResult(description = "AI 对话流式响应", streaming = true)
 * public Flux<String> streamChat(String message) { ... }
 * }</pre>
 *
 * @see AfOperation
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface AfResult {

    /**
     * 返回值描述。
     * <p>
     * 用于文档生成和 AI 工具定义。
     *
     * @return 返回值描述，默认空字符串
     */
    String description() default "";

    /**
     * 返回值是否为分页列表。
     * <p>
     * 标记为分页时，AI 工具定义会包含分页参数和分页元数据。
     *
     * @return 是否分页，默认 false
     */
    boolean paged() default false;

    /**
     * 返回值是否为流式响应。
     * <p>
     * 标记为流式时，AI 工具定义会标记为 SSE 流式响应。
     *
     * @return 是否流式，默认 false
     */
    boolean streaming() default false;
}
