package io.github.afgprojects.framework.ai.core.tool;

import io.github.afgprojects.framework.data.core.scope.DataScope;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * 安全工具接口。
 *
 * <p>扩展 {@link Tool} 接口，增加安全相关能力：
 * <ul>
 *   <li>权限定义 - 定义调用工具所需的权限</li>
 *   <li>数据权限配置 - 定义数据查询的数据范围</li>
 *   <li>敏感操作标记 - 标记需要特殊审计的操作</li>
 *   <li>上下文感知执行 - 支持带安全上下文的执行</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * public class UserQueryTool implements SecureTool<UserQueryInput, UserQueryOutput> {
 *
 *     @Override
 *     public String name() {
 *         return "query_users";
 *     }
 *
 *     @Override
 *     public String requiredPermission() {
 *         return "user:read";
 *     }
 *
 *     @Override
 *     public DataScope getDataScope(ToolContext context) {
 *         return DataScope.of("sys_user", "dept_id", DataScopeType.DEPT_AND_CHILD);
 *     }
 *
 *     @Override
 *     public UserQueryOutput execute(UserQueryInput input, ToolContext context) {
 *         // 带上下文执行
 *     }
 * }
 * }</pre>
 *
 * @param <I> 输入类型
 * @param <O> 输出类型
 * @since 1.0.0
 */
public interface SecureTool<I, O> extends Tool<I, O> {

    /**
     * 获取工具所需的权限。
     *
     * <p>返回调用此工具所需的权限标识。
     * 权限格式为 "resource:action"，例如：
     * <ul>
     *   <li>"user:read" - 用户读取权限</li>
     *   <li>"order:create" - 订单创建权限</li>
     *   <li>"system:admin" - 系统管理权限</li>
     * </ul>
     *
     * @return 权限标识，null 表示无需特定权限
     */
    @Nullable
    default String requiredPermission() {
        return null;
    }

    /**
     * 获取工具所需的角色。
     *
     * <p>返回调用此工具所需的角色列表。
     * 多个角色之间是 OR 关系（满足任一即可）。
     *
     * @return 角色集合，空表示无需特定角色
     */
    @NonNull
    default Set<String> requiredRoles() {
        return Set.of();
    }

    /**
     * 判断是否为敏感操作。
     *
     * <p>敏感操作会触发更严格的审计和审批流程：
     * <ul>
     *   <li>记录完整的输入输出</li>
     *   <li>可能需要二次确认</li>
     *   <li>审计日志保留更长时间</li>
     * </ul>
     *
     * @return 如果是敏感操作返回 true
     */
    default boolean isSensitive() {
        return false;
    }

    /**
     * 判断是否需要审计日志。
     *
     * <p>默认所有工具都需要审计，敏感工具会记录更详细的信息。
     *
     * @return 如果需要审计返回 true
     */
    default boolean isAuditable() {
        return true;
    }

    /**
     * 获取数据权限配置。
     *
     * <p>用于数据查询工具，定义数据权限范围。
     * 返回的 DataScope 会被应用到查询中。
     *
     * @param context 工具上下文
     * @return 数据权限配置，null 表示不应用数据权限
     */
    @Nullable
    default DataScope getDataScope(@NonNull ToolContext context) {
        return null;
    }

    /**
     * 带上下文执行工具。
     *
     * <p>这是 SecureTool 的主要执行方法。
     * 实现类应该在此方法中：
     * <ul>
     *   <li>使用 context 中的用户信息进行权限判断</li>
     *   <li>使用 context 中的租户信息进行数据隔离</li>
     *   <li>记录必要的审计信息</li>
     * </ul>
     *
     * @param input   输入参数
     * @param context 工具上下文
     * @return 执行结果
     * @throws ToolExecutionException 执行失败
     */
    O execute(I input, @NonNull ToolContext context);

    /**
     * 默认执行方法（无上下文）。
     *
     * <p>向后兼容，使用空上下文执行。
     * 不推荐直接调用，应使用 {@link #execute(Object, ToolContext)}。
     *
     * @param input 输入参数
     * @return 执行结果
     */
    @Override
    default O execute(I input) {
        return execute(input, ToolContext.empty());
    }

    /**
     * 输入参数校验。
     *
     * <p>在执行前校验输入参数的合法性。
     * 校验失败应抛出 {@link ToolValidationException}。
     *
     * @param input   输入参数
     * @param context 工具上下文
     * @throws ToolValidationException 校验失败
     */
    default void validate(I input, @NonNull ToolContext context) {
        // 默认不校验
    }

    /**
     * 输出结果过滤。
     *
     * <p>根据用户权限过滤输出结果中的敏感数据。
     * 例如：普通用户看不到手机号、身份证等敏感字段。
     *
     * @param output  原始输出
     * @param context 工具上下文
     * @return 过滤后的输出
     */
    @Nullable
    default O filterOutput(@Nullable O output, @NonNull ToolContext context) {
        return output;
    }

    /**
     * 获取工具调用参数。
     *
     * <p>某些工具可能需要从上下文中获取额外的调用参数。
     *
     * @param context 工具上下文
     * @return 调用参数
     */
    @NonNull
    default Map<String, Object> getCallParameters(@NonNull ToolContext context) {
        return Map.of();
    }

    /**
     * 判断是否允许在给定上下文中执行。
     *
     * <p>提供更细粒度的执行控制。
     * 例如：某些工具只能在特定租户下执行。
     *
     * @param context 工具上下文
     * @return 如果允许执行返回 true
     */
    default boolean canExecute(@NonNull ToolContext context) {
        return true;
    }
}
