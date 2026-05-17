package io.github.afgprojects.framework.ai.agent.tool.data.example;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 用户查询输出结果。
 *
 * @param users   用户列表
 * @param total   总数量
 */
public record UserQueryOutput(
    @NonNull List<UserInfo> users,
    long total
) {
    /**
     * 创建空结果。
     */
    public static UserQueryOutput empty() {
        return new UserQueryOutput(List.of(), 0);
    }
}
