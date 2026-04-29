/**
 * API 版本管理包
 *
 * <p>提供 API 版本管理功能，支持运行时版本路由。
 *
 * <h2>核心组件</h2>
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.core.web.version.ApiVersion} - 版本标记注解</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.version.ApiVersionInfo} - 版本信息</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.version.ApiVersionResolver} - 版本解析器</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.version.ApiVersionInterceptor} - 版本拦截器</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.version.ApiVersionRequestMappingHandlerMapping} - 版本路由映射</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.version.ApiVersionRequestCondition} - 版本请求条件</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.version.ApiVersionProperties} - 配置属性</li>
 * </ul>
 *
 * <h2>使用方式</h2>
 * <p>在 Controller 类或方法上标注 {@code @ApiVersion} 注解：
 * <pre>{@code
 * @RestController
 * @ApiVersion("1.0")
 * @RequestMapping("/users")
 * public class UserApiV1 {
 *     @GetMapping
 *     public List<User> getUsers() { ... }
 * }
 * }</pre>
 *
 * <h2>版本解析策略</h2>
 * <p>支持三种版本指定方式（按优先级）：
 * <ol>
 *   <li>Header: X-API-Version=1.0</li>
 *   <li>URL: /v1/users</li>
 *   <li>Parameter: ?version=1.0</li>
 * </ol>
 *
 * @since 1.0.0
 */
package io.github.afgprojects.framework.core.web.version;