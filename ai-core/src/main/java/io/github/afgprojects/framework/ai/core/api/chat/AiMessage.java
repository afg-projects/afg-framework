package io.github.afgprojects.framework.ai.core.api.chat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * AI 消息抽象 - 与具体 AI 框架解耦的消息表示
 *
 * @param role      消息角色
 * @param content   消息文本内容
 * @param media     多媒体内容列表
 * @param metadata  扩展元数据
 * @author afg-projects
 * @since 1.0.0
 */
public record AiMessage(
    @NonNull AiRole role,
    @Nullable String content,
    @NonNull List<AiMedia> media,
    @NonNull Map<String, Object> metadata
) {

    public AiMessage {
        media = media != null ? List.copyOf(media) : List.of();
        metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    public static AiMessage system(@NonNull String content) {
        return new AiMessage(AiRole.SYSTEM, content, List.of(), Map.of());
    }

    public static AiMessage user(@NonNull String content) {
        return new AiMessage(AiRole.USER, content, List.of(), Map.of());
    }

    public static AiMessage user(@NonNull String content, @NonNull List<AiMedia> media) {
        return new AiMessage(AiRole.USER, content, media, Map.of());
    }

    public static AiMessage assistant(@NonNull String content) {
        return new AiMessage(AiRole.ASSISTANT, content, List.of(), Map.of());
    }

    public static AiMessage assistant(@Nullable String content, @NonNull Map<String, Object> metadata) {
        return new AiMessage(AiRole.ASSISTANT, content, List.of(), metadata);
    }

    public static AiMessage tool(@NonNull String content, @NonNull Map<String, Object> metadata) {
        return new AiMessage(AiRole.TOOL, content, List.of(), metadata);
    }

    public boolean hasMedia() {
        return !media.isEmpty();
    }

    public List<AiMedia> getMedia() {
        return Collections.unmodifiableList(media);
    }
}