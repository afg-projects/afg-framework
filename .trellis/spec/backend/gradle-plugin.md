# Gradle 插件规格

> PRD 来源：§5.9 Gradle 插件
> CLAUDE.md 来源：Gradle 插件章节

## 1. 定位

项目脚手架和代码生成工具——afgInit 生成项目骨架，generateMigration 生成迁移脚本。

## 2. 插件标识

- **Plugin ID**：`io.github.afg-projects.framework-plugin`
- **包路径**：`io.github.afgprojects.framework.core.gradle`

## 3. 扩展配置

通过 `afg {}` 扩展块配置：

```kotlin
afg {
    springBootVersion.set("4.0.6")     // 默认 4.0.6
    springAiVersion.set("2.0.0-M7")    // 默认 2.0.0-M7
    frameworkVersion.set("1.0.0-SNAPSHOT")
    standalone.set(true)               // true=独立部署(Spring Boot jar), false=聚合模块(plain jar)
    useLombok.set(true)                // 自动添加 Lombok 依赖
    enableCodegen.set(true)            // 启用 APT 代码生成
    basePackage.set("com.example")     // 基础包路径
    securityMode.set("MONOLITH")       // AUTH_SERVER / RESOURCE_SERVER / MONOLITH / null
    databaseType.set("mysql")          // 数据库类型
}
```

### 3.1 配置属性说明

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `springBootVersion` | String | `"4.0.6"` | Spring Boot 版本，控制 BOM 依赖管理 |
| `springAiVersion` | String | `"2.0.0-M7"` | Spring AI 版本 |
| `frameworkVersion` | String | `"1.0.0-SNAPSHOT"` | AFG 框架版本，所有模块使用统一版本 |
| `standalone` | Boolean | `true` | `true` = 独立部署（Spring Boot 可执行 jar），`false` = 聚合模块（plain jar） |
| `useLombok` | Boolean | `true` | 自动添加 Lombok 依赖 |
| `enableCodegen` | Boolean | `true` | 启用 APT 代码生成（配置 annotationProcessor） |
| `basePackage` | String | `"com.example"` | 基础包路径，影响代码生成输出目录 |
| `securityMode` | String | `"MONOLITH"` | 安全模式：`"AUTH_SERVER"` / `"RESOURCE_SERVER"` / `"MONOLITH"` / `null` |
| `databaseType` | String | `"mysql"` | 数据库类型，影响迁移脚本生成和代码生成 |

### 3.2 securityMode 说明

`securityMode` 仅影响 `afgInit` 任务生成的初始代码，**不影响运行时**：

| securityMode 值 | afgInit 生成内容 |
|-----------------|-----------------|
| `"AUTH_SERVER"` | 生成独立认证服务配置（含 auth-server 依赖、安全配置类、AfgUserDetailsService 实现） |
| `"RESOURCE_SERVER"` | 生成资源服务配置（含 resource-server 依赖、JWT 验证配置） |
| `"MONOLITH"` | 生成聚合部署配置（同时包含 auth-server 和 resource-server 依赖） |
| `null` | 不生成安全相关代码 |

运行时的部署模式由 Spring 配置属性 `afg.security.auth-server.enabled` 和 `afg.security.resource-server.enabled` 控制，与 `securityMode` 无关。

## 4. 模块版本策略

所有框架模块使用统一版本号（`frameworkVersion`），不单独版本化：

- `afg-framework-core`
- `afg-framework-apt-api`
- `afg-framework-apt-impl`
- `afg-framework-data-core`
- `afg-framework-data-jdbc`
- `afg-framework-ai-core`
- ... 其他模块

## 5. 注册的任务

| 任务 | 说明 |
|------|------|
| `afgInfo` | 显示框架配置信息（版本、securityMode、databaseType 等） |
| `afgInit` | 生成项目脚手架代码（Application.java + application.yml + AfgUserDetailsService + 目录结构 + Liquibase 迁移 + README.md + Dockerfile + .gitignore） |
| `generateMigration` | 从实体生成 Liquibase changeSet |
| `generateEntity` | 生成实体类 |
| `generateEntityFromDb` | 从数据库反向生成实体 |
| `dbMigrate` | 执行数据库迁移 |
| `generateDbDoc` | 从实体生成 Markdown/HTML 数据库文档 |

### 5.1 afgInit 生成内容

`afgInit` 根据 `securityMode` 生成不同的安全配置代码，具体包括：

- `Application.java` —— Spring Boot 启动类
- `application.yml` —— 应用配置（含框架默认配置）
- `AfgUserDetailsService` 实现 —— 包含 DataManager 使用示例
- 目录结构 —— controller / service / entity / repository 等标准包
- Liquibase 迁移 —— 主 changelog 文件和目录结构
- `README.md` —— 项目说明
- `Dockerfile` —— 容器化部署
- `.gitignore` —— Git 忽略规则

### 5.2 代码生成器 SPI

代码生成器支持 SPI 扩展，可注册自定义 Generator。

## 6. 自动行为

插件应用后自动执行以下行为：

1. **应用 `java-library` 插件** —— 提供Java库开发能力（`api` / `implementation` 依赖区分）
2. **配置 Spring Boot BOM 依赖管理** —— 根据 `springBootVersion` 管理所有 Spring Boot 依赖版本
3. **自动添加框架核心依赖**：
   - `afg-framework-core`
   - `afg-framework-apt-impl`
   - `afg-framework-apt-api`
4. **配置 APT 依赖** —— 当 `enableCodegen = true` 时，配置 annotationProcessor
5. **配置 Lombok** —— 当 `useLombok = true` 时，自动添加 Lombok 依赖和 annotationProcessor
6. **配置 JaCoCo** —— 版本 0.8.14，代码覆盖率
7. **配置 PMD** —— 版本 7.23.0，静态代码检查
8. **Spring Boot 插件** —— 当 `standalone = true` 时，应用 Spring Boot 插件生成可执行 jar

## 7. Standalone vs 聚合部署

| 模式 | standalone 值 | 构建产物 | 说明 |
|------|-------------|---------|------|
| 独立部署 | `true` | Spring Boot 可执行 jar | 包含嵌入容器，可直接 `java -jar` 运行 |
| 聚合模块 | `false` | plain jar | 作为多模块项目的子模块，被其他模块依赖 |

聚合模块（无 main class 的中间模块）需额外配置：

```kotlin
bootJar.enabled = false
jar.enabled = false
```

## 8. 使用示例

```kotlin
// build.gradle.kts
plugins {
    id("io.github.afg-projects.afg-framework") version "1.0.0-SNAPSHOT"
}

afg {
    springBootVersion.set("4.0.6")
    frameworkVersion.set("1.0.0-SNAPSHOT")
    standalone.set(true)
    useLombok.set(true)
    enableCodegen.set(true)
    basePackage.set("com.example")
    securityMode.set("MONOLITH")
    databaseType.set("mysql")
}
```

```bash
./gradlew afgInfo              # 显示框架配置信息
./gradlew afgInit              # 生成项目脚手架代码
./gradlew generateMigration    # 从实体生成迁移脚本
./gradlew generateEntity       # 生成实体类
./gradlew generateEntityFromDb # 从数据库反向生成实体
./gradlew dbMigrate            # 执行数据库迁移
./gradlew generateDbDoc        # 生成数据库文档
```
