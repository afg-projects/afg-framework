# 异常处理规范

> AFG Framework 统一异常体系、错误码规范与全局异常处理机制。

**PRD 来源：** §5.1 Commons 模块、§6.2 API 设计规范、§6.3 异常处理规范、附录 B CommonErrorCode 完整列表

---

## 异常体系总览

AFG Framework 采用统一的异常体系，所有业务异常必须通过 `BusinessException` 抛出，由 `GlobalExceptionHandler` 统一捕获并转换为 `Result<T>` 响应。禁止在业务代码中抛出原始 `RuntimeException`。

### 类层次结构

```
RuntimeException
  └── AfgException (abstract, @Getter)
        ├── int code                    // 数值错误码
        ├── formatCode() → "E{code}"    // 格式化错误码，如 "E10001"
        └── BusinessException (@Getter)
              ├── ErrorCode errorCode       // 错误码枚举
              ├── String businessMessage    // 业务消息
              ├── Object[] args             // i18n 消息模板参数
              ├── boolean customMessage     // 是否自定义消息（true=不走 i18n）
              └── getMessage(Locale)        // 支持 i18n 的消息获取
```

**包路径：** `io.github.afgprojects.framework.commons.exception`

---

## 核心接口与类

### ErrorCode 接口

```java
public interface ErrorCode {
    int getCode();                                    // 数值错误码，如 10001
    String getMessage();                              // 默认错误消息
    default ErrorCategory getCategory();              // 错误分类，默认 BUSINESS
    default String formatCode();                      // "E" + getCode()，如 "E10001"
    default String getMessage(@Nullable Locale locale);           // i18n 消息
    default String getMessage(@Nullable Object[] args, @Nullable Locale locale); // 带参数的 i18n 消息
}
```

### ErrorCategory 枚举

| 枚举值 | 前缀 | 含义 |
|--------|------|------|
| `BUSINESS` | `"B"` | 业务逻辑校验失败 |
| `SYSTEM` | `"S"` | 系统内部异常 |
| `NETWORK` | `"N"` | 网络通信异常 |
| `SECURITY` | `"A"` | 认证授权异常 |

### BusinessException 构造方式

```java
// 1. 仅消息（使用 CommonErrorCode.FAIL，customMessage=true）
throw new BusinessException("用户名不能为空");

// 2. 仅 ErrorCode（使用 ErrorCode 自带消息，customMessage=false，支持 i18n）
throw new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND);

// 3. ErrorCode + 自定义消息（customMessage=true，不走 i18n）
throw new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "用户不存在: " + userId);

// 4. ErrorCode + 自定义消息 + 原因异常
throw new BusinessException(CommonErrorCode.QUERY_ERROR, "查询用户失败", cause);

// 5. ErrorCode + i18n 模板参数（customMessage=false，走 i18n）
throw new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, new Object[]{"用户", userId});

// 6. ErrorCode + i18n 模板参数 + 原因异常
throw new BusinessException(CommonErrorCode.QUERY_ERROR, new Object[]{"用户表"}, cause);
```

**关键区别：** `customMessage=true` 时 `getMessage(Locale)` 直接返回 `businessMessage`，不经过 i18n 解析；`customMessage=false` 时委托 `errorCode.getMessage(args, locale)` 进行 i18n 消息模板渲染。

---

## CommonErrorCode 错误码表

框架标准错误码范围：**10000-19999**，共 44 个枚举常量 + 11 个 i18n 模板码。

### 通用 (10001-10099)

| 枚举值 | Code | 消息 | 分类 |
|--------|------|------|------|
| `FAIL` | 10001 | 操作失败 | BUSINESS |
| `PARAM_ERROR` | 10002 | 参数错误 | BUSINESS |
| `PARAM_MISSING` | 10003 | 参数缺失 | BUSINESS |
| `PARAM_FORMAT_ERROR` | 10004 | 参数格式错误 | BUSINESS |

### 资源 (10100-10199)

