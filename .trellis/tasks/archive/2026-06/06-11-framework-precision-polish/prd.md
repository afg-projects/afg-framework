# AFG Framework 精细化打磨 — 按 PRD 逐模块拆解整改验收

## Goal

基于 `docs/framework-prd.md`（2814 行完整 PRD），将框架每个功能模块拆解为精细化开发任务，按标准流程逐一整改验收。不是简单检查"是否已实现"，而是确保每个实现都达到 PRD 定义的质量底线：每个功能有 NoOp 降级、有开/关条件、有测试覆盖、AutoConfiguration 声明依赖排序、配置项有合理默认值、SPI 有本地默认实现。

## What I already know

### PRD 结构（12 章）
1. 产品概述（设计哲学、价值观、质量底线、差异化优势）
2. 快速开始（环境、创建项目、最小配置、CRUD 示例）
3. 核心概念（DataManager vs JPA、APT、自动配置、基类决策树、模块化、增强原则）
4. 模块架构（20 模块、依赖关系、Maven 坐标、选择指南、Boot 对比）
5. 逐模块功能需求（9 个模块的 6 段式详细描述）
6. 框架开发规范体系（8 大类规范）
7. 版本策略与演进
8. 框架边界定义
9. 方言完整度矩阵
10. 脚手架生成规范
11. 配置属性完整参考
12. 附录（AutoConfiguration 清单、ErrorCode、竞品对比、AI 节点、基类速查、操作符速查、约定速查卡）

### 质量底线（PRD §1.5）
1. 每个功能必须有 NoOp 降级实现
2. 每个功能必须有开/关注解或条件
3. 每个功能必须有测试覆盖（集成测试 + Testcontainers，禁止 Mockito）
4. 每个 AutoConfiguration 必须声明依赖排序
5. 每个配置项必须有合理默认值
6. 每个 SPI 必须有本地默认实现

### 模块分布
- commons（底层工具）
- apt-api + apt-impl（编译时元数据）
- core（31+ AutoConfiguration，最复杂）
- data-core + data-sql + data-jdbc + data-liquibase（DataManager 核心）
- security-core + auth-server + resource-server（安全）
- ai-core + ai-spring-ai + ai-langchain4j（AI）
- afg-redis（Redis 集成）
- governance/proto + client + server（治理）
- gradle-plugin（脚手架）

## Decisions (confirmed)

1. **整改而非重写**：基于现有代码按 PRD 标准整改，非推翻重来
2. **逐模块验收**：每个模块独立拆任务，独立验收
3. **质量底线不可妥协**：6 条质量底线是硬性要求
4. **按依赖链排序**：从底层到上层，commons → apt → data → core → security → ai → integration → governance
5. **按模块同步推进**：每个模块同时整改已有代码质量 + 实现 PRD 缺失功能，完全对齐 PRD 后再进入下一个
6. **每个任务包含**：需求分析（vs PRD 差距）→ 代码整改 → 新功能实现 → 测试补充 → 规范对齐 → 验收确认

## Gap Analysis Summary (from codebase exploration)

### commons 模块
- **5 类缺失**：ArgumentAssert、DateUtils、CollectionUtils、StringUtils、IoUtils
- **1 类不完整**：CommonErrorCode 59/94，缺 35 个错误码
- ErrorCode.getMessage(args, locale) 未实现模板替换（args 被存储但不用于消息格式化）
- 已有 8 个类的测试覆盖良好

### core 模块
- **11 AutoConfiguration 缺失**：AccessLog、Validation、SSE、StateMachine、EnumManagement、ImportExport、Notification、Webhook、DuplicateSubmit、ApiVersion、IdGenerator
- **零 @AutoConfigureAfter 使用**（31 个现有 AutoConfiguration 全部未声明排序）
- **2 个用 @Configuration 而非 @AutoConfiguration**：CloudNative、KubernetesProbe
- **3 个缺少 @ConditionalOnMissingBean**：CloudNative、KubernetesProbe、BeanInvocation（partial）
- **4 注解缺失**：@DuplicateSubmit、@StateMachine、@AfgEnum、@ExcelSheet
- 已有注解（@Lock、@DistributedTask、@FeatureToggle、@Audited 等）存在

