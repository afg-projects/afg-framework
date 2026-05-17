package io.github.afgprojects.framework.ai.agent.tool.data.example;

import org.jspecify.annotations.Nullable;

/**
 * 用户查询输入参数。
 *
 * @param deptId   部门 ID
 * @param status   用户状态
 * @param keyword  搜索关键词
 * @param pageNum  页码
 * @param pageSize 每页数量
 */
public record UserQueryInput(
    @Nullable String deptId,
    @Nullable Integer status,
    @Nullable String keyword,
    @Nullable Integer pageNum,
    @Nullable Integer pageSize
) {
    /**
     * 创建默认输入。
     */
    public static UserQueryInput empty() {
        return new UserQueryInput(null, null, null, 1, 10);
    }

    /**
     * 创建按部门查询的输入。
     */
    public static UserQueryInput byDept(String deptId) {
        return new UserQueryInput(deptId, null, null, 1, 10);
    }

    /**
     * 创建按关键词搜索的输入。
     */
    public static UserQueryInput byKeyword(String keyword) {
        return new UserQueryInput(null, null, keyword, 1, 10);
    }
}
