# AFG Framework 验收矩阵

> **版本**: 1.0.0-SNAPSHOT
> **日期**: 2026-06-09
> **状态**: DRAFT

## 概述

本文档定义 afg-framework 的完整验收标准，用于收敛项目质量。每个模块沿 8 个维度评估，每个维度分三级：

| 级别 | 含义 |
|------|------|
| **MUST** | 阻塞项，必须 PASS 才能通过阶段 Gate |
| **SHOULD** | 推荐项，不阻塞但应在当前阶段完成 |
| **COULD** | 延期项，可在后续迭代完成 |

**收敛目标**: 测试全过 + 零 P0 缺陷

**状态标记**: `[PASS]` `[FAIL]` `[PARTIAL]` `[EXEMPT]` `[NA]`

---

## 8 维验收标准定义

| 维度 | MUST（阻塞） | SHOULD | COULD |
|------|------------|--------|-------|
| **D1 测试健康** | 0 失败 | 0 失败 + 0 @Disabled | Flaky rate <1% |
| **D2 覆盖率** | 模块特定阈值 | MUST + 10% | SHOULD + 10% |
| **D3 P0 缺陷** | 0 P0 | 0 P0 + 0 新 P1 | P1 总数有界 |
| **D4 功能完整** | AutoConfig 无 TODO/stub；NoOp 实现有文档 | AOP aspect 全部接入 | 声明 API 100% 有测试 |
| **D5 AutoConfig** | 每个 AutoConfig 能加载（smoke test） | 条件逻辑 on/off 测试 | 全部条件路径测试 |
| **D6 API 稳定** | 无 @Deprecated 无替代 | @Deprecated 有移除时间线 | 零 @Deprecated |
| **D7 文档对齐** | README 存在；版本目录匹配 | AutoConfig 列匹配 .imports | 全部 public API 有 Javadoc |
| **D8 架构完整性** | 无循环依赖 | ArchUnit 测试存在 | ArchUnit 覆盖全部分层规则 |

---

## 模块覆盖率阈值

| 模块 | 当前估算 | MUST | SHOULD | COULD | Phase |
|------|---------|------|--------|-------|-------|
| commons | 14.8% | 40% | 50% | 70% | 1 |
| apt-api | 0% | **豁免** | 豁免 | 50% | 1 |
| apt-impl | 63.7% | 50% | 70% | 80% | 1 |
| core | 70%+ | 60% | 70% | 80% | 3 |
| data-core | 56.7% | 60% | 70% | 80% | 2 |
| data-sql | 88.9% | 60% | 70% | 80% | 2 |
| data-jdbc | 19.2% | 40% | 60% | 70% | 2 |
| data-liquibase | 0% | 20% | 40% | 60% | 2 |
| security-core | 78.2% | 50% | 65% | 80% | 3 |
| auth-server | ~25% | 40% | 55% | 70% | 4 |
| resource-server | 0% | 25% | 40% | 60% | 4 |
| ai-core | ~15% | 30% | 45% | 60% | 5 |
| ai-langchain4j | 0% | 15% | 30% | 50% | 5 |
| afg-redis | 0% | 20% | 35% | 50% | 4 |
| afg-storage | 0% | 20% | 35% | 50% | 4 |
| afg-websocket | 0% | 15% | 30% | 50% | 4 |
| afg-rabbitmq | 0% | 20% | 35% | 50% | 4 |
| afg-jdbc | 0% | 20% | 35% | 50% | 4 |
| governance/proto | 0% | **豁免** | 豁免 | 豁免 | 5 |
| governance/client | 0% | 20% | 40% | 60% | 5 |
| governance/server | 0% | 30% | 45% | 60% | 5 |
| gradle-plugin | 0% | 15% | 30% | 50 | 5 |

---

## Phase 0: 前置阻塞项

> 必须在所有模块收敛前解决。任何 Phase 0 项未通过 = 后续阶段无法评估。

