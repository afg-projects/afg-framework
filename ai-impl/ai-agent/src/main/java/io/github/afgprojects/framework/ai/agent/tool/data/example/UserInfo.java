package io.github.afgprojects.framework.ai.agent.tool.data.example;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 用户基本信息（不含敏感字段）。
 *
 * <p>用于 AI 工具返回，已过滤敏感字段如手机号、身份证等。
 */
public record UserInfo(
    @NonNull String id,
    @NonNull String username,
    @Nullable String realName,
    @Nullable String deptId,
    int status
) {
    /**
     * 获取状态描述。
     */
    public String getStatusDesc() {
        return status == 1 ? "启用" : "禁用";
    }
}
