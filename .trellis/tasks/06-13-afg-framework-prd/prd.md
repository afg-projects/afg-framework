# 完善 AFG Framework PRD — 使用者视角全面评估与规范制定

## Goal

从使用者（特别是初学者）的角度，全面评估和完善 AFG Framework 的产品需求文档（PRD），使其达到行业标杆框架（Spring Boot、Quarkus、Spring AI）的文档标准。PRD 定位为产品需求蓝图，定义"框架应该达到什么状态"，引导框架向完整齐全的目标推进。

## What I already know

### 当前 PRD 状态（docs/framework-prd.md）
- 2814 行，结构已较完善，12 个章节
- 快速开始、核心概念章节已有，但存在与代码库不一致的问题
- 8 大规范体系章节已有，但深度不及 Quarkus 的 rule+example+antipattern 标准
- AI 模块章节偏接口清单，缺少使用场景和 4 级代码示例
- 安全模块缺少 OAuth2 流程图、生产加固指南

### 代码验证发现的差异（14 处）
- 3 处硬伤：插件 ID 错误、signing-key 不存在、@Table/@Column 来源未注明
- 5 处描述不准确：零反射过于绝对、缓存描述不精确、注解触发依赖 AutoConfiguration、afg-redis 需 RedissonClient 前提、page() 返回值
- 6 处内容缺失：springAiVersion、TreeEntity、core/data-core 双实体体系、SPI 扩展机制、自定义条件注解、Jackson 增强

### 行业标准研究结论
- Spring Boot 采用 3 层内容体系（Reference / How-to / Tutorials），是文档金标准
- Quarkus 明确采用 Diataxis 框架（4 种内容类型），贡献者文档格式最完善（rule+example+antipattern）
- Spring AI 按 capability 组织（Chat → per-provider），每个功能遵循 concept→min example→common→advanced→config→degradation 7 步模式
- 行业 4 级代码示例标准：最小可用(3-10行) → 常见用法(20-50行) → 高级用法(50-150行) → 完整配置
- 安全文档必须包含生产加固指南（行业普遍缺失，但 AFG 应做到）
- 37 种工作流节点需按 7 个类别分组（Control Flow / AI Operations / Data / Human / Integration / Utility / Observability）
- 配置属性必须 6 字段文档化（name, type, default, description, example, related）
- 行业常见 PRD 缺陷：示例不可运行、安全缺生产加固、教程假设经验、NoOp 降级行为未文档化

## Decisions (confirmed)

1. **文档定位**：单一文档，产品需求蓝图，定义"框架应该达到什么状态"
2. **规范范围**：全量规范体系（8 大类）
3. **AI 定位**：核心模块，完整定义每个子功能
4. **未实现标注**：纯目标状态，不区分当前是否已实现
5. **文档结构**：分层递进式
6. **描述模板**：6 段式
7. **执行策略**：集中火力做好每一步，按优先级逐个推进
8. **行业标准对标**：以 Spring Boot（结构金标准）+ Quarkus（贡献者文档金标准）+ Spring AI（AI 文档金标准）为标杆
9. **代码示例标准**：遵循 4 级深度（最小可用 → 常见用法 → 高级用法 → 完整配置）
10. **规范格式标准**：遵循 Quarkus rule + good example + bad example + rationale 格式

## Open Questions

- 无

## Requirements (evolving)

### Phase 1：快速开始 — 对标 Spring Boot Getting Started 标准 🔴

行业标准：7 个必需元素（prerequisites, project creation, dependencies, minimal config, first example, verification, next steps）

- [ ] 修正插件 ID：`io.github.afg-projects.framework-plugin`
- [ ] 补充 `springAiVersion` 属性到 afg 扩展块
- [ ] 注明 `@Table/@Column` 来自 `jakarta.persistence`
- [ ] 修正安全配置：`signing-key` → 完整的 Token 配置说明（含 RS256 密钥对和 HS256 对称密钥两种方案）
- [ ] 添加 Maven 依赖声明（与 Gradle 并列展示，行业标准要求）
- [ ] 补充"下一步"链接（指向核心概念和功能指南）
- [ ] 补充故障排查小节（常见启动错误和解决方案）
- [ ] 确保 Controller 示例的 `page()` 用法与实际 API 一致
- [ ] 补充 Result vs Results 使用推荐

