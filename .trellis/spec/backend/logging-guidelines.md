# 日志规范

> AFG Framework 日志体系、MDC 上下文、结构化日志与敏感数据脱敏。

**PRD 来源：** §5.3 Core 模块（LoggingAutoConfiguration、AccessLogAutoConfiguration）、§6.1 编码规范（日志规范）

---

## 核心原则

**铁律：统一使用 Lombok `@Slf4j`，禁止手动创建 Logger。**

```java
// 正确
@Slf4j
@Service
public class UserService {
    public void createUser(User user) {
        log.info("Creating user: {}", user.getUsername());
    }
}

// 禁止
@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class); // 禁止！
}
```

---

## 日志级别使用规范

| 级别 | 使用场景 | 示例 |
|------|----------|------|
| **ERROR** | 异常、严重错误、需要立即处理的问题 | 未捕获异常、数据库连接失败、外部服务不可用 |
| **WARN** | 潜在问题、可恢复的异常、需要关注但不阻塞业务 | 配置缺失使用默认值、重试成功、性能下降 |
| **INFO** | 关键业务事件、系统状态变更 | 用户登录/登出、订单创建、任务开始/完成 |
| **DEBUG** | 调试信息、详细业务流程 | 查询条件、中间结果、分支判断 |
| **TRACE** | 最详细的追踪信息、框架内部流程 | SQL 语句、方法入参出参、详细执行路径 |

### 级别使用示例

```java
@Slf4j
@Service
public class OrderService {

    // ERROR — 异常场景
    public void processPayment(Long orderId) {
        try {
            paymentClient.charge(orderId);
        } catch (PaymentException e) {
            log.error("Payment failed for order {}", orderId, e); // 异常对象放在最后
            throw new BusinessException(OrderErrorCode.PAYMENT_FAILED, e);
        }
    }

    // WARN — 潜在问题
    public void applyDiscount(Long orderId, String code) {
        Discount discount = discountService.getByCode(code);
        if (discount == null) {
            log.warn("Discount code not found: {}, using default", code);
            discount = Discount.DEFAULT;
        }
    }

    // INFO — 关键业务事件
    public Order createOrder(CreateOrderRequest request) {
        Order order = buildOrder(request);
        dataManager.save(Order.class, order);
        log.info("Order created: orderId={}, userId={}, amount={}",
            order.getId(), order.getUserId(), order.getAmount());
        return order;
    }

    // DEBUG — 调试信息
    public List<Order> queryOrders(OrderQuery query) {
        log.debug("Querying orders with condition: {}", query);
        List<Order> orders = dataManager.findList(Order.class, buildCondition(query));
        log.debug("Found {} orders", orders.size());
        return orders;
    }

    // TRACE — 详细追踪（生产环境通常关闭）
    public Order getById(Long id) {
        log.trace("Entering getById with id={}", id);
        Order order = dataManager.findById(Order.class, id).orElse(null);
        log.trace("Exiting getById with result={}", order);
        return order;
    }
}
```

---

## MDC 上下文字段

框架通过 `MdcFilter` 自动注入以下 MDC 字段，无需手动设置：

| 字段 | 来源 | 说明 |
|------|------|------|
| `traceId` | `AfgRequestContextHolder` 或 Micrometer Tracing | 分布式追踪 ID，贯穿整个调用链 |
| `requestId` | `AfgRequestContextHolder` | 当前请求唯一 ID |
| `userId` | `AfgRequestContextHolder` → SecurityContext | 当前登录用户 ID |
| `username` | `AfgRequestContextHolder` → SecurityContext | 当前登录用户名 |
| `tenantId` | `AfgRequestContextHolder` → TenantContextHolder | 当前租户 ID |
| `clientIp` | `AfgRequestContextHolder` | 客户端 IP（支持 X-Forwarded-For） |
| `requestPath` | `AfgRequestContextHolder` | 请求路径 |
| `requestMethod` | `AfgRequestContextHolder` | HTTP 方法 |

