package io.github.afgprojects.framework.core.web.version;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API 版本管理使用示例
 *
 * <p>本类仅作为使用示例，不参与实际运行。
 *
 * <h2>基本用法</h2>
 *
 * <h3>1. 类级版本标记</h3>
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
 * <h3>2. 方法级版本标记</h3>
 * <pre>{@code
 * @RestController
 * @RequestMapping("/users")
 * public class UserApi {
 *     @ApiVersion("1.0")
 *     @GetMapping
 *     public List<User> getUsersV1() { ... }
 *
 *     @ApiVersion("2.0")
 *     @GetMapping
 *     public List<User> getUsersV2() { ... }
 * }
 * }</pre>
 *
 * <h3>3. 废弃 API 标记</h3>
 * <pre>{@code
 * @RestController
 * @ApiVersion(
 *     value = "1.0",
 *     deprecated = true,
 *     until = "2.0",
 *     replacement = "UserApiV2",
 *     reason = "性能优化，请使用 v2 版本"
 * )
 * @RequestMapping("/users")
 * public class UserApiV1 {
 *     // ...
 * }
 * }</pre>
 *
 * <h2>版本指定方式</h2>
 *
 * <h3>1. 通过 Header</h3>
 * <pre>{@code
 * curl -H "X-API-Version: 1.0" http://localhost:8080/users
 * }</pre>
 *
 * <h3>2. 通过 URL</h3>
 * <pre>{@code
 * curl http://localhost:8080/v1/users
 * curl http://localhost:8080/v1.5/users
 * }</pre>
 *
 * <h3>3. 通过参数</h3>
 * <pre>{@code
 * curl http://localhost:8080/users?version=1.0
 * }</pre>
 *
 * <h2>配置</h2>
 * <pre>{@code
 * # application.yml
 * afg:
 *   api-version:
 *     enabled: true
 *     default-version: "1.0.0"
 *     header-name: "X-API-Version"
 *     parameter-name: "version"
 *     url-prefix: "/v"
 *     resolution-order:
 *       - HEADER
 *       - URL
 *       - PARAMETER
 *     deprecation:
 *       enabled: true
 *       warning-header: "X-API-Deprecated"
 *       log-deprecation: true
 * }</pre>
 *
 * @since 1.0.0
 */
@SuppressWarnings("unused")
public class ApiVersionExample {

    /**
     * v1 版本用户 API
     */
    @RestController
    @ApiVersion("1.0")
    @RequestMapping("/users")
    static class UserApiV1 {

        @GetMapping
        public List<String> getUsers() {
            return List.of("User V1");
        }
    }

    /**
     * v2 版本用户 API
     */
    @RestController
    @ApiVersion("2.0")
    @RequestMapping("/users")
    static class UserApiV2 {

        @GetMapping
        public List<String> getUsers() {
            return List.of("User V2");
        }
    }

    /**
     * 废弃的 API 示例
     */
    @RestController
    @ApiVersion(
            value = "1.5",
            deprecated = true,
            until = "2.0",
            replacement = "UserApiV2",
            reason = "性能优化")
    @RequestMapping("/legacy")
    static class LegacyApi {

        @GetMapping
        public String getData() {
            return "legacy data";
        }
    }
}