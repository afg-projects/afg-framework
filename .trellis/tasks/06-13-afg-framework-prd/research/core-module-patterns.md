# Research: Core Module Patterns for Feature Alignment

- **Query**: Deep analysis of core module code organization and patterns, to prepare for adding 11 missing AutoConfiguration and related SPI/annotation features
- **Scope**: internal
- **Date**: 2026-06-13

## Findings

### 1. Core Module Directory Structure

`core/src/main/java/io/github/afgprojects/framework/core/` contains the following top-level packages:

```
annotation/       # Method-level annotations (@ScheduledTask, @DistributedTask, @DelayTask, @IgnoreModuleContextPath)
api/              # SPI interface definitions + NoOp defaults + local default implementations
  config/         # RemoteConfigClient, ConfigChangeListener, ConfigChangeEvent
  event/          # EventPublisher<T>, EventSubscriber<T>, NoOpEventPublisher, MessageEvent
  ratelimit/      # RateLimiter, RateLimitStorage, LocalRateLimitStorage, NoOpRateLimitStorage, etc.
  registry/       # ServiceRegistry, ServiceDiscovery, NoOpServiceRegistry, NoOpServiceDiscovery
  scheduler/      # DistributedTaskScheduler, DelayQueue, TaskScheduler, NoOp*, InMemory*, etc.
  storage/        # FileStorage, FileStorageFactory, NoOpFileStorage, NoOpFileStorageFactory, models/
audit/            # Audited annotation, AuditLogAspect, AuditLogStorage SPI, LogAuditLogStorage, NoOpAuditLogStorage, etc.
autoconfigure/    # 31 AutoConfiguration classes (29 existing + imports file has 31 entries)
  condition/      # Custom conditions: ConditionalOnFeature, ConditionalOnPropertyNotEmpty, ConditionalOnTenant
  openapi/        # AfgOpenApiAutoConfiguration, AfgOpenApiProperties
batch/            # BatchOperationTemplate, BatchResult, BatchError, BatchProgressCallback
cache/            # AfgCache<V>, CacheManager, DefaultCacheManager, CacheAspect, annotations (@Cached, @CacheEvict, @CachePut)
  exception/      # CacheException
  metrics/        # CacheMetrics, CacheMetricsBinder
  spi/            # CacheStorageProvider, DistributedCacheStorage, NoOp* implementations
client/           # HTTP client related
cloud/            # Cloud-native support
codegen/          # Code generation support
config/           # AfgCoreProperties (unified), AfgConfigRegistry, ConfigRefresher
context/          # ThreadLocal context propagation
datasource/       # Multi-datasource support, load balance
  lb/             # Load balance strategies
env/              # Environment detection
event/            # DomainEventPublisher (interface), LocalEventPublisher (impl), DomainEvent, EventHandler, etc.
exception/        # Core-level exceptions
feature/          # FeatureToggle annotation, FeatureToggleAspect, FeatureFlagManager, GrayscaleRule, etc.
invocation/       # BeanInvocationEngine, interceptors, processors, resolvers
lock/             # DistributedLock (SPI), Lock annotation, LockAspect, NoOpDistributedLock, LockType
  exception/      # LockAcquisitionException, LockException
metrics/          # Metrics support
model/            # Core model types
  entity/         # Core entities
  exception/      # Model-level exceptions
  result/         # Results utility class
  version/        # Version info
module/           # ModuleContext, ModuleRegistry, ModuleDefinition, AfgModuleProcessor
properties/       # Individual @ConfigurationProperties classes (split per feature, NOT all in AfgCoreProperties)
  audit/          # AfgCoreAuditProperties, AuditStorageType
  batch/          # AfgCoreBatchProperties, etc.
  cache/          # AfgCoreCacheProperties, local/distributed sub-properties
  cloudnative/    # Kubernetes, probe, graceful shutdown properties
  datascope/      # AfgCoreDataScopeProperties
  datasource/     # AfgCoreDataSourceProperties, etc.
  encryption/     # AfgCoreEncryptionProperties
  event/          # AfgCoreEventProperties, kafka/rabbitmq/retry/deadletter sub-properties
  feature/        # AfgCoreFeatureProperties, redis sub-properties
  health/         # AfgCoreHealthProperties, liveness/readiness/deep/datasource sub-properties
  httpclient/     # AfgCoreHttpClientProperties, retry/circuitbreaker sub-properties
  invocation/     # AfgInvocationProperties
  lock/           # AfgCoreLockProperties, annotation sub-properties
  logging/        # AfgCoreLoggingProperties, mdc/structured/file/async sub-properties
  metrics/        # AfgCoreMetricsProperties, histogram sub-properties
  openapi/        # AfgOpenApiProperties
  ratelimit/      # AfgCoreRateLimitProperties, dimension/fallback/whitelist/responseheaders/local sub-properties
  scheduler/      # AfgCoreSchedulerProperties, logstorage/metrics/dynamic-task/annotation/api sub-properties
  security/       # AfgCoreSecurityProperties, xss/sql-injection/sensitive/signature sub-properties
  shutdown/       # AfgCoreShutdownProperties, phase sub-properties
  tracing/        # AfgCoreTracingProperties, annotation/sampling/baggage/propagation/zipkin/jaeger sub-properties
  virtualthread/  # AfgCoreVirtualThreadProperties
scheduler/        # LocalTaskScheduler, ScheduledTaskAspect, DistributedTaskAspect, DynamicTaskManager, SchedulerHealthIndicator
security/         # Core security (data scope)
  datascope/      # DataScopeContext, DataScopeContextHolder, autoconfigure/
trace/            # Tracing support
util/             # JacksonMapper, JacksonUtils, utility classes
web/              # Web-layer features (filters, interceptors, controllers)
  context/        # AfgRequestContextHolder, RequestContext
  exception/      # GlobalExceptionHandler
  feature/        # Feature flag web endpoint
  health/         # Health check web
  health/spi/     # Health SPI
  i18n/           # LocaleFilter
  logging/        # Access log, MDC filter
  metrics/        # Metrics web
  ratelimit/      # RateLimitInterceptor
  scheduler/      # Scheduler web endpoint
  security/       # Security web (XSS, SQL injection, signature, sanitizers, filters)
  shutdown/       # Graceful shutdown web
  trace/          # Tracing web
  validation/     # Phone constraint annotation + validator
```

