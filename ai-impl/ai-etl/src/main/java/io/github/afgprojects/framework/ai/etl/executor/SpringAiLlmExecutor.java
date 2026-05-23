package io.github.afgprojects.framework.ai.etl.executor;

import io.github.afgprojects.framework.ai.core.etl.LlmExecutor;
import io.github.afgprojects.framework.ai.core.etl.PromptTemplate;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Map;

/**
 * 基于 Spring AI 的 LLM 执行器。
 *
 * <p>使用 Spring AI 的 ChatClient 执行 LLM 调用，支持模板渲染和直接执行。
 *
 * <p><b>注意：</b>此类依赖 Spring AI，需要在项目中添加相关依赖。
 *
 * <p>使用示例：
 * <pre>{@code
 * ChatClient chatClient = ChatClient.create(chatModel);
 * LlmExecutor executor = new SpringAiLlmExecutor(chatClient);
 *
 * // 直接执行
 * String response = executor.execute("你好，请介绍一下自己");
 *
 * // 使用模板执行
 * PromptTemplate template = PromptTemplate.summarize();
 * String summary = executor.execute(template, Map.of("content", "长文本内容..."));
 * }</pre>
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public class SpringAiLlmExecutor implements LlmExecutor {

    private static final Logger log = LoggerFactory.getLogger(SpringAiLlmExecutor.class);

    private final ChatClient chatClient;

    /**
     * 创建 Spring AI LLM 执行器。
     *
     * @param chatClient Spring AI ChatClient
     */
    public SpringAiLlmExecutor(@NonNull ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public @NonNull String execute(@NonNull PromptTemplate template, @NonNull Map<String, Object> variables) {
        String prompt = template.render(variables);
        log.debug("Executing prompt template: type={}", template.getType());
        return execute(prompt);
    }

    @Override
    public @NonNull String execute(@NonNull String prompt) {
        log.debug("Executing prompt, length={}", prompt.length());

        try {
            String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

            if (response == null) {
                log.warn("LLM returned null response");
                return "";
            }

            log.debug("LLM response length: {}", response.length());
            return response;
        } catch (Exception e) {
            log.error("Failed to execute LLM prompt: {}", e.getMessage());
            throw new LlmExecutionException("Failed to execute LLM prompt", e);
        }
    }

    /**
     * LLM 执行异常。
     */
    public static class LlmExecutionException extends RuntimeException {
        public LlmExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 获取 ChatClient。
     */
    @NonNull
    public ChatClient getChatClient() {
        return chatClient;
    }
}
