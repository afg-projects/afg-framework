package io.github.afgprojects.framework.core.pmd;

/**
 * Controller API 文档规则测试说明
 * <p>
 * 此文件用于说明如何测试 PMD 自定义规则。
 * 规则要求 Controller 方法使用 SpringDoc OpenAPI 注解进行 API 文档说明。
 *
 * <h2>测试方法</h2>
 *
 * <h3>方法一：运行 PMD 任务</h3>
 * <pre>
 * ./gradlew pmdMain
 * </pre>
 *
 * <h3>方法二：使用 PMD 命令行</h3>
 * <pre>
 * pmd -d src/main/java -R config/pmd/pmd-ruleset.xml -f text
 * </pre>
 *
 * <h3>方法三：在 IDE 中配置 PMD 插件</h3>
 * 配置 IDE 的 PMD 插件使用项目的 ruleset.xml 文件。
 *
 * <h2>规则说明</h2>
 *
 * <h3>ControllerApiOperationDoc</h3>
 * 检查 Controller 类中 public 方法是否有 @Operation 注解。
 *
 * <h3>ControllerApiParameterDoc</h3>
 * 检查 Controller 方法的参数是否有 @Parameter 注解或 @PathVariable/@RequestParam 的 description 属性。
 *
 * <h3>ControllerApiResponseDoc</h3>
 * 检查 Controller 方法是否有 @ApiResponse 或 @ApiResponses 注解。
 *
 * <h2>正确示例</h2>
 * <pre>{@code
 * @RestController
 * @RequestMapping("/users")
 * @Tag(name = "用户管理", description = "用户相关接口")
 * public class UserController {
 *
 *     @Operation(
 *         summary = "根据ID查询用户",
 *         description = "返回指定用户的详细信息"
 *     )
 *     @ApiResponse(
 *         responseCode = "200",
 *         description = "查询成功"
 *     )
 *     @GetMapping("/{id}")
 *     public Result<User> getUser(
 *         @Parameter(description = "用户ID") @PathVariable Long id
 *     ) {
 *         return userService.getById(id);
 *     }
 * }
 * }</pre>
 *
 * <h2>错误示例（会触发违规）</h2>
 * <pre>{@code
 * @RestController
 * @RequestMapping("/users")
 * public class UserController {
 *
 *     // 错误：缺少 @Operation 注解
 *     @GetMapping("/{id}")
 *     public Result<User> getUser(@PathVariable Long id) {
 *         return userService.getById(id);
 *     }
 *
 *     // 错误：参数缺少 @Parameter 注解
 *     @Operation(summary = "查询用户")
 *     @GetMapping("/{id}")
 *     public Result<User> getUser2(@PathVariable Long id) {
 *         return userService.getById(id);
 *     }
 *
 *     // 错误：缺少 @ApiResponse 注解
 *     @Operation(summary = "查询用户")
 *     @GetMapping("/{id}")
 *     public Result<User> getUser3(
 *         @Parameter(description = "用户ID") @PathVariable Long id
 *     ) {
 *         return userService.getById(id);
 *     }
 * }
 * }</pre>
 *
 * <h2>依赖配置</h2>
 * <p>
 * 需要添加 SpringDoc OpenAPI 依赖：
 * <pre>
 * implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6")
 * </pre>
 *
 * @since 1.0.0
 */
public final class ControllerApiDocRuleTestInfo {

    private ControllerApiDocRuleTestInfo() {
        // 工具类不允许实例化
    }
}
