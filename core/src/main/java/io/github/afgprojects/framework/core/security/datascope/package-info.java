/**
 * 数据权限模块
 * <p>
 * 提供行级数据权限控制能力，支持：
 * <ul>
 *   <li>部门级权限（本部门、本部门及子部门）</li>
 *   <li>用户级权限（仅本人数据）</li>
 *   <li>自定义权限条件</li>
 *   <li>MyBatis-Plus 集成（自动注入 SQL 条件）</li>
 * </ul>
 *
 * @see io.github.afgprojects.framework.core.security.datascope.DataScope
 * @see io.github.afgprojects.framework.core.security.datascope.DataScopeContext
 * @see io.github.afgprojects.framework.core.security.datascope.DataScopeContextHolder
 */
package io.github.afgprojects.framework.core.security.datascope;