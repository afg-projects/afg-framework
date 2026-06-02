package io.github.afgprojects.framework.ai.core.api.pipeline;

public record SourceReference(
    String chunkId,
    String documentId,
    String documentTitle,
    String content,
    double score,
    String knowledgeBaseName
) {}