| 枚举值 | Code | 消息 | 分类 |
|--------|------|------|------|
| `NOT_FOUND` | 10100 | 资源不存在 | BUSINESS |
| `RESOURCE_EXISTS` | 10101 | 资源已存在 | BUSINESS |
| `RESOURCE_LOCKED` | 10102 | 资源已锁定 | BUSINESS |

### 请求 (10200-10299)

| 枚举值 | Code | 消息 | 分类 |
|--------|------|------|------|
| `METHOD_NOT_ALLOWED` | 10200 | 请求方法不支持 | BUSINESS |
| `UNSUPPORTED_MEDIA_TYPE` | 10201 | 不支持的媒体类型 | BUSINESS |
| `REQUEST_TIMEOUT` | 10202 | 请求超时 | NETWORK |
| `PAYLOAD_TOO_LARGE` | 10203 | 请求体过大 | BUSINESS |

### 限流 (10300-10399)

| 枚举值 | Code | 消息 | 分类 |
|--------|------|------|------|
| `TOO_MANY_REQUESTS` | 10300 | 请求过于频繁 | BUSINESS |
| `RATE_LIMIT_EXCEEDED` | 10301 | 超过限流阈值 | BUSINESS |
| `CIRCUIT_BREAKER_OPEN` | 10302 | 熔断器已开启 | SYSTEM |

### 认证授权 (10400-10499)

| 枚举值 | Code | 消息 | 分类 |
|--------|------|------|------|
| `UNAUTHORIZED` | 10400 | 未登录或登录已过期 | SECURITY |
| `TOKEN_EXPIRED` | 10401 | Token已过期 | SECURITY |
| `TOKEN_INVALID` | 10402 | Token无效 | SECURITY |
| `FORBIDDEN` | 10403 | 无权限访问 | SECURITY |
| `PERMISSION_DENIED` | 10404 | 权限不足 | SECURITY |
| `ACCOUNT_DISABLED` | 10405 | 账号已禁用 | SECURITY |
| `ACCOUNT_LOCKED` | 10406 | 账号已锁定 | SECURITY |
| `PASSWORD_EXPIRED` | 10407 | 密码已过期 | SECURITY |

### 数据层 (11000-11999)

| 枚举值 | Code | 消息 | 分类 |
|--------|------|------|------|
| `ENTITY_NOT_FOUND` | 11000 | 实体不存在 | BUSINESS |
| `ENTITY_ALREADY_EXISTS` | 11001 | 实体已存在 | BUSINESS |
| `FIELD_NOT_FOUND` | 11002 | 字段不存在 | BUSINESS |
| `TABLE_NOT_FOUND` | 11003 | 表不存在 | SYSTEM |
| `DDL_ERROR` | 11004 | DDL执行失败 | SYSTEM |
| `QUERY_ERROR` | 11005 | 查询执行失败 | SYSTEM |
| `DATA_INTEGRITY_VIOLATION` | 11006 | 数据完整性冲突 | BUSINESS |
| `OPTIMISTIC_LOCK_ERROR` | 11007 | 乐观锁冲突 | BUSINESS |

### 存储 (12000-12999)

| 枚举值 | Code | 消息 | 分类 |
|--------|------|------|------|
| `FILE_NOT_FOUND` | 12000 | 文件不存在 | BUSINESS |
| `FILE_UPLOAD_ERROR` | 12001 | 文件上传失败 | SYSTEM |
| `FILE_DOWNLOAD_ERROR` | 12002 | 文件下载失败 | SYSTEM |
| `FILE_TYPE_NOT_ALLOWED` | 12003 | 文件类型不允许 | BUSINESS |
| `FILE_SIZE_EXCEEDED` | 12004 | 文件大小超限 | BUSINESS |
| `STORAGE_FULL` | 12005 | 存储空间不足 | SYSTEM |

### 任务 (13000-13999)

| 枚举值 | Code | 消息 | 分类 |
|--------|------|------|------|
| `JOB_NOT_FOUND` | 13000 | 任务不存在 | BUSINESS |
| `JOB_EXECUTION_ERROR` | 13001 | 任务执行失败 | SYSTEM |
| `JOB_ALREADY_RUNNING` | 13002 | 任务已在运行中 | BUSINESS |
| `JOB_PAUSED` | 13003 | 任务已暂停 | BUSINESS |
| `JOB_DISABLED` | 13004 | 任务已禁用 | BUSINESS |

