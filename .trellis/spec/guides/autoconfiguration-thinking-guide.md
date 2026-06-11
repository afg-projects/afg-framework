# AFG Framework AutoConfiguration 思维指南

> **目的**：编写或审查 AutoConfiguration 前，确保遵循 AFG 框架的强制规则和最佳实践。
> **溯源**：PRD 第 6.6 节"AutoConfiguration 编写规则"、第 1.5 节"质量底线"、附录 A"AutoConfiguration 完整清单"

---

## 为什么需要专门的 AutoConfiguration 指南？

AFG 框架拥有 **80+ 个 AutoConfiguration**（PRD 附录 A），是框架最核心的扩展机制。AutoConfiguration 的编写质量直接影响：

- **启动可靠性**：Bean 创建顺序错误导致启动失败
- **可替换性**：缺少 @ConditionalOnMissingBean 导致无法替换默认实现
- **降级能力**：缺少 NoOp 实现导致无外部依赖时启动失败
- **测试可靠性**：@ImportAutoConfiguration 不自动解析排序，测试中需显式列出前置配置

---

## 编写新 AutoConfiguration 前

### 检查 1：是否已有类似的 AutoConfiguration？

框架已有 80+ 个 AutoConfiguration，新增前搜索：

```bash
# 搜索是否已有类似功能的 AutoConfiguration
grep -r "AutoConfiguration" --include="*.java" . | grep -i "keyword"

# 搜索已有模块的 AutoConfiguration 数量
grep -r "AutoConfiguration" --include="*.java" ai-core/src/ | wc -l
```

**已有 AutoConfiguration 分布**（PRD 附录 A）：

| 模块 | 数量 | 示例 |
|------|------|------|
| core | 31+ | CacheAutoConfiguration, LockAutoConfiguration, EventAutoConfiguration |
| data-core | 2 | TenantContextAutoConfiguration, TransactionAutoConfiguration |
| data-jdbc | 4 | DataManagerAutoConfiguration, EntityCacheAutoConfiguration |
| data-liquibase | 1 | LiquibaseAutoConfiguration |
| ai-core | 16 | AiChatAutoConfiguration, AiAgentAutoConfiguration, AiWorkflowAutoConfiguration |
| ai-langchain4j | 7 | Lc4jChatAutoConfiguration, Lc4jEmbeddingAutoConfiguration |
| ai-spring-ai | 7 | SpringAiChatAutoConfiguration, SpringAiEmbeddingAutoConfiguration |
| auth-server | 9 | AuthorizationServerAutoConfiguration, CasbinAutoConfiguration |
| resource-server | 2 | ResourceServerAutoConfiguration, DefaultSecurityAutoConfiguration |
| afg-redis | 1+ | RedisAutoConfiguration |
| governance-client | 1 | GovernanceClientAutoConfiguration |
| governance-server | 1 | GovernanceServerAutoConfiguration |

**如果已有类似 AutoConfiguration**：参照其模式，保持一致性。不要发明新模式。

### 检查 2：是否遵循强制规则？

#### 规则 2a：必须使用 @AutoConfiguration（非 @Configuration）

```java
// 错误 — 使用 @Configuration
@Configuration
public class CacheAutoConfiguration { }

// 正确 — 使用 @AutoConfiguration
@AutoConfiguration
public class CacheAutoConfiguration { }
```

**原因**：@AutoConfiguration 是 Spring Boot 3.2+ 的标准注解，标识自动配置类，支持排序和条件过滤。

**溯源**：PRD 第 6.6 节"AutoConfiguration 编写规则"第 1 条

#### 规则 2b：必须声明 @AutoConfigureAfter / @AutoConfigureBefore

```java
// 错误 — 未声明排序
@AutoConfiguration
public class DataManagerAutoConfiguration { }

// 正确 — 声明依赖排序
@AutoConfiguration(after = {
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class
})
public class DataManagerAutoConfiguration { }
```

**原因**：Bean 创建顺序不确定会导致启动失败。DataManager 依赖 DataSource，必须在 DataSource AutoConfiguration 之后创建。

**溯源**：PRD 第 6.6 节"AutoConfiguration 编写规则"第 2 条

