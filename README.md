# AFG Framework

[![Maven Central](https://img.shields.io/maven-central/v/io.github.afg-projects/afg-framework-core.svg?label=Maven%20Central)](https://central.sonatype.com/search?q=g:io.github.afg-projects)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Test Coverage](https://img.shields.io/badge/Test%20Coverage-95%25-brightgreen.svg)](https://github.com/afg-projects/afg-framework/actions)

企业级 Java 开发框架，基于 Spring Boot 4 构建。

## 特性

- **Core** - 核心：缓存管理、事件发布、异常处理、安全防护
- **Data** - 数据访问：SQL 构建器、JDBC 增强、数据库迁移
- **Integration** - 中间件集成：Redis、Kafka、RabbitMQ、WebSocket、Storage
- **Spring Boot Starter** - 自动配置

## 技术栈

- Java 25
- Spring Boot 4.0.5
- Gradle 9.4 (Kotlin DSL)

## 快速开始

### 引入依赖

**Maven:**

```xml
<dependency>
    <groupId>io.github.afg-projects</groupId>
    <artifactId>afg-framework-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Gradle (Kotlin DSL):**

```kotlin
implementation("io.github.afg-projects:afg-framework-core:1.0.0")
```

**Gradle (Groovy DSL):**

```groovy
implementation 'io.github.afg-projects:afg-framework-core:1.0.0'
```

### Snapshot 版本

如需使用最新的 Snapshot 版本，添加 Maven Central Snapshot 仓库：

**Maven:**

```xml
<repository>
    <id>sonatype-snapshots</id>
    <url>https://central.sonatype.com/repository/maven-snapshots/</url>
    <snapshots>
        <enabled>true</enabled>
    </snapshots>
</repository>
```

**Gradle (Kotlin DSL):**

```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
    }
}
```

### 从源码构建

```bash
# 克隆仓库
git clone https://github.com/afg-projects/afg-framework.git
cd afg-framework

# 构建
./gradlew build

# 运行测试
./gradlew test
```

## 模块说明

| 模块 | ArtifactId | 说明 |
|------|------------|------|
| core | afg-framework-core | 核心功能：缓存、事件、异常处理、安全防护 |
| data-core | afg-framework-data-core | 数据访问抽象层 |
| data-impl/data-sql | afg-framework-data-sql | SQL 解析与构建器 |
| data-impl/data-jdbc | afg-framework-data-jdbc | JDBC 增强实现 |
| data-impl/data-liquibase | afg-framework-data-liquibase | Liquibase 数据库迁移集成 |
| spring-boot-starter | afg-framework-spring-boot-starter | Spring Boot 自动配置 |
| integration/afg-redis | afg-framework-afg-redis | Redis 集成：缓存、分布式锁、延迟队列 |
| integration/afg-kafka | afg-framework-afg-kafka | Kafka 事件发布集成 |
| integration/afg-rabbitmq | afg-framework-afg-rabbitmq | RabbitMQ 消息队列集成 |
| integration/afg-websocket | afg-framework-afg-websocket | WebSocket 实时通信集成 |
| integration/afg-storage | afg-framework-afg-storage | 文件存储抽象层：本地、OSS、S3 |
| integration/afg-jdbc | afg-framework-afg-jdbc | JDBC 审计日志存储 |
| integration/afg-nacos | afg-framework-afg-nacos | Nacos 配置中心集成 |
| integration/afg-apollo | afg-framework-afg-apollo | Apollo 配置中心集成 |
| integration/afg-consul | afg-framework-afg-consul | Consul 服务发现与配置集成 |

## 文档

- [快速开始指南](docs/quick-start.md)
- [核心功能](docs/core/README.md)
- [数据访问](docs/data/README.md)
- [中间件集成](docs/integration/README.md)

## 贡献

欢迎贡献代码！请查看 [贡献指南](CONTRIBUTING.md)。

## 许可证

本项目基于 [Apache License 2.0](LICENSE) 许可证开源。