### 2. AutoConfiguration Code Pattern

Every AutoConfiguration follows a consistent pattern. Here is the canonical template, extracted from `CacheAutoConfiguration` and `LockAutoConfiguration`:

**Package**: `io.github.afgprojects.framework.core.autoconfigure`

**Class structure**:
```java
@AutoConfiguration(after = AfgAutoConfiguration.class)
@ConditionalOnProperty(prefix = "afg.core.{feature}", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AfgCoreProperties.class)  // NOTE: also uses feature-specific Properties
public class {Feature}AutoConfiguration {

    // 1. NoOp/SPI default Bean — always @ConditionalOnMissingBean
    @Bean
    @ConditionalOnMissingBean({SpiInterface}.class)
    public {SpiInterface} noOp{SpiInterface}() {
        return new NoOp{SpiInterface}();
    }

    // 2. Local/default implementation Bean
    @Bean
    @ConditionalOnMissingBean
    public {DefaultImpl} {defaultImplBeanName}({Properties} properties) {
        return new {DefaultImpl}(properties);
    }

    // 3. AOP Aspect Bean (for annotation-driven features) — conditional on impl + annotations.enabled
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({DefaultImpl}.class)
    @ConditionalOnProperty(
        prefix = "afg.core.{feature}.annotations",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
    public {Aspect} {feature}Aspect({DefaultImpl} impl, {Properties} properties) {
        return new {Aspect}(impl, properties);
    }
}
```

**Key annotations on the class**:
- `@AutoConfiguration(after = AfgAutoConfiguration.class)` — always declares dependency on base config
- `@ConditionalOnProperty(prefix = "afg.core.{feature}", name = "enabled", havingValue = "true", matchIfMissing = true)` — enable/disable with default true
- `@EnableConfigurationProperties(AfgCoreProperties.class)` — binds unified properties

**Bean method annotations**:
- `@ConditionalOnMissingBean` — for every bean, allows override by integration modules
- `@ConditionalOnBean({Impl}.class)` — aspect beans require the implementation bean
- `@ConditionalOnProperty` — annotations sub-feature has its own enabled switch (default true)

