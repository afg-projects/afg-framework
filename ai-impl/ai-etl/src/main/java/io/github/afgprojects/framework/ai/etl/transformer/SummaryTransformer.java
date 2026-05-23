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
 * 摘要生成转换器。
 *
 * <p>使用 LLM 为文档生成摘要，适用于长文档的精简处理。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public class SummaryTransformer implements DocumentTransformer {

    private static final Logger log = LoggerFactory.getLogger(SummaryTransformer.class);

    private final LlmExecutor llmExecutor;
    private final PromptTemplate promptTemplate;
    private final boolean replaceContent;

    /**
     * 创建摘要转换器。
     *
     * @param llmExecutor LLM 执行器
     */
    public SummaryTransformer(@NonNull LlmExecutor llmExecutor) {
        this(llmExecutor, PromptTemplate.summarize(), false);
    }

    /**
     * 创建摘要转换器。
     *
     * @param llmExecutor    LLM 执行器
     * @param promptTemplate Prompt 模板
     * @param replaceContent 是否用摘要替换原内容
     */
    public SummaryTransformer(@NonNull LlmExecutor llmExecutor,
                              @NonNull PromptTemplate promptTemplate,
                              boolean replaceContent) {
        this.llmExecutor = llmExecutor;
        this.promptTemplate = promptTemplate;
        this.replaceContent = replaceContent;
    }

    @Override
    public @NonNull List<Document> transform(@NonNull List<Document> documents) {
        List<Document> result = new ArrayList<>(documents.size());

        for (Document doc : documents) {
            try {
                String summary = llmExecutor.execute(promptTemplate,
                    Map.of("content", doc.content()));

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

    @Override
    public @NonNull String getName() {
        return "SummaryTransformer";
    }

    @Override
    public int getOrder() {
        return 200;
    }
}