| ID | 项 | 解决方案 | 状态 |
|----|-----|---------|------|
| P0-01 | H2 vs Testcontainers 决策 | H2 允许在 afg-framework 内部用于数据层测试；Testcontainers 用于中间件集成测试和下游 afg-infra。更新 `testing-standards.md` 明确范围 | `[FAIL]` |
| P0-02 | 11 个测试失败修复 | ai-core: 添加 MySQL JDBC driver (7)；data-jdbc: 修正断言 (1)；auth-server: Casbin grouping + 密码长度 (3) | `[FAIL]` |
| P0-03 | ai-spring-ai 处置 | 排除出验收矩阵，标记 "not started"。Phase 5 完成前决定是否填充或移除 | `[FAIL]` |

### P0-02 失败测试详情

| 模块 | 测试类 | 根因 | 修复方式 |
|------|--------|------|---------|
| ai-core (×7) | AiResourceControllerTest, AiChatControllerTest, AiFullFlowTest, AiModelControllerTest, AiKnowledgeControllerTest, AiWorkflowControllerTest, AiAgentControllerTest | MySQL Testcontainers 缺少 JDBC driver | `testImplementation(libs.mysql.connector.java)` |
| data-jdbc (×1) | JdbcDataManagerCrudTest$ConditionQuery | 断言 expects 1 但数据有 2 匹配行 | 修正测试数据或断言值 |
| auth-server (×2) | CasbinAfgEnforcerTest$EnforceTests, CasbinAfgEnforcerTest$ClearPoliciesTests | RBAC-domain model 需要 grouping policy | 添加 self-referencing grouping policy |
| auth-server (×1) | DefaultPasswordValidatorTest$StrictPolicyTests | 密码 "AbcdEfgh1!2" 仅 11 字符，STRICT 要求 12 | 改为 12 字符密码 |

---

## Phase 1: Foundation

### commons

| 维度 | MUST | SHOULD | COULD | 当前 | 状态 |
|------|------|--------|-------|------|------|
| D1 测试健康 | 0 失败 | 0 @Disabled | Flaky <1% | 0 失败 | `[PASS]` |
| D2 覆盖率 | ≥40% | ≥50% | ≥70% | 14.8% | `[FAIL]` |
| D3 P0 缺陷 | 0 P0 | 0 新 P1 | P1 有界 | 0 P0 | `[PASS]` |
| D4 功能完整 | 无 TODO/stub | — | — | 待确认 | `[PARTIAL]` |
| D5 AutoConfig | 豁免 | — | — | — | `[EXEMPT]` |
| D6 API 稳定 | 无 @Deprecated 无替代 | — | — | 待确认 | `[PARTIAL]` |
| D7 文档对齐 | README 存在 | 包结构已文档化 | Javadoc | 待确认 | `[PARTIAL]` |
| D8 架构完整 | 不依赖其他 afg 模块 | ArchUnit 测试 | — | 待确认 | `[PARTIAL]` |

### apt-api

| 维度 | MUST | SHOULD | COULD | 当前 | 状态 |
|------|------|--------|-------|------|------|
| D1 测试健康 | 编译通过（豁免测试） | — | — | 编译通过 | `[PASS]` |
| D2 覆盖率 | **豁免** | 豁免 | ≥50% | 0% | `[EXEMPT]` |
| D3 P0 缺陷 | 0 P0 | — | — | 0 P0 | `[PASS]` |
| D4 功能完整 | 注解属性有合理默认值 | — | — | 待确认 | `[PARTIAL]` |
| D5 AutoConfig | 豁免 | — | — | — | `[EXEMPT]` |
| D6 API 稳定 | 无 @Deprecated 无替代 | — | — | 待确认 | `[PARTIAL]` |
| D7 文档对齐 | 注解 Javadoc 含 @since | — | — | 待确认 | `[PARTIAL]` |
| D8 架构完整 | 不依赖实现模块 | — | — | 待确认 | `[PARTIAL]` |

### apt-impl

| 维度 | MUST | SHOULD | COULD | 当前 | 状态 |
|------|------|--------|-------|------|------|
| D1 测试健康 | 0 失败 | 0 @Disabled | Flaky <1% | 0 失败 | `[PASS]` |
| D2 覆盖率 | ≥50% | ≥70% | ≥80% | 63.7% | `[PASS]` |
| D3 P0 缺陷 | 0 P0 | 0 新 P1 | — | 0 P0 | `[PASS]` |
| D4 功能完整 | Processor 覆盖：有效实体、缺失 @AfEntity、重复名 | — | — | 待确认 | `[PARTIAL]` |
| D5 AutoConfig | 豁免 | — | — | — | `[EXEMPT]` |
| D6 API 稳定 | 无 @Deprecated 无替代 | — | — | 待确认 | `[PARTIAL]` |
| D7 文档对齐 | README 存在；Processor 选项已文档 | — | — | 待确认 | `[PARTIAL]` |
| D8 架构完整 | 与 apt-api 无循环 | ArchUnit 测试 | — | 待确认 | `[PARTIAL]` |

