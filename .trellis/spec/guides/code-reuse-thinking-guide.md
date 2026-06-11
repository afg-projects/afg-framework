# AFG Framework 代码复用思维指南

> **目的**：编写新代码前停下来思考 — AFG 框架中是否已存在相同或等价的能力？
> **溯源**：PRD 第 1.2 节"设计哲学"（约定优于配置、增强而非替代）、第 6.1 节"编码规范"

---

## 问题本质

**重复代码是不一致性缺陷的首要来源**。在 AFG 框架中，重复不仅发生在业务代码，更常发生在：

- Conditions 构建器 vs 手写 SQL
- DataManager 快捷方法 vs EntityProxy 链式调用
- AutoConfiguration 模式重复
- ErrorCode 枚举重复定义
- SPI 默认实现重复创建

当复制粘贴或重写已有逻辑时：
- 缺陷修复不会传播
- 行为随时间分化
- 代码库更难理解

---

## 编写新代码前

### 步骤 1：搜索优先

```bash
# 搜索类似功能名
grep -r "functionName" --include="*.java" .

# AFG 特有搜索模式
# 搜索 CommonErrorCode 是否已有对应错误码
grep -r "NOT_FOUND\|PARAM_ERROR\|ENTITY_NOT_FOUND" commons/src/main/java/io/github/afgprojects/framework/commons/model/

# 搜索是否已有类似 AutoConfiguration
grep -r "AutoConfiguration" --include="*.java" . | grep -i "keyword"

# 搜索 SPI 默认/NoOp 实现
grep -r "Default\|NoOp" --include="*.java" . | grep -i "SpiName"

# 搜索 Conditions 构建器是否已有 Lambda 方式
grep -r "builder(" --include="*.java" . | grep -v "string"

# 搜索 DataManager 操作是否已有快捷方法
grep -r "dataManager\." --include="*.java" . | grep "methodName"
```

### 步骤 2：提出这些问题

| 问题 | 如果是... |
|------|-----------|
| CommonErrorCode 是否已有对应错误码？ | 使用 CommonErrorCode，不要新建 |
| 是否已有 Lambda Conditions 构建器？ | 使用 `builder(Entity.class).eq(Entity::getField, value)`，不要用字符串 |
| DataManager 是否已有快捷方法？ | 使用 `findById` / `findAll` / `save` 等快捷方法 |
| 是否已有 Default/NoOp SPI 实现？ | 复用或扩展，不要新建 |
| 是否已有类似 AutoConfiguration 模式？ | 参照已有模式，保持一致性 |
| 跨模块通信是否应使用事件？ | 优先 EventPublisher，避免直接 Bean 调用 |

---

## AFG 特有重复模式

### 模式 1：Conditions 构建器 vs 字符串条件

**错误**：使用字符串条件查询

```java
// 错误 — 字符串方式，无类型安全，易拼错字段名
Condition condition = builder()
    .eq("status", 1)
    .like("username", "张")
    .build();
```

**正确**：使用 Lambda 条件查询

```java
// 正确 — Lambda 方式，类型安全，编译期检查
Condition condition = builder(User.class)
    .eq(User::getStatus, 1)
    .like(User::getUsername, "张")
    .build();
```

**溯源**：PRD 第 1.4 节"开发者体验理念" — Lambda > 字符串

### 模式 2：DataManager 快捷方法 vs EntityProxy 链式调用

**何时使用快捷方法**：简单 CRUD 操作

```java
// 简单查询 — 使用快捷方法
User user = dataManager.findById(User.class, id).orElse(null);
List<User> users = dataManager.findAll(User.class);
User saved = dataManager.save(User.class, user);
```

**何时使用 EntityProxy 链式调用**：需要条件、排序、分页、数据权限、关联预加载

```java
// 复杂查询 — 使用 EntityProxy 链式调用
PageData<User> result = dataManager.entity(User.class)
    .query()
    .where(builder(User.class).eq(User::getStatus, 1).build())
    .orderByDesc(User::getCreatedAt)
    .page(PageRequest.of(1, 20));

// 需要数据权限
List<User> users = dataManager.entity(User.class)
    .query()
    .withDataScope()
    .list();

// 需要关联预加载
User user = dataManager.entity(User.class)
    .query()
    .withAssociation("orders")
    .where(builder(User.class).eq(User::getId, id).build())
    .one()
    .orElse(null);
```

**决策规则**：
- 仅需 findById/findAll/save/deleteById → 快捷方法
- 需要 where/orderBy/page/withDataScope/withAssociation → EntityProxy 链式

**溯源**：PRD 第 5.4 节"DataManager 数据访问模块"

### 模式 3：AutoConfiguration 模式重复

框架已有 80+ 个 AutoConfiguration（PRD 附录 A），新增时应参照已有模式：

```java
// 标准模式 — 参照 CacheAutoConfiguration
@AutoConfiguration(after = {前置配置.class})
@ConditionalOnClass(依赖类.class)
@EnableConfigurationProperties(配置属性.class)
public class FeatureAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SpiInterface.class)
    public SpiInterface spiInterface(配置属性 props,
                                      @Nullable 可替换组件 component) {
        DefaultSpi impl = new DefaultSpi(props);
        if (component != null) {
            impl.setComponent(component);
        }
        return impl;
    }
}
```

