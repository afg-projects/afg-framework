# Phase 1 验证报告：核心概念章节与代码库对比

## 检查结果汇总

| 验证项 | 结论 | 问题数 |
|--------|------|--------|
| 3.1 DataManager vs JPA Repository | 基本准确 | 2 处需修正 |
| 3.2 APT 编译时元数据 | 准确 | 3 处需补充 |
| 3.3 自动配置约定 | 准确 | 2 处需修正 |
| 3.4 实体基类选择决策树 | 基本准确 | 3 处需修正 |
| 3.5 模块化架构理念 | 准确 | 2 处需补充 |
| 3.6 增强而非替代原则 | 基本准确 | 2 处需修正 |

---

## 🔴 关键发现

### 1. core 与 data-core 双重实体体系（最严重）

`core` 模块的 `BaseEntity`（String id, LocalDateTime）与 `data-core` 模块的 `BaseEntity`（Long id, Instant）存在设计分歧：
- `data-core`: `BaseEntity` → Long id + Instant 时间戳（推荐，PRD 以此为准）
- `core`: `BaseEntity` → String id + LocalDateTime + Serializable

PRD 必须明确推荐使用 `data-core` 的实体体系。

### 2. TreeEntity 缺失

PRD 决策树遗漏了 `TreeEntity<T> extends BaseEntity implements Treeable<T>`，提供树形结构（parentId, level, path, sortOrder, children）。

### 3. APT 不是唯一路径

PRD 过度强调"零反射"。实际 APT 是优先路径（priority=0），反射是降级路径（priority=1000），且 SPI 扩展可插入自定义加载器。

### 4. 自定义条件注解未提及

框架提供了三个自定义条件注解（PRD 未提及）：
- `@ConditionalOnFeature`
- `@ConditionalOnPropertyNotEmpty`
- `@ConditionalOnTenant`

### 5. "引入即生效"有前提条件

afg-redis 需要 `RedissonClient` Bean 存在才生效，不是简单引入 JAR 即可。PRD 应明确各模块的外部依赖前提。

---

## 详细验证

### 3.1 DataManager vs JPA Repository

**需修正：**
1. "APT 编译时生成，运行时零反射" → 应改为"APT 编译时生成优先，反射降级"
2. "EntityCache + Caffeine/Redis" → 应改为"AfgCache 统一抽象 + 本地/Redis 实现"，实际是框架自己的 `AfgCache<V>` 接口

### 3.2 APT 编译时元数据

**需补充：**
1. "IDE 中可直接跳转到 UserMetadata.TABLE_NAME" → 补充前提条件：IDE annotation processing 需启用
2. PRD 未提及 SPI 扩展机制 — `EntityMetadataCache.discoverLoaders()` 通过 `ServiceLoader<EntityMetadataLoader>` SPI 发现加载器
3. ReflectiveMetadataLoader 实际通过 `MetadataProvider` SPI 接口获取元数据，不是直接反射

### 3.3 自动配置约定

**需修正：**
1. "@AiChat/@DistributedTask 等注解自动触发"不够准确 — 它们依赖对应的 AutoConfiguration 先注册切面 Bean
2. "引入 afg-framework-afg-redis → 分布式缓存/锁/调度自动升级" → 需补充前提条件：需 `RedissonClient` Bean

### 3.4 实体基类选择决策树

**需修正：**
1. PRD 决策树缺少 `TreeEntity`（实际存在的基类）
2. FullEntity 组合规则表应更明确标注"不含多租户"
3. core 与 data-core 双重实体体系 — PRD 应明确推荐 data-core 体系

### 3.5 模块化架构理念

**需补充：**
1. `@AutoConfigureAfter` 与 `@AfgModuleAnnotation.dependencies` 是两套独立体系
2. `basePackage` 属性通过 `@AliasFor(annotation = ComponentScan.class)` 实现自动组件扫描

### 3.6 增强而非替代原则

**需修正/补充：**
1. 补充框架在 Jackson 之上的增强（`JacksonUtils`/`JacksonMapper`）
2. 补充三个自定义条件注解作为"增强而非替代"的案例

---

## 相关源文件

- `data-core/.../entity/BaseEntity.java` — Long id + Instant
- `data-core/.../entity/TreeEntity.java` — 树形结构基类
- `data-core/.../metadata/AptMetadataLoader.java` — priority=0
- `data-core/.../metadata/ReflectiveMetadataLoader.java` — priority=1000
- `data-core/.../metadata/EntityMetadataCache.java` — ServiceLoader SPI
- `core/.../autoconfigure/condition/ConditionalOnFeature.java`
- `core/.../autoconfigure/condition/ConditionalOnPropertyNotEmpty.java`
- `core/.../autoconfigure/condition/ConditionalOnTenant.java`
- `core/.../model/entity/BaseEntity.java` — String id + LocalDateTime
- `integration/afg-redis/.../RedisAutoConfiguration.java` — @ConditionalOnBean(RedissonClient.class)
