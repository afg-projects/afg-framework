package io.github.afgprojects.framework.ai.core.pipeline;

public record SearchResult(
    String chunkId,
    String documentId,
    String documentTitle,
    String content,
    double score,
    String knowledgeBaseId,
    String knowledgeBaseName
) {}