### HTTP 客户端 (14000-14999)

| 枚举值 | Code | 消息 | 分类 |
|--------|------|------|------|
| `CLIENT_REQUEST_FAILED` | 14000 | HTTP请求失败 | NETWORK |
| `CLIENT_TIMEOUT` | 14001 | HTTP请求超时 | NETWORK |
| `CLIENT_CONNECT_FAILED` | 14002 | HTTP连接失败 | NETWORK |
| `CLIENT_RETRY_EXHAUSTED` | 14003 | HTTP重试耗尽 | NETWORK |
| `CLIENT_CIRCUIT_OPEN` | 14004 | HTTP熔断器开启 | SYSTEM |

### 模块 (15000-15999)

| 枚举值 | Code | 消息 | 分类 |
|--------|------|------|------|
| `MODULE_NOT_FOUND` | 15000 | 模块不存在 | SYSTEM |
| `MODULE_DUPLICATE` | 15001 | 模块已存在 | SYSTEM |
| `MODULE_CIRCULAR_DEPENDENCY` | 15002 | 模块循环依赖 | SYSTEM |
| `MODULE_INIT_FAILED` | 15003 | 模块初始化失败 | SYSTEM |

### 配置 (16000-16999)

| 枚举值 | Code | 消息 | 分类 |
|--------|------|------|------|
| `CONFIG_NOT_FOUND` | 16000 | 配置不存在 | SYSTEM |
| `CONFIG_BINDING_ERROR` | 16001 | 配置绑定失败 | SYSTEM |

### 功能开关 (17000-17999)

| 枚举值 | Code | 消息 | 分类 |
|--------|------|------|------|
| `FEATURE_DISABLED` | 17000 | 功能已禁用 | BUSINESS |
| `FEATURE_FALLBACK_FAILED` | 17001 | 功能回退失败 | SYSTEM |

### 系统 (19000-19999)

| 枚举值 | Code | 消息 | 分类 |
|--------|------|------|------|
| `SYSTEM_ERROR` | 19000 | 系统异常 | SYSTEM |
| `INTERNAL_ERROR` | 19001 | 内部错误 | SYSTEM |
| `SERVICE_UNAVAILABLE` | 19002 | 服务不可用 | SYSTEM |
| `DEPENDENCY_ERROR` | 19003 | 依赖服务异常 | SYSTEM |
| `CONFIG_ERROR` | 19004 | 配置错误 | SYSTEM |

### i18n 模板码（仅存在于 messages.properties，无对应枚举常量）

以下错误码仅在 `messages.properties` / `messages_zh_CN.properties` / `messages_en.properties` 中定义，用于带参数的 i18n 消息模板，通过 `ErrorCode.getMessage(Object[] args, Locale locale)` 使用：

| Code | 中文模板 | 英文模板 |
|------|----------|----------|
| 10000 | 操作成功 | Success |
| 10103 | {0}不存在 | {0} not found |
| 10104 | {0}已存在 | {0} already exists |
| 11008 | {0}不存在: {1} | {0} not found: {1} |
| 11009 | {0}已存在: {1} | {0} already exists: {1} |
| 12006 | 文件大小超过限制，最大允许 {0}MB | File size exceeds limit, maximum allowed is {0}MB |
| 15004 | 模块 {0} 不存在 | Module {0} not found |
| 15005 | 模块 {0} 已存在 | Module {0} already exists |
| 15006 | 模块存在循环依赖: {0} | Module has circular dependency: {0} |
| 16002 | 配置 {0} 不存在 | Configuration {0} not found |
| 17002 | 功能 {0} 已禁用 | Feature {0} is disabled |

---

## 业务应用错误码规范