### data-core + data-impl 模块
- DataManager 接口完整（所有 PRD 方法已实现）
- **EntityProxy.withPessimisticLock() 缺失**
- **TreeEntity + Treeable 接口缺失**
- **@EncryptedField 注解缺失**
- **条件构建器 IfPresent 系列缺失**：eqIfPresent、likeIfPresent、inIfPresent、betweenIfPresent
- **SqlInsertBuilder ON DUPLICATE KEY UPDATE 缺失**
- 10 种方言已实现，关联注解已实现，SchemaComparator 已实现
- ProjectedQuery、EntityCache、SqlMetrics 已实现

### security 模块
- security-core SPI 接口完整（36 个接口/类）
- auth-server 9 个 AutoConfiguration 全部存在
- resource-server 2 个 AutoConfiguration 存在（imports 多列了 ResourceSecurityConfigValidator）
- **4 个社交登录策略缺失**：Wechat、DingTalk、Feishu、WeCom（仅有 LoginStrategy SPI 和 THIRD_PARTY 扩展点）
- **2FA/TOTP 完全缺失**
- **密码重置流程完全缺失**
- @RequirePermission 和 @RequireRole 已实现

### AI 模块
- ai-core 16 个 AutoConfiguration 全部存在
- SPI 接口完整（agent、chat、workflow、rag、tool、skill、pipeline、planning、etl、resilience、security、observability、performance、persistence、memory、model）
- 9 个注解全部存在
- ai-langchain4j 完整（7 AutoConfiguration + 13 实现类）
- **ai-spring-ai 未被 settings.gradle.kts include，且 src/ 目录无源文件**

### afg-redis 模块
- RedisAutoConfiguration 存在但应拆分为子 Configuration
- 10 个 SPI 实现已存在（Cache、Lock、Scheduler、DelayQueue、Audit、RateLimit、Feature、Health 等）

## Open Questions

- 每个模块的整改粒度如何划分？按 AutoConfiguration？按功能域？按 SPI？
- PRD 中标记为"新增需求"但尚无任何代码的功能（如 @DuplicateSubmit、@StateMachine、@AfgEnum、TreeEntity、@EncryptedField、社交登录、2FA、密码重置），是先新建还是先整改已有代码？
- 已有代码的质量整改优先级如何排序？

## Requirements — 精细化任务拆解

> 每个子任务按依赖链顺序排列，同一模块内的子任务按功能域分组。
> 每个子任务验收标准：6 条质量底线达标 + PRD 功能对齐 + 测试覆盖。

### 阶段 1：commons 模块（零依赖，最底层）

**T1.1 commons 现有代码质量整改**
- ErrorCode.getMessage(args, locale) 实现模板替换（当前 args 被存储但未用于格式化）
- CommonErrorCode 补齐 35 个缺失错误码（PRD 要求 94 个，当前 59 个）
- 已有 8 个类的代码规范检查（Javadoc、命名、@Slf4j 规范）

**T1.2 commons 新增工具类实现**
- ArgumentAssert（notNull/notEmpty/isTrue/state → 抛 BusinessException）
- DateUtils（format/parse/between/isExpired）
- CollectionUtils（isEmpty/isNotEmpty/first/last/partition）
- StringUtils（isBlank/truncate/join/splitAndTrim）
- IoUtils（readAsString/copy/closeQuietly）
- 每个工具类的单元测试

### 阶段 2：APT 模块（apt-api + apt-impl）

**T2.1 APT 注解整改 + 新增**
- apt-api: 新增 @AfgEnum 注解（valueField/labelField/i18nPrefix）
- apt-api: 新增 @EncryptedField 注解（algorithm/keyRef）
- apt-impl: 新增 EnumMetadataProcessor
- apt-impl: 编译期校验规则实现（@Table 无 @AfEntity 报错、字段类型不支持报错、表名冲突报错等 5 条规则）
- 已有 Processor 的代码质量整改

**T2.2 APT 模块测试补充**
- 编译期校验规则的测试（使用 compiler testing framework）
- 元数据生成正确性的测试

### 阶段 3：data-core 模块

