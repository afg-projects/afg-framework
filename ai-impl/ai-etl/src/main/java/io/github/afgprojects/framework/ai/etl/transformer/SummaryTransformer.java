package io.github.afgprojects.framework.ai.etl.transformer;

import io.github.afgprojects.framework.ai.core.api.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.api.rag.Document;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 摘要生成转换器。
 *
 * <p>使用 LLM 为文档生成摘要，适用于长文档的精简处理。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public class SummaryTransformer implements DocumentTransformer {

    private static final Logger log = LoggerFactory.getLogger(SummaryTransformer.class);

    private static final String DEFAULT_SYSTEM_PROMPT = """
            You are a summarization assistant. Your task is to generate a concise summary of the given text.
            Preserve the key information and main points while reducing the length.
            """;

    private final AfgChatClient chatClient;
    private final String systemPrompt;
    private final boolean replaceContent;

    /**
     * 创建摘要转换器。
     *
     * @param chatClient 对话客户端
     */
    public SummaryTransformer(@NonNull AfgChatClient chatClient) {
        this(chatClient, DEFAULT_SYSTEM_PROMPT, false);
    }

    /**
     * 创建摘要转换器。
     *
     * @param chatClient     对话客户端
     * @param systemPrompt   系统提示词
     * @param replaceContent 是否用摘要替换原内容
     */
    public SummaryTransformer(@NonNull AfgChatClient chatClient,
                              @NonNull String systemPrompt,
                              boolean replaceContent) {
        this.chatClient = chatClient;
        this.systemPrompt = systemPrompt;
        this.replaceContent = replaceContent;
    }

    @Override
    public @NonNull List<Document> transform(@NonNull List<Document> documents) {
        List<Document> result = new ArrayList<>(documents.size());

        AfgChatClient summaryClient = chatClient.withSystemPrompt(systemPrompt);

        for (Document doc : documents) {
            try {
                String summary = summaryClient.chat(doc.content()).content();

                String newContent = replaceContent ? summary : doc.content();
                Document newDoc = new Document(
                    doc.id(),
                    newContent,
                    doc.embedding(),
                    doc.metadata()
                ).withMetadata("summary", summary);

                result.add(newDoc);
                log.debug("Generated summary for document {}", doc.id());
            } catch (Exception e) {
                log.warn("Failed to generate summary for document {}: {}",
                    doc.id(), e.getMessage());
                // 保持原文
                result.add(doc);
            }
        }

        log.debug("Summarized {} documents", documents.size());
        return result;
    }

    public @NonNull String getName() {
        return "SummaryTransformer";
    }

    public int getOrder() {
        return 200;
    }
}