# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0-SNAPSHOT] - 2026-06-14

### Added

#### Core 模块

- **ID 生成器**: `IdGenerator` SPI（SnowflakeIdGenerator / UuidIdGenerator / NoOpIdGenerator），配置 `afg.core.id-generator.type`
- **SSE 连接管理**: `SseConnectionManager` SPI（LocalSseConnectionManager / NoOpSseConnectionManager），配置 `afg.core.sse.*`
- **通知服务**: `NotificationService` SPI（LogNotificationService / NoOpNotificationService），配置 `afg.core.notification.enabled`
- **Webhook 分发**: `WebhookService` SPI（LocalWebhookService / NoOpWebhookService），配置 `afg.core.webhook.*`
- **状态机**: `@StateMachine` / `@Transition` 注解，`StateMachineFactory` SPI（LocalStateMachineFactory / NoOpStateMachineFactory），配置 `afg.core.state-machine.enabled`
- **数据导入导出**: `@ExcelSheet` / `@ExcelColumn` 注解，`DataExporter` / `DataImporter` SPI（CsvDataExporter / CsvDataImporter / NoOp），配置 `afg.core.import-export.enabled`
- **枚举管理**: `EnumRegistry` SPI（LocalEnumRegistry / NoOpEnumRegistry），REST 端点 `GET /afg/enums`、`GET /afg/enums/{name}`，配置 `afg.core.enum-management.enabled`
- **防重复提交**: `@DuplicateSubmit` 注解 + `DuplicateSubmitAspect` AOP，`DuplicateSubmitChecker` SPI（Local / NoOp），配置 `afg.core.duplicate-submit.*`
- **访问日志**: `AccessLogFilter`，配置 `afg.core.access-log.*`
- **参数校验增强**: `ValidationAutoConfiguration`（集成 GlobalExceptionHandler + MethodValidationPostProcessor），配置 `afg.core.validation.enabled`
- AutoConfiguration 数量从 29 增加到 41（新增 AccessLog, Validation, SSE, IdGenerator, Notification, Webhook, StateMachine, ImportExport, EnumManagement, DuplicateSubmit, 以及原有 CloudNative, DataScope, Encryption, FeatureFlag, FeatureFlagWeb, Health, HttpClient, KubernetesProbe, Logging, Metrics, OpenApi, RateLimit, RemoteConfig, Shutdown, Signature, VirtualThread）
- 所有 AutoConfiguration 声明 `@AutoConfigureAfter` 依赖排序
- 所有 SPI 接口提供 NoOp 降级实现

#### Data 模块

- **TreeEntity**: 树形结构实体基类（+ parentId + path + sortOrder + transient children，implements Treeable<T>）
- **TreeQuery**: 树形查询接口（findChildren / findDescendants / findAncestors / findRoots / buildTree / moveNode），JdbcTreeQuery 实现
- **EntityChangedEvent**: 实体变更事件（entityType, entity, oldEntity, changeType: CREATED/UPDATED/DELETED/RESTORED），Spring ApplicationEvent 桥接
- **@DataSource 多数据源路由**: `@DataSource` 注解 + `DataSourceAspect` AOP + `DataSourceContextHolder`（ThreadLocal 栈式 push/pop）
- **IfPresent 动态条件**: `eqIfPresent` / `likeIfPresent` / `geIfPresent` / `leIfPresent` / `inIfPresent` / `betweenIfPresent` 空值自动跳过
- **FieldEncryptor SPI**: 字段加密 SPI，`@EncryptedField` 注解支持
- **withPessimisticLock**: 悲观锁查询支持
- **AUDITABLE / TREEABLE trait**: APT 和反射元数据正确检测审计和树形特征
- IdGenerator 集成 JdbcDataManager，insert 时预生成分布式 ID

#### Security 模块