**T3.1 data-core 条件构建器增强**
- TypedConditionBuilder 新增 IfPresent 系列：eqIfPresent/likeIfPresent/inIfPresent/betweenIfPresent
- ConditionBuilder 新增对应 IfPresent 方法
- 空值语义文档化 + 测试

**T3.2 data-core 实体体系增强**
- 新增 TreeEntity<T> 基类（parentId/level/path/sortOrder/children）
- 新增 Treeable<T> 特征接口
- 新增 @EncryptedField 注解（从 apt-api 依赖）
- EntityProxy 新增 withPessimisticLock() 方法

**T3.3 data-core 代码质量整改**
- SPI 接口检查（每个 SPI 是否有默认实现 + NoOp 降级）
- 已有代码的 Javadoc + 规范对齐

### 阶段 4：data-sql 模块

**T4.1 data-sql SQL 构建器增强**
- SqlInsertBuilder 新增 ON DUPLICATE KEY UPDATE 支持
- 10 种方言的功能覆盖度验证（对齐 PRD §9 方言矩阵）
- 代码质量整改 + 测试补充

### 阶段 5：data-jdbc 模块

**T5.1 data-jdbc 核心实现整改**
- JdbcDataManager 代码质量整改
- JdbcEntityProxy 实现 withPessimisticLock()
- JdbcProjectedQuery 代码质量整改
- AutoConfiguration 规范整改（@AutoConfiguration + @AutoConfigureAfter + @ConditionalOnMissingBean）

**T5.2 data-jdbc AuditLogStorage 迁移**
- PRD 规划：DatabaseAuditLogStorage 从 integration/afg-jdbc 迁移到 data-jdbc
- 迁移后更新 AutoConfiguration 和依赖关系

**T5.3 data-jdbc 测试补充**
- 集成测试 + Testcontainers 覆盖核心 CRUD 路径
- 条件查询测试（含 IfPresent 系列）
- 分页、聚合、关联加载测试

### 阶段 6：data-liquibase 模块

**T6.1 data-liquibase 整改验收**
- LiquibaseAutoConfiguration 规范整改
- SchemaComparator 测试补充
- 内置迁移脚本完整性验证

### 阶段 7：core 模块（最复杂，按功能域拆分）

**T7.1 core AutoConfiguration 基础规范整改（影响全部 31 个）**
- 全部 31 个 AutoConfiguration 添加 @AutoConfigureAfter 声明
- CloudNativeAutoConfiguration、KubernetesProbeAutoConfiguration 从 @Configuration 改为 @AutoConfiguration
- 补齐缺失的 @ConditionalOnMissingBean（CloudNative、KubernetesProbe、BeanInvocation）
- 梳理完整的依赖链图

**T7.2 core 缓存域整改**
- CacheAutoConfiguration 规范整改 + @AutoConfigureAfter
- AfgCache SPI 接口验证（默认实现 Caffeine + NoOp 降级）
- 缓存注解（@Cached/@CacheEvict/@CachePut）代码质量
- EntityCache 与业务缓存关系梳理
- 测试补充

**T7.3 core 锁域整改**
- LockAutoConfiguration 规范整改
- @Lock 注解 + AOP 实现代码质量
- DistributedLock SPI 验证（默认内存锁 + NoOp）
- 测试补充

**T7.4 core 事件域整改**
- EventAutoConfiguration 规范整改
- EventPublisher/EventSubscriber SPI 验证
- @EventHandler 注解 + AOP 代码质量
- 本地/分布式事件自动升级逻辑
- 测试补充

**T7.5 core 调度域整改**
- SchedulerAutoConfiguration 规范整改
- @DistributedTask/@ScheduledTask/@DelayTask 注解代码质量
- DistributedTaskScheduler SPI 验证
- DelayQueue SPI 验证
- 动态任务管理功能验证
- 测试补充

**T7.6 core 审计域整改**
- AuditLogAutoConfiguration 规范整改
- @Audited 注解 + AOP 代码质量
- AuditLogStorage SPI 验证（默认 log + JDBC + Redis 三级）
- AccessLogAutoConfiguration **新建**（访问日志过滤器）
- 测试补充

**T7.7 core 校验域整改**
- ValidationAutoConfiguration **新建**（Bean Validation 统一异常处理）
- GlobalExceptionHandler 与校验异常统一处理
- @Phone 验证器注册
- 测试补充