---

## Phase 2: Data Layer

### data-core

| 维度 | MUST | SHOULD | COULD | 当前 | 状态 |
|------|------|--------|-------|------|------|
| D1 测试健康 | 0 失败 | 0 @Disabled | Flaky <1% | 0 失败 | `[PASS]` |
| D2 覆盖率 | ≥60% | ≥70% | ≥80% | 56.7% | `[FAIL]` |
| D3 P0 缺陷 | 0 P0 | 0 新 P1 | — | 0 P0 | `[PASS]` |
| D4 功能完整 | DataManager 无 TODO；双 Condition API 有文档+迁移计划 | — | — | 待确认 | `[PARTIAL]` |
| D5 AutoConfig | TenantContextAutoConfiguration + TransactionAutoConfiguration 能加载 | on/off 测试 | — | 待确认 | `[PARTIAL]` |
| D6 API 稳定 | Operator enum 有 @Deprecated+替代 或文档声明稳定 | — | — | 待确认 | `[PARTIAL]` |
| D7 文档对齐 | README 覆盖 DataManager API、Condition 双 API、方言列表 | AutoConfig 列匹配 .imports | — | 待确认 | `[PARTIAL]` |
| D8 架构完整 | `api(project(":core"))` 移除或降为 implementation；ArchUnit: 不导入 data-impl | — | — | `[FAIL]` (api(core) 无用依赖) |

### data-sql

| 维度 | MUST | SHOULD | COULD | 当前 | 状态 |
|------|------|--------|-------|------|------|
| D1 测试健康 | 0 失败 | 0 @Disabled | Flaky <1% | 0 失败 | `[PASS]` |
| D2 覆盖率 | ≥60% | ≥70% | ≥80% | 88.9% | `[PASS]` |
| D3 P0 缺陷 | 0 P0 | — | — | 0 P0 | `[PASS]` |
| D4 功能完整 | SQL builder 覆盖 ≥3 方言 | — | — | 待确认 | `[PARTIAL]` |
| D5 AutoConfig | 豁免（纯库） | — | — | — | `[EXEMPT]` |
| D6 API 稳定 | 无 @Deprecated 无替代 | — | — | 待确认 | `[PARTIAL]` |
| D7 文档对齐 | 方言支持已文档 | — | — | 待确认 | `[PARTIAL]` |
| D8 架构完整 | 不依赖 data-jdbc | ArchUnit 测试 | — | 待确认 | `[PARTIAL]` |

### data-jdbc

| 维度 | MUST | SHOULD | COULD | 当前 | 状态 |
|------|------|--------|-------|------|------|
| D1 测试健康 | 0 失败 | 0 @Disabled | Flaky <1% | **1 失败** | `[FAIL]` |
| D2 覆盖率 | ≥40% | ≥60% | ≥70% | 19.2% | `[FAIL]` |
| D3 P0 缺陷 | 0 P0 | — | — | 0 P0 | `[PASS]` |
| D4 功能完整 | JdbcDataManager 实现所有 DataManager 方法 | — | — | 待确认 | `[PARTIAL]` |
| D5 AutoConfig | DataManagerAutoConfiguration 能加载（H2 DataSource） | EntityCache 条件 on/off | — | 待确认 | `[PARTIAL]` |
| D6 API 稳定 | 无 @Deprecated 无替代 | — | — | 待确认 | `[PARTIAL]` |
| D7 文档对齐 | README 覆盖使用方式 + H2 测试策略 | — | — | 待确认 | `[PARTIAL]` |
| D8 架构完整 | 无循环依赖 | ArchUnit 测试 | — | 待确认 | `[PARTIAL]` |

### data-liquibase

