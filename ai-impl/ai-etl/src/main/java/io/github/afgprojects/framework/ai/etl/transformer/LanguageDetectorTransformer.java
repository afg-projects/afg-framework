package io.github.afgprojects.framework.ai.etl.transformer;

import io.github.afgprojects.framework.ai.core.etl.Document;
import io.github.afgprojects.framework.ai.core.etl.DocumentTransformer;
import io.github.afgprojects.framework.ai.core.etl.LlmExecutor;
import io.github.afgprojects.framework.ai.core.etl.PromptTemplate;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    private final LlmExecutor llmExecutor;
    private final PromptTemplate promptTemplate;

    /**
     * 创建语言检测转换器。
     *
     * @param llmExecutor LLM 执行器
     */
    public LanguageDetectorTransformer(@NonNull LlmExecutor llmExecutor) {
        this(llmExecutor, PromptTemplate.detectLanguage());
    }

    /**
     * 创建语言检测转换器。
     *
     * @param llmExecutor    LLM 执行器
     * @param promptTemplate Prompt 模板
     */
    public LanguageDetectorTransformer(@NonNull LlmExecutor llmExecutor,
                                       @NonNull PromptTemplate promptTemplate) {
        this.llmExecutor = llmExecutor;
        this.promptTemplate = promptTemplate;
    }

    @Override
    public @NonNull List<Document> transform(@NonNull List<Document> documents) {
        List<Document> result = new ArrayList<>(documents.size());

        for (Document doc : documents) {
            try {
                String language = llmExecutor.execute(promptTemplate,
                    Map.of("content", doc.content()));

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