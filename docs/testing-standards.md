# AFG 框架测试规范

## 核心理念

测试的目的是**建立对代码行为的信心**，不是凑覆盖数字。每个测试都应该有明确的防御价值。

## 四条基本原则

### 1. 不测"语言自己担保的事"

以下内容**禁止**编写测试：

| 禁止的测试类型 | 理由 | 示例 |
|---|---|---|
| 纯 POJO getter/setter | Lombok/Record 已保证 | `User::getName` 返回值等于构造传入值 |
| 异常类构造器 | 只透传 message/cause | `new AfgException("err").getMessage()` |
| 配置属性默认值 | Spring 已保证绑定正确 | `new CacheProperties().getTtl()` |
| Enum 值枚举 | 定义即文档 | `assertThat(Status.values()).hasSize(3)` |

### 2. 一个功能一个测试类

一个功能只有一个测试文件。禁止拆分：

```
✅ 正确：
  JdbcDataManagerTest.java        ← 所有 DataManager 测试集中在这里

❌ 错误：
  JdbcDataManagerTest.java              ← 基础操作
  JdbcDataManagerAdditionalTest.java    ← 边界条件（应该放同一个文件 @Nested）
  JdbcDataManagerCoverageTest.java      ← 覆盖率补充
  JdbcDataManagerIntegrationTest.java   ← 集成验证
```

**用 `@Nested` 组织不同场景**：

```java
class JdbcDataManagerTest {
    @Nested
    class BasicOperations { ... }

    @Nested
    class EdgeCases {
        @Test
        void shouldHandleNullEntity() { ... }
    }

    @Nested
    class Integration {
        @Test
        void shouldWorkWithFullSpringContext() { ... }
    }
}
```

### 3. 优先集成测试，减少 Mock

| 适用场景 | 推荐方式 | 说明 |
|---|---|---|
| DataManager 数据操作 | H2 集成测试 | 连真实数据库验证 SQL 生成和执行 |
| Security 权限校验 | 轻量级 @SpringBootTest | 只加载 security slice |
| AOP 切面（Cache/Lock）| 轻量级 @SpringBootTest | WARM 模式，只加载必要切面和依赖 |
| Controller | @WebMvcTest | Spring MockMvc slice |
| 纯算法/工具类 | 纯单元测试 | 无依赖，直接 new |
| **禁止**纯 Mock 验证框架核心 | — | DataManager/Security 用 Mock 测不出真实问题 |

### 4. 失败测试不留残骸

- **测试失败 = 修复或删除，二选一**
- 不允许有已知失败的 `@Test` 长期存在于代码库中
- 不允许提交含失败测试的 PR
- 临时调试可以用 `@Disabled("原因 + 日期")`，但最多存活 1 个 Sprint

## 命名规范

| 类型 | 命名 | 示例 |
|---|---|---|
| 核心测试 | `{Class}Test` | `JdbcDataManagerTest` |
| 参数化测试 | `{Class}Test`（方法上用 `@ParameterizedTest`） | 同上 |
| 集成测试 | 合并在主测试类中 | 不加 Integration 后缀 |

**不用**的后缀（禁止）：
- `{Class}AdditionalTest`
- `{Class}CoverageTest`
- `{Class}IntegrationTest`
- `{Class}InternalTest`

## 测试内容规范

### DO

- 测核心业务逻辑
- 测边界条件（null、空集合、超大值、非法输入）
- 测异常路径（事务回滚、权限不足、资源不存在）
- 测并发场景（锁争用、幂等性）
- 用 `assertThrows` 验证异常，用 `assertDoesNotThrow` 验证无异常

```java
@Test
void shouldThrowExceptionWhenEntityNotFound() {
    assertThrows(EntityNotFoundException.class,
        () -> dataManager.findById(User.class, -1L));
}
```

### DON'T

- 测 getter/setter/toString/equals/hashCode
- 测构造器参数透传
- 测枚举值数量
- 测 Spring 配置默认值绑定
- 测 IDE 生成的代码

## 技术选型

| 工具 | 用途 |
|---|---|
| JUnit 5 | 测试框架 |
| AssertJ | 断言（流式断言，比 JUnit assert 更可读） |
| Mockito | 仅在非核心模块或不可实例化的外部依赖时使用 |
| H2 | 框架内部数据层集成测试（内存模式，见下方说明） |
| Testcontainers | 中间件集成测试（Redis、RabbitMQ 等）及下游项目 |
| @SpringBootTest | 仅轻量级 slice 加载 |

### H2 vs Testcontainers 使用范围

| 场景 | 数据库策略 | 说明 |
|------|-----------|------|
| 框架内部数据层测试（data-jdbc、data-core） | **H2 允许** | 快速、可移植；验证 SQL 生成和 DataManager 逻辑 |
| 框架内部中间件测试（ai-core、auth-server） | **H2 或 Testcontainers** | ai-core 使用 MySQL Testcontainers；auth-server 可用 H2 |
| 中间件集成测试（Redis、RabbitMQ、Kafka） | **必须 Testcontainers** | H2 无法模拟中间件行为 |
| 下游项目（afg-infra、afg-ai-platform） | **必须 Testcontainers + PostgreSQL** | 生产环境一致性验证 |

**原则**：H2 用于框架内部快速验证 SQL 生成逻辑和 DataManager 行为；Testcontainers 用于需要真实中间件行为或生产环境一致性的场景。
