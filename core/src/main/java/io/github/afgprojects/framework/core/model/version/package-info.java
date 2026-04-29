/**
 * API 版本管理包。
 *
 * <p>提供 API 版本管理的核心组件，用于标记和检查 API 版本。
 *
 * <h2>核心组件</h2>
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.core.model.version.ApiVersion} - 语义化版本号，支持版本比较</li>
 *   <li>{@link io.github.afgprojects.framework.core.model.version.Since} - 标记 API 引入版本</li>
 *   <li>{@link io.github.afgprojects.framework.core.model.version.DeprecatedApi} - 标记废弃 API，提供迁移指导</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 标记 API 引入版本
 * @Since("1.0.0")
 * public void newMethod() { }
 *
 * // 带说明的版本标记
 * @Since(value = "1.2.0", note = "支持批量操作")
 * public void batchProcess(List<Item> items) { }
 *
 * // 标记废弃 API
 * @DeprecatedApi(
 *     since = "1.5.0",
 *     removedIn = "2.0.0",
 *     replacement = "use newMethod() instead",
 *     reason = "性能问题，新方法使用更高效的算法"
 * )
 * public void oldMethod() { }
 * }</pre>
 *
 * <h2>版本比较</h2>
 * <pre>{@code
 * ApiVersion v1 = ApiVersion.parse("1.2.3");
 * ApiVersion v2 = ApiVersion.parse("2.0.0");
 *
 * v1.isOlderThan(v2);      // 返回 true
 * v1.isCompatibleWith(v2); // 返回 false（主版本号不同）
 * v1.isMajorChange(v2);    // 返回 true
 * }</pre>
 *
 * @see io.github.afgprojects.core.web.version Web 层版本管理（运行时版本路由）
 * @since 1.0.0
 */
package io.github.afgprojects.framework.core.model.version;