| 维度 | MUST | SHOULD | COULD | 当前 | 状态 |
|------|------|--------|-------|------|------|
| D1 测试健康 | 0 失败 + ≥1 smoke test | 0 @Disabled | Flaky <1% | **0 测试** | `[FAIL]` |
| D2 覆盖率 | ≥20% | ≥40% | ≥60% | 0% | `[FAIL]` |
| D3 P0 缺陷 | 0 P0 | — | — | 0 P0 | `[PASS]` |
| D4 功能完整 | LiquibaseAutoConfiguration 能加载；迁移文件可解析 | — | — | 待确认 | `[PARTIAL]` |
| D5 AutoConfig | LiquibaseAutoConfiguration + DataManager 存在时加载 | on/off | — | 待确认 | `[PARTIAL]` |
| D6 API 稳定 | 无 @Deprecated 无替代 | — | — | 待确认 | `[PARTIAL]` |
| D7 文档对齐 | 迁移策略已文档 | — | — | 待确认 | `[PARTIAL]` |
| D8 架构完整 | 不依赖 data-jdbc（仅 data-core） | ArchUnit 测试 | — | 待确认 | `[PARTIAL]` |

---

## Phase 3: Core + Security Core

### core

| 维度 | MUST | SHOULD | COULD | 当前 | 状态 |
|------|------|--------|-------|------|------|
| D1 测试健康 | 0 失败 | 0 @Disabled | Flaky <1% | 0 失败 | `[PASS]` |
| D2 覆盖率 | ≥60% | ≥70% | ≥80% | 70%+ | `[PASS]` |
| D3 P0 缺陷 | **0 P0** | 0 新 P1 | P1 有界 | **2 P0** | `[FAIL]` |
| D4 功能完整 | 29 AutoConfig 每个 ≥1 smoke test；无空 inner @Configuration | — | — | 待确认 | `[PARTIAL]` |
| D5 AutoConfig | CacheAutoConfiguration、AfgSecurityAutoConfiguration、DataScopeAutoConfiguration 排序验证 | on/off 测试 | — | 待确认 | `[PARTIAL]` |
| D6 API 稳定 | DefaultCacheManager @Deprecated + 替代指向 AfgCache | @Deprecated 有移除时间线 | 零 @Deprecated | 待确认 | `[PARTIAL]` |
| D7 文档对齐 | 29 AutoConfig 名称+条件属性文档化；.imports 匹配 | — | — | 待确认 | `[PARTIAL]` |
| D8 架构完整 | core 不导入 data-impl、security-impl、ai-core | ArchUnit 测试 | 全部分层规则 | 待确认 | `[PARTIAL]` |

**P0 缺陷详情**:
1. XSS 过滤器默认使用可绕过的 regex（AntiSamy 存在但非默认）→ XssFilter 默认构造器改用 EnhancedInputSanitizer
2. 6 个 ThreadLocal 持有者缺乏统一异步传播 → 创建 ThreadLocalContextPropagator 注册器

### security-core

| 维度 | MUST | SHOULD | COULD | 当前 | 状态 |
|------|------|--------|-------|------|------|
| D1 测试健康 | 0 失败 | 0 @Disabled | Flaky <1% | 0 失败 | `[PASS]` |
| D2 覆盖率 | ≥50% | ≥65% | ≥80% | 78.2% | `[PASS]` |
| D3 P0 缺陷 | 0 P0 | — | — | 0 P0 | `[PASS]` |
| D4 功能完整 | SPI 接口完整无 stub | — | — | 待确认 | `[PARTIAL]` |
| D5 AutoConfig | 豁免（SPI 层） | — | — | — | `[EXEMPT]` |
| D6 API 稳定 | 无 @Deprecated 无替代 | — | — | 待确认 | `[PARTIAL]` |
| D7 文档对齐 | SPI 接口有使用示例 | — | — | 待确认 | `[PARTIAL]` |
| D8 架构完整 | security-core 不导入 security-impl | ArchUnit 测试 | — | 待确认 | `[PARTIAL]` |

---

## Phase 4: Security Impl + Integration

### auth-server