#### 规则 2c：可替换组件必须 @ConditionalOnMissingBean

```java
// 错误 — 无条件创建 Bean，无法替换
@Bean
public CacheManager cacheManager() {
    return new DefaultCacheManager();
}

// 正确 — 允许替换
@Bean
@ConditionalOnMissingBean(CacheManager.class)
public CacheManager cacheManager(CacheProperties properties,
                                  @Nullable CacheStorageProvider storageProvider) {
    DefaultCacheManager manager = new DefaultCacheManager(properties);
    if (storageProvider != null) {
        manager.setCacheStorageProvider(storageProvider);
    }
    return manager;
}
```

**原因**：@ConditionalOnMissingBean 允许业务应用或集成模块（如 afg-redis）替换默认实现。

**溯源**：PRD 第 6.6 节"AutoConfiguration 编写规则"第 4 条

### 检查 3：跨模块排序是否使用 afterName？

```java
// 错误 — 类引用需要额外的 compileOnly 依赖
@AutoConfiguration(after = DataSourceAutoConfiguration.class)
public class LiquibaseAutoConfiguration { }
// data-liquibase 模块需要 compileOnly 依赖 spring-boot-jdbc

// 正确 — 字符串引用，不需要编译期依赖
@AutoConfiguration(afterName = "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration")
public class LiquibaseAutoConfiguration { }
```

**规则**：
- 同模块内的 AutoConfiguration → 使用 `after = {XxxAutoConfiguration.class}`
- 跨模块的 AutoConfiguration → 使用 `afterName = "全限定类名"`

**溯源**：PRD 第 6.6 节"AutoConfiguration 编写规则"第 3 条

### 检查 4：SPI 模式是否正确？

每个 SPI 必须遵循 Default + NoOp + @ConditionalOnBean 模式：

```
SPI 接口 (api 子包)
  ├── Default 实现 (功能子包) — 本地默认，不依赖外部
  ├── NoOp 实现 (功能子包) — 降级实现，功能完全不需要时
  └── 外部实现 (集成模块) — 生产级实现，通过 @ConditionalOnBean 自动发现
```

**示例**：CacheManager SPI

```
core/api/cache/CacheManager.java          (SPI 接口)
core/cache/DefaultCacheManager.java       (Default — Caffeine 本地缓存)
ai-core/.../NoOpVectorStore.java          (NoOp — 空操作向量存储)
afg-redis/.../RedisCacheManager.java      (外部实现 — Redis 分布式缓存)
```

**溯源**：PRD 第 6.6 节"SPI 接口设计规则"、第 1.5 节"质量底线"第 1 条和第 6 条

---

## 常见错误

### 错误 1：使用 @Configuration 而非 @AutoConfiguration

**症状**：AutoConfiguration 排序不生效，Bean 创建顺序不确定

**修复**：将 @Configuration 替换为 @AutoConfiguration

**预防**：IDE 代码检查规则 — @Configuration 类名以 AutoConfiguration 结尾时报警告

**溯源**：PRD 第 6.6 节"AutoConfiguration 编写规则"第 1 条

### 错误 2：缺少 @AutoConfigureAfter 导致 Bean 创建顺序问题

**症状**：启动时报 `NoSuchBeanDefinitionException` 或 `UnsatisfiedDependencyException`，但依赖的 Bean 在其他 AutoConfiguration 中已定义

**根因**：AutoConfiguration 执行顺序不确定，依赖的 Bean 尚未创建

**修复**：添加 @AutoConfigureAfter 声明依赖排序

**预防**：每个 AutoConfiguration 必须声明其依赖的前置 AutoConfiguration

**溯源**：PRD 第 6.6 节"AutoConfiguration 编写规则"第 2 条

### 错误 3：忘记 @ConditionalOnMissingBean

**症状**：业务应用定义了自定义实现，但框架的默认实现仍被创建，导致多个 Bean 冲突

**修复**：在 @Bean 方法上添加 @ConditionalOnMissingBean

**预防**：所有可替换组件（SPI 接口的实现）必须添加 @ConditionalOnMissingBean