### Phase 2：核心概念 — 对标 Spring Boot/Quarkus 概念章节标准 🔴

行业标准：概念解释 + 竞品对比 + 设计决策说明 + 架构图 + 关键接口

- [ ] 3.1 修正"零反射"为"APT 优先，反射降级"，补充 SPI 扩展机制
- [ ] 3.1 修正缓存描述为"AfgCache 统一抽象 + 本地/Redis 实现"
- [ ] 3.2 补充 APT SPI 扩展点（ServiceLoader 发现加载器）
- [ ] 3.2 补充 IDE annotation processing 启用前提
- [ ] 3.3 修正"注解自动触发"为"依赖 AutoConfiguration 注册切面 Bean"
- [ ] 3.3 补充 afg-redis 需 `RedissonClient` Bean 的前提条件
- [ ] 3.3 补充 3 个自定义条件注解（`@ConditionalOnFeature`, `@ConditionalOnPropertyNotEmpty`, `@ConditionalOnTenant`）
- [ ] 3.4 补充 `TreeEntity` 到决策树
- [ ] 3.4 明确推荐 data-core 实体体系，说明 core 与 data-core 的差异
- [ ] 3.5 补充 `@AutoConfigureAfter` 与 `@AfgModuleAnnotation.dependencies` 的关系
- [ ] 3.5 补充 `basePackage` 通过 `@AliasFor(ComponentScan.class)` 实现组件扫描
- [ ] 3.6 补充 Jackson 增强和自定义条件注解案例
- [ ] 为每个核心概念添加架构关系图（Mermaid）

### Phase 3：功能使用说明 — 对标 Spring AI 7 步文档模式 🔴

行业标准：每个功能遵循 concept → min example → common usage → advanced usage → configuration → degradation → limitations

- [ ] 校验/Validation 使用说明（4 级示例：@Valid 注解 → 常见校验 → 自定义校验器 → 完整配置）
- [ ] 异常全局处理说明（GlobalExceptionHandler 行为 + 自定义异常 + i18n）
- [ ] 缓存使用说明（AfgCache API → Caffeine 本地 → Redis 分布式 → 多级缓存）
- [ ] 分布式锁使用说明（@Lock 注解 → Redisson 分布式锁 → 锁超时/重入）
- [ ] 事件发布/订阅使用说明（@EventHandler → 本地事件 → RabbitMQ 分布式事件）
- [ ] 定时任务/调度使用说明（@DistributedTask → 本地调度 → Redis 分布式调度）
- [ ] 多数据源使用说明（DataSource 路由 → 读写分离 → 动态切换）
- [ ] 国际化/i18n 使用说明（messages.properties → BusinessException i18n → 自定义 LocaleResolver）
- [ ] 功能开关使用说明（@ConditionalOnFeature → afg.{module}.enabled → 自定义条件）
- [ ] API 文档/Swagger 使用说明（SpringDoc 集成 → 模块 context-path 处理 → 安全端点文档化）
- [ ] 日志使用说明（@Slf4j → MDC 增强 → 结构化日志 → 敏感信息脱敏）
- [ ] Bean 动态调用使用说明（@AfService/@AfOperation → 服务注册 → 动态调用）

### Phase 4：DataManager 完整化 — 对标 MyBatis-Plus 文档深度 🔴

行业标准：完整 API 速查 + 每个操作 4 级示例 + 生命周期说明 + 最佳实践

