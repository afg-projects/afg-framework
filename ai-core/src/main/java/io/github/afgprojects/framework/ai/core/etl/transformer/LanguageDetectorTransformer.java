package io.github.afgprojects.framework.ai.core.etl.transformer;

import io.github.afgprojects.framework.ai.core.api.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.api.rag.Document;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 语言检测转换器。
 *
 * <p>使用 LLM 检测文档内容的语言。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public class LanguageDetectorTransformer implements DocumentTransformer {

    private static final Logger log = LoggerFactory.getLogger(LanguageDetectorTransformer.class);

    private static final String DEFAULT_SYSTEM_PROMPT = """
            You are a language detection assistant. Your task is to identify the language of the given text.
            Respond with ONLY the language name in English (e.g., "English", "Chinese", "Japanese", "French", etc.).
            Do not include any explanation or additional text.
            """;

    private final AfgChatClient chatClient;
    private final String systemPrompt;

    /**
     * 创建语言检测转换器。
     *
     * @param chatClient 对话客户端
     */
    public LanguageDetectorTransformer(@NonNull AfgChatClient chatClient) {
        this(chatClient, DEFAULT_SYSTEM_PROMPT);
    }

    /**
     * 创建语言检测转换器。
     *
     * @param chatClient   对话客户端
     * @param systemPrompt 系统提示词
     */
    public LanguageDetectorTransformer(@NonNull AfgChatClient chatClient,
                                       @NonNull String systemPrompt) {
        this.chatClient = chatClient;
        this.systemPrompt = systemPrompt;
    }

    @Override
    public @NonNull List<Document> transform(@NonNull List<Document> documents) {
        List<Document> result = new ArrayList<>(documents.size());

        AfgChatClient languageClient = chatClient.withSystemPrompt(systemPrompt);

        for (Document doc : documents) {
            try {
                String language = languageClient.chat(doc.content()).content();

                // 清理语言名称
                language = language.trim();

                Document newDoc = doc.withMetadata("language", language);
                result.add(newDoc);

                log.debug("Detected language '{}' for document {}", language, doc.id());
            } catch (Exception e) {
                log.warn("Failed to detect language for document {}: {}",
                    doc.id(), e.getMessage());
                result.add(doc);
            }
        }

        log.debug("Detected language for {} documents", documents.size());
        return result;
    }

    @Override
    public @NonNull String getName() {
        return "LanguageDetectorTransformer";
    }

    @Override
    public int getOrder() {
        return 150;
    }
}