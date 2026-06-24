package io.github.afgprojects.framework.ai.core.dto.chat;

import lombok.Data;

/**
 * 对话日志查询参数
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Data
public class ChatLogQuery {

    private String applicationId;

    private String sessionId;

    private String userId;

    private Integer page = 1;

    private Integer size = 10;
}
