package io.github.afgprojects.framework.core.security.datascope;

import org.jspecify.annotations.Nullable;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 数据权限上下文提供者接口
 * <p>
 * 用于自定义数据权限上下文的构建逻辑。
 * 实现此接口可以：
 * <ul>
 *   <li>从自定义数据源加载用户的部门信息</li>
 *   <li>查询用户可访问的部门列表（用于 DEPT_AND_CHILD 类型）</li>
 *   <li>设置自定义权限条件</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>
 * {@code @Bean}
 * public DataScopeContextProvider dataScopeContextProvider(DeptService deptService) {
 *     return request -> {
 *         Long userId = getCurrentUserId();
 *         Long deptId = deptService.getUserDeptId(userId);
 *         Set&lt;Long&gt; childDeptIds = deptService.getChildDeptIds(deptId);
 *
 *         return DataScopeContext.builder()
 *             .userId(userId)
 *             .deptId(deptId)
 *             .accessibleDeptIds(childDeptIds)
 *             .build();
 *     };
 * }
 * </pre>
 */
@FunctionalInterface
public interface DataScopeContextProvider {

    /**
     * 提供数据权限上下文
     * <p>
     * 在每个请求开始时调用，用于构建当前用户的数据权限上下文。
     *
     * @param request HTTP 请求
     * @return 数据权限上下文，如果返回 null 则使用默认上下文
     */
    @Nullable DataScopeContext provide(HttpServletRequest request);
}