**T7.8 core 功能开关系列整改**
- FeatureFlagAutoConfiguration + FeatureFlagWebAutoConfiguration 规范整改
- @FeatureToggle 注解 + AOP 代码质量
- FeatureFlagManager SPI 验证
- 测试补充

**T7.9 core 限流域整改**
- RateLimitAutoConfiguration 规范整改
- @RateLimit 注解 + AOP 代码质量
- RateLimitStorage SPI 验证
- DuplicateSubmitAutoConfiguration **新建** + @DuplicateSubmit 注解
- 测试补充

**T7.10 core 安全增强域整改**
- AfgSecurityAutoConfiguration 规范整改
- SignatureAutoConfiguration 规范整改
- @SignatureRequired 代码质量
- DataScopeAutoConfiguration 规范整改
- @DataScope 注解代码质量

**T7.11 core 新功能域实现**
- SseAutoConfiguration **新建** + SseConnectionManager SPI + 默认内存实现
- IdGeneratorAutoConfiguration **新建** + IdGenerator SPI + SnowflakeIdGenerator 默认实现
- NotificationAutoConfiguration **新建** + NotificationService SPI + LogNotificationService 默认
- WebhookAutoConfiguration **新建** + WebhookService SPI + 默认内存实现
- StateMachineAutoConfiguration **新建** + @StateMachine + @Transition + StateMachine SPI
- EnumManagementAutoConfiguration **新建** + @AfgEnum（配合 apt-impl）
- ImportExportAutoConfiguration **新建** + @ExcelSheet + DataExporter/DataImporter SPI
- ApiVersionAutoConfiguration **新建** + @ApiVersion 注解
- 每个新功能域的 NoOp 降级实现 + 测试

**T7.12 core 其余 AutoConfiguration 整改**
- WebAutoConfiguration / HttpClientAutoConfiguration / LoggingAutoConfiguration
- MetricsAutoConfiguration / HealthAutoConfiguration / ShutdownAutoConfiguration
- EncryptionAutoConfiguration / RemoteConfigAutoConfiguration / LocaleAutoConfiguration
- BeanInvocationAutoConfiguration / ContextAutoConfiguration / AfgOpenApiAutoConfiguration
- MultiDataSourceAutoConfiguration / VirtualThreadAutoConfiguration
- ModuleAutoConfiguration / ModuleWebAutoConfiguration
- AfgCoreAutoConfiguration / AfgAutoConfiguration

### 阶段 8：security-core 模块

**T8.1 security-core SPI 接口整改**
- 全部 36 个 SPI 接口的 Javadoc + 规范对齐
- 验证每个 SPI 是否有默认实现
- 测试补充

### 阶段 9：auth-server 模块

**T9.1 auth-server AutoConfiguration 规范整改**
- 9 个 AutoConfiguration 添加 @AutoConfigureAfter
- @ConditionalOnMissingBean 检查

**T9.2 auth-server 社交登录实现**
- WechatLoginStrategy 实现
- DingTalkLoginStrategy 实现
- FeishuLoginStrategy 实现
- WeComLoginStrategy 实现
- 每个策略的配置属性 + 测试

**T9.3 auth-server 2FA/TOTP 实现**
- TOTP 服务 + QR 码生成
- 二次认证流程
- 配置属性 + 测试

**T9.4 auth-server 密码重置实现**
- 重置请求 → 令牌发送 → 令牌验证 → 密码重置流程
- 配置属性 + 测试

**T9.5 auth-server 其余功能整改**
- LoginService/TokenService/CaptchaService 代码质量
- Casbin RBAC 集成验证
- 多租户流程验证
- OAuth2 授权码流程验证（含 PKCE）
- 数据权限自动注入验证

### 阶段 10：resource-server 模块

**T10.1 resource-server 整改验收**
- 2 个 AutoConfiguration 规范整改
- @RequirePermission/@RequireRole AOP 代码质量
- JWT 验证流程验证
- 远程权限校验验证
- ResourceSecurityConfigValidator 是否应从 imports 移除
- 测试补充

### 阶段 11：ai-core 模块