| 维度 | MUST | SHOULD | COULD | 当前 | 状态 |
|------|------|--------|-------|------|------|
| D1 测试健康 | 0 失败 | 0 @Disabled | Flaky <1% | **3 失败** | `[FAIL]` |
| D2 覆盖率 | ≥40% | ≥55% | ≥70% | ~25% | `[FAIL]` |
| D3 P0 缺陷 | 0 P0 | 0 新 P1 | — | 0 P0 | `[PASS]` |
| D4 功能完整 | 10 AutoConfig 每个 context-load smoke test；OAuth2 授权码流程端到端测试 | — | — | 待确认 | `[PARTIAL]` |
| D5 AutoConfig | JWT 统一为 Nimbus 后 AuthorizationServerAutoConfiguration 能加载 | on/off | — | 待确认 | `[PARTIAL]` |
| D6 API 稳定 | 无 @Deprecated 无替代 | — | — | 待确认 | `[PARTIAL]` |
| D7 文档对齐 | README 覆盖 OAuth2、Casbin、登录策略、Token 生命周期 | — | — | 待确认 | `[PARTIAL]` |
| D8 架构完整 | auth-server 不导入 resource-server | ArchUnit 测试 | — | 待确认 | `[PARTIAL]` |

**P1 缺陷**:
- 双 JWT 库（Nimbus + JJWT）→ MUST 统一为 Nimbus
- Device ID 缺少服务端 HMAC → SHOULD

### resource-server

| 维度 | MUST | SHOULD | COULD | 当前 | 状态 |
|------|------|--------|-------|------|------|
| D1 测试健康 | 0 失败 + ≥3 测试文件 | 0 @Disabled | Flaky <1% | **0 测试** | `[FAIL]` |
| D2 覆盖率 | ≥25% | ≥40% | ≥60% | 0% | `[FAIL]` |
| D3 P0 缺陷 | 0 P0 | — | — | 0 P0 | `[PASS]` |
| D4 功能完整 | JWT 验证、多租户解析、权限检查有测试 | — | — | 待确认 | `[PARTIAL]` |
| D5 AutoConfig | ResourceServerAutoConfiguration、DefaultSecurityAutoConfiguration 能加载 | on/off | — | 待确认 | `[PARTIAL]` |
| D6 API 稳定 | 无 @Deprecated 无替代 | — | — | 待确认 | `[PARTIAL]` |
| D7 文档对齐 | README 覆盖资源服务器设置、JWT 配置、权限模型 | — | — | 待确认 | `[PARTIAL]` |
| D8 架构完整 | resource-server 不导入 auth-server | ArchUnit 测试 | — | 待确认 | `[PARTIAL]` |

### 集成模块（afg-redis / afg-storage / afg-websocket / afg-rabbitmq / afg-jdbc）

统一模板，每个模块：

| 维度 | MUST | 当前 | 状态 |
|------|------|------|------|
| D1 测试健康 | 0 失败 + ≥1 smoke test | **0 测试** | `[FAIL]` |
| D2 覆盖率 | ≥20% | 0% | `[FAIL]` |
| D3 P0 缺陷 | 0 P0 | 0 P0 | `[PASS]` |
| D4 功能完整 | 核心适配器操作可工作 | 待确认 | `[PARTIAL]` |
| D5 AutoConfig | AutoConfig 能加载 | 待确认 | `[PARTIAL]` |
| D6 API 稳定 | 无 @Deprecated 无替代 | 待确认 | `[PARTIAL]` |
| D7 文档对齐 | README 覆盖中间件、功能、配置 | 待确认 | `[PARTIAL]` |
| D8 架构完整 | 不导入其他 integration 模块 | 待确认 | `[PARTIAL]` |

**模块特定 D4 要求**:
- afg-redis: 分布式锁获取/释放
- afg-storage: 文件上传/下载（Local 后端）
- afg-websocket: 建立连接
- afg-rabbitmq: 事件发布
- afg-jdbc: 审计日志存储

---

## Phase 5: AI + Governance

### ai-core