**Inner static class pattern** (used in `EventAutoConfiguration`):
```java
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "afg.core.event", name = "type", havingValue = "LOCAL", matchIfMissing = true)
static class LocalEventConfiguration {
    // beans for local implementation only
}
```

**SPI type-based storage selection pattern** (used in `AuditLogAutoConfiguration`):
```java
@Bean
@ConditionalOnProperty(prefix = "afg.core.audit", name = "storage-type", havingValue = "log", matchIfMissing = true)
@ConditionalOnMissingBean
public LogAuditLogStorage logAuditLogStorage() { ... }

@Bean
@ConditionalOnProperty(prefix = "afg.core.audit", name = "storage-type", havingValue = "none")
@ConditionalOnMissingBean
public NoOpAuditLogStorage noOpAuditLogStorage() { ... }
```

**AutoConfiguration.imports file** (31 entries currently):
Path: `core/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

Each AutoConfiguration class is listed as a fully qualified class name, one per line.

### 3. SPI Interface Code Pattern

SPI interfaces follow two organizational patterns:

**Pattern A: SPI in `api/` subpackage** (cross-module, integration-upgradable)

- Interface: `io.github.afgprojects.framework.core.api.{feature}.{SpiInterface}`
- NoOp default: `io.github.afgprojects.framework.core.api.{feature}.NoOp{SpiInterface}`
- Local default: `io.github.afgprojects.framework.core.api.{feature}.Local{SpiInterface}` or `InMemory{SpiInterface}`
- Registered in AutoConfiguration via `@ConditionalOnMissingBean`

Examples:
- `api.event.EventPublisher<T>` / `api.event.NoOpEventPublisher`
- `api.ratelimit.RateLimitStorage` / `api.ratelimit.NoOpRateLimitStorage` / `api.ratelimit.LocalRateLimitStorage`
- `api.scheduler.DistributedTaskScheduler` / `api.scheduler.NoOpDistributedTaskScheduler`
- `api.scheduler.DelayQueue<T>` / `api.scheduler.NoOpDelayQueue<T>`
- `api.registry.ServiceRegistry` / `api.registry.NoOpServiceRegistry`

**Pattern B: SPI in feature package** (same-module, not upgradable)

- Interface: `io.github.afgprojects.framework.core.{feature}.{SpiInterface}`
- NoOp default: in same package, `NoOp{SpiInterface}`
- Local default: in same package, `Local{SpiInterface}` or `Default{SpiInterface}`

Examples:
- `lock.DistributedLock` / `lock.NoOpDistributedLock`
- `audit.AuditLogStorage` / `audit.LogAuditLogStorage` / `audit.NoOpAuditLogStorage`
- `cache.spi.CacheStorageProvider` / `cache.spi.NoOpCacheStorageProvider`

**NoOp semantic rules** (from CLAUDE.md):
- tryXxx -> true (always succeed)
- isXxx -> false (always negative)
- void -> no-op (silent)
- query -> empty/null/0 (return empty value)

**NoOp registration pattern**:
```java
@Bean
@ConditionalOnMissingBean({SpiInterface}.class)
public {SpiInterface} noOp{SpiInterface}() {
    return new NoOp{SpiInterface}();
}
```

Some NoOp beans have additional conditions (e.g., `EventAutoConfiguration`'s NoOpEventPublisher only registers when `afg.core.event.fallback=true`, to avoid blocking RabbitMQ module from registering real implementation).

### 4. Annotation + AOP Code Pattern

The complete chain for annotation-driven features follows this structure:

**Annotation** (in feature package or `annotation/` package):

```java
// e.g., io.github.afgprojects.framework.core.lock.Lock
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Lock {
    String key();
    String prefix() default "";
    long waitTime() default -1;
    long leaseTime() default -1;
    // ... more attributes with defaults
}
```

Annotation location varies:
- `@Lock` -> `core.lock.Lock` (same package as SPI)
- `@Audited` -> `core.audit.Audited` (same package as SPI)
- `@FeatureToggle` -> `core.feature.FeatureToggle` (same package as SPI)
- `@ScheduledTask` -> `core.annotation.ScheduledTask` (dedicated annotation package)
- `@DistributedTask` -> `core.annotation.DistributedTask`
- `@Cached` / `@CacheEvict` / `@CachePut` -> `core.cache.*` (same package as SPI)
- `@Phone` -> `core.web.validation.Phone` (web-specific validation)

**AOP Aspect** (in feature package):

```java
// e.g., io.github.afgprojects.framework.core.lock.LockAspect
@Aspect
@Slf4j
public class LockAspect {
    private final {SpiInterface} spiService;
    private final AfgCoreProperties properties;

