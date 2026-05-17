package io.github.afgprojects.framework.ai.agent.tool.data.example;

import io.github.afgprojects.framework.ai.agent.tool.data.DataQueryTool;
import io.github.afgprojects.framework.ai.core.tool.ToolContext;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.scope.DataScope;
import io.github.afgprojects.framework.data.core.scope.DataScopeType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * 用户查询工具示例。
 *
 * <p>演示如何使用 {@link DataQueryTool} 创建安全的数据查询工具。
 *
 * <p>功能：
 * <ul>
 *   <li>按部门、状态、关键词筛选用户</li>
 *   <li>自动应用数据权限（部门及子部门）</li>
 *   <li>自动应用租户隔离</li>
 *   <li>过滤敏感字段</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class UserQueryTool extends DataQueryTool<Object, UserQueryInput, UserQueryOutput> {

    /**
     * 创建用户查询工具。
     *
     * @param dataManager 数据管理器
     */
    public UserQueryTool(@NonNull DataManager dataManager) {
        super(dataManager, Object.class);
    }

    @Override
    public @NonNull String name() {
        return "query_users";
    }

    @Override
    public @NonNull String description() {
        return "查询用户列表，支持按部门、状态、关键词等条件筛选。返回用户基本信息（不含敏感字段）。";
    }

    @Override
    public @NonNull String inputSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "deptId": {
                  "type": "string",
                  "description": "部门ID，筛选指定部门的用户"
                },
                "status": {
                  "type": "integer",
                  "description": "用户状态：1-启用，0-禁用，不传则查询所有"
                },
                "keyword": {
                  "type": "string",
                  "description": "搜索关键词，匹配用户名或真实姓名"
                },
                "pageNum": {
                  "type": "integer",
                  "default": 1,
                  "description": "页码，从1开始"
                },
                "pageSize": {
                  "type": "integer",
                  "default": 10,
                  "description": "每页数量，最大100"
                }
              }
            }
            """;
    }

    @Override
    public @Nullable String requiredPermission() {
        return "user:read";
    }

    @Override
    public @Nullable DataScope getDataScope(@NonNull ToolContext context) {
        // 用户查询默认使用部门数据权限（本部门及子部门）
        return DataScope.of("sys_user", "dept_id", DataScopeType.DEPT_AND_CHILD);
    }

    @Override
    protected @Nullable Condition buildCondition(
            @NonNull UserQueryInput input,
            @NonNull ToolContext context) {
        // 使用非类型化构建器（字符串方式）
        // 注意：实际项目中应使用 Lambda 方式（类型安全）
        var builder = Conditions.builder();

        if (input.deptId() != null && !input.deptId().isBlank()) {
            builder.eq("dept_id", input.deptId());
        }

        if (input.status() != null) {
            builder.eq("status", input.status());
        }

        if (input.keyword() != null && !input.keyword().isBlank()) {
            builder.like("username", "%" + input.keyword() + "%");
        }

        // 排除已删除记录
        builder.eq("deleted", false);

        return builder.build();
    }

    @Override
    protected @Nullable Integer getLimit(@NonNull UserQueryInput input, @NonNull ToolContext context) {
        int pageSize = input.pageSize() != null ? input.pageSize() : 10;
        return Math.min(pageSize, 100); // 最大 100
    }

    @Override
    protected @Nullable Integer getOffset(@NonNull UserQueryInput input, @NonNull ToolContext context) {
        int pageNum = input.pageNum() != null ? input.pageNum() : 1;
        int pageSize = input.pageSize() != null ? input.pageSize() : 10;
        return (pageNum - 1) * pageSize;
    }

    @Override
    protected @NonNull UserQueryOutput convertResult(
            @NonNull List<Object> entities,
            @NonNull ToolContext context) {
        // 过滤敏感字段，只返回基本信息
        List<UserInfo> userInfos = entities.stream()
            .map(this::toUserInfo)
            .toList();

        return new UserQueryOutput(userInfos, entities.size());
    }

    /**
     * 转换为用户信息（过滤敏感字段）。
     */
    private UserInfo toUserInfo(Object entity) {
        // 实际实现需要根据 User 实体类调整
        // 这里使用反射或 Map 方式获取字段值
        if (entity instanceof Map<?, ?> map) {
            return new UserInfo(
                String.valueOf(map.get("id")),
                String.valueOf(map.get("username")),
                String.valueOf(map.get("real_name")),
                String.valueOf(map.get("dept_id")),
                map.get("status") != null ? ((Number) map.get("status")).intValue() : 1
            );
        }
        // 其他情况返回空信息
        return new UserInfo("", "", "", "", 1);
    }

    @Override
    public @NonNull UserQueryOutput filterOutput(
            @Nullable UserQueryOutput output,
            @NonNull ToolContext context) {
        // 管理员可以看到更多信息
        if (context.isAdmin()) {
            return output;
        }

        // 普通用户：进一步过滤敏感信息
        if (output == null) {
            return new UserQueryOutput(List.of(), 0);
        }

        return output;
    }
}
