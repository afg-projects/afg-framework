package io.github.afgprojects.framework.ai.spring.memory;

import io.github.afgprojects.framework.ai.core.api.chat.AiMedia;
import io.github.afgprojects.framework.ai.core.api.chat.AiMessage;
import io.github.afgprojects.framework.ai.core.api.chat.AiRole;
import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.util.MimeTypeUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AFG AiMessage 与 Spring AI Message 双向转换器
 *
 * @author afg-projects
 * @since 1.0.0
 */
public final class AiMessageConverter {

    private AiMessageConverter() {}

    /**
     * AFG AiMessage → Spring AI Message
     */
    public static org.springframework.ai.chat.messages.Message toSpringAi(@NonNull AiMessage msg) {
        return switch (msg.role()) {
            case SYSTEM -> new SystemMessage(msg.content());
            case USER -> {
                if (msg.media().isEmpty()) {
                    yield new UserMessage(msg.content());
                } else {
                    yield UserMessage.builder()
                        .text(msg.content())
                        .media(toSpringAiMedia(msg.media()))
                        .build();
                }
            }
            case ASSISTANT -> AssistantMessage.builder()
                .content(msg.content())
                .properties(msg.metadata())
                .build();
            case TOOL -> AssistantMessage.builder()
                .content(msg.content())
                .build();
        };
    }

    /**
     * Spring AI Message → AFG AiMessage
     */
    public static AiMessage fromSpringAi(org.springframework.ai.chat.messages.@NonNull Message msg) {
        var messageType = msg.getMessageType();
        var content = msg.getText();
        Map<String, Object> metadata;
        if (msg.getMetadata() != null) {
            metadata = new HashMap<>(msg.getMetadata());
        } else {
            metadata = new HashMap<>();
        }

        AiRole role = switch (messageType) {
            case SYSTEM -> AiRole.SYSTEM;
            case USER -> AiRole.USER;
            case ASSISTANT -> AiRole.ASSISTANT;
            case TOOL -> AiRole.TOOL;
        };

        List<AiMedia> mediaList = List.of();
        if (msg instanceof org.springframework.ai.content.MediaContent mediaContent
                && mediaContent.getMedia() != null) {
            mediaList = fromSpringAiMedia(mediaContent.getMedia());
        }

        return new AiMessage(role, content, mediaList, Map.copyOf(metadata));
    }

    /**
     * AFG AiMedia 列表 → Spring AI Media 列表
     */
    public static List<Media> toSpringAiMedia(@NonNull List<AiMedia> mediaList) {
        var result = new ArrayList<Media>(mediaList.size());
        for (AiMedia aiMedia : mediaList) {
            result.add(toSpringAiMedia(aiMedia));
        }
        return result;
    }

    /**
     * AFG AiMedia → Spring AI Media
     */
    public static Media toSpringAiMedia(@NonNull AiMedia aiMedia) {
        var mimeType = MimeTypeUtils.parseMimeType(aiMedia.mimeType());

        if (aiMedia.url() != null) {
            return new Media(mimeType, URI.create(aiMedia.url()));
        }

        if (aiMedia.data() != null) {
            return Media.builder()
                .mimeType(mimeType)
                .data(aiMedia.data())
                .build();
        }

        throw new BusinessException(CommonErrorCode.PARAM_ERROR, "AiMedia must have either url or data: " + aiMedia);
    }

    /**
     * Spring AI Media 列表 → AFG AiMedia 列表
     */
    public static List<AiMedia> fromSpringAiMedia(@NonNull List<Media> mediaList) {
        var result = new ArrayList<AiMedia>(mediaList.size());
        for (Media media : mediaList) {
            result.add(fromSpringAiMedia(media));
        }
        return result;
    }

    /**
     * Spring AI Media → AFG AiMedia
     */
    public static AiMedia fromSpringAiMedia(@NonNull Media media) {
        var mimeType = media.getMimeType().toString();
        var data = media.getData();

        if (data instanceof URI uri) {
            return AiMedia.imageUrl(mimeType, uri.toString());
        }
        if (data instanceof byte[] bytes) {
            return AiMedia.imageBytes(mimeType, bytes);
        }

        if (data != null) {
            return AiMedia.imageUrl(mimeType, data.toString());
        }

        return AiMedia.imageUrl(mimeType, "");
    }

    /**
     * 批量转换 AFG AiMessage 列表 → Spring AI Message 列表
     */
    public static List<org.springframework.ai.chat.messages.Message> toSpringAiMessages(
            @NonNull List<AiMessage> messages) {
        var result = new ArrayList<org.springframework.ai.chat.messages.Message>(messages.size());
        for (AiMessage msg : messages) {
            result.add(toSpringAi(msg));
        }
        return result;
    }

    /**
     * 批量转换 Spring AI Message 列表 → AFG AiMessage 列表
     */
    public static List<AiMessage> fromSpringAiMessages(
            @NonNull List<org.springframework.ai.chat.messages.Message> messages) {
        var result = new ArrayList<AiMessage>(messages.size());
        for (org.springframework.ai.chat.messages.Message msg : messages) {
            result.add(fromSpringAi(msg));
        }
        return result;
    }
}