    public LockAspect({SpiInterface} spiService, AfgCoreProperties properties) { ... }

    @Around("@annotation(annotation)")
    public Object around{Feature}(ProceedingJoinPoint joinPoint, {Annotation} annotation) throws ... {
        // 1. Extract key/params from annotation + SpEL
        // 2. Call SPI service
        // 3. Execute target method
        // 4. Handle exceptions
        // 5. Cleanup/release
    }
}
```

Key aspects of Aspect pattern:
- Constructor injection (not Spring `@Autowired`), takes SPI interface + AfgCoreProperties
- `@Around("@annotation(annotation)")` pattern to get annotation instance
- SpEL expression support for dynamic keys (using SpelExpressionParser)
- `proceedSafely()` helper method to handle checked exceptions from `joinPoint.proceed()`
- Detailed logging (debug for success, warn for failures)
- Framework exceptions thrown on failure (LockAcquisitionException, etc.)

**AutoConfiguration registration**:
```java
@Bean
@ConditionalOnMissingBean
@ConditionalOnBean({SpiImpl}.class)
@ConditionalOnProperty(prefix = "afg.core.{feature}.annotations", name = "enabled", havingValue = "true", matchIfMissing = true)
public {Aspect} {feature}Aspect({SpiImpl} impl, AfgCoreProperties properties) {
    return new {Aspect}(impl, properties);
}
```

### 5. Core Module build.gradle.kts Dependencies

Key dependencies currently in `core/build.gradle.kts`:

| Category | Dependency | Type |
|----------|-----------|------|
| **Framework internal** | `project(":commons")`, `project(":apt-api")` | api |
| **Jackson** | `libs.bundles.jackson` | api |
| **Micrometer** | `libs.micrometer.tracing`, `libs.micrometer.observation`, `libs.micrometer.core` | api |
| **Micrometer bridges** | `libs.micrometer.tracing.bridge.brave`, `libs.micrometer.tracing.bridge.otel` | compileOnly |
| **Security** | `spring-security-core`, `spring-boot-starter-security` | api / compileOnly |
| **Validation** | `libs.jakarta.validation.api`, `libs.spring.boot.starter.validation` | api |
| **Spring Boot Web** | `libs.spring.boot.starter.web` | compileOnly |
| **Spring Boot Actuator** | `libs.spring.boot.starter.actuator`, `libs.spring.boot.health` | api |
| **AOP** | `libs.spring.boot.starter.aspectj` | api |
| **Caffeine** | `libs.caffeine` | api |
| **Logback** | `libs.logback.classic` | api |
| **Casbin** | `libs.jcasbin` | api |
| **JSpecify** | `libs.jspecify` | api |
| **HTML/XSS** | `libs.jsoup`, `libs.antisamy` | api |
| **Data** | `libs.spring.boot.starter.data.jdbc`, `libs.dynamic.datasource` | compileOnly |
| **Redis** | `libs.spring.boot.starter.data.redis` | compileOnly |
| **SpringDoc** | `libs.springdoc.openapi.starter.webmvc` | compileOnly |
| **Spring Expression** | `libs.spring.expression` | compileOnly |
| **SnakeYAML** | `libs.snakeyaml` | compileOnly |

For the 11 missing features, likely needed NEW dependencies:
- **ImportExport (Excel)**: EasyExcel (Alibaba) or Apache POI
- **ImportExport (CSV)**: OpenCSV or built-in
- **StateMachine**: No external dependency (internal lightweight implementation)
- **Notification (Email)**: JavaMailSender (Spring Boot starter-mail)
- **Webhook**: No external dependency (HTTP client already available)
- **SSE**: No external dependency (Spring Web MVC SseEmitter built-in)
- **IdGenerator**: No external dependency (Snowflake algorithm is pure math)
- **DuplicateSubmit**: No external dependency (memory-based + Redis upgrade)
- **ApiVersion**: No external dependency (Spring WebMvc mapping customization)
- **AccessLog**: No external dependency (Servlet Filter)
- **Validation**: Already has jakarta.validation.api + spring-boot-starter-validation

### 6. Existing Test Patterns

Core module tests are **pure unit tests** (no Spring context, no Testcontainers, no @SpringBootTest). The test file structure follows:

```java
@DisplayName("{ClassName}")
class {ClassName}Test {

