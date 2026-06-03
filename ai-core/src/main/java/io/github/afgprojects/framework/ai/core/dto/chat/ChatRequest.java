package io.github.afgprojects.framework.ai.core.dto.chat;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 对话消息请求
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Data
public class ChatRequest {

    @NotBlank(message = "消息内容不能为空")
    private String message;

    private boolean stream = false;
}