- 框架错误码范围：**10000-19999**（`CommonErrorCode`）
- 业务应用错误码范围：**20000+**（自定义 `ErrorCode` 枚举）
- 新错误码必须分配到正确的范围区间
- 错误码必须关联 `ErrorCategory`
- 错误消息支持 i18n（通过 `messages.properties`）

### 业务模块自定义 ErrorCode 示例

```java
@Getter
@RequiredArgsConstructor
public enum OrderErrorCode implements ErrorCode {

    ORDER_NOT_FOUND(20001, "订单不存在", ErrorCategory.BUSINESS),
    ORDER_STATUS_INVALID(20002, "订单状态不允许此操作", ErrorCategory.BUSINESS),
    ORDER_AMOUNT_EXCEEDED(20003, "订单金额超限", ErrorCategory.BUSINESS),
    PAYMENT_FAILED(20004, "支付失败", ErrorCategory.NETWORK),
    ;

    private final int code;
    private final String message;
    private final ErrorCategory category;
}
```

对应的 `messages.properties`：

```properties
# 订单模块错误码
20001=订单不存在
20002=订单状态不允许此操作
20003=订单金额超限
20004=支付失败
# 带参数的模板
20005=订单 {0} 不存在
20006=订单状态 {0} 不允许此操作，当前状态: {1}
```

---

## Result 统一响应

### Result 记录类

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Result<T>(
    int code,
    String message,
    @Nullable T data,
    @Nullable String traceId,
    @Nullable String requestId
) {
    public static final int SUCCESS_CODE = 0;
    public boolean isSuccess() { return code == SUCCESS_CODE; }

    public static <T> Result<T> success(T data) { ... }
    public static <T> Result<T> success(String message, T data) { ... }
    public static <T> Result<T> fail(int code, String message) { ... }
}
```

### 响应格式

**成功响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": { "id": 1, "username": "admin" }
}
```

**失败响应：**

```json
{
  "code": 11000,
  "message": "实体不存在",
  "data": null,
  "traceId": "abc-123-def",
  "requestId": "req-456"
}
```

**分页响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [...],
    "total": 100,
    "page": 1,
    "size": 20,
    "pages": 5
  }
}
```

**参数校验失败响应：**

```json
{
  "code": 10002,
  "message": "参数错误",
  "data": "username: 不能为空; email: 格式不正确"
}
```

**注意：** `@JsonInclude(JsonInclude.Include.NON_NULL)` — `data`、`traceId`、`requestId` 为 null 时自动省略，不会出现在 JSON 中。

---

## GlobalExceptionHandler 全局异常处理

**类路径：** `io.github.afgprojects.framework.core.web.exception.GlobalExceptionHandler`

`@RestControllerAdvice` 全局异常处理器，自动将各类异常转换为 `Result<T>` 统一响应。

### 异常映射表

| 异常类型 | HTTP 状态码 | 使用的 ErrorCode | 说明 |
|----------|-------------|-------------------|------|
| `BusinessException` | 200 OK | 异常自身携带的 ErrorCode | 业务异常，HTTP 200 + 业务码非零 |
| `MethodArgumentNotValidException` | 400 | `PARAM_ERROR` (10002) | Bean Validation 校验失败，data 包含字段错误详情 |
| `BindException` | 400 | `PARAM_ERROR` (10002) | 参数绑定失败 |
| `ConstraintViolationException` | 400 | `PARAM_ERROR` (10002) | 约束校验失败 |
| `MissingServletRequestParameterException` | 400 | `PARAM_MISSING` (10003) | 缺少请求参数 |
| `MethodArgumentTypeMismatchException` | 400 | `PARAM_FORMAT_ERROR` (10004) | 参数类型不匹配，敏感数据自动脱敏 |
| `HttpMessageNotReadableException` | 400 | `PARAM_FORMAT_ERROR` (10004) | 请求体不可读/JSON 格式错误 |
| `HttpRequestMethodNotSupportedException` | 405 | `METHOD_NOT_ALLOWED` (10200) | HTTP 方法不支持 |
| `HttpMediaTypeNotSupportedException` | 415 | `UNSUPPORTED_MEDIA_TYPE` (10201) | 媒体类型不支持 |
| `NoHandlerFoundException` | 404 | `NOT_FOUND` (10100) | 接口不存在 |
| `AccessDeniedException` | 403 | `FORBIDDEN` (10403) | 无权限访问 |
| `AuthenticationException` | 401 | `UNAUTHORIZED` (10400) | 未认证 |
| `Exception`（兜底） | 500 | `SYSTEM_ERROR` (19000) | 未知系统异常 |

### i18n 支持

`GlobalExceptionHandler` 通过 `LocaleContextHolder.getLocale()` 获取当前请求的 Locale（由 `LocaleFilter` 根据 `Accept-Language` 请求头设置），传递给 `ErrorCode.getMessage(locale)` 或 `BusinessException.getMessage(locale)` 进行 i18n 消息解析。

### traceId / requestId 注入

- `traceId`：从 `AfgRequestContextHolder` 获取，由 `RequestContextFilter` 在请求入口设置
- `requestId`：从 `AfgRequestContextHolder` 获取，每个请求唯一

### 参数校验错误详情格式

`MethodArgumentNotValidException` / `BindException` 的 `data` 字段格式为：

```
"fieldName1: defaultMessage1; fieldName2: defaultMessage2"
```

由 `extractFieldErrors(BindingResult)` 方法提取，将所有字段错误以分号连接。

---

## ArgumentAssert 参数断言工具

**包路径：** `io.github.afgprojects.framework.commons.exception.ArgumentAssert`

提供流式参数校验，断言失败直接抛出 `BusinessException`：

```java
// 参数非空校验
ArgumentAssert.notNull(user.getUsername(), "用户名不能为空");