### MDC 配置

```yaml
afg:
  core:
    logging:
      mdc:
        enabled: true  # 默认 true
        fields:        # 可配置启用的字段
          - traceId
          - tenantId
          - userId
          - requestPath
```

### MDC 在日志中的体现

**结构化 JSON 日志（生产环境）：**

```json
{
  "timestamp": "2026-06-11T10:30:00.123+08:00",
  "level": "INFO",
  "logger": "io.github.afgprojects.framework.service.OrderService",
  "thread": "http-nio-8080-exec-1",
  "message": "Order created: orderId=123, userId=1, amount=99.00",
  "traceId": "abc-123-def-456",
  "requestId": "req-789",
  "userId": "1",
  "tenantId": "tenant-001",
  "clientIp": "192.168.1.100",
  "requestPath": "/api/orders",
  "requestMethod": "POST"
}
```

**纯文本日志（开发环境）：**

```
2026-06-11 10:30:00.123 INFO  [traceId=abc-123-def-456,userId=1,tenantId=tenant-001] i.g.a.f.s.OrderService : Order created: orderId=123, userId=1, amount=99.00
```

---

## 结构化日志

### 配置

```yaml
afg:
  core:
    logging:
      structured:
        enabled: true      # 生产环境建议开启
        pretty-print: false # 生产环境 false，开发环境可 true
      file:
        path: ./logs
        max-size: 100MB
        max-history: 30
        total-size-cap: 10GB
      async:
        queue-size: 512
        discarding-threshold: 0
        include-caller-data: true
        never-block: true
```

### StructuredLogbackLayout

框架提供 `StructuredLogbackLayout`，自动将日志输出为 JSON 格式，包含：

- `timestamp` — ISO-8601 时间戳
- `level` — 日志级别
- `logger` — Logger 名称
- `thread` — 线程名
- `message` — 日志消息
- `exception` — 异常堆栈（如有）
- 所有 MDC 字段

### logback-spring.xml 配置示例

```xml
<configuration>
    <springProfile name="prod">
        <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>./logs/application.log</file>
            <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
                <fileNamePattern>./logs/application.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
                <maxFileSize>100MB</maxFileSize>
                <maxHistory>30</maxHistory>
                <totalSizeCap>10GB</totalSizeCap>
            </rollingPolicy>
            <layout class="io.github.afgprojects.framework.core.web.logging.StructuredLogbackLayout">
                <prettyPrint>false</prettyPrint>
            </layout>
        </appender>
        <root level="INFO">
            <appender-ref ref="FILE"/>
        </root>
    </springProfile>

    <springProfile name="dev">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %highlight(%-5level) [%X{traceId},%X{userId},%X{tenantId}] %cyan(%logger{36}) : %msg%n</pattern>
            </encoder>
        </appender>
        <root level="DEBUG">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>
</configuration>
```

---

## 敏感数据脱敏

### 自动脱敏字段

框架通过 `SensitiveDataMasker` 自动脱敏以下敏感字段：

| 字段名模式 | 脱敏策略 | 示例 |
|------------|----------|------|
| `password`, `passwd`, `pwd` | 全部遮盖 | `***` |
| `token`, `accessToken`, `refreshToken`, `secret`, `apiKey` | 全部遮盖 | `***` |
| `phone`, `mobile`, `telephone` | 保留前 3 位 | `138***` |
| `idCard`, `idcard`, `ssn` | 保留前 3 位 | `110***` |
| `bankCard`, `bankcard`, `cardNo` | 保留前 3 位 | `622***` |
| `email` | 保留前 3 位 | `abc***` |

### 脱敏实现

`SensitiveDataMasker` 是静态工具类，默认敏感字段列表约 30 个，支持动态扩展：

```java
// 注册自定义敏感字段
SensitiveDataMasker.registerSensitiveField("creditCard");

// 手动脱敏
String masked = SensitiveDataMasker.mask("password", "mySecret123"); // → "***"
```

