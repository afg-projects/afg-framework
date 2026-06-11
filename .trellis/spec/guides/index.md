# AFG Framework 思维指南

> **目的**：帮助开发者和 AI 助手正确思考 AFG 框架特有的关注点，避免"没想到这个"导致的缺陷和技术债。
> **权威来源**：[docs/framework-prd.md](../../docs/framework-prd.md) 是框架产品需求的唯一权威定义。本目录所有指南均溯源至 PRD。

---

## 为什么需要思维指南？

**大多数缺陷和技术债源于"没想到这个"，而非技能不足**：

- 没想到 AutoConfiguration 排序问题 → Bean 创建顺序不确定
- 没想到 Conditions 构建器已有 Lambda 方式 → 写出字符串条件查询
- 没想到 CommonErrorCode 已有对应错误码 → 创建重复错误码
- 没想到软删除在 JDBC 层自动过滤 → 在实体层重复过滤
- 没想到模块 context-path 影响 Security 路径匹配 → 403 误报

这些指南帮助你在编码前**提出正确的问题**。

---

## 可用指南

| 指南 | 目的 | 何时使用 |
|------|------|----------|
| [代码复用思维指南](./code-reuse-thinking-guide.md) | 识别 AFG 特有重复模式，减少重复代码 | 编写 Conditions 查询、DataManager 操作、AutoConfiguration、ErrorCode 时 |
| [跨层思维指南](./cross-layer-thinking-guide.md) | 理解 AFG 特有层边界和数据流 | 功能涉及 Controller→DataManager→JDBC→Database、AI 调用链、安全校验链时 |
| [AutoConfiguration 思维指南](./autoconfiguration-thinking-guide.md) | 正确编写和审查 AFG AutoConfiguration | 新增或修改 AutoConfiguration、编写 SPI 默认实现、跨模块依赖排序时 |

---

## 快速参考：AFG 特有触发条件

### 何时思考跨层问题

- [ ] 功能涉及 Controller → DataManager → JDBC → Database 链路
- [ ] 使用 AI 注解（@AiChat / @AiAgent / @Workflow）— 涉及声明式→AOP→SPI→引擎多层
- [ ] 安全相关功能 — 涉及 @RequirePermission → AfgEnforcer → Casbin → JDBC 策略存储
- [ ] 分布式能力 — 涉及 @Lock → DistributedLock SPI → Redis/内存实现
- [ ] 多租户/数据权限 — 涉及 SQL 重写层自动注入条件
- [ ] 不确定逻辑应放在哪一层（实体层 vs JDBC 层 vs SQL 重写层）

→ 阅读 [跨层思维指南](./cross-layer-thinking-guide.md)

### 何时思考代码复用

- [ ] 编写 Conditions.builder() 查询 — Lambda 方式是否已存在？
- [ ] 使用 DataManager 操作 — 快捷方法 vs EntityProxy 链式？
- [ ] 创建新 ErrorCode — CommonErrorCode 是否已有对应码？
- [ ] 编写 AutoConfiguration — 是否有类似模式可参照？
- [ ] 创建 SPI 默认实现 — Default/NoOp 实现是否已存在？
- [ ] 跨模块通信 — 是否应使用事件而非直接 Bean 调用？

→ 阅读 [代码复用思维指南](./code-reuse-thinking-guide.md)

### 何时思考 AutoConfiguration 问题

- [ ] 新增 AutoConfiguration 类
- [ ] 修改 AutoConfiguration 的 Bean 定义或依赖
- [ ] 跨模块引用其他模块的 AutoConfiguration
- [ ] 编写测试使用 @ImportAutoConfiguration
- [ ] SPI 接口需要 Default + NoOp 实现
- [ ] Bean 创建顺序问题或启动报错

→ 阅读 [AutoConfiguration 思维指南](./autoconfiguration-thinking-guide.md)

---

## 修改前规则（关键）

> **修改任何值之前，必须先搜索！**

```bash
# 搜索即将修改的值
grep -r "value_to_change" .

# AFG 特有：搜索 CommonErrorCode 是否已有对应错误码
grep -r "NOT_FOUND\|ENTITY_NOT_FOUND" commons/src/main/java/

# AFG 特有：搜索是否已有类似 AutoConfiguration
grep -r "AutoConfiguration" --include="*.java" . | grep -i "keyword"

# AFG 特有：搜索 SPI 默认实现
grep -r "Default\|NoOp" --include="*.java" . | grep -i "SpiName"
```

这一习惯能防止大多数"忘了更新 X"的缺陷。

---

## 如何使用本目录

1. **编码前**：快速浏览相关思维指南
2. **编码中**：如果感觉有重复或复杂性，查阅指南
3. **缺陷修复后**：将新认知添加到相关指南（从错误中学习）

---

## 贡献

发现新的"没想到这个"时刻？将其添加到相关指南中。特别是：
- 同类问题出现 2 次及以上时，必须总结到 CLAUDE.md 的"常见问题与踩坑记录"章节
- 框架级别的模式问题，同步更新到对应的思维指南

---

**核心原则**：30 分钟的思考节省 3 小时的调试。

**溯源声明**：本目录所有指南内容均溯源至 [docs/framework-prd.md](../../docs/framework-prd.md) 中定义的产品需求和设计哲学。