// 集合非空校验
ArgumentAssert.notEmpty(order.getItems(), "订单项不能为空");

// 布尔条件校验
ArgumentAssert.isTrue(balance.compareTo(amount) >= 0, "余额不足");

// 状态校验
ArgumentAssert.state(order.getStatus() == OrderStatus.PENDING, "订单状态不允许此操作");
```

---

## 正确使用模式

### Service 层抛出业务异常

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final DataManager dataManager;

    public User getUserById(Long id) {
        return dataManager.findById(User.class, id)
            .orElseThrow(() -> new BusinessException(
                CommonErrorCode.ENTITY_NOT_FOUND,
                new Object[]{"用户", id}  // i18n 模板参数
            ));
    }

    public void deleteUser(Long id) {
        User user = dataManager.findById(User.class, id)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND));
        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new BusinessException(CommonErrorCode.RESOURCE_LOCKED, "活跃用户不允许删除");
        }
        dataManager.deleteById(User.class, id);
    }
}
```

### Controller 层无需 try-catch

```java
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // GlobalExceptionHandler 自动捕获 BusinessException → Result.fail
    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        return Result.success(userService.getUserById(id));
    }
}
```

### 参数校验使用 Bean Validation

```java
public record CreateUserRequest(
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过50")
    String username,

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, message = "密码长度不能少于8")
    String password,

    @Email(message = "邮箱格式不正确")
    String email
) {}
```

校验失败由 `GlobalExceptionHandler` 自动处理，返回 `Result.fail(PARAM_ERROR, "username: 不能为空; email: 格式不正确")`。

---

## 禁止模式

### 禁止抛出原始 RuntimeException

```java
// 禁止
throw new RuntimeException("用户不存在");
throw new IllegalArgumentException("参数错误");
throw new IllegalStateException("状态异常");

// 正确
throw new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "用户不存在");
throw new BusinessException(CommonErrorCode.PARAM_ERROR, "参数错误");
throw new BusinessException(CommonErrorCode.RESOURCE_LOCKED, "状态异常");
```

### 禁止用 IllegalArgumentException 表示"实体不存在"

Controller 中查询实体不存在时，禁止抛出 `IllegalArgumentException`。根据场景选择以下两种方式之一：