**溯源**：PRD 第 6.6 节"AutoConfiguration 编写规则"第 4 条

### 错误 4：未使用 @Nullable 注入可替换协作组件

**症状**：AutoConfiguration 构造函数或 @Bean 方法参数缺少可选依赖，启动时报 NoSuchBeanDefinitionException

```java
// 错误 — 可选依赖未标记 @Nullable
@Bean
public JdbcDataManager dataManager(DataSource ds,
                                    TenantContextHolder tenantContextHolder) {
    // 如果 TenantContextHolder Bean 不存在，启动失败
}

// 正确 — 可选依赖标记 @Nullable
@Bean
public JdbcDataManager dataManager(DataSource ds,
                                    @Nullable TenantContextHolder tenantContextHolder,
                                    @Nullable TransactionAdapter transactionAdapter) {
    JdbcDataManager dm = new JdbcDataManager(ds);
    if (tenantContextHolder != null) dm.setTenantContextHolder(tenantContextHolder);
    if (transactionAdapter != null) dm.setTransactionAdapter(transactionAdapter);
    return dm;
}
```

**规则**：
- AutoConfiguration 中可替换/可选的协作组件必须用 @Nullable
- 实现类保留默认实例向后兼容（构造函数中 new 默认实例，提供 setter 方法）

**溯源**：PRD 第 6.6 节"AutoConfiguration 编写规则"第 5 条、CLAUDE.md"可注入协作组件"

### 错误 5：@ImportAutoConfiguration 不自动解析排序

**症状**：测试中使用 @ImportAutoConfiguration，但 Bean 创建顺序与运行时不同，测试通过但生产环境失败

```java
// 错误 — @ImportAutoConfiguration 不自动解析 @AutoConfigureAfter
@SpringBootConfiguration
@ImportAutoConfiguration({
    DataManagerAutoConfiguration.class,  // 依赖 DataSource，但未列出
    LiquibaseAutoConfiguration.class
})
public class JdbcDataTestConfiguration { }

// 正确 — 显式列出所有前置配置
@SpringBootConfiguration
@ImportAutoConfiguration({
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    JdbcTemplateAutoConfiguration.class,
    DataManagerAutoConfiguration.class,
    LiquibaseAutoConfiguration.class
})
public class JdbcDataTestConfiguration { }
```

**规则**：@ImportAutoConfiguration 不会自动解析 @AutoConfigureAfter，需显式列出前置配置

**溯源**：CLAUDE.md"测试配置"

### 错误 6：AutoConfiguration.imports 文件未同步

**症状**：新增了 AutoConfiguration 类，但 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件未更新，AutoConfiguration 不生效

**修复**：在 imports 文件中添加新 AutoConfiguration 的全限定类名

**预防**：新增 AutoConfiguration 后，检查并更新 imports 文件

---

## AutoConfiguration 依赖链示例

理解框架中 AutoConfiguration 的依赖链，有助于正确声明排序：

### 数据访问依赖链

```
DataSourceAutoConfiguration (Spring Boot)
  └→ DataSourceTransactionManagerAutoConfiguration (Spring Boot)
       └→ TransactionAutoConfiguration (data-core) — TransactionAdapter
       └→ TenantContextAutoConfiguration (data-core) — TenantContextHolder
            └→ DataManagerAutoConfiguration (data-jdbc) — JdbcDataManager
                 └→ LiquibaseAutoConfiguration (data-liquibase) — SpringLiquibase
```

### AI 模块依赖链

```
AiCoreAutoConfiguration (ai-core) — AI 核心初始化
  └→ AiChatAutoConfiguration (ai-core) — Chat 客户端注册
  └→ AiAgentAutoConfiguration (ai-core) — Agent 执行器
  └→ AiWorkflowAutoConfiguration (ai-core) — 工作流引擎
  └→ AiRagAutoConfiguration (ai-core) — RAG
  └→ ...

SpringAiChatAutoConfiguration (ai-spring-ai) — Spring AI 适配
  └→ after: AiChatAutoConfiguration

Lc4jChatAutoConfiguration (ai-langchain4j) — LangChain4J 适配
  └→ after: AiChatAutoConfiguration
```