**不要**为每个 AutoConfiguration 发明新模式。参照已有模式保持一致性。

**溯源**：PRD 第 6.6 节"AutoConfiguration 编写规则"

### 模式 4：ErrorCode 重复定义

**常见错误**：创建新 ErrorCode 枚举，而 CommonErrorCode 已有对应码

```java
// 错误 — CommonErrorCode.ENTITY_NOT_FOUND 已存在（11000）
public enum MyErrorCode {
    NOT_FOUND(11000, "资源不存在");  // 与 CommonErrorCode 冲突！
}

// 正确 — 使用 CommonErrorCode
return dataManager.findById(User.class, id)
    .map(Result::success)
    .orElse(Result.fail(CommonErrorCode.ENTITY_NOT_FOUND));
```

**CommonErrorCode 区间速查**（PRD 附录 B）：

| 区间 | 类别 | 示例 |
|------|------|------|
| 10001-10099 | 通用 | PARAM_ERROR(10002) |
| 10100-10199 | 资源 | NOT_FOUND(10100) |
| 10400-10499 | 认证 | UNAUTHORIZED(10400), FORBIDDEN(10403) |
| 11000-11999 | 数据 | ENTITY_NOT_FOUND(11000), OPTIMISTIC_LOCK_ERROR(11007) |
| 19000-19999 | 系统 | SYSTEM_ERROR(19000) |

**规则**：
- 框架错误码范围：10000-19999（CommonErrorCode）
- 业务应用错误码范围：20000+
- 新增前必须搜索 CommonErrorCode

**溯源**：PRD 第 5.1 节"Commons 通用模块"、附录 B

### 模式 5：SPI 默认实现重复创建

**规则**：每个 SPI 必须有 Default + NoOp 实现（PRD 第 1.5 节"质量底线"）

创建新 SPI 前检查：

```bash
# 搜索是否已有 Default 实现
grep -r "DefaultCacheManager\|DefaultSessionStore\|DefaultCircuitBreaker" --include="*.java" .

# 搜索是否已有 NoOp 实现
grep -r "NoOpVectorStore\|NoOp" --include="*.java" .
```

已有 SPI 默认实现（PRD 第 5.6 节）：

| SPI | Default 实现 | NoOp 实现 |
|-----|-------------|-----------|
| ChatClientRegistry | DefaultChatClientRegistry | — |
| ModelRegistry | InMemoryModelRegistry | — |
| VectorStore | — | NoOpVectorStore |
| SessionStore | DefaultSessionStore | — |
| CircuitBreaker | DefaultCircuitBreaker | — |
| Cache | DefaultCache (Caffeine) | — |

**溯源**：PRD 第 1.5 节"质量底线"第 1 条和第 6 条

---

## 跨模块通信模式

### 优先事件，而非直接 Bean 调用

**规则**：模块间通信优先使用 EventPublisher，而非直接 Bean 调用（PRD 第 6.6 节"模块间通信约定"）

```java
// 错误 — 直接调用其他模块的 Bean
@Autowired
private OtherModuleService otherModuleService;  // 跨模块直接依赖

// 正确 — 通过事件通信
domainEventPublisher.publish(new UserCreatedEvent(user));

@DomainEventListener
public void onUserCreated(UserCreatedEvent event) { ... }
```

**好处**：
- 解耦模块间依赖
- 支持分布式事件（引入 RabbitMQ/Kafka 后自动升级）
- 事件定义放在 `api.event` 子包，实现放在功能子包

**溯源**：PRD 第 6.6 节"模块间通信约定"

---

## 何时抽象

**抽象条件**：
- 同一代码出现 3+ 次
- 逻辑复杂到容易产生缺陷
- 多人可能需要此功能
- AFG 框架级别的通用模式

**不要抽象条件**：
- 仅使用一次
- 简单的一行代码
- 抽象比重复更复杂

---

## 批量修改后

对多个文件做了类似修改时：

1. **审查**：是否遗漏了某些实例？
2. **搜索**：运行 grep 确认无遗漏
3. **考虑**：是否应抽象为共享逻辑？
4. **AFG 特有**：修改 AutoConfiguration 后，检查 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 是否同步

---

## 不对称机制产生相同输出的陷阱

**问题**：当两种不同机制必须产生相同的文件集（如 APT 编译时生成 vs ReflectiveMetadataLoader 运行时降级），结构变更（重命名、移动、添加字段）只通过一种机制传播，另一种静默漂移。

**症状**：APT 生成的元数据正确，但运行时反射降级路径使用了旧结构。

**预防清单**：
- [ ] 修改实体字段时，搜索 AptMetadataLoader 和 ReflectiveMetadataLoader 两条路径
- [ ] APT 生成的 Metadata 类和反射降级路径必须保持一致
- [ ] 添加集成测试覆盖反射降级路径

**溯源**：PRD 第 3.2 节"APT 编译时元数据"

---

## 提交前清单

- [ ] 搜索了 CommonErrorCode 是否已有对应错误码
- [ ] 使用了 Lambda Conditions 而非字符串 Conditions
- [ ] 选择了正确的 DataManager 调用方式（快捷 vs 链式）
- [ ] 搜索了已有 Default/NoOp SPI 实现
- [ ] 跨模块通信使用了事件而非直接 Bean 调用
- [ ] 常量定义在一处，其他地方引用
- [ ] 相似模式遵循已有结构
