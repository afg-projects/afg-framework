package io.github.afgprojects.framework.ai.langchain4j.observation;

import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import io.github.afgprojects.framework.ai.core.api.observability.MetricsCollector;
import io.github.afgprojects.framework.ai.core.api.observability.Tracer;
import io.github.afgprojects.framework.ai.core.api.observability.Tracer.Span;
import io.github.afgprojects.framework.ai.core.api.observability.Tracer.SpanStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * LangChain4J ChatModelListener 适配器
 *
 * <p>将 LangChain4J 的 {@link ChatModelListener} 事件桥接到 AFG 的
 * {@link Tracer} 和 {@link MetricsCollector} 接口，实现统一的可观测性支持。
 *
 * <p>功能：
 * <ul>
 *   <li>onRequest：创建 Span，启动计时器</li>
 *   <li>onResponse：结束 Span，记录 Token 使用量和延迟</li>
 *   <li>onError：记录错误，结束 Span</li>
 * </ul>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class Lc4jObservationAdapter implements ChatModelListener {

    private static final String OPERATION_TYPE = "chat";
    private static final String SPAN_NAME = "langchain4j.chat";
    private static final String ATTR_SPAN = "afg.span";
    private static final String ATTR_TIMER = "afg.timer";
    private static final String ATTR_MODEL_NAME = "afg.modelName";

    private final Tracer tracer;
    private final MetricsCollector metricsCollector;

    @Override
    public void onRequest(@NonNull ChatModelRequestContext requestContext) {
        ChatRequest chatRequest = requestContext.chatRequest();
        ModelProvider modelProvider = requestContext.modelProvider();

        // 获取模型名称
        String modelName = extractModelName(chatRequest, modelProvider);

        // 创建 Span
        Span span = tracer.startSpan(SPAN_NAME);
        span.setAttribute("model.provider", modelProvider != null ? modelProvider.name() : "UNKNOWN");
        span.setAttribute("model.name", modelName);
        span.setAttribute("operation.type", OPERATION_TYPE);

        // 记录请求消息数量
        if (chatRequest.messages() != null) {
            span.setAttribute("request.message.count", chatRequest.messages().size());
        }

        // 启动计时器
        Map<String, String> tags = createTags(modelProvider, modelName);
        MetricsCollector.Timer timer = metricsCollector.startTimer(OPERATION_TYPE, modelName, tags);

        // 存储到 attributes 中，供后续使用
        Map<Object, Object> attributes = requestContext.attributes();
        attributes.put(ATTR_SPAN, span);
        attributes.put(ATTR_TIMER, timer);
        attributes.put(ATTR_MODEL_NAME, modelName);

        log.debug("LC4J observation started: spanId={}, modelName={}", span.getSpanId(), modelName);
    }

    @Override
    public void onResponse(@NonNull ChatModelResponseContext responseContext) {
        Map<Object, Object> attributes = responseContext.attributes();
        Span span = (Span) attributes.get(ATTR_SPAN);
        MetricsCollector.Timer timer = (MetricsCollector.Timer) attributes.get(ATTR_TIMER);
        String modelName = (String) attributes.get(ATTR_MODEL_NAME);

        if (span == null || timer == null) {
            log.warn("LC4J observation response received but no span/timer found");
            return;
        }

        ChatResponse chatResponse = responseContext.chatResponse();
        ModelProvider modelProvider = responseContext.modelProvider();

        // 记录响应信息
        if (chatResponse != null) {
            // Token 使用量
            TokenUsage tokenUsage = chatResponse.tokenUsage();
            if (tokenUsage != null) {
                long inputTokens = tokenUsage.inputTokenCount() != null ? tokenUsage.inputTokenCount() : 0L;
                long outputTokens = tokenUsage.outputTokenCount() != null ? tokenUsage.outputTokenCount() : 0L;

                span.setAttribute("token.input", inputTokens);
                span.setAttribute("token.output", outputTokens);
                span.setAttribute("token.total", tokenUsage.totalTokenCount() != null ? tokenUsage.totalTokenCount() : 0L);

                // 记录指标
                Map<String, String> tags = createTags(modelProvider, modelName);
                metricsCollector.recordTokenUsage(modelName, inputTokens, outputTokens, tags);
            }

            // 响应 ID
            if (chatResponse.id() != null) {
                span.setAttribute("response.id", chatResponse.id());
            }

            // 实际使用的模型名称
            if (chatResponse.modelName() != null) {
                span.setAttribute("response.model", chatResponse.modelName());
            }

            // Finish reason
            if (chatResponse.metadata() != null && chatResponse.metadata().finishReason() != null) {
                span.setAttribute("response.finish_reason", chatResponse.metadata().finishReason().name());
            }
        }

        // 结束 Span 和计时器
        span.setStatus(SpanStatus.OK);
        span.end();
        timer.stop("success");

        log.debug("LC4J observation completed: spanId={}, duration={}", span.getSpanId(), timer.getElapsed());
    }

    @Override
    public void onError(@NonNull ChatModelErrorContext errorContext) {
        Map<Object, Object> attributes = errorContext.attributes();
        Span span = (Span) attributes.get(ATTR_SPAN);
        MetricsCollector.Timer timer = (MetricsCollector.Timer) attributes.get(ATTR_TIMER);

        if (span == null || timer == null) {
            log.warn("LC4J observation error received but no span/timer found");
            return;
        }

        Throwable error = errorContext.error();

        // 记录异常
        if (error instanceof Exception exception) {
            span.recordException(exception);
        } else if (error != null) {
            span.setAttribute("error.type", error.getClass().getName());
            span.setAttribute("error.message", error.getMessage());
        }

        // 结束 Span 和计时器
        span.setStatus(SpanStatus.ERROR);
        span.end();
        timer.stop("failure");

        log.debug("LC4J observation error: spanId={}, error={}", span.getSpanId(), error != null ? error.getMessage() : "unknown");
    }

    /**
     * 从 ChatRequest 中提取模型名称
     */
    private String extractModelName(ChatRequest chatRequest, ModelProvider modelProvider) {
        if (chatRequest != null && chatRequest.parameters() != null) {
            String modelName = chatRequest.parameters().modelName();
            if (modelName != null && !modelName.isEmpty()) {
                return modelName;
            }
        }
        // 降级使用 provider 名称
        return modelProvider != null ? modelProvider.name().toLowerCase() : "unknown";
    }

    /**
     * 创建指标标签
     */
    private Map<String, String> createTags(ModelProvider modelProvider, String modelName) {
        Map<String, String> tags = new HashMap<>();
        tags.put("provider", modelProvider != null ? modelProvider.name() : "UNKNOWN");
        tags.put("model", modelName);
        tags.put("framework", "langchain4j");
        return tags;
    }
}