    private final {ClassUnderTest} instance = new {ClassUnderTest}();

    @Nested
    @DisplayName("{method or feature}")
    class {NestedClass} {

        @Test
        @DisplayName("should {behavior} when {condition}")
        void should{Behavior}_when{Condition}() {
            // assertJ assertions
            assertThat(result).isEqualTo(expected);
            assertThatThrownBy(() -> ...).isInstanceOf(...);
        }
    }
}
```

Key test conventions:
- No `@SpringBootTest` in core module tests (all are unit tests)
- `@DisplayName` on test class and nested classes
- `@Nested` for grouping related test cases
- `shouldXxx_whenYyy` naming convention
- AssertJ assertions (`assertThat`, `assertThatThrownBy`)
- Direct instantiation (`new ClassUnderTest()`) without Spring DI
- No Mockito (framework rule)
- Test files mirror source structure: `core/src/test/java/io/github/afgprojects/framework/core/{feature}/`

For integration tests that need Spring context, they would typically go in a separate test configuration or in downstream modules (data-jdbc, auth-server, etc.). The core module itself only has unit tests.

### 7. PRD Definitions for 11 Missing Features

The PRD (`docs/framework-prd.md`) defines these 11 missing features primarily through:
- AutoConfiguration table (lines 2218-2228)
- SPI interface table (lines 2241-2246)
- API experience code examples (lines 1060-1131)
- Configuration properties (lines 2276-2291)

**Note**: The PRD only has full 7-step documentation (concept -> min -> common -> advanced -> configuration -> degradation -> limitations) for the first 12 features (5.3.1-5.3.12). The 11 missing features are defined through:
1. AutoConfiguration name + activation condition in the table
2. SPI interface + default implementation in the table
3. API experience code snippets
4. Configuration properties examples

Below are the extracted PRD definitions for each missing feature:

---

#### 7.1 AccessLogAutoConfiguration

**PRD AutoConfiguration table**: `AccessLogAutoConfiguration` | 访问日志过滤器 | 自动（可配置排除路径）

**PRD API example**:
```java
// 访问日志——自动记录每个请求
// 无需注解，AccessLogFilter 自动生效
// 日志格式：method=POST path=/users status=201 duration=125ms userId=1 tenantId=tenant-1 clientIp=192.168.1.1
```

**PRD configuration**:
```yaml
afg:
  core:
    access-log:
      enabled: true
      exclude-paths: /health,/actuator/**  # 排除路径
```

**PRD notes**: 访问日志默认记录所有请求，生产环境建议排除健康检查等高频低价值路径。Boot 原生无访问日志 — AFG 提供 AccessLogFilter，结构化记录每个请求。

---

#### 7.2 ValidationAutoConfiguration

**PRD AutoConfiguration table**: `ValidationAutoConfiguration` | Bean Validation + 统一异常处理 | 自动

**PRD has full 7-step documentation (5.3.1)**. Key points:
- Framework provides `GlobalExceptionHandler` that auto-catches `MethodArgumentNotValidException` and `ConstraintViolationException`, converting to `Result.fail(CommonErrorCode.PARAM_ERROR, details)`
- Built-in `@Phone` constraint annotation
- No new annotation needed (uses standard `@Valid`/`@Validated` + Jakarta Validation)
- Configuration: `afg.core.validation.enabled: true`

**Current state**: `GlobalExceptionHandler` already exists in `core.web.exception`. `@Phone` already exists in `core.web.validation`. The missing piece is an AutoConfiguration class that registers the `GlobalExceptionHandler` as a bean with conditional enable/disable, and potentially the validation properties configuration.

---

#### 7.3 SseAutoConfiguration

**PRD AutoConfiguration table**: `SseAutoConfiguration` | SSE 基础设施 | 自动

**PRD SPI table**: `api.sse` | `SseConnectionManager` | 内存连接管理

**PRD API example**:
```java
@GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter events() {
    return sseService.createConnection(userId);
}
```

---

#### 7.4 StateMachineAutoConfiguration

**PRD AutoConfiguration table**: `StateMachineAutoConfiguration` | 轻量级状态机 | `@StateMachine` 注解触发

**PRD SPI table**: `api.statemachine` | `StateMachine<T, S>` / `StateMachineFactory` | 内存状态机

**PRD API example**:
```java
@StateMachine(entity = Order.class)
public enum OrderStatus {
    PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED;

    @Transition(from = PENDING, to = CONFIRMED)
    public void confirm(Order order) { ... }

    @Transition(from = CONFIRMED, to = CANCELLED)
    public void cancel(Order order) { ... }
}
```

**PRD status**: Alpha | 无 | 无 | PRD 计划功能

---

#### 7.5 EnumManagementAutoConfiguration

**PRD AutoConfiguration table**: `EnumManagementAutoConfiguration` | 枚举元数据管理 | `@AfgEnum` 注解触发

**PRD API example**:
```java
@AfgEnum(valueField = "code", labelField = "label", i18nPrefix = "enum.user-status")
public enum UserStatus {
    ACTIVE(1, "激活"),
    DISABLED(0, "禁用");
    private final int code;
    private final String label;
}
```

**PRD APT annotation table**: `@AfgEnum` | TYPE | 枚举元数据 | `valueField` / `labelField` / `i18nPrefix`

---

#### 7.6 ImportExportAutoConfiguration

**PRD AutoConfiguration table**: `ImportExportAutoConfiguration` | 导入/导出（Excel/CSV） | `@ExcelSheet` / `@CsvSheet` 注解触发

**PRD SPI table**: `api.importexport` | `DataExporter` / `DataImporter` / `FormatHandler` | Excel (EasyExcel) + CSV

**PRD API example**:
```java
@ExcelSheet(name = "用户列表")
public class UserExportVO {
    @ExcelColumn(name = "用户名", order = 1)
    private String username;
    @ExcelColumn(name = "状态", order = 2, enumConverter = UserStatus.class)
    private Integer status;
}
ExcelExporter.export(users, UserExportVO.class, response.getOutputStream());
ImportResult<UserImportVO> result = ExcelImporter.importAs(file.getInputStream(), UserImportVO.class);
```

**PRD status**: Alpha | 无 | 无 | PRD 计划功能

---

#### 7.7 NotificationAutoConfiguration

**PRD AutoConfiguration table**: `NotificationAutoConfiguration` | 通知服务 SPI | 自动

**PRD SPI table**: `api.notification` | `NotificationService` / `NotificationChannel` | LogNotificationService（邮件/短信 SPI）

**PRD API example**:
```java
notificationService.send(Notification.builder()
    .to(userId)
    .channel(NotificationChannel.EMAIL)
    .template("welcome")
    .variable("username", user.getUsername())
    .build());
```

---

#### 7.8 WebhookAutoConfiguration

**PRD AutoConfiguration table**: `WebhookAutoConfiguration` | Webhook 分发 | 自动

**PRD SPI table**: `api.webhook` | `WebhookService` / `WebhookRepository` | 内存注册 + HTTP 分发

**PRD API example**:
```java
webhookService.dispatch("order.created", OrderCreatedPayload.from(order));
```

---

#### 7.9 DuplicateSubmitAutoConfiguration

**PRD AutoConfiguration table**: `DuplicateSubmitAutoConfiguration` | 重复提交防护 | `@DuplicateSubmit` 注解触发

**PRD API example**:
```java
@DuplicateSubmit(interval = 3000)
@PostMapping("/orders")
public Result<Order> createOrder(@RequestBody OrderRequest request) { ... }
```

**PRD notes**: 基于 Redis 实现分布式去重，无 Redis 时降级为内存去重（仅单实例有效）

---

#### 7.10 ApiVersionAutoConfiguration

**PRD AutoConfiguration table**: `ApiVersionAutoConfiguration` | API 版本路由 | `@ApiVersion` 注解触发

**PRD API design**: URL版本（推荐）：`/v1/users` / `/v2/users`；Header版本：`Api-Version: 2`
```java
@ApiVersion("v2")  // 注解在 Controller 或方法上声明版本
```

---

#### 7.11 IdGeneratorAutoConfiguration

**PRD AutoConfiguration table**: `IdGeneratorAutoConfiguration` | 分布式 ID 生成 | 自动（替代数据库自增）

**PRD SPI table**: `api.id` | `IdGenerator` | SnowflakeIdGenerator

**PRD configuration**:
```yaml
afg:
  core:
    id-generator:
      type: SNOWFLAKE  # SNOWFLAKE / SEGMENT / UUID
      snowflake:
        worker-id: 1
        datacenter-id: 1
```

**PRD full description**: IdGenerator SPI — Snowflake / Segment / UUID 三种策略，`@GeneratedValue(generator=SNOWFLAKE)`

---

### Files Found

| File Path | Description |
|---|---|
| `core/src/main/java/io/github/afgprojects/framework/core/autoconfigure/CacheAutoConfiguration.java` | Cache AutoConfiguration — canonical pattern example |
| `core/src/main/java/io/github/afgprojects/framework/core/autoconfigure/LockAutoConfiguration.java` | Lock AutoConfiguration — annotation + SPI + NoOp pattern |
| `core/src/main/java/io/github/afgprojects/framework/core/autoconfigure/SchedulerAutoConfiguration.java` | Scheduler AutoConfiguration — complex multi-bean pattern with inner classes |
| `core/src/main/java/io/github/afgprojects/framework/core/autoconfigure/AuditLogAutoConfiguration.java` | AuditLog AutoConfiguration — storage-type SPI selection pattern |
| `core/src/main/java/io/github/afgprojects/framework/core/autoconfigure/EventAutoConfiguration.java` | Event AutoConfiguration — inner static class + NoOp conditional pattern |
| `core/src/main/java/io/github/afgprojects/framework/core/autoconfigure/AfgAutoConfiguration.java` | Base AutoConfiguration — all others declare `after = AfgAutoConfiguration.class` |
| `core/src/main/java/io/github/afgprojects/framework/core/config/AfgCoreProperties.java` | Unified configuration properties — 1630 lines, all nested config classes |
| `core/src/main/java/io/github/afgprojects/framework/core/lock/Lock.java` | @Lock annotation — method-level annotation pattern |
| `core/src/main/java/io/github/afgprojects/framework/core/lock/LockAspect.java` | LockAspect — canonical AOP aspect pattern with SpEL |
| `core/src/main/java/io/github/afgprojects/framework/core/lock/DistributedLock.java` | DistributedLock SPI interface |
| `core/src/main/java/io/github/afgprojects/framework/core/lock/NoOpDistributedLock.java` | NoOp implementation pattern — tryXxx=true, isXxx=false, void=no-op |
| `core/src/main/java/io/github/afgprojects/framework/core/feature/FeatureToggle.java` | @FeatureToggle annotation — annotation with fallbackMethod |
| `core/src/main/java/io/github/afgprojects/framework/core/feature/FeatureToggleAspect.java` | FeatureToggle aspect — fallback method invocation pattern |
| `core/src/main/java/io/github/afgprojects/framework/core/audit/Audited.java` | @Audited annotation — annotation with sensitiveFields, recordArgs |
| `core/src/main/java/io/github/afgprojects/framework/core/audit/AuditLogAspect.java` | AuditLog aspect |
| `core/src/main/java/io/github/afgprojects/framework/core/cache/spi/CacheStorageProvider.java` | SPI interface pattern — factory/provider SPI |
| `core/src/main/java/io/github/afgprojects/framework/core/cache/spi/NoOpCacheStorageProvider.java` | NoOp factory/provider pattern — isAvailable()=false |
| `core/src/main/java/io/github/afgprojects/framework/core/api/scheduler/NoOpDelayQueue.java` | NoOp generic SPI pattern — NoOpDelayQueue<T> |
| `core/src/main/java/io/github/afgprojects/framework/core/web/exception/GlobalExceptionHandler.java` | GlobalExceptionHandler — already exists, validation exception handling already works |
| `core/src/main/java/io/github/afgprojects/framework/core/web/validation/Phone.java` | @Phone validation annotation — already exists |
| `core/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | Current 31 AutoConfiguration entries |
| `core/build.gradle.kts` | Module dependencies |
| `docs/framework-prd.md` | PRD — 11 missing feature definitions |

### Code Patterns Summary

**Pattern 1: AutoConfiguration class**
```
Package: io.github.afgprojects.framework.core.autoconfigure
Class annotations: @AutoConfiguration(after=AfgAutoConfiguration.class) + @ConditionalOnProperty + @EnableConfigurationProperties(AfgCoreProperties.class)
Method annotations: @Bean + @ConditionalOnMissingBean (for SPI defaults) + @ConditionalOnBean + @ConditionalOnProperty(prefix="afg.core.{feature}.annotations")
```

**Pattern 2: SPI interface**
```
Cross-module: io.github.afgprojects.framework.core.api.{feature}.{SpiInterface}
Same-module: io.github.afgprojects.framework.core.{feature}.{SpiInterface}
NoOp naming: NoOp{InterfaceName}
NoOp semantics: tryXxx=true, isXxx=false, void=no-op, query=empty/null/0
Registration: @Bean + @ConditionalOnMissingBean in AutoConfiguration
```

**Pattern 3: Annotation + AOP**
```
Annotation: @Target(METHOD) + @Retention(RUNTIME) — in feature package or annotation package
Aspect: @Aspect + @Slf4j — constructor injection of SPI + AfgCoreProperties
AOP method: @Around("@annotation(annotation)") — get annotation instance for attribute extraction
SpEL: SpelExpressionParser for dynamic keys
AutoConfiguration: @Bean + @ConditionalOnMissingBean + @ConditionalOnBean + @ConditionalOnProperty(annotations.enabled)
```

**Pattern 4: Configuration Properties**
Two approaches coexist:
1. Nested classes in `AfgCoreProperties` (e.g., `AfgCoreProperties.LockConfig`)
2. Separate `@ConfigurationProperties` classes in `properties/{feature}/` package (e.g., `AfgCoreLockProperties`)
Both are `@EnableConfigurationProperties`-registered.

**Pattern 5: Web-layer features**
```
Filters/Interceptors: io.github.afgprojects.framework.core.web.{feature}/
Registered in AutoConfiguration via @Bean + @ConditionalOnMissingBean
```

**Pattern 6: Test**
```
Pure unit tests (no Spring context)
@DisplayName + @Nested grouping
shouldXxx_whenYyy naming
AssertJ assertions
Direct instantiation
```

### Related Specs

- `.trellis/spec/backend/` — backend spec directory exists but content not examined
- `.trellis/spec/guides/` — guides spec directory exists but content not examined

## Caveats / Not Found

1. **No full 7-step PRD documentation exists for the 11 missing features** — only AutoConfiguration table entries, SPI table entries, and brief API code snippets. The implementer will need to extrapolate from existing feature patterns and the partial PRD definitions.

2. **GlobalExceptionHandler already exists** — it already handles `MethodArgumentNotValidException` and `ConstraintViolationException` in `core.web.exception`. The ValidationAutoConfiguration may only need to register it as a conditional bean and add validation-specific properties.

3. **Properties dual approach** — AfgCoreProperties has nested classes AND there are separate @ConfigurationProperties classes in `properties/` package. Both patterns coexist. The `properties/` approach seems to be the newer pattern (individual files per feature). New features should follow the `properties/{feature}/` approach.

4. **No existing tests for AutoConfiguration classes** — the core module only has unit tests for business logic, not for AutoConfiguration wiring. Integration tests for AutoConfiguration would need `@SpringBootConfiguration` + `@ImportAutoConfiguration`.

5. **PRD mentions `@AfgEnum` as an APT annotation** (line 974) — this may need to be defined in `apt-api` module rather than core, similar to `@AfEntity`.

6. **Existing AutoConfiguration count is 31** (from imports file). The PRD lists 41 (31 existing + 11 missing). The missing 10 are: AccessLog, Validation, SSE, StateMachine, EnumManagement, ImportExport, Notification, Webhook, DuplicateSubmit, ApiVersion, IdGenerator (actually 11).