| 维度 | MUST | SHOULD | COULD | 当前 | 状态 |
|------|------|--------|-------|------|------|
| D1 测试健康 | 0 失败 | 0 @Disabled | Flaky <1% | **7 失败** | `[FAIL]` |
| D2 覆盖率 | ≥30% | ≥45% | ≥60% | ~15% | `[FAIL]` |
| D3 P0 缺陷 | 0 P0 | — | — | 0 P0 | `[PASS]` |
| D4 功能完整 | (a) AiEntityAutoConfiguration 空类 → 移除 TODO 或文档为 placeholder；(b) 3 注释 AOP aspect → 接入或移除；(c) NoOp 实现有测试确认优雅降级；(d) api/ 包 Default* 类有 Javadoc 区分 | api/ Default* 迁移到领域包 | — | 待确认 | `[PARTIAL]` |
| D5 AutoConfig | 16 AutoConfig 每个能加载 | on/off | — | 待确认 | `[PARTIAL]` |
| D6 API 稳定 | 无 @Deprecated 无替代 | — | — | 待确认 | `[PARTIAL]` |
| D7 文档对齐 | 16 AutoConfig 名称+条件文档化；.imports 匹配 | — | — | 待确认 | `[PARTIAL]` |
| D8 架构完整 | api/ 包不含 >5 个非 api 包依赖的类 | ArchUnit 测试 | — | 待确认 | `[PARTIAL]` |

### ai-langchain4j

| 维度 | MUST | SHOULD | COULD | 当前 | 状态 |
|------|------|--------|-------|------|------|
| D1 测试健康 | 0 失败 + ≥2 smoke test | 0 @Disabled | Flaky <1% | **0 测试** | `[FAIL]` |
| D2 覆盖率 | ≥15% | ≥30% | ≥50% | 0% | `[FAIL]` |
| D3 P0 缺陷 | 0 P0 | — | — | 0 P0 | `[PASS]` |
| D4 功能完整 | 7 AutoConfig 能加载；Lc4jChatAutoConfiguration 在 LangChain4j 存在时创建 ChatClient | — | — | 待确认 | `[PARTIAL]` |
| D5 AutoConfig | 7 AutoConfig 条件 on/off 测试 | — | — | 待确认 | `[PARTIAL]` |
| D6 API 稳定 | 无 @Deprecated 无替代 | — | — | 待确认 | `[PARTIAL]` |
| D7 文档对齐 | README 覆盖 LangChain4J 集成、配置 | — | — | 待确认 | `[PARTIAL]` |
| D8 架构完整 | ai-langchain4j 不导入 ai-spring-ai | ArchUnit 测试 | — | 待确认 | `[PARTIAL]` |

### governance/proto

| 维度 | MUST | 当前 | 状态 |
|------|------|------|------|
| D1 测试健康 | 编译通过 | 编译通过 | `[PASS]` |
| D2 覆盖率 | **豁免** | 0% | `[EXEMPT]` |
| D3 P0 缺陷 | 0 P0 | 0 P0 | `[PASS]` |
| D4-D8 | 同通用标准 | — | `[PARTIAL]` |

### governance/client

| 维度 | MUST | SHOULD | 当前 | 状态 |
|------|------|--------|------|------|
| D1 测试健康 | 0 失败 + ≥1 smoke test | 0 @Disabled | **0 测试** | `[FAIL]` |
| D2 覆盖率 | ≥20% | ≥40% | 0% | `[FAIL]` |
| D4 功能完整 | gRPC 客户端能连接服务端 | — | 待确认 | `[PARTIAL]` |
| D5 AutoConfig | GovernanceClientAutoConfiguration 能加载 | on/off | 待确认 | `[PARTIAL]` |

### governance/server

| 维度 | MUST | SHOULD | 当前 | 状态 |
|------|------|--------|------|------|
| D1 测试健康 | 0 失败 + ≥3 smoke test | 0 @Disabled | **0 测试** | `[FAIL]` |
| D2 覆盖率 | ≥30% | ≥45% | 0% | `[FAIL]` |
| D4 功能完整 | 服务、配置、订阅 CRUD 有测试 | — | 待确认 | `[PARTIAL]` |
| D5 AutoConfig | GovernanceServerAutoConfiguration 能加载 | on/off | 待确认 | `[PARTIAL]` |

---

## 排除模块

| 模块 | 原因 | 处置 |
|------|------|------|
| ai-spring-ai | 空壳（0 源文件，AutoConfiguration 仅在 build/ 中） | 标记 "not started"；Phase 5 完成前决定是否填充或从 settings.gradle.kts 移除 |

---

## 跨模块不一致清单

