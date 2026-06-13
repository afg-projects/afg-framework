# Phase 1 验证报告：快速开始章节与代码库对比

## 检查结果汇总

| # | 检查点 | 状态 | 问题 |
|---|---------|--------|------|
| 1 | Gradle 插件 ID | ❌ 不匹配 | PRD 写 `io.github.afg-projects.afg-framework`，实际是 `io.github.afg-projects.framework-plugin` |
| 2 | afg 扩展块 | ✅ 匹配 | 8 个属性名称一致；PRD 缺少 `springAiVersion` |
| 3 | @AfEntity + @Table + @Column | ⚠️ 部分不匹配 | @AfEntity 正确；@Table/@Column 来自 `jakarta.persistence`，PRD 应注明；AfEntity Javadoc 引用了不存在的框架注解 |
| 4 | DataManager API | ⚠️ 部分不匹配 | 快捷方法需要实体类作为首参；`page()` 直接返回 `PageData<T>`，后面不接 `.list()` |
| 5 | SoftDeleteEntity | ✅ 匹配 | 类名、包路径、继承链正确 |
| 6 | Conditions.builder | ✅ 匹配 | API 模式 `Conditions.builder(Class).likeIfPresent().eq().build()` 正确 |
| 7 | Result / PageData | ⚠️ 部分不匹配 | `Result.fail()` 无参重载不存在于 `Result` 中，仅存在于 `Results` 辅助类 |
| 8 | @AiChat | ✅ 匹配 | `client`、`systemPrompt` 正确；3 个额外属性未列出（`memoryKey`、`temperature`、`maxTokens`） |
| 9 | 应用配置 | ❌ 不匹配 | `signing-key` 不存在；实际属性是 `key-store-path`；AfgInitTask 生成了无法绑定的配置 |

---

## 1. Gradle 插件 ID

**PRD：** `id("io.github.afg-projects.afg-framework") version "1.0.0-SNAPSHOT"`
**实际：** `id("io.github.afg-projects.framework-plugin") version "1.0.0-SNAPSHOT"`

来源：`gradle-plugin/build.gradle.kts` 第 23 行

## 2. afg 扩展块

PRD 列出的 8 个属性全部存在且名称一致。额外属性 `springAiVersion`（默认 `2.0.0-M7`）PRD 中未列出。

## 3. 实体注解

- `@AfEntity`：包路径 `io.github.afgprojects.framework.apt.entity`，正确
- `@Table` / `@Column`：来自 `jakarta.persistence`，不是框架自定义注解
- AfEntity 的 Javadoc 引用了 `io.github.afgprojects.framework.data.core.annotation.Table/Column`，但这些类不存在

## 4. DataManager API

关键差异：
- 快捷方法签名是 `save(Class<T>, T)`，不是 `save(T)`
- `page(PageRequest)` 直接返回 `PageData<T>`，不接 `.list()`
- Controller 示例中 `dataManager.save(User.class, user)` 正确，但 PRD 2.5 节示例写法正确

## 7. Result / PageData

两个类并存：
- `Result`（commons）：基础记录类，`success(data)` 和 `fail(code, message)` 正确，无 `fail()` 无参重载
- `Results`（core）：增强辅助类，自动填充 traceId，有 `fail()` 无参重载

PRD 示例用的是 `Result.fail(CommonErrorCode.NOT_FOUND)`，这在 Result 类中有对应方法 `Result.fail(ErrorCode)`，是正确的。

## 9. 应用配置

- `afg.security.auth-server.enabled` ✅ 正确
- `afg.security.auth-server.token.signing-key` ❌ 不存在
- 实际属性是 `afg.security.auth-server.token.key-store-path`（RSA 密钥对路径）
- AfgInitTask 生成了 `signing-key` YAML，但 TokenConfig 没有对应属性，配置无法绑定

---

## 需要修复的 PRD 内容

1. 修正插件 ID：`io.github.afg-projects.framework-plugin`
2. 补充 `springAiVersion` 属性
3. 注明 `@Table/@Column` 来自 `jakarta.persistence`
4. 确认 DataManager `page()` 返回 `PageData<T>`，不接 `.list()`
5. 修正安全配置：`signing-key` → `key-store-path`
6. 说明 Result vs Results 的区别和使用推荐