- **社交登录**: `AbstractSocialLoginStrategy` 模板方法（exchangeToken → getUserInfo → mapToSystemUser），WechatLoginStrategy / DingTalkLoginStrategy / FeishuLoginStrategy / WeComLoginStrategy 实现，配置 `afg.security.auth-server.social.*`（Alpha 状态）
- **TOTP/2FA**: `TotpService` SPI（generateSecret / generateQrCodeUrl / verifyCode），`DefaultTotpService`（RFC 6238），`TwoFactorAuthenticationService`（setup / enable / disable / verify / recovery codes），REST 端点 `POST /auth-api/auth/totp/{setup,verify,enable,disable}`、`GET /auth-api/auth/totp/status`，配置 `afg.security.auth-server.totp.*`
- `SocialLoginAutoConfiguration` 和 `TotpAutoConfiguration` 新增
- auth-server AutoConfiguration 数量从 9 增加到 11
- Security Core SPI 新增 `totp` 子包

#### AI 模块

- **@Skill / @Tool 注解**: `@Skill(name, description, intentKeywords, category)` 技能声明，`@Tool(name, description, async)` 工具声明，`@ToolParam(name, description, required, defaultValue)` 工具参数
- **Registrar 机制**: `SkillRegistrar` / `ToolRegistrar`（ContextRefreshedEvent 扫描注册），`SkillAspect` / `ToolAspect` AOP
- **工作流节点补齐**: 从定义补齐到 35 种完整实现节点，9 个类别（input / ai / logic / tool / output / human / transform / rag / checkpoint）
- AutoConfiguration 数量从 16 增加到 18（新增 PersistenceAutoConfiguration, JdbcPersistenceAutoConfiguration）

#### Gradle 插件

- **generateDbDoc 任务**: 从实体类生成数据库文档（Markdown 格式输出到 docs/db-schema.md）

### Changed

#### 全模块

- **BusinessException 统一替换**: 135 处 IllegalArgumentException → BusinessException(CommonErrorCode.XXX) 替换（commons 9 + core 32 + security-core 2 + security-impl 22 + ai-core 32 + ai-impl 3 + integration 6 + governance 27 + gradle-plugin 2）
- **AutoConfiguration 依赖排序**: 全量修复 91 个 AutoConfiguration 的 `@AutoConfigureAfter` 声明
- **NoOp 降级实现**: 所有核心 SPI 接口提供 NoOp 降级，AutoConfiguration 中用 `@ConditionalOnMissingBean` 注册

#### Data 模块

- **PageData 统一迁移**: Page → PageData 统一分页模型
- **APT TIMESTAMPED/AUDITABLE trait 修正**: 正确区分时间戳特征和审计特征
- **时间戳自动填充**: JdbcDataManager 自动填充 createdAt / updatedAt

#### Security 模块

- **TenantFilter skip**: 跳过 OAuth2 authorize 端点和公共端点
- **Session endpoint**: SSO 会话端点，BearerTokenFilter 集成

### Fixed

- P0-01 XSS filter regex bypass 修复
- P0-02 ThreadLocal async propagation 修复
- Instant SQL parameter bug 修复
- JwksController module path 修复
- EntityConditionQueryHandler Page → PageData 迁移遗漏修复
- protobuf/gRPC 版本对齐（protobuf 4.34.1 → 3.25.8, protoc 3.25.2 → 3.25.8, protoc-gen-grpc 1.62.2 → 1.81.0）
- APT index includes basePackage from @AfgModuleAnnotation
- gRPC server port mapping 和 grpc-netty-shaded 排除
- PMD @Override 修复 + governance/server PMD UnusedPrivateMethod 修复
- LocalEventPublisherTest 异步测试时序问题修复
- GlobalExceptionHandlerTest 国际化测试失败修复
- CI 测试编译错误和依赖更新检查任务修复
- Gradle 插件生成模板异常替换

### Tests

- P2 全量测试补齐 — 55 个文件 +7647 行，覆盖 13 个零测试模块
- Commons 模块测试覆盖率 14.8% → 99.4%
- Data Layer convergence — data-core 56.7% → 72%, data-liquibase 0% → 40%
- Core 模块测试覆盖率提升至 84%
- AI-core 集成测试基础设施（Testcontainers + Ollama），Controller 测试覆盖
- Phase 0 convergence — 修复 11 个测试失败 + H2/Testcontainers 决策

### Docs

- PRD 行业标准深度优化 — 2814 → 6193 行
- PRD 与代码现状不一致项修正
- Framework PRD 新增 + 过时文档删除
- Trellis spec 更新 — 测试/自动配置/异常处理/实体设计规范
