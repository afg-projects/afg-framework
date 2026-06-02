package io.github.afgprojects.framework.ai.core.chat;

import io.github.afgprojects.framework.ai.core.api.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.api.chat.ChatClientRegistry;
import io.github.afgprojects.framework.ai.core.api.chat.AiChatResponse;
import io.github.afgprojects.framework.ai.core.chat.annotation.AiChat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * @AiChat 注解的 AOP 切面，拦截标注了 @AiChat 的方法，自动调用 AfgChatClient。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Aspect
@Slf4j
@RequiredArgsConstructor
public class AiChatAspect {

    private final ChatClientRegistry chatClientRegistry;

    @Around("@annotation(aiChat)")
    public Object aroundAiChat(ProceedingJoinPoint joinPoint, AiChat aiChat) throws Throwable {
        log.debug("AiChat aspect intercepting: {}", joinPoint.getSignature().getName());

        // 获取 ChatClient
        AfgChatClient client = chatClientRegistry.get(aiChat.client())
                .orElseGet(chatClientRegistry::getDefault);

        // 从方法参数提取用户消息
        Object[] args = joinPoint.getArgs();
        String userMessage = args.length > 0 ? String.valueOf(args[0]) : "";

        // 获取返回类型
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Class<?> returnType = method.getReturnType();
        Type genericReturnType = method.getGenericReturnType();

        // 构建请求
        AfgChatClient.ChatRequestSpec requestSpec = client.prompt(userMessage);

        // 设置系统提示词
        String systemPrompt = aiChat.systemPrompt();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            requestSpec = requestSpec.systemPrompt(systemPrompt);
        }

        // 设置对话记忆 key
        String memoryKey = aiChat.memoryKey();
        if (memoryKey != null && !memoryKey.isEmpty()) {
            requestSpec = requestSpec.conversationId(memoryKey);
        }

        // 设置额外选项
        Map<String, Object> options = buildOptions(aiChat);
        if (!options.isEmpty()) {
            requestSpec = requestSpec.options(options);
        }

        // 根据返回类型调用
        Object response;
        if (returnType == String.class) {
            // 返回字符串
            AiChatResponse chatResponse = requestSpec.call();
            response = chatResponse.content();
        } else if (returnType == AiChatResponse.class) {
            // 返回完整响应
            response = requestSpec.call();
        } else if (isReactiveType(returnType)) {
            // 流式响应
            response = requestSpec.stream();
        } else {
            // 结构化输出
            Class<?> entityType = extractEntityType(genericReturnType, returnType);
            if (entityType != null && entityType != Object.class) {
                response = requestSpec.entity(entityType);
            } else {
                // 默认返回字符串
                AiChatResponse chatResponse = requestSpec.call();
                response = chatResponse.content();
            }
        }

        return response;
    }

    /**
     * 构建额外选项
     */
    private Map<String, Object> buildOptions(AiChat aiChat) {
        Map<String, Object> options = new java.util.HashMap<>();

        if (aiChat.temperature() >= 0) {
            options.put("temperature", aiChat.temperature());
        }
        if (aiChat.maxTokens() >= 0) {
            options.put("maxTokens", aiChat.maxTokens());
        }

        return options;
    }

    /**
     * 判断是否是响应式类型
     */
    private boolean isReactiveType(Class<?> type) {
        String typeName = type.getName();
        return typeName.contains("Flux") || typeName.contains("Mono") || typeName.contains("Publisher");
    }

    /**
     * 提取泛型实体类型
     */
    @Nullable
    private Class<?> extractEntityType(Type genericType, Class<?> rawType) {
        if (genericType instanceof ParameterizedType pt) {
            Type[] typeArgs = pt.getActualTypeArguments();
            if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?> clazz) {
                return clazz;
            }
        }
        return null;
    }
}
