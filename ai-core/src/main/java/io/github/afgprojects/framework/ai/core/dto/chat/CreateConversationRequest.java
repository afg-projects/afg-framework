package io.github.afgprojects.framework.ai.core.dto.chat;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建对话请求
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Data
public class CreateConversationRequest {

    @NotBlank(message = "应用ID不能为空")
    private String applicationId;

    private String title;
}
