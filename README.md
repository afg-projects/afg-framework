# AFG Framework

[![Maven Central](https://img.shields.io/maven-central/v/io.github.afg-projects/afg-framework-core.svg?label=Maven%20Central)](https://central.sonatype.com/search?q=g:io.github.afg-projects)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Test Coverage](https://img.shields.io/badge/Test%20Coverage-95%25-brightgreen.svg)](https://github.com/afg-projects/afg-framework/actions)

企业级 Java 开发框架，基于 Spring Boot 4 构建。

## 特性

- **Core** - 核心：缓存管理、事件发布、异常处理、安全防护
- **Data** - 数据访问：轻量级 ORM、SQL 构建器、JDBC 增强、数据库迁移
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
    <artifactId>afg-framework-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Gradle (Kotlin DSL):**

```kotlin
implementation("io.github.afg-projects:afg-framework-spring-boot-starter:1.0.0")
```

### 数据访问示例

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final DataManager dataManager;
    
    // 保存实体
    public User save(User user) {
        return dataManager.entity(User.class).save(user);
    }
    
    // 根据 ID 查询
    public Optional<User> findById(Long id) {
        return dataManager.entity(User.class).findById(id);
    }
    
    // 条件查询（Lambda 风格）
    public List<User> findActiveUsers() {
        return dataManager.entity(User.class)
            .query()
            .where(Conditions.builder(User.class)
                .eq(User::getStatus, "ACTIVE")
                .build())
            .list();
    }
    
    // 分页查询
    public Page<User> listUsers(int page, int size) {
        return dataManager.entity(User.class)
            .query()
            .where(Conditions.builder(User.class)
                .eq(User::getDeleted, false)
                .build())
            .page(PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"))));
    }
}
```

### 实体定义

```java
@Getter
@Setter
public class User extends BaseEntity {
    
    private Long id;
    private String username;
    private String password;
    private String realName;
    private UserStatus status = UserStatus.ACTIVE;
    
    public enum UserStatus {
        ACTIVE, DISABLED, LOCKED
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

# 运行单个模块测试
./gradlew :data-impl:data-jdbc:test
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

## 支持的数据库

- MySQL / OceanBase
- PostgreSQL / GaussDB / OpenGauss / Kingbase
- Oracle
- SQL Server
- H2
- DM (达梦)

## 文档

- [快速开始指南](docs/quick-start.md)
- [数据访问层](docs/data/README.md)
- [核心功能](docs/core/README.md)

## 贡献

欢迎贡献代码！请查看 [贡献指南](CONTRIBUTING.md)。

## 许可证

本项目基于 [Apache License 2.0](LICENSE) 许可证开源。