- [ ] DataManager 完整使用指南（初始化 → CRUD → 条件查询 → 分页 → 聚合 → 关联）
- [ ] 条件查询动态条件说明（eqIfPresent / likeIfPresent / inIfPresent 等空值跳过语义）
- [ ] 关联关系完整示例（@ManyToOne EAGER → @OneToMany LAZY + 预加载 → @ManyToMany 中间实体）
- [ ] 软删除完整生命周期（创建 → 查询自动过滤 → 软删除 → 恢复 → 物理删除 → 唯一约束处理）
- [ ] 乐观锁使用说明（@Versioned → 冲突检测 → OptimisticLockException → 重试策略）
- [ ] 审计字段自动填充说明（Auditable → SecurityContext 自动注入 → 手动覆盖）
- [ ] 多租户完整配置步骤（3 种策略：TOKEN/HEADER/DOMAIN → TenantEntity → 数据隔离验证）
- [ ] 数据权限完整使用说明（DataScopeType → @DataScope → 行级自动注入 → 自定义数据权限）
- [ ] SQL 构建器使用示例（Conditions.builder → 动态条件 → 组合条件 allOf/anyOf → 嵌套条件）
- [ ] 类型处理器自定义说明（@JsonTypeHandler → 自定义 TypeHandler → 注册方式）
- [ ] 文件存储使用说明（Local → MinIO → S3/OSS → 配置切换）

### Phase 5：安全模块 — 对标 Spring Security 文档深度 🔴

行业标准：认证架构图 + 每种认证机制 + OAuth2 流程图 + 权限模型 + 生产加固 + 安全测试

- [ ] 安全架构概览（Mermaid 架构图：3 种部署模式的组件交互）
- [ ] 认证机制完整说明（用户名密码 → 手机号 → 邮箱 → 验证码 → OAuth2 → 设备绑定）
- [ ] AfgUserDetailsService 实现指南（接口契约 → 完整实现示例 → 测试验证）
- [ ] OAuth2 授权码流程说明（Mermaid 序列图 → 客户端注册 → PKCE → Token 管理）
- [ ] 权限模型说明（Casbin RBAC-Domain → 策略配置 → 数据权限 → 角色继承）
- [ ] 密码策略说明（BCrypt → 密码强度校验 → 密码过期 → 历史密码检查）
- [ ] 生产加固指南（HTTPS → CORS → CSRF → 限流 → IP 限制 → 安全头 → 密钥轮换）
- [ ] 安全测试指南（Testcontainers + 真实数据库 → 认证测试 → 权限测试 → 多租户隔离测试）

### Phase 6：AI 模块 — 对标 Spring AI 文档深度 🟡

行业标准：AI Concepts → Engine Selection → Per-feature 7 步 → Workflow 分类 → 降级文档

- [ ] AI 使用场景说明（智能客服 → 文档问答 → 代码助手 → 数据分析 → Agent 自动化）
- [ ] AI 引擎选择指南（Spring AI vs LangChain4J 对比表 → 场景推荐 → 迁移路径）
- [ ] 各功能 4 级示例（Chat → Embedding → Agent → RAG → Tool → Memory → Streaming）
- [ ] 工作流 37 种节点分类说明（7 个类别：Control Flow / AI Operations / Data / Human / Integration / Utility / Observability）
- [ ] Skill 系统完整说明（概念 → 注册 → 发现 → 执行 → 组合）
- [ ] ETL 管线使用说明（Source → Transformer → Splitter → 向量化 → 存储）
- [ ] ai-spring-ai 适配模块说明（ChatClient 适配 → Advisor → Memory → Observation）
- [ ] 降级行为文档化（NoOp → Default → Provider 实现，每个 SPI 的降级路径）

### Phase 7：规范体系 — 对标 Quarkus rule+example+antipattern 格式 🟡

行业标准：规则 + 正确示例 + 错误示例 + 原理 + 检查清单

- [ ] 编码规范（命名 + 注释 + 日志 + Lombok — 每条规则 good/bad example）
- [ ] API 设计规范（RESTful + 统一响应 + 版本管理 + 错误格式 — good/bad example）
- [ ] 异常处理规范（分类 + 错误码 + 全局处理 — good/bad example）
- [ ] 配置规范（命名 + 层级 + 默认值 + 属性分级 — good/bad example）
- [ ] 实体设计规范（基类选择 + 字段约定 + 关联设计 — good/bad example）
- [ ] 模块开发规范（AutoConfiguration 7 条铁律 + SPI 设计 — good/bad example）
- [ ] 依赖管理规范（依赖规则 + 版本管理 + 禁止项 — good/bad example）
- [ ] 安全规范（编码规则 + 敏感数据 + 注入防护 — good/bad example）

### Phase 8：扩展维度 🟡

- [ ] 版本策略（alpha/beta/GA 功能分级 — 对标 Quarkus maturity matrix）
- [ ] 脚手架生成规范（afgInit 输出规范 + 生成文件完整性验证）
- [ ] 框架边界定义（功能边界矩阵 + 不造轮子清单 — 对标 Spring Boot 原生功能完整对照）
- [ ] 方言完整度矩阵（10 种数据库 × 11 个功能 — 支持/部分/不支持/计划 + Testcontainers 测试要求）

### Phase 9：缺失章节补充 🟢

对标 Spring Boot 完整章节结构，补充当前 PRD 缺少的章节：

- [ ] Testing 章节（测试策略 + Testcontainers + DataManager 数据准备 + 安全测试 — 对标 Spring Boot Testing 章节）
- [ ] How-to Guides 章节（常见任务导向的指南 — 对标 Spring Boot How-to）
- [ ] Migration 章节（版本升级说明 + 弃用 API 生命周期 — 对标 Spring Boot Upgrading）
- [ ] Production 章节（生产部署指南 + 配置加固 + 监控 + Actuator — 对标 Spring Boot Production-ready）
- [ ] 配置属性完整参考（6 字段文档化：name, type, default, description, example, related — 对标 Spring Boot Appendix）
- [ ] Troubleshooting 章节（常见错误 + 解决方案 — 对标 Quarkus FAQ）

## Acceptance Criteria (evolving)

- [ ] 一个 Java 初学者能在 30 分钟内跑通第一个 CRUD 应用（对标 Spring Boot Getting Started 标准）
- [ ] 每个功能模块都有至少 4 级代码示例（最小可用 → 常见用法 → 高级用法 → 完整配置）
- [ ] 每条规范都有 good/bad example 对比（对标 Quarkus 贡献者文档格式）
- [ ] 框架开发规范体系完整，新贡献者能按规范开发模块
- [ ] PRD 文档结构清晰，对标 Diataxis 4 种内容类型
- [ ] 所有配置属性 6 字段文档化（name, type, default, description, example, related）
- [ ] 安全模块包含生产加固指南
- [ ] AI 模块每个功能包含使用场景和降级行为说明
- [ ] 快速开始章节的代码示例与当前代码一致
- [ ] 核心概念章节的描述与实际行为一致

## Definition of Done

- PRD 文档达到行业标杆框架（Spring Boot + Quarkus + Spring AI）的文档标准
- 所有代码示例可运行或结构完整
- 规范体系每条规则都有 good/bad example
- 安全模块包含完整的生产加固指南
- AI 模块每个功能有使用场景 + 4 级示例 + 降级说明
- 配置属性完整参考，6 字段文档化

## Out of Scope (explicit)

- 实际代码实现修改（本次仅完善 PRD 文档和规范）
- 具体的 API 接口设计细节（属于实现阶段）
- 性能基准测试数据（属于实现验证阶段）
- 生成实际架构图（Mermaid 语法写入 PRD，渲染在实现阶段）

## Technical Notes

- 当前 PRD 路径：`docs/framework-prd.md`（2814 行）
- 插件 ID：`io.github.afg-projects.framework-plugin`（非 afg-framework）
- Token 配置：`key-store-path`（非 signing-key），支持 RS256 密钥对
- @Table/@Column 来自 `jakarta.persistence`，非框架自定义注解
- 双重实体体系：data-core（Long id + Instant）为推荐，core（String id + LocalDateTime）为旧版
- TreeEntity 存在于 data-core 模块，PRD 决策树遗漏
- 3 个自定义条件注解：@ConditionalOnFeature, @ConditionalOnPropertyNotEmpty, @ConditionalOnTenant
- APT 优先（priority=0），反射降级（priority=1000），SPI 扩展可插入
- afg-redis 需要 RedissonClient Bean 前提

### Research References

- [research/phase1-quickstart-verification.md](research/phase1-quickstart-verification.md) — 快速开始章节 9 项代码验证，3 处硬伤
- [research/phase1-concepts-verification.md](research/phase1-concepts-verification.md) — 核心概念章节 6 项验证，2 处关键发现（双实体体系、TreeEntity 缺失）
- [research/industry-prd-standards.md](research/industry-prd-standards.md) — 6 个标杆框架行业标准研究，40+ 条标准清单
