package io.github.afgprojects.framework.ai.core.properties.persistence;

import lombok.Data;

/**
 * 会话存储配置。
 */
@Data
public class SessionConfig {

    /**
     * 每用户最大会话数。
     */
    private int maxSessionsPerUser = 100;

    /**
     * 会话表名。
     */
    private String tableName = "ai_agent_session";
}