| 不一致 | 解决方案 | 阶段 | 嵌入模块 | 优先级 |
|--------|---------|------|---------|--------|
| H2 vs Testcontainers | H2 允许框架内部数据层测试；Testcontainers 用于中间件和下游。更新 testing-standards.md | Phase 0 | 全局 | MUST |
| DataManager ISP (40+ 方法) | 不阻塞。SHOULD: 添加 EntityOperations/SqlOperations/TransactionOperations 子接口 | Phase 2 SHOULD | data-core | SHOULD |
| 双 Condition API | MUST: 文档 + 迁移计划。SHOULD: 旧 API 标记 @Deprecated | Phase 2 | data-core | MUST |
| 双 JWT 库 | MUST: 统一为 Nimbus，移除 JJWT | Phase 4 MUST | auth-server | MUST |
| data-core api(core) 无用依赖 | MUST: 移除或降为 implementation | Phase 2 MUST | data-core | MUST |
| ai-core api/ 实现混入 | SHOULD: 迁移 Default* 到领域包。MUST: Javadoc 区分 | Phase 5 | ai-core | MUST |
| 异常双栖 (core + commons) | SHOULD: 完成 commons 迁移。MUST: 文档声明 commons 为权威 | Phase 3 SHOULD | core | SHOULD |
| DefaultCacheManager 迁移 | MUST: @Deprecated + 替代 | Phase 3 MUST | core | MUST |

---

## 验证流程

### 自动化检查

| 检查 | 命令 | 验证维度 |
|------|------|---------|
| 全部测试通过 | `./gradlew test` | D1 |
| JaCoCo 覆盖率 | `./gradlew jacocoTestReport` → 解析 XML | D2 |
| 覆盖率阈值 | `./gradlew jacocoCoverageCheck`（全局 60%） | D2 地板 |
| PMD 代码质量 | `./gradlew pmdMain` | 基线 |
| OWASP 依赖检查 | `./gradlew dependencyCheckAnalyze` | CVE |
| ArchUnit | 新增测试类（Phase 3+） | D8 |
| 构建 | `./gradlew build` | 编译 + 全检查 |

### 手动审查项

- P0/P1 缺陷盘点（每阶段开始时）
- AutoConfig 列 vs .imports 文件（每阶段 Gate）
- README 准确性（每模块收敛）
- 安全审查（Phase 3+ Gate）

### Smoke Test 模式

```java
@SpringBootTest(classes = {ModuleAutoConfig.class, MinimalDependencies.class})
class ModuleAutoConfigSmokeTest {
    @Test void contextLoads() { }
}

// 条件逻辑模块增加 on/off 变体
@SpringBootTest(classes = {ModuleAutoConfig.class}, properties = "afg.module.feature.enabled=false")
class ModuleAutoConfigDisabledTest {
    @Test void contextLoadsWhenDisabled() { }
}
```

---

## Phase Gate 规则

阶段 Gate 通过条件：
1. 该阶段全部模块 MUST 项 PASS
2. `./gradlew test` 对阶段模块 0 失败
3. `./gradlew jacocoCoverageCheck` 通过全局 60%
4. 该阶段模块无 P0 缺陷
5. 该阶段跨模块 MUST 项已解决

SHOULD/COULD 不阻塞 Gate。

---

## Phase Gate 记录

| 阶段 | Gate 日期 | 结果 | 备注 |
|------|----------|------|------|
| Phase 0 | — | — | — |
| Phase 1 | — | — | — |
| Phase 2 | — | — | — |
| Phase 3 | — | — | — |
| Phase 4 | — | — | — |
| Phase 5 | — | — | — |

---

## 实施顺序

```
Phase 0 → 修 11 测试失败 + H2 决策 + ai-spring-ai 处置
Phase 1 → commons(40%), apt-api(豁免), apt-impl(50%) 收敛
Phase 2 → data-core(60%), data-sql(60%), data-jdbc(40%), data-liquibase(20%) 收敛
           + 移除 api(core) + 文档双 Condition API
Phase 3 → core(60%) + security-core(50%) 收敛
           + 修 P0: XSS → AntiSamy + ThreadLocal 统一传播
           + @Deprecated DefaultCacheManager
Phase 4 → auth-server(40%) + resource-server(25%) + 5 integration(20%) 收敛
           + 统一 JWT 为 Nimbus
Phase 5 → ai-core(30%) + ai-langchain4j(15%) + governance(20-30%) 收敛
           + ai-spring-ai 处置决定
```