### 安全模块依赖链

```
DataManagerAutoConfiguration (data-jdbc)
  └→ AuthorizationServerAutoConfiguration (auth-server)
  └→ CasbinAutoConfiguration (auth-server)
  └→ PermissionAutoConfiguration (auth-server)
  └→ TenantAutoConfiguration (auth-server)
  └→ DataScopeAutoConfiguration (auth-server)
  └→ ...
```

**溯源**：PRD 附录 A、CLAUDE.md"自动配置依赖链"

---

## 新增 AutoConfiguration 检查清单

### 编写前

- [ ] 搜索了是否已有类似 AutoConfiguration（80+ 已存在）
- [ ] 确认使用 @AutoConfiguration 而非 @Configuration
- [ ] 确认需要 @AutoConfigureAfter / @AutoConfigureBefore
- [ ] 跨模块引用使用 afterName 字符串引用
- [ ] 确认可替换组件使用 @ConditionalOnMissingBean
- [ ] 确认可选协作组件使用 @Nullable
- [ ] 确认 SPI 模式：Default + NoOp + @ConditionalOnBean

### 编写中

- [ ] @Bean 方法参数中可选依赖标记 @Nullable
- [ ] 实现类保留默认实例向后兼容（构造函数 + setter）
- [ ] 配置属性使用 @EnableConfigurationProperties
- [ ] 功能开关使用 @ConditionalOnProperty 或 enabled 配置项
- [ ] 条件注解使用 @ConditionalOnClass / @ConditionalOnBean

### 编写后

- [ ] 更新 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- [ ] 测试中 @ImportAutoConfiguration 显式列出所有前置配置
- [ ] 验证无外部依赖时 NoOp 降级正常工作
- [ ] 验证有外部实现时 @ConditionalOnMissingBean 正确替换
- [ ] 验证 Bean 创建顺序正确（启动无报错）

---

## AutoConfiguration 命名约定

| 类型 | 命名模式 | 示例 |
|------|---------|------|
| AutoConfiguration 类 | `{Feature}AutoConfiguration` | `CacheAutoConfiguration` |
| 配置属性类 | `{Feature}Properties` | `CacheProperties` |
| SPI 接口 | 名词或动词 | `DistributedLock` / `EventPublisher` |
| Default 实现 | `Default{Spi}` | `DefaultCacheManager` |
| 技术特定实现 | `{Technology}{Spi}` | `RedisDistributedLock` |
| NoOp 实现 | `NoOp{Spi}` | `NoOpVectorStore` |
| 配置前缀 | `afg.{module}.{feature}` | `afg.core.cache` |

**溯源**：PRD 第 6.1 节"命名约定"

---

## 配置属性设计规则

### 核心原则

```
引入依赖即生效 → 有 Bean 即增强 → 注解即启用 → 配置只覆盖默认值
```

### 启用方式优先级

1. **自动装配**（优先）：@ConditionalOnClass / @ConditionalOnBean
2. **注解启用**（次选）：@AiChat / @DistributedTask / @Lock
3. **配置属性**（最后）：仅覆盖默认值

### 禁止配置爆炸

- 每个功能最多 1 个 `enabled` 开关 + 必要的行为参数
- 不暴露框架内部实现细节的配置项
- 基础设施配置（数据库/Redis/MQ 地址）由 Spring Boot 原生管理
- 框架只配置"行为参数"（超时、重试次数、策略选择），不配置"连接参数"

### 属性命名约定

- 前缀：`afg.{module}.{feature}`
- 风格：kebab-case
- 层级：不超过 4 层

```yaml
afg:
  ai:
    rag:
      enabled: true              # 功能开关
      embedding-dimensions: 1536  # 行为参数
      # LLM 连接配置由 Spring AI / LangChain4J 原生管理
```

**溯源**：PRD 第 6.4 节"配置规范"

---

**溯源声明**：本指南内容均溯源至 [docs/framework-prd.md](../../docs/framework-prd.md) 中第 6.6 节"AutoConfiguration 编写规则"、第 1.5 节"质量底线"、第 6.4 节"配置规范"、附录 A"AutoConfiguration 完整清单"。