**T11.1 ai-core AutoConfiguration 规范整改**
- 16 个 AutoConfiguration 添加 @AutoConfigureAfter
- @ConditionalOnMissingBean 检查
- 确保 NoOp 降级实现完整

**T11.2 ai-core 功能域整改（按子域）**
- Chat: @AiChat AOP + ChatClientRegistry + DefaultChatClientRegistry
- Agent: @AiAgent AOP + AgentExecutor + ReAct/PlanExecute/Reflection
- Workflow: DagEngine + 37 节点实现验证 + CheckpointManager
- RAG: VectorStore + KnowledgeBaseService + NoOpVectorStore
- Tool: @ToolExecution + ToolRegistry + 远程工具发现
- Skill: SkillRegistry + SkillDispatcher + IntentAnalyzer
- Pipeline: ChatPipeline + PipelineStep
- Resilience: @AiResilient + CircuitBreaker + RetryPolicy + FallbackStrategy
- Security: @ContentSafety + PiiDetector + ApiKeyManager
- Observability: @AiAudited + AuditLogger + MetricsCollector + Tracer
- Persistence: SessionStore + MessageHistoryStore
- ETL: EtlPipeline + Source + Transformer + 文档分片
- 每个子域的 NoOp 降级验证 + 测试

### 阶段 12：ai-langchain4j 模块

**T12.1 ai-langchain4j 整改验收**
- 7 个 AutoConfiguration 规范整改
- 13 个适配类代码质量
- @ConditionalOnClass(LangChain4J) 条件验证
- 测试补充（WireMock 模拟 LLM API）

### 阶段 13：afg-redis 模块

**T13.1 afg-redis AutoConfiguration 拆分**
- RedisAutoConfiguration 拆分为子 Configuration：Cache/Lock/Scheduler/Audit/RateLimit/Feature/Health
- 每个子 Configuration 的 @AutoConfigureAfter
- 10 个 SPI 实现的代码质量整改
- 测试补充（Testcontainers Redis）

### 阶段 14：governance 模块

**T14.1 governance 模块整改验收**
- proto: gRPC 定义验证
- client: GovernanceClientAutoConfiguration 规范整改
- server: GovernanceServerAutoConfiguration 规范整改
- 测试补充

### 阶段 15：gradle-plugin 模块

**T15.1 gradle-plugin 整改验收**
- afgInit 输出规范验证
- generateMigration 代码生成正确性
- generateDbDoc 新任务实现
- 测试补充

## Acceptance Criteria

- [ ] 每个模块的 6 条质量底线全部达标：
  1. 每个功能有 NoOp 降级实现
  2. 每个功能有开/关注解或条件
  3. 每个功能有测试覆盖（集成测试 + Testcontainers，禁止 Mockito）
  4. 每个 AutoConfiguration 声明 @AutoConfigureAfter
  5. 每个配置项有合理默认值
  6. 每个 SPI 有本地默认实现
- [ ] 每个模块的代码与 PRD 功能清单对齐
- [ ] 每个 AutoConfiguration 使用 @AutoConfiguration（非 @Configuration）
- [ ] 可替换 Bean 使用 @ConditionalOnMissingBean
- [ ] 测试全绿（`./gradlew test`）
- [ ] 代码无 PMD 告警
- [ ] 所有新增功能有 Javadoc 和使用示例

## Definition of Done

- 所有模块按 PRD 标准整改验收完毕
- 质量底线全部达标
- 测试全绿（`./gradlew test`）
- 代码无 PMD 告警

## Out of Scope (explicit)

- 新增 PRD 中标记为"新增需求"但尚未设计 SPI 接口的功能（需先在 PRD 中完成设计）
- 性能基准测试
- Native Image 支持
- ai-spring-ai 模块（当前未被 settings.gradle.kts include）

## Technical Notes

- PRD 路径：`docs/framework-prd.md`
- 已有任务：`06-11-afg-framework-prd`（PRD 文档完善任务，in_progress）
- Trellis spec 目录：`.trellis/spec/backend/`（已有 13 个规范文件）
- Trellis guides 目录：`.trellis/spec/guides/`（已有 4 个思维指南）
- 测试铁律：禁止 Mockito，使用 Testcontainers + 真实数据
- 当前 git 状态：64 个未提交变更