### 日志中避免敏感数据

```java
// 禁止 — 直接打印敏感数据
log.info("User login: username={}, password={}", username, password);

// 正确 — 敏感数据脱敏或不打印
log.info("User login: username={}", username);

// 正确 — 使用 SensitiveDataMasker
log.info("User data: {}", SensitiveDataMasker.mask(user));
```

### AuditLogSerializer 自动脱敏

`AuditLogSerializer` 在序列化方法参数和返回值时，自动通过 `SensitiveFieldProcessor` 对敏感字段进行脱敏，无需手动处理。

---

## 审计日志

### @Audited 注解

框架通过 AOP 自动记录审计日志，在需要审计的方法上添加 `@Audited` 注解：

```java
@PostMapping
@Audited(operation = "创建用户", module = "用户管理")
public Result<User> createUser(@RequestBody CreateUserRequest request) {
    return Result.success(userService.create(request));
}
```

### AuditLog 记录结构

| 字段 | 说明 |
|------|------|
| `id` | 审计日志 ID |
| `userId` | 操作用户 ID |
| `username` | 操作用户名 |
| `tenantId` | 租户 ID |
| `operation` | 操作名称 |
| `module` | 模块名称 |
| `target` | 操作目标 |
| `args` | 方法参数（敏感字段自动脱敏） |
| `oldValue` | 修改前值 |
| `newValue` | 修改后值 |
| `result` | 结果（SUCCESS/FAILURE） |
| `errorMessage` | 错误消息（失败时） |
| `timestamp` | 时间戳 |
| `durationMs` | 执行耗时（毫秒） |
| `traceId` | 追踪 ID |
| `requestId` | 请求 ID |
| `clientIp` | 客户端 IP |
| `className` | 类名 |
| `methodName` | 方法名 |

### AuditLogStorage SPI

审计日志存储通过 SPI 扩展：

| 实现 | 存储方式 | 配置 |
|------|----------|------|
| `LogAuditLogStorage` | 输出到 `AUDIT_LOG` Logger（默认） | `afg.core.audit.storage-type: log` |
| `NoOpAuditLogStorage` | 不存储 | `afg.core.audit.storage-type: none` |
| `DatabaseAuditLogStorage` | 存储到数据库 | `afg.core.audit.storage-type: database`（需 afg-jdbc 模块） |
| `RedisAuditLogStorage` | 存储到 Redis | `afg.core.audit.storage-type: redis`（需 afg-redis 模块） |

### 配置

```yaml
afg:
  core:
    audit:
      enabled: true
      storage-type: log  # log | database | redis | none
```

---

## 异步上下文传播

### ThreadLocalContextPropagator

框架通过 `ThreadLocalContextPropagator` 实现 ThreadLocal 上下文在异步线程间的传播，确保 `@Async`、线程池、`CompletableFuture` 等场景下 MDC 字段不丢失。

### ContextSnapshotProvider SPI

| 实现 | 传播的上下文 |
|------|--------------|
| `RequestContextSnapshotProvider` | `AfgRequestContextHolder`（traceId、requestId、userId 等） |
| `DataScopeContextSnapshotProvider` | `DataScopeContext`（数据权限上下文） |
| `BaggageContextSnapshotProvider` | Micrometer Tracing Baggage |

### CompositeContextTaskDecorator

框架自动注册 `CompositeContextTaskDecorator` 作为 Spring `TaskDecorator`，包装所有 `Runnable`：

```java
// 自动生效，无需配置
@Async
public void asyncProcess() {
    // MDC 字段自动传播，traceId、userId 等可用
    log.info("Async processing");
}
```

### 手动使用

```java
@Autowired
private ThreadLocalContextPropagator propagator;

public void manualAsync() {
    Runnable task = propagator.wrap(() -> {
        log.info("Running in async thread with MDC");
    });
    executor.submit(task);
}
```