```java
// 方式 1：返回 404（Controller 层直接处理，无统一错误码）
@GetMapping("/{id}")
public ResponseEntity<User> getById(@PathVariable Long id) {
    return dataManager.findById(User.class, id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
}

// 方式 2：抛出 BusinessException（Service 层，通过 GlobalExceptionHandler 统一返回 Result）
@GetMapping("/{id}")
public Result<User> getById(@PathVariable Long id) {
    User user = dataManager.findById(User.class, id)
        .orElseThrow(() -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND));
    return Result.success(user);
}
```

**选择规则**：
- Controller 返回 `ResponseEntity<T>`（直接暴露 HTTP 语义）→ 方式 1
- Controller 返回 `Result<T>`（统一响应包装）→ 方式 2
- 同一 Controller 内必须保持一致，禁止混用

### 禁止在 Controller 层 try-catch BusinessException

```java
// 禁止 — GlobalExceptionHandler 已统一处理
@GetMapping("/{id}")
public Result<User> getById(@PathVariable Long id) {
    try {
        return Result.success(userService.getUserById(id));
    } catch (BusinessException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }
}

// 正确 — 直接返回，让 GlobalExceptionHandler 处理
@GetMapping("/{id}")
public Result<User> getById(@PathVariable Long id) {
    return Result.success(userService.getUserById(id));
}
```

### 禁止在异常消息中暴露内部实现细节

```java
// 禁止 — 暴露 SQL 和表结构
throw new BusinessException(CommonErrorCode.QUERY_ERROR,
    "SELECT * FROM sys_user WHERE id = ? failed: Connection refused");

// 正确 — 使用用户可理解的消息
throw new BusinessException(CommonErrorCode.QUERY_ERROR, "查询用户信息失败");
```

### 禁止吞掉异常

```java
// 禁止
try {
    dataManager.deleteById(User.class, id);
} catch (Exception e) {
    // 吞掉异常
}

// 正确 — 记录日志并重新抛出或包装
try {
    dataManager.deleteById(User.class, id);
} catch (BusinessException e) {
    log.error("删除用户失败, userId={}", id, e);
    throw e;
} catch (Exception e) {
    log.error("删除用户异常, userId={}", id, e);
    throw new BusinessException(CommonErrorCode.SYSTEM_ERROR, "删除用户失败", e);
}
```

### 禁止使用错误码 0 表示失败

```java
// 禁止 — code=0 表示成功
throw new BusinessException(0, "操作失败");

// 正确 — 使用非零错误码
throw new BusinessException(CommonErrorCode.FAIL);
```

---

## 常见错误

### 1. 实体类缺少 @AfEntity 导致 APT 不生成元数据

**症状：** DataManager 操作实体时报错，提示找不到实体元数据

**原因：** 实体类只有 `@Table`/`@Column` 但缺少 `@AfEntity` 注解

**解决：** 在所有实体类上添加 `@AfEntity` 注解

### 2. BusinessException 消息不支持 i18n

**症状：** 切换 `Accept-Language` 后错误消息不变

**原因：** 使用了 `new BusinessException(ErrorCode, String message)` 构造方式，`customMessage=true` 跳过了 i18n

**解决：** 需要支持 i18n 时，使用 `new BusinessException(ErrorCode, Object[] args)` 构造方式，并在 `messages.properties` 中定义模板

### 3. 参数校验注解不生效

**症状：** `@NotBlank` 等注解未触发校验

**原因：** Controller 方法参数缺少 `@Valid` / `@Validated` 注解

**解决：** 在请求参数前添加 `@Valid` 或 `@Validated`

### 4. 业务错误码与框架冲突

**症状：** 自定义错误码与 `CommonErrorCode` 重复

**原因：** 业务应用使用了 10000-19999 范围内的错误码

**解决：** 业务应用错误码从 20000 开始

### 5. GlobalExceptionHandler 未生效

**症状：** 异常未被统一处理，返回 Spring 默认错误页面

**原因：** 自定义了 `@RestControllerAdvice` 但未正确扫描，或存在多个 `@RestControllerAdvice` 优先级冲突

**解决：** 确保框架的 `WebAutoConfiguration` 被正确导入，不要自定义覆盖 `GlobalExceptionHandler`