---

## 日志最佳实践

### 1. 使用占位符，避免字符串拼接

```java
// 正确 — 使用占位符
log.info("User {} logged in from {}", username, clientIp);

// 禁止 — 字符串拼接（性能差，DEBUG 级别也会执行拼接）
log.info("User " + username + " logged in from " + clientIp);
```

### 2. 异常对象放在最后

```java
// 正确 — 异常对象作为最后一个参数
log.error("Failed to process order {}", orderId, e);

// 禁止 — 异常对象不在最后（堆栈不会打印）
log.error(e.getMessage() + " for order " + orderId);  // 无堆栈
log.error("Failed to process order {}", orderId, e.getMessage());  // 无堆栈
```

### 3. 条件日志（TRACE/DEBUG）

```java
// 生产环境 TRACE/DEBUG 通常关闭，可使用条件判断避免不必要的计算
if (log.isTraceEnabled()) {
    log.trace("Detailed object state: {}", objectMapper.writeValueAsString(complexObject));
}
```

### 4. 关键业务事件必须记录

```java
// 用户登录/登出
log.info("User logged in: userId={}, username={}, clientIp={}", userId, username, clientIp);
log.info("User logged out: userId={}", userId);

// 订单创建/状态变更
log.info("Order created: orderId={}, userId={}, amount={}", orderId, userId, amount);
log.info("Order status changed: orderId={}, from={}, to={}", orderId, oldStatus, newStatus);

// 支付成功/失败
log.info("Payment succeeded: orderId={}, transactionId={}", orderId, transactionId);
log.warn("Payment failed: orderId={}, reason={}", orderId, reason);

// 任务执行
log.info("Job started: jobName={}, jobId={}", jobName, jobId);
log.info("Job completed: jobName={}, jobId={}, duration={}ms", jobName, jobId, duration);
```

### 5. 避免过度日志

```java
// 禁止 — 循环内大量日志
for (Order order : orders) {
    log.debug("Processing order: {}", order.getId());  // 10000 条订单 = 10000 条日志
}

// 正确 — 批量处理汇总
log.debug("Processing {} orders", orders.size());
// ... 处理逻辑
log.info("Processed {} orders, success={}, failed={}", total, success, failed);
```

### 6. 日志消息清晰明确

```java
// 禁止 — 模糊不清
log.info("Processing...");
log.error("Error occurred");

// 正确 — 明确具体
log.info("Processing order import from file: {}", filename);
log.error("Failed to connect to payment gateway: {}", gatewayUrl);
```

---

## 常见错误

### 1. MDC 字段丢失

**症状：** 异步线程中日志缺少 traceId、userId 等 MDC 字段

**原因：** 未使用框架提供的 `CompositeContextTaskDecorator`，或自定义线程池未配置 `TaskDecorator`

**解决：** 使用框架自动配置的线程池，或手动使用 `ThreadLocalContextPropagator.wrap()`

### 2. 敏感数据泄露

**症状：** 日志中出现明文密码、Token

**原因：** 直接打印包含敏感字段的对象

**解决：** 使用 `SensitiveDataMasker.mask()` 或避免打印敏感字段

### 3. 日志级别配置错误

**症状：** 生产环境 DEBUG 日志过多，磁盘满

**原因：** `logback-spring.xml` 中 root level 配置为 DEBUG

**解决：** 生产环境 root level 设为 INFO，特定包可单独配置 DEBUG

### 4. 异常堆栈未打印

**症状：** 日志只有错误消息，无堆栈信息

**原因：** 异常对象未作为日志方法最后一个参数

**解决：** `log.error("message", exception)` — 异常对象放最后

### 5. 日志文件过大

**症状：** 单个日志文件超过 GB

**原因：** 未配置滚动策略或 `maxFileSize` 过大

**解决：** 配置 `SizeAndTimeBasedRollingPolicy`，`maxFileSize=100MB